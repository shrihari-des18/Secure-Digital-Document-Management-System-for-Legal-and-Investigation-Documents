package com.shrihari.evault.service.tsa;

import com.shrihari.evault.dto.TimestampTokenDto;
import com.shrihari.evault.dto.TimestampVerificationResponse;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.nist.NISTObjectIdentifiers;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.ExtendedKeyUsage;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.cms.SignerInformationVerifier;
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.DigestCalculator;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.bouncycastle.asn1.cmp.PKIStatus;
import org.bouncycastle.tsp.TimeStampRequest;
import org.bouncycastle.tsp.TimeStampRequestGenerator;
import org.bouncycastle.tsp.TimeStampResponse;
import org.bouncycastle.tsp.TimeStampToken;
import org.bouncycastle.tsp.TimeStampTokenGenerator;
import org.bouncycastle.tsp.TimeStampTokenInfo;
import org.bouncycastle.util.Store;
import org.bouncycastle.util.encoders.Hex;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.Base64;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@Service
@Slf4j
public class Rfc3161TimestampService {

    @Value("${evault.tsa.url:https://freetsa.org/tsr}")
    private String tsaUrl;

    @Value("${evault.tsa.mode:AUTO}")
    private String tsaMode; // REMOTE, LOCAL, AUTO

    @Value("${evault.tsa.timeout-ms:5000}")
    private int timeoutMs;

    private static final String BC_PROVIDER = "BC";
    private static final ASN1ObjectIdentifier DEFAULT_TSA_POLICY_OID = new ASN1ObjectIdentifier("1.3.6.1.4.1.61234.1.1");
    private static final String LOCAL_TSA_NAME = "E-Vault Root Timestamp Authority (RFC 3161 / BSA 2023 §63)";

    // In-memory fallback TSA keypair and certificate for reliable local generation and testing
    private KeyPair localTsaKeyPair;
    private X509Certificate localTsaCertificate;
    private TimeStampTokenGenerator localTokenGenerator;
    private final AtomicLong localSerialCounter = new AtomicLong(System.currentTimeMillis());

    @PostConstruct
    public void init() {
        if (Security.getProvider(BC_PROVIDER) == null) {
            Security.addProvider(new BouncyCastleProvider());
            log.info("BouncyCastle Security Provider registered for RFC 3161 TSA");
        }
        initLocalTsaEngine();
    }

    private synchronized void initLocalTsaEngine() {
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA", BC_PROVIDER);
            keyGen.initialize(2048, new SecureRandom());
            localTsaKeyPair = keyGen.generateKeyPair();

            X500Name issuer = new X500Name("CN=" + LOCAL_TSA_NAME + ", O=E-Vault Forensic Judiciary, C=IN");
            BigInteger serial = BigInteger.valueOf(System.currentTimeMillis());
            Date notBefore = new Date(System.currentTimeMillis() - 86400000L);
            Date notAfter = new Date(System.currentTimeMillis() + 365L * 86400000L);

            JcaX509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
                    issuer, serial, notBefore, notAfter, issuer, localTsaKeyPair.getPublic()
            );

            // RFC 3161 requirement: ExtendedKeyUsage id_kp_timeStamping (1.3.6.1.5.5.7.3.8)
            certBuilder.addExtension(Extension.extendedKeyUsage, true, new ExtendedKeyUsage(KeyPurposeId.id_kp_timeStamping));

            ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA")
                    .setProvider(BC_PROVIDER)
                    .build(localTsaKeyPair.getPrivate());

            X509CertificateHolder certHolder = certBuilder.build(signer);
            localTsaCertificate = new JcaX509CertificateConverter()
                    .setProvider(BC_PROVIDER)
                    .getCertificate(certHolder);

            DigestCalculator calc = new JcaDigestCalculatorProviderBuilder()
                    .setProvider(BC_PROVIDER)
                    .build()
                    .get(new AlgorithmIdentifier(NISTObjectIdentifiers.id_sha256));

            var signerInfoGen = new JcaSignerInfoGeneratorBuilder(
                    new JcaDigestCalculatorProviderBuilder().setProvider(BC_PROVIDER).build()
            ).build(signer, localTsaCertificate);

            localTokenGenerator = new TimeStampTokenGenerator(signerInfoGen, calc, DEFAULT_TSA_POLICY_OID);
            localTokenGenerator.addCertificates(new JcaCertStore(List.of(certHolder)));

