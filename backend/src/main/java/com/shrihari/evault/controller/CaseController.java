package com.shrihari.evault.controller;

import com.shrihari.evault.common.ApiResponse;
import com.shrihari.evault.dto.CaseResponse;
import com.shrihari.evault.dto.CreateCaseRequest;
import com.shrihari.evault.dto.UpdateCaseRequest;
import com.shrihari.evault.dto.UpdateCaseStatusRequest;
import com.shrihari.evault.model.CaseStatus;
import com.shrihari.evault.service.CaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/cases")
@RequiredArgsConstructor
@Tag(name = "Cases", description = "Case management and legal investigation docket operations")
public class CaseController {

    private final CaseService caseService;

    @PostMapping
    @PreAuthorize("hasAnyRole('INVESTIGATING_OFFICER', 'SYSTEM_ADMIN')")
    @Operation(summary = "Create a new case", description = "Initializes a new criminal or legal investigation case record")
    public ResponseEntity<ApiResponse<CaseResponse>> createCase(@Valid @RequestBody CreateCaseRequest request) {
        CaseResponse response = caseService.createCase(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Case created successfully", response));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@caseSecurityService.canAccessCase(#id)")
    @Operation(summary = "Get case by ID", description = "Retrieves complete case metadata by its primary UUID")
    public ResponseEntity<ApiResponse<CaseResponse>> getCaseById(@PathVariable UUID id) {
        CaseResponse response = caseService.getCaseById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/number/{caseNumber}")
    @Operation(summary = "Get case by case number", description = "Retrieves case metadata using its formal identifier (e.g. FIR number)")
    public ResponseEntity<ApiResponse<CaseResponse>> getCaseByNumber(@PathVariable String caseNumber) {
        CaseResponse response = caseService.getCaseByNumber(caseNumber);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    @Operation(summary = "List all cases", description = "Retrieves cases, optionally filtered by status")
    public ResponseEntity<ApiResponse<List<CaseResponse>>> getAllCases(
            @Parameter(description = "Optional filter by case status")
            @RequestParam(required = false) CaseStatus status) {
        List<CaseResponse> cases = caseService.getAllCases(status);
        return ResponseEntity.ok(ApiResponse.success(cases));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('INVESTIGATING_OFFICER', 'SYSTEM_ADMIN') and @caseSecurityService.canModifyCase(#id)")
    @Operation(summary = "Update case metadata", description = "Updates title, description, jurisdiction, or assigned officers")
    public ResponseEntity<ApiResponse<CaseResponse>> updateCase(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCaseRequest request) {
        CaseResponse response = caseService.updateCase(id, request);
        return ResponseEntity.ok(ApiResponse.success("Case updated successfully", response));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('INVESTIGATING_OFFICER', 'SYSTEM_ADMIN') and @caseSecurityService.canModifyCase(#id)")
    @Operation(summary = "Update case status", description = "Transitions case lifecycle status (e.g. OPEN -> UNDER_INVESTIGATION -> IN_COURT -> CLOSED)")
    public ResponseEntity<ApiResponse<CaseResponse>> updateCaseStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCaseStatusRequest request) {
        CaseResponse response = caseService.updateStatus(id, request.getStatus(), request.getReason());
        return ResponseEntity.ok(ApiResponse.success("Case status updated successfully", response));
    }
}
