package com.dtvi.Pet.Ecommerce.controller;

import com.dtvi.Pet.Ecommerce.dto.ApiResponse;
import com.dtvi.Pet.Ecommerce.dto.payment.PaymentResultResponse;
import com.dtvi.Pet.Ecommerce.dto.payment.PaymentUrlResponse;
import com.dtvi.Pet.Ecommerce.dto.payment.VnPayIpnResponse;
import com.dtvi.Pet.Ecommerce.security.AuthUser;
import com.dtvi.Pet.Ecommerce.service.impl.VnPayService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/payments/vnpay")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final VnPayService vnPayService;

    @PostMapping("/create/{orderId}")
    public ApiResponse<PaymentUrlResponse> createPaymentUrl(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long orderId,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                "VNPay payment URL created",
                vnPayService.createPaymentUrl(authUser.getUser(), orderId, request.getRemoteAddr())
        );
    }

    @GetMapping("/ipn")
    public VnPayIpnResponse ipn(@RequestParam Map<String, String> parameters) {
        try {
            return vnPayService.processIpn(parameters);
        } catch (RuntimeException ex) {
            log.error(
                    "VNPay IPN processing failed for transaction reference {}",
                    parameters.get("vnp_TxnRef"),
                    ex
            );
            return new VnPayIpnResponse("99", "Unknown error");
        }
    }

    @GetMapping("/return")
    public ApiResponse<PaymentResultResponse> paymentReturn(
            @RequestParam Map<String, String> parameters
    ) {
        return ApiResponse.success("VNPay return verified", vnPayService.verifyReturn(parameters));
    }
}