            log.info("Local RFC 3161 TSA Engine successfully initialized with RSA-2048 key and X.509 certificate");
        } catch (Exception e) {
            log.error("Failed to initialize local RFC 3161 TSA Engine", e);
        }
    }

    /**
     * Request a certified RFC 3161 timestamp token for a SHA-256 hash.
     */
    public TimestampTokenDto timestampHash(String sha256Hex, String description) {
        if (sha256Hex == null || sha256Hex.isBlank()) {
            throw new IllegalArgumentException("SHA-256 hash must not be null or blank");
        }

        byte[] digestBytes;
        try {
            digestBytes = Hex.decode(sha256Hex.trim().toLowerCase());
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid SHA-256 hex string: " + e.getMessage(), e);
        }
        if (digestBytes.length != 32) {
            throw new IllegalArgumentException("Invalid SHA-256 digest length: expected 32 bytes");
        }

        if ("LOCAL".equalsIgnoreCase(tsaMode)) {
            return generateLocalTimestamp(digestBytes, sha256Hex, description);
        }

        // Try REMOTE first if REMOTE or AUTO
        try {
            return requestRemoteTimestamp(digestBytes, sha256Hex, description);
        } catch (Exception e) {
            if ("REMOTE".equalsIgnoreCase(tsaMode)) {
                log.error("Remote TSA request failed in strict REMOTE mode: {}", e.getMessage());
                throw new IllegalStateException("Remote RFC 3161 TSA server unreachable: " + e.getMessage(), e);
            }
            log.warn("Remote TSA at {} failed ({}), falling back to local RFC 3161 cryptographic TSA", tsaUrl, e.getMessage());
            return generateLocalTimestamp(digestBytes, sha256Hex, description);
        }
    }

    /**
     * Timestamp arbitrary payload bytes by computing SHA-256 and stamping it.
     */
    public TimestampTokenDto timestampBytes(byte[] payload, String description) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(payload);
            String hex = Hex.toHexString(digest);
            return timestampHash(hex, description);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to calculate SHA-256 for payload timestamping", e);
        }
    }

    private TimestampTokenDto requestRemoteTimestamp(byte[] digestBytes, String sha256Hex, String description) throws Exception {
        log.info("Requesting remote RFC 3161 timestamp from {} for hash {}", tsaUrl, sha256Hex);

        TimeStampRequestGenerator reqGen = new TimeStampRequestGenerator();
        reqGen.setCertReq(true);
        BigInteger nonce = BigInteger.valueOf(System.currentTimeMillis() + new SecureRandom().nextLong());
        TimeStampRequest request = reqGen.generate(NISTObjectIdentifiers.id_sha256, digestBytes, nonce);

        byte[] requestDer = request.getEncoded();

        URL endpoint = URI.create(tsaUrl).toURL();
        HttpURLConnection conn = (HttpURLConnection) endpoint.openConnection();
        conn.setDoOutput(true);
        conn.setDoInput(true);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/timestamp-query");
        conn.setRequestProperty("Accept", "application/timestamp-reply");
        conn.setConnectTimeout(timeoutMs);
        conn.setReadTimeout(timeoutMs);

        try (OutputStream out = conn.getOutputStream()) {
            out.write(requestDer);
            out.flush();
        }

        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            throw new IllegalStateException("Remote TSA returned HTTP status " + responseCode);
        }

        byte[] responseDer;
        try (InputStream in = conn.getInputStream()) {
            responseDer = in.readAllBytes();
        }

        TimeStampResponse tsResponse = new TimeStampResponse(responseDer);
        tsResponse.validate(request);

        if (tsResponse.getStatus() != PKIStatus.GRANTED && tsResponse.getStatus() != PKIStatus.GRANTED_WITH_MODS) {
            throw new IllegalStateException("Remote TSA rejected timestamp request with status " + tsResponse.getStatus() + ": " + tsResponse.getStatusString());
        }

        TimeStampToken token = tsResponse.getTimeStampToken();
        return parseAndBuildTokenDto(token, tsaUrl, sha256Hex);
    }

    private TimestampTokenDto generateLocalTimestamp(byte[] digestBytes, String sha256Hex, String description) {
        try {
            if (localTokenGenerator == null) {
                initLocalTsaEngine();
            }

            TimeStampRequestGenerator reqGen = new TimeStampRequestGenerator();
            reqGen.setCertReq(true);
            BigInteger nonce = BigInteger.valueOf(System.currentTimeMillis() + new SecureRandom().nextLong());
            TimeStampRequest request = reqGen.generate(NISTObjectIdentifiers.id_sha256, digestBytes, nonce);

            BigInteger serial = BigInteger.valueOf(localSerialCounter.incrementAndGet());
            TimeStampToken token = localTokenGenerator.generate(request, serial, new Date());

            return parseAndBuildTokenDto(token, LOCAL_TSA_NAME, sha256Hex);
        } catch (Exception e) {
            log.error("Failed to generate local RFC 3161 timestamp", e);
            throw new IllegalStateException("Failed to generate local RFC 3161 timestamp token", e);
        }
    }

    private TimestampTokenDto parseAndBuildTokenDto(TimeStampToken token, String authorityName, String sha256Hex) throws Exception {
        TimeStampTokenInfo info = token.getTimeStampInfo();
        byte[] tokenDer = token.getEncoded();
        String tokenBase64 = Base64.getEncoder().encodeToString(tokenDer);

        // Verify newly minted token
        TimestampVerificationResponse ver = verifyTimestampToken(tokenDer, sha256Hex);

        return TimestampTokenDto.builder()
                .tsaAuthority(authorityName)
                .genTime(info.getGenTime().toInstant())
                .serialNumber(info.getSerialNumber().toString(16).toUpperCase())
                .hashAlgorithm("SHA-256")
                .hashedMessage(sha256Hex.toLowerCase())
                .tokenBase64(tokenBase64)
                .policyOid(info.getPolicy() != null ? info.getPolicy().getId() : null)
                .verified(ver.isValid())
                .build();
    }

    /**
     * Verify an RFC 3161 TimeStampToken against an expected SHA-256 hex string.
     */
    public TimestampVerificationResponse verifyTimestampToken(String tokenBase64, String expectedSha256Hex) {
        if (tokenBase64 == null || tokenBase64.isBlank()) {
            return TimestampVerificationResponse.builder()
                    .valid(false)
                    .hashMatches(false)
                    .signatureVerified(false)
                    .message("Token Base64 is missing or empty")
                    .build();
        }
        try {
            byte[] tokenBytes = Base64.getDecoder().decode(tokenBase64.trim());
            return verifyTimestampToken(tokenBytes, expectedSha256Hex);
        } catch (Exception e) {
            log.warn("Failed to decode token Base64: {}", e.getMessage());
            return TimestampVerificationResponse.builder()
                    .valid(false)
                    .hashMatches(false)
                    .signatureVerified(false)
                    .message("Invalid Base64 token encoding: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Verify DER-encoded TimeStampToken against expected SHA-256 hex digest.
     */
    public TimestampVerificationResponse verifyTimestampToken(byte[] tokenBytes, String expectedSha256Hex) {
        try {
            TimeStampToken token = new TimeStampToken(new org.bouncycastle.cms.CMSSignedData(tokenBytes));
            TimeStampTokenInfo info = token.getTimeStampInfo();

            // 1. Verify Message Imprint Digest
            byte[] stampedDigest = info.getMessageImprintDigest();
            String stampedHex = Hex.toHexString(stampedDigest);
            boolean hashMatches = stampedHex.equalsIgnoreCase(expectedSha256Hex.trim());

            // 2. Extract Signer Certificate and Validate Signature
            Store<X509CertificateHolder> certs = token.getCertificates();
            Collection<X509CertificateHolder> matches = certs.getMatches(token.getSID());

            boolean signatureVerified = false;
            String tsaSubject = "Unknown TSA";

            if (!matches.isEmpty()) {
                X509CertificateHolder certHolder = matches.iterator().next();
                tsaSubject = certHolder.getSubject().toString();
                SignerInformationVerifier verifier = new JcaSimpleSignerInfoVerifierBuilder()
                        .setProvider(BC_PROVIDER)
                        .build(certHolder);

                token.validate(verifier);
                signatureVerified = true;
            } else {
                // If certificates not bundled in token, check if signed by local TSA cert
                if (localTsaCertificate != null) {
                    try {
                        SignerInformationVerifier verifier = new JcaSimpleSignerInfoVerifierBuilder()
                                .setProvider(BC_PROVIDER)
                                .build(localTsaCertificate);
                        token.validate(verifier);
                        signatureVerified = true;
                        tsaSubject = LOCAL_TSA_NAME;
                    } catch (Exception ignored) {
                    }
                }
            }

            boolean valid = hashMatches && signatureVerified;
            String message = valid
                    ? "RFC 3161 timestamp token cryptographically valid: message digest matches and TSA signature verified."
                    : (!hashMatches
                    ? "INTEGRITY MISMATCH: Token message imprint does not match expected SHA-256."
                    : "Cryptographic signature verification failed.");

            return TimestampVerificationResponse.builder()
                    .valid(valid)
                    .hashMatches(hashMatches)
                    .signatureVerified(signatureVerified)
                    .genTime(info.getGenTime().toInstant())
                    .tsaAuthority(tsaSubject)
                    .serialNumber(info.getSerialNumber().toString(16).toUpperCase())
                    .message(message)
                    .build();

        } catch (Exception e) {
            log.error("Failed to verify RFC 3161 TimeStampToken: {}", e.getMessage());
            return TimestampVerificationResponse.builder()
                    .valid(false)
                    .hashMatches(false)
                    .signatureVerified(false)
                    .message("Token verification error: " + e.getMessage())
                    .build();
        }
    }

    public String getTsaUrl() {
        return tsaUrl;
    }

    public String getTsaMode() {
        return tsaMode;
    }
}
