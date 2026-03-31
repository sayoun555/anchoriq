package com.anchoriq.api.controller.workflow;

import com.anchoriq.api.application.workflow.WorkflowApplicationService;
import com.anchoriq.api.dto.request.workflow.WorkflowCreateRequest;
import com.anchoriq.api.dto.request.workflow.WorkflowUpdateRequest;
import com.anchoriq.api.dto.response.workflow.WorkflowExecutionResponse;
import com.anchoriq.api.dto.response.workflow.WorkflowResponse;
import com.anchoriq.api.global.response.ApiResponse;
import com.anchoriq.api.global.response.PageResponse;
import com.anchoriq.api.infrastructure.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 워크플로우 Controller — CRUD, 활성화/비활성화, 실행 이력.
 */
@RestController
@RequestMapping("/api/workflows")
@RequiredArgsConstructor
public class WorkflowController {

    private final WorkflowApplicationService workflowService;

    @PostMapping
    public ResponseEntity<ApiResponse<WorkflowResponse>> createWorkflow(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody WorkflowCreateRequest request) {
        WorkflowResponse response = workflowService.createWorkflow(principal.userId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<WorkflowResponse>>> getWorkflows(
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<WorkflowResponse> page = workflowService.getWorkflows(principal.userId(), pageable);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(page)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<WorkflowResponse>> getWorkflow(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        WorkflowResponse response = workflowService.getWorkflow(principal.userId(), id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<WorkflowResponse>> updateWorkflow(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody WorkflowUpdateRequest request) {
        WorkflowResponse response = workflowService.updateWorkflow(principal.userId(), id, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteWorkflow(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        workflowService.deleteWorkflow(principal.userId(), id);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @PostMapping("/{id}/activate")
    public ResponseEntity<ApiResponse<WorkflowResponse>> activateWorkflow(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        WorkflowResponse response = workflowService.activateWorkflow(principal.userId(), id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{id}/deactivate")
    public ResponseEntity<ApiResponse<WorkflowResponse>> deactivateWorkflow(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        WorkflowResponse response = workflowService.deactivateWorkflow(principal.userId(), id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}/executions")
    public ResponseEntity<ApiResponse<PageResponse<WorkflowExecutionResponse>>> getExecutionHistory(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<WorkflowExecutionResponse> page =
                workflowService.getExecutionHistory(principal.userId(), id, pageable);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(page)));
    }
}
