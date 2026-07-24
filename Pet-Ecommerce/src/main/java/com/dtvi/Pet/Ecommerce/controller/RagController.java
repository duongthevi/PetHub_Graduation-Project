package com.dtvi.Pet.Ecommerce.controller;

import com.dtvi.Pet.Ecommerce.dto.ApiResponse;
import com.dtvi.Pet.Ecommerce.dto.rag.*;
import com.dtvi.Pet.Ecommerce.security.AuthUser;
import com.dtvi.Pet.Ecommerce.service.impl.KnowledgeService;
import com.dtvi.Pet.Ecommerce.service.impl.RagChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/rag")
@RequiredArgsConstructor
public class RagController {

    private final RagChatService ragChatService;
    private final KnowledgeService knowledgeService;

    @PostMapping("/chat")
    public ApiResponse<ChatResponse> chat(
            @AuthenticationPrincipal AuthUser authUser,
            @Valid @RequestBody ChatRequest request
    ) {
        return ApiResponse.success(
                "Chat response generated",
                ragChatService.chat(authUser == null ? null : authUser.getUser(), request)
        );
    }

    @GetMapping("/admin/documents")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ApiResponse<List<KnowledgeDocumentResponse>> getDocuments() {
        return ApiResponse.success("Knowledge documents retrieved", knowledgeService.getAll());
    }

    @PostMapping("/admin/documents")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ApiResponse<KnowledgeDocumentResponse> ingest(
            @Valid @RequestBody KnowledgeDocumentRequest request
    ) {
        return ApiResponse.success("Knowledge document indexed", knowledgeService.ingest(request));
    }

    @PatchMapping("/admin/documents/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ApiResponse<KnowledgeDocumentResponse> setActive(
            @PathVariable Long id,
            @RequestParam boolean active
    ) {
        return ApiResponse.success("Knowledge document updated", knowledgeService.setActive(id, active));
    }

    @DeleteMapping("/admin/documents/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        knowledgeService.delete(id);
        return ApiResponse.success("Knowledge document deleted");
    }
}
