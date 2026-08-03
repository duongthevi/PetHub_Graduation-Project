package com.dtvi.Pet.Ecommerce.config;

import com.dtvi.Pet.Ecommerce.entity.AuditLog;
import com.dtvi.Pet.Ecommerce.repository.AuditLogRepo;
import com.dtvi.Pet.Ecommerce.security.AuthUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuditLoggingInterceptor implements HandlerInterceptor {

    private final AuditLogRepo auditLogRepository;

    private static final String[] AUDITED_PATHS = {
            "/admin/",
            "/user/admin/",
            "/orders/admin/",
            "/product/",
            "/category/",
            "/coupons",
            "/rag/admin/"
    };

    @Override
    public void afterCompletion(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler,
            Exception ex
    ) {
        try {
            // ✅ Kiểm tra điều kiện ghi audit log
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            
            if (authentication == null || !isAdmin(authentication)) {
                return;
            }

            if (request.getMethod().equals("GET")) {
                return;
            }

            if (response.getStatus() >= 400) {
                return;
            }

            if (!isAuditedPath(request.getRequestURI())) {
                return;
            }

            // ✅ Lấy thông tin user
            String userEmail = authentication.getName();
            Object principal = authentication.getPrincipal();
            Long userId = extractUserId(principal);

            if (userId == null) {
                log.warn(
                        "Skipping audit log because authenticated actor ID is unavailable"
                );
                return;
            }

            // ✅ Tạo audit log
            AuditLog auditLog = new AuditLog();
            auditLog.setActorUserId(userId);
            auditLog.setActorEmail(userEmail);
            auditLog.setHttpMethod(request.getMethod());
            auditLog.setRequestPath(request.getRequestURI());
            auditLog.setResponseStatus(response.getStatus());

            auditLogRepository.save(auditLog);
            
            log.info("Audit log recorded: User {} performed {} on {}",
                    userEmail, request.getMethod(), request.getRequestURI());

        } catch (Exception e) {
            log.error("Error recording audit log", e);
            // ❌ Không throw exception để không ảnh hưởng request flow
        }
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities()
                .stream()
                .anyMatch(a -> a.getAuthority().equals("ADMIN"));
    }

    private boolean isAuditedPath(String path) {
        for (String auditedPath : AUDITED_PATHS) {
            if (path.startsWith(auditedPath)) {
                return true;
            }
        }
        return false;
    }

    private Long extractUserId(Object principal) {
        if (principal instanceof AuthUser authUser
                && authUser.getUser() != null) {
            return authUser.getUser().getId();
        }

        return null;
    }
}
