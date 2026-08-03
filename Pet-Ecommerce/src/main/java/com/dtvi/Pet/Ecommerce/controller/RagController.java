package com.dtvi.Pet.Ecommerce.controller;

import com.dtvi.Pet.Ecommerce.dto.ApiResponse;
import com.dtvi.Pet.Ecommerce.dto.rag.*;
import com.dtvi.Pet.Ecommerce.security.AuthUser;
import com.dtvi.Pet.Ecommerce.service.impl.KnowledgeService;
import com.dtvi.Pet.Ecommerce.service.impl.RagChatService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;
import com.dtvi.Pet.Ecommerce.service.impl.RequestRateLimiter;

import java.time.Duration;
import java.util.List;

@RestController
@RequestMapping("/rag")
@RequiredArgsConstructor
public class RagController {

    private final RagChatService ragChatService;
    private final KnowledgeService knowledgeService;
    private final RequestRateLimiter requestRateLimiter;

    @PostMapping("/chat")
    public ApiResponse<ChatResponse> chat(
            @AuthenticationPrincipal AuthUser authUser,
            @Valid @RequestBody ChatRequest request,
            HttpServletRequest servletRequest
    ) {
        String identity = authUser == null
                ? clientIp(servletRequest)
                : "user-" + authUser.getUser().getId();

        requestRateLimiter.check(
                "rag-chat",
                identity,
                authUser == null ? 10 : 30,
                Duration.ofMinutes(1)
        );

        return ApiResponse.success(
                "Chat response generated",
                ragChatService.chat(
                        authUser == null
                                ? null
                                : authUser.getUser(),
                        request
                )
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

    private String clientIp(HttpServletRequest request) {
        String cloudflareIp =
                request.getHeader("CF-Connecting-IP");

        if (cloudflareIp != null && !cloudflareIp.isBlank()) {
            return cloudflareIp.trim();
        }

        String forwarded =
                request.getHeader("X-Forwarded-For");

        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }

        return request.getRemoteAddr();
    }
}
