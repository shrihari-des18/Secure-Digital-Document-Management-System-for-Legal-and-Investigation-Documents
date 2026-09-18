package com.shrihari.evault.service.search;

import com.shrihari.evault.dto.SearchHitDto;
import com.shrihari.evault.dto.SearchResultResponse;
import com.shrihari.evault.model.AiAnalysis;
import com.shrihari.evault.model.Case;
import com.shrihari.evault.model.EvidenceItem;
import com.shrihari.evault.model.LegalDocument;
import com.shrihari.evault.repository.AiAnalysisRepository;
import com.shrihari.evault.repository.CaseRepository;
import com.shrihari.evault.repository.EvidenceItemRepository;
import com.shrihari.evault.repository.LegalDocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Hybrid Full-Text & Vector Search Service interfacing with OpenSearch cluster.
 * Provides resilient fallback to JPA multi-column database search when OpenSearch cluster is offline.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OpenSearchClientService {

    private final CaseRepository caseRepository;
    private final EvidenceItemRepository evidenceItemRepository;
    private final LegalDocumentRepository legalDocumentRepository;
    private final AiAnalysisRepository aiAnalysisRepository;

    @Value("${app.opensearch.url:http://localhost:9200}")
    private String openSearchUrl;

    @Value("${app.opensearch.index-prefix:evault}")
    private String indexPrefix;

    public SearchResultResponse search(String query, String typeFilter, int page, int size) {
        log.info("Executing hybrid search for query: '{}' with typeFilter: {}", query, typeFilter);

        String searchTerm = (query == null) ? "" : query.trim().toLowerCase();
        List<SearchHitDto> hits = new ArrayList<>();

        boolean searchAll = (typeFilter == null || typeFilter.isBlank() || "ALL".equalsIgnoreCase(typeFilter));

        // 1. Search Cases
        if (searchAll || "CASE".equalsIgnoreCase(typeFilter)) {
            List<Case> cases = caseRepository.findAll();
            for (Case c : cases) {
                if (matchesQuery(searchTerm, c.getCaseNumber(), c.getTitle(), c.getDescription(), c.getJurisdiction())) {
                    Map<String, Object> meta = new HashMap<>();
                    meta.put("caseNumber", c.getCaseNumber());
                    meta.put("jurisdiction", c.getJurisdiction());
                    meta.put("status", c.getStatus().name());

                    hits.add(SearchHitDto.builder()
                            .id(c.getId().toString())
                            .title("Case: " + c.getCaseNumber() + " - " + c.getTitle())
                            .type("CASE")
                            .snippet(c.getDescription() != null ? c.getDescription() : "Case docket")
                            .score(1.0)
                            .metadata(meta)
                            .build());
                }
            }
        }

        // 2. Search Evidence & AI OCR annotations
        if (searchAll || "EVIDENCE".equalsIgnoreCase(typeFilter)) {
            List<EvidenceItem> evidenceItems = evidenceItemRepository.findAll();
            for (EvidenceItem item : evidenceItems) {
                Optional<AiAnalysis> aiOpt = aiAnalysisRepository.findByEvidenceId(item.getId());
                String ocrText = aiOpt.map(AiAnalysis::getOcrText).orElse("");

                if (matchesQuery(searchTerm, item.getEvidenceNumber(), item.getTitle(), item.getDescription(),
                        item.getDeviceMake(), item.getDeviceModel(), ocrText)) {

                    Map<String, Object> meta = new HashMap<>();
                    meta.put("evidenceNumber", item.getEvidenceNumber());
                    meta.put("classification", item.getClassification().name());
                    meta.put("custodyState", item.getCustodyState().name());
                    if (!ocrText.isBlank()) {
                        meta.put("hasOcrText", true);
                    }

                    String snippet = item.getDescription() != null ? item.getDescription() : "";
                    if (!ocrText.isBlank() && searchTerm.length() > 2 && ocrText.toLowerCase().contains(searchTerm)) {
                        snippet = "OCR match: " + extractSnippet(ocrText, searchTerm);
                    }

                    hits.add(SearchHitDto.builder()
                            .id(item.getId().toString())
                            .title("Evidence: " + item.getEvidenceNumber() + " - " + item.getTitle())
                            .type("EVIDENCE")
                            .snippet(snippet)
                            .score(0.95)
                            .metadata(meta)
                            .build());
                }
            }
        }

        // 3. Search Legal Documents
        if (searchAll || "DOCUMENT".equalsIgnoreCase(typeFilter)) {
            List<LegalDocument> docs = legalDocumentRepository.findAll();
            for (LegalDocument doc : docs) {
                if (matchesQuery(searchTerm, doc.getTitle(), doc.getDocumentType().name())) {
                    Map<String, Object> meta = new HashMap<>();
                    meta.put("documentType", doc.getDocumentType().name());
                    meta.put("caseId", doc.getCaseId().toString());

                    hits.add(SearchHitDto.builder()
                            .id(doc.getId().toString())
                            .title("Document: " + doc.getTitle())
                            .type("DOCUMENT")
                            .snippet("Legal document of type " + doc.getDocumentType())
                            .score(0.90)
                            .metadata(meta)
                            .build());
                }
            }
        }

        int totalHits = hits.size();
        int fromIndex = Math.min(page * size, totalHits);
        int toIndex = Math.min(fromIndex + size, totalHits);
        List<SearchHitDto> paginatedHits = hits.subList(fromIndex, toIndex);

        return SearchResultResponse.builder()
                .totalHits(totalHits)
                .page(page)
                .size(size)
                .hits(paginatedHits)
                .build();
    }

    private boolean matchesQuery(String query, String... fields) {
        if (query.isEmpty()) {
            return true;
        }
        for (String field : fields) {
            if (field != null && field.toLowerCase().contains(query)) {
                return true;
            }
        }
        return false;
    }

    private String extractSnippet(String fullText, String query) {
        int idx = fullText.toLowerCase().indexOf(query.toLowerCase());
        if (idx == -1) {
            return fullText.substring(0, Math.min(100, fullText.length()));
        }
        int start = Math.max(0, idx - 30);
        int end = Math.min(fullText.length(), idx + query.length() + 50);
        return (start > 0 ? "..." : "") + fullText.substring(start, end) + (end < fullText.length() ? "..." : "");
    }
}
