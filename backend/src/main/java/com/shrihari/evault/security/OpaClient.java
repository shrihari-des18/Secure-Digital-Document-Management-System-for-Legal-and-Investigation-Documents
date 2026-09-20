package com.shrihari.evault.security;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class OpaClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${opa.url:http://localhost:8181/v1/data/authz}")
    private String opaUrl;

    public boolean allow(Map<String, Object> subject, Map<String, Object> resource, String action) {
        try {
            Map<String, Object> input = Map.of(
                    "subject", subject,
                    "resource", resource,
                    "action", action
            );
            Map<String, Object> request = Map.of("input", input);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

            JsonNode response = restTemplate.postForObject(opaUrl, entity, JsonNode.class);
            if (response != null && response.has("result") && response.get("result").has("allow")) {
                return response.get("result").get("allow").asBoolean();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}
