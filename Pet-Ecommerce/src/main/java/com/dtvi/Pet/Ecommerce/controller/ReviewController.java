package com.dtvi.Pet.Ecommerce.controller;

import com.dtvi.Pet.Ecommerce.dto.ApiResponse;
import com.dtvi.Pet.Ecommerce.dto.commerce.ReviewRequest;
import com.dtvi.Pet.Ecommerce.dto.commerce.ReviewResponse;
import com.dtvi.Pet.Ecommerce.dto.commerce.AdminReviewReplyRequest;
import com.dtvi.Pet.Ecommerce.dto.commerce.ReviewStatisticsResponse;
import com.dtvi.Pet.Ecommerce.security.AuthUser;
import com.dtvi.Pet.Ecommerce.service.impl.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @GetMapping("/product/{productId}")
    public ApiResponse<Page<ReviewResponse>> getPublished(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ApiResponse.success("Reviews retrieved", reviewService.getPublished(productId, page, size));
    }

    @PostMapping("/product/{productId}")
    public ApiResponse<ReviewResponse> create(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long productId,
            @Valid @RequestBody ReviewRequest request
    ) {
        return ApiResponse.success(
                "Review created",
                reviewService.create(authUser.getUser(), productId, request)
        );
    }

    @PutMapping("/{reviewId}")
    public ApiResponse<ReviewResponse> update(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long reviewId,
            @Valid @RequestBody ReviewRequest request
    ) {
        return ApiResponse.success(
                "Review updated",
                reviewService.update(authUser.getUser(), reviewId, request)
        );
    }

    @DeleteMapping("/{reviewId}")
    public ApiResponse<Void> delete(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long reviewId
    ) {
        reviewService.delete(authUser.getUser(), reviewId, false);
        return ApiResponse.success("Review deleted");
    }

    @DeleteMapping("/admin/{reviewId}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ApiResponse<Void> adminDelete(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long reviewId
    ) {
        reviewService.delete(authUser.getUser(), reviewId, true);
        return ApiResponse.success("Review deleted by administrator");
    }

    @GetMapping("/admin")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ApiResponse<Page<ReviewResponse>> getAllForAdmin(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ApiResponse.success("Reviews retrieved", reviewService.getAllForAdmin(page, size));
    }

    @GetMapping("/admin/statistics")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ApiResponse<ReviewStatisticsResponse> getStatistics() {
        return ApiResponse.success("Review statistics retrieved", reviewService.getStatistics());
    }

    @PutMapping("/admin/{reviewId}/reply")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ApiResponse<ReviewResponse> reply(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long reviewId,
            @Valid @RequestBody AdminReviewReplyRequest request
    ) {
        return ApiResponse.success("Review reply saved",
                reviewService.reply(authUser.getUser(), reviewId, request.reply()));
    }
}
