package com.shrihari.evault.controller;

import com.shrihari.evault.common.ApiResponse;
import com.shrihari.evault.dto.SearchResultResponse;
import com.shrihari.evault.service.search.OpenSearchClientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Search Engine Subsystem", description = "Hybrid Full-Text & Vector Search across cases, evidence OCR, and legal documents")
public class SearchController {

    private final OpenSearchClientService openSearchClientService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Perform hybrid full-text & semantic search",
               description = "Searches across case dockets, sealed evidence metadata, OCR text, PII, and versioned legal documents.")
    public ResponseEntity<ApiResponse<SearchResultResponse>> search(
            @RequestParam(name = "query", required = false, defaultValue = "") String query,
            @RequestParam(name = "type", required = false, defaultValue = "ALL") String type,
            @RequestParam(name = "page", required = false, defaultValue = "0") int page,
            @RequestParam(name = "size", required = false, defaultValue = "10") int size) {

        log.info("REST search request received: query='{}', type='{}', page={}, size={}", query, type, page, size);
        SearchResultResponse results = openSearchClientService.search(query, type, page, size);
        return ResponseEntity.ok(ApiResponse.success("Search completed successfully", results));
    }
}
