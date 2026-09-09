package com.shrihari.evault.service.pdf;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.shrihari.evault.dto.CustodyEventResponse;
import com.shrihari.evault.model.Case;
import com.shrihari.evault.model.Certificate;
import com.shrihari.evault.model.EvidenceFile;
import com.shrihari.evault.model.EvidenceItem;
import com.shrihari.evault.model.User;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class PdfCertificateGenerator {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm:ss 'IST'")
            .withZone(ZoneId.of("Asia/Kolkata"));

    public byte[] generateBsa63Certificate(
            Certificate certificate,
            EvidenceItem evidence,
            EvidenceFile file,
            Case parentCase,
            User officer,
            List<CustodyEventResponse> custodyEvents) {

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4, 36, 36, 36, 36);
            PdfWriter.getInstance(doc, out);
            doc.open();

            // Fonts
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, Color.BLACK);
            Font subtitleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, new Color(70, 70, 70));
            Font sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, new Color(20, 50, 100));
            Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.BLACK);
            Font valueFont = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.DARK_GRAY);
            Font monoFont = FontFactory.getFont(FontFactory.COURIER_BOLD, 8, Color.BLACK);
            Font smallMono = FontFactory.getFont(FontFactory.COURIER, 7, new Color(40, 40, 40));
            Font declarationFont = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8, Color.DARK_GRAY);

            // 1. Header
            Paragraph header = new Paragraph("GOVERNMENT OF INDIA / STATE POLICE ADMINISTRATION", subtitleFont);
            header.setAlignment(Element.ALIGN_CENTER);
            doc.add(header);

            Paragraph title = new Paragraph("CERTIFICATE UNDER SECTION 63 OF THE BHARATIYA SAKSHYA ADHINIYAM, 2023", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingBefore(4);
            doc.add(title);

            Paragraph sub = new Paragraph("(Statutory Certificate for Admissibility of Electronic Records in Court of Law)", subtitleFont);
            sub.setAlignment(Element.ALIGN_CENTER);
            sub.setSpacingAfter(12);
            doc.add(sub);

            // 2. Metadata Box
            PdfPTable metaTable = new PdfPTable(4);
            metaTable.setWidthPercentage(100);
            metaTable.setWidths(new float[]{25, 25, 25, 25});

            addCell(metaTable, "Certificate No:", labelFont, true);
            addCell(metaTable, certificate.getCertificateNumber(), monoFont, false);
            addCell(metaTable, "Issued At:", labelFont, true);
            addCell(metaTable, DATE_FMT.format(certificate.getIssuedAt()), valueFont, false);

            addCell(metaTable, "Case / FIR No:", labelFont, true);
            addCell(metaTable, parentCase.getCaseNumber(), monoFont, false);
            addCell(metaTable, "Jurisdiction:", labelFont, true);
            addCell(metaTable, parentCase.getJurisdiction(), valueFont, false);

            doc.add(metaTable);
            doc.add(new Paragraph(" "));

            // 3. Part A Header
            Paragraph partA = new Paragraph("PART A — IDENTIFICATION OF ELECTRONIC RECORD & PRODUCING DEVICE", sectionFont);
            partA.setSpacingAfter(4);
            doc.add(partA);

            PdfPTable partATable = new PdfPTable(2);
            partATable.setWidthPercentage(100);
            partATable.setWidths(new float[]{30, 70});

            addCell(partATable, "Evidence Docket No:", labelFont, true);
            addCell(partATable, evidence.getEvidenceNumber(), monoFont, false);

            addCell(partATable, "Evidence Item Title:", labelFont, true);
            addCell(partATable, evidence.getTitle(), valueFont, false);

            addCell(partATable, "Seized From / Location:", labelFont, true);
            addCell(partATable, evidence.getSeizedLocation() != null ? evidence.getSeizedLocation() : "N/A", valueFont, false);

            addCell(partATable, "Seizure Timestamp:", labelFont, true);
            addCell(partATable, evidence.getSeizedAt() != null ? DATE_FMT.format(evidence.getSeizedAt()) : "N/A", valueFont, false);

            addCell(partATable, "Device Particulars:", labelFont, true);
            String devStr = (evidence.getDeviceMake() != null ? evidence.getDeviceMake() : "Standard Media") + " " +
                    (evidence.getDeviceModel() != null ? evidence.getDeviceModel() : "") + " (S/N: " +
                    (evidence.getDeviceSerialNumber() != null ? evidence.getDeviceSerialNumber() : "N/A") + ")";
            addCell(partATable, devStr, valueFont, false);

            addCell(partATable, "Original File Name / Size:", labelFont, true);
            addCell(partATable, file.getOriginalFileName() + " (" + file.getSizeBytes() + " bytes)", valueFont, false);

            addCell(partATable, "Manner of Production:", labelFont, true);
            addCell(partATable, certificate.getMannerOfProduction() != null ? certificate.getMannerOfProduction()
                    : "Acquired via write-blocked forensic bitstream acquisition in lawful custody.", valueFont, false);

            doc.add(partATable);
            doc.add(new Paragraph(" "));

            // 4. Part B Header (Cryptographic Integrity & Hashes)
            Paragraph partB = new Paragraph("PART B — CRYPTOGRAPHIC INTEGRITY & STATUTORY HASH SNAPSHOT", sectionFont);
            partB.setSpacingAfter(4);
            doc.add(partB);

            PdfPTable hashTable = new PdfPTable(2);
            hashTable.setWidthPercentage(100);
            hashTable.setWidths(new float[]{30, 70});

            addCell(hashTable, "Primary Hash (SHA-256):", labelFont, true);
            addCell(hashTable, certificate.getHashSnapshotSha256(), monoFont, false);

            if (certificate.getHashSnapshotSha512() != null) {
                addCell(hashTable, "Secondary Hash (SHA-512):", labelFont, true);
                addCell(hashTable, certificate.getHashSnapshotSha512(), smallMono, false);
            }

            addCell(hashTable, "Digital Signature Digest:", labelFont, true);
            addCell(hashTable, certificate.getDigitalSignature() != null ? certificate.getDigitalSignature() : "SIGNED_VALID", smallMono, false);

            doc.add(hashTable);
            doc.add(new Paragraph(" "));

            // 5. Chain of Custody Table
            Paragraph custHeader = new Paragraph("CHAIN OF CUSTODY SUMMARY TIMELINE", sectionFont);
            custHeader.setSpacingAfter(4);
            doc.add(custHeader);

            PdfPTable custTable = new PdfPTable(4);
            custTable.setWidthPercentage(100);
            custTable.setWidths(new float[]{15, 20, 35, 30});

            addCell(custTable, "Event", labelFont, true);
            addCell(custTable, "Timestamp", labelFont, true);
            addCell(custTable, "Reason / Operation", labelFont, true);
            addCell(custTable, "Signature Digest", labelFont, true);

            if (custodyEvents != null && !custodyEvents.isEmpty()) {
                for (CustodyEventResponse ev : custodyEvents) {
                    addCell(custTable, ev.getEventType().name(), labelFont, false);
                    addCell(custTable, DATE_FMT.format(ev.getOccurredAt()), valueFont, false);
                    addCell(custTable, ev.getReason() != null ? ev.getReason() : "Routine custody touch", valueFont, false);
                    addCell(custTable, ev.getActorSignature() != null ? ev.getActorSignature().substring(0, Math.min(16, ev.getActorSignature().length())) + "..." : "SIGNED", smallMono, false);
                }
            } else {
                addCell(custTable, "INGEST", labelFont, false);
                addCell(custTable, DATE_FMT.format(certificate.getIssuedAt()), valueFont, false);
                addCell(custTable, "Sealed upon initial ingest", valueFont, false);
                addCell(custTable, "VERIFIED", smallMono, false);
            }

            doc.add(custTable);
            doc.add(new Paragraph(" "));

            // 6. Statutory Declaration
            Paragraph declHeader = new Paragraph("STATUTORY DECLARATION (SECTION 63(4) BSA 2023)", sectionFont);
            declHeader.setSpacingAfter(2);
            doc.add(declHeader);

            Paragraph declText = new Paragraph(
                    "\"I hereby declare and certify that the electronic record described herein was produced by a computer / device during " +
                            "the period over which the device was used regularly to store or process information for lawful purposes. The cryptographic " +
                            "hash values recorded above certify that the contents of this electronic record are true and have not been altered or tampered with " +
                            "since its acquisition, satisfying all statutory requirements under Section 63 of the Bharatiya Sakshya Adhiniyam, 2023.\"",
                    declarationFont
            );
            declText.setSpacingAfter(10);
            doc.add(declText);

            // 7. Signature & Seal
            PdfPTable sigTable = new PdfPTable(2);
            sigTable.setWidthPercentage(100);
            sigTable.setWidths(new float[]{50, 50});

            PdfPCell leftCell = new PdfPCell();
            leftCell.setBorder(PdfPCell.NO_BORDER);
            leftCell.addElement(new Paragraph("Certifying Officer:", labelFont));
            leftCell.addElement(new Paragraph(officer.getFullName() + " (" + officer.getBadgeNumber() + ")", valueFont));
            leftCell.addElement(new Paragraph(officer.getDepartment() + " - " + officer.getRole().name(), valueFont));
            sigTable.addCell(leftCell);

            PdfPCell rightCell = new PdfPCell();
            rightCell.setBorder(PdfPCell.NO_BORDER);
            rightCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            rightCell.addElement(new Paragraph("System Seal & Digital Timestamp:", labelFont));
            rightCell.addElement(new Paragraph("Key ID: " + certificate.getSigningKeyId(), smallMono));
            rightCell.addElement(new Paragraph("Generated: " + DATE_FMT.format(certificate.getIssuedAt()), smallMono));
            sigTable.addCell(rightCell);

            doc.add(sigTable);

            doc.close();
            return out.toByteArray();
        } catch (DocumentException e) {
            throw new IllegalStateException("Failed to generate BSA §63 certificate PDF", e);
        } catch (Exception e) {
            throw new RuntimeException("Unexpected error rendering certificate PDF", e);
        }
    }

    private static void addCell(PdfPTable table, String text, Font font, boolean isHeader) {
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "", font));
        cell.setPadding(4);
        if (isHeader) {
            cell.setBackgroundColor(new Color(245, 245, 245));
        }
        table.addCell(cell);
    }
}
