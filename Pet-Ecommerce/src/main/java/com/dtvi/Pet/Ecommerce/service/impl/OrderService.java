package com.dtvi.Pet.Ecommerce.service.impl;

import com.dtvi.Pet.Ecommerce.dto.order.CheckoutRequest;
import com.dtvi.Pet.Ecommerce.dto.order.CheckoutResponse;
import com.dtvi.Pet.Ecommerce.dto.order.OrderResponse;
import com.dtvi.Pet.Ecommerce.entity.*;
import com.dtvi.Pet.Ecommerce.enums.*;
import com.dtvi.Pet.Ecommerce.exception.BusinessException;
import com.dtvi.Pet.Ecommerce.exception.NotFoundException;
import com.dtvi.Pet.Ecommerce.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.scheduling.annotation.Scheduled;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {

    private static final BigDecimal MAX_ORDER_TOTAL = new BigDecimal("10000000000");

    private static final Set<OrderStatus> USER_CANCELLABLE =
            EnumSet.of(OrderStatus.PENDING_PAYMENT, OrderStatus.PENDING);

    private static final Map<OrderStatus, Set<OrderStatus>> ADMIN_TRANSITIONS = Map.of(
            OrderStatus.PENDING_PAYMENT, EnumSet.of(OrderStatus.PENDING, OrderStatus.CANCELLED),
            OrderStatus.PENDING, EnumSet.of(OrderStatus.CONFIRMED, OrderStatus.CANCELLED),
            OrderStatus.CONFIRMED, EnumSet.of(OrderStatus.PROCESSING, OrderStatus.CANCELLED),
            OrderStatus.PROCESSING, EnumSet.of(OrderStatus.SHIPPING, OrderStatus.CANCELLED),
            OrderStatus.SHIPPING, EnumSet.of(OrderStatus.DELIVERED),
            OrderStatus.DELIVERED, EnumSet.of(OrderStatus.RETURNED)
    );

    private final CartRepo cartRepo;
    private final ProductRepo productRepo;
    private final OrderRepo orderRepo;
    private final PaymentRepo paymentRepo;
    private final CouponService couponService;

    @Value("${app.checkout.shipping-fee:30000}")
    private BigDecimal defaultShippingFee;

    @Value("${app.checkout.free-shipping-threshold:500000}")
    private BigDecimal freeShippingThreshold;

    @Value("${app.payment.vnpay.pending-minutes:15}")
    private long vnpayPendingMinutes;

    public CheckoutResponse checkout(User user, CheckoutRequest request) {
        Cart cart = cartRepo.findWithItemsByUserId(user.getId())
                .orElseThrow(() -> BusinessException.badRequest("CART_EMPTY", "Cart is empty"));
        if (cart.getItems().isEmpty()) {
            throw BusinessException.badRequest("CART_EMPTY", "Cart is empty");
        }

        List<Long> productIds = cart.getItems().stream()
                .map(item -> item.getProduct().getId())
                .sorted()
                .toList();
        Map<Long, Product> lockedProducts = productIds.stream()
                .map(this::lockProduct)
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        BigDecimal subtotal = BigDecimal.ZERO;
        for (CartItem cartItem : cart.getItems()) {
            Product product = lockedProducts.get(cartItem.getProduct().getId());
            validatePurchasable(product, cartItem.getQuantity());
            subtotal = subtotal.add(product.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())));
        }

        Coupon coupon = null;
        BigDecimal discount = BigDecimal.ZERO;
        if (request.couponCode() != null && !request.couponCode().isBlank()) {
            coupon = couponService.requireValid(request.couponCode(), subtotal, user);
            discount = couponService.calculateDiscount(coupon, subtotal);
        }
        BigDecimal shippingFee = subtotal.compareTo(freeShippingThreshold) >= 0
                ? BigDecimal.ZERO
                : defaultShippingFee;
        BigDecimal total = subtotal.subtract(discount).add(shippingFee);
        if (total.compareTo(MAX_ORDER_TOTAL) > 0) {
            throw BusinessException.badRequest(
                    "ORDER_TOTAL_TOO_LARGE",
                    "Order total must not exceed 10,000,000,000 VND. Please check product prices and quantities."
            );
        }

        Order order = buildOrder(user, request, subtotal, discount, shippingFee, coupon);
        List<OrderItem> items = new ArrayList<>();
        for (CartItem cartItem : cart.getItems()) {
            Product product = lockedProducts.get(cartItem.getProduct().getId());
            OrderItem item = buildOrderItem(order, user, product, cartItem.getQuantity());
            items.add(item);
            product.setQuantity(product.getQuantity() - cartItem.getQuantity());
        }
        order.setOrderItemList(items);
        Order saved = orderRepo.save(order);

        if (coupon != null) {
            couponService.recordUsage(coupon, user, saved);
        }
        if (request.paymentMethod() == PaymentMethod.COD) {
            createCodPayment(saved);
        }

        cart.getItems().clear();
        cartRepo.save(cart);
        return new CheckoutResponse(OrderResponse.from(saved), request.paymentMethod() == PaymentMethod.VNPAY);
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> getMyOrders(User user, int page, int size) {
        return orderRepo.findByUserIdOrderByCreatedAtDesc(
                user.getId(),
                PageRequest.of(page, Math.min(size, 50))
        ).map(OrderResponse::from);
    }

    @Transactional(readOnly = true)
    public OrderResponse getMyOrder(User user, Long orderId) {
        Order order = getDetailedOrder(orderId);
        ensureOwner(user, order);
        return OrderResponse.from(order);
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> getAllOrders(int page, int size) {
        return orderRepo.findAll(
                PageRequest.of(page, Math.min(size, 100), Sort.by(Sort.Direction.DESC, "createdAt"))
        ).map(OrderResponse::from);
    }

    public OrderResponse cancelByUser(User user, Long orderId) {
        Order order = getOrderForUpdate(orderId);
        ensureOwner(user, order);
        if (!USER_CANCELLABLE.contains(order.getStatus())) {
            throw BusinessException.conflict(
                    "ORDER_CANNOT_BE_CANCELLED",
                    "Only pending orders can be cancelled"
            );
        }
        cancelOrder(order, "Cancelled by customer");
        return OrderResponse.from(order);
    }

    public OrderResponse updateStatus(Long orderId, OrderStatus newStatus, String adminMessage) {
        Order order = getOrderForUpdate(orderId);
        OrderStatus current = order.getStatus();
        if (current == newStatus) {
            return OrderResponse.from(order);
        }
        if (!ADMIN_TRANSITIONS.getOrDefault(current, Set.of()).contains(newStatus)) {
            throw BusinessException.conflict(
                    "INVALID_ORDER_TRANSITION",
                    "Cannot change order status from " + current + " to " + newStatus
            );
        }
        if (newStatus == OrderStatus.CANCELLED) {
            cancelOrder(order, adminMessage == null ? "Cancelled by administrator" : adminMessage);
            return OrderResponse.from(order);
        }
        if (current == OrderStatus.PENDING_PAYMENT && newStatus == OrderStatus.PENDING
                && order.getPaymentStatus() != PaymentStatus.PAID) {
            throw BusinessException.conflict("PAYMENT_REQUIRED", "VNPay order has not been paid");
        }

        order.setStatus(newStatus);
        order.setAdminMessage(adminMessage);
        mirrorItemStatus(order, newStatus);

        if (newStatus == OrderStatus.DELIVERED) {
            markDelivered(order);
        } else if (newStatus == OrderStatus.RETURNED) {
            processReturn(order);
        }
        return OrderResponse.from(orderRepo.save(order));
    }

    public Order getOwnedVnpayOrder(User user, Long orderId) {
        Order order = getOrderForUpdate(orderId);
        ensureOwner(user, order);
        if (order.getPaymentMethod() != PaymentMethod.VNPAY) {
            throw BusinessException.badRequest("PAYMENT_METHOD_INVALID", "Order does not use VNPay");
        }
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT || order.getPaymentStatus() != PaymentStatus.PENDING) {
            throw BusinessException.conflict("ORDER_NOT_PAYABLE", "Order is not awaiting online payment");
        }
        if (order.getPaymentExpiresAt() != null && order.getPaymentExpiresAt().isBefore(LocalDateTime.now())) {
            cancelOrder(order, "Online payment expired");
            throw BusinessException.conflict("PAYMENT_EXPIRED", "Online payment window has expired");
        }
        return order;
    }

    public void markVnpayPaid(Order order) {
        order = getOrderForUpdate(order.getId());
        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            return;
        }
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw BusinessException.conflict("ORDER_NOT_PAYABLE", "Order is not awaiting payment");
        }
        order.setPaymentStatus(PaymentStatus.PAID);
        order.setPaidAt(LocalDateTime.now());
        order.setStatus(OrderStatus.PENDING);
        mirrorItemStatus(order, OrderStatus.PENDING);
        orderRepo.save(order);
    }

    public void cancelFailedOnlinePayment(Order order, String reason) {
        order = getOrderForUpdate(order.getId());
        if (order.getStatus() == OrderStatus.PENDING_PAYMENT) {
            cancelOrder(order, reason);
        }
    }

    @Scheduled(fixedDelayString = "${app.payment.vnpay.expiry-check-ms:60000}")
    public void expirePendingOnlinePayments() {
        orderRepo.findByStatusAndPaymentExpiresAtBefore(
                        OrderStatus.PENDING_PAYMENT,
                        LocalDateTime.now()
                )
                .forEach(order -> {
                    Order lockedOrder = getOrderForUpdate(order.getId());
                    if (lockedOrder.getStatus() == OrderStatus.PENDING_PAYMENT) {
                        cancelOrder(lockedOrder, "Online payment expired");
                    }
                });
    }

    public Order getByOrderCode(String orderCode) {
        return orderRepo.findByOrderCode(orderCode)
                .orElseThrow(() -> new NotFoundException("Order not found"));
    }

    public Order lockForPayment(Long orderId) {
        return getOrderForUpdate(orderId);
    }

    private Order buildOrder(
            User user,
            CheckoutRequest request,
            BigDecimal subtotal,
            BigDecimal discount,
            BigDecimal shippingFee,
            Coupon coupon
    ) {
        Order order = new Order();
        order.setOrderCode(generateOrderCode());
        order.setUser(user);
        order.setSubtotal(subtotal);
        order.setDiscountAmount(discount);
        order.setShippingFee(shippingFee);
        order.setTotalPrice(subtotal.subtract(discount).add(shippingFee));
        order.setCoupon(coupon);
        order.setPaymentMethod(request.paymentMethod());
        order.setPaymentStatus(PaymentStatus.PENDING);
        order.setStatus(request.paymentMethod() == PaymentMethod.VNPAY
                ? OrderStatus.PENDING_PAYMENT
                : OrderStatus.PENDING);
        if (request.paymentMethod() == PaymentMethod.VNPAY) {
            order.setPaymentExpiresAt(LocalDateTime.now().plusMinutes(vnpayPendingMinutes));
        }
        order.setRecipientName(request.recipientName().trim());
        order.setRecipientPhone(request.recipientPhone().trim());
        order.setShippingStreet(request.street().trim());
        order.setShippingWard(trimToNull(request.ward()));
        order.setShippingDistrict(request.district().trim());
        order.setShippingCity(request.city().trim());
        order.setShippingCountry(request.country().trim());
        order.setNote(trimToNull(request.note()));
        return order;
    }

    private OrderItem buildOrderItem(Order order, User user, Product product, int quantity) {
        BigDecimal lineTotal = product.getPrice().multiply(BigDecimal.valueOf(quantity));
        OrderItem item = new OrderItem();
        item.setOrder(order);
        item.setUser(user);
        item.setProduct(product);
        item.setQuantity(quantity);
        item.setUnitPrice(product.getPrice());
        item.setLineTotal(lineTotal);
        item.setPrice(lineTotal);
        item.setProductName(product.getName());
        item.setProductSku(product.getSku());
        item.setProductImageUrl(product.getImageUrl());
        item.setStatus(order.getStatus());
        return item;
    }

    private void createCodPayment(Order order) {
        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setProvider(PaymentProvider.COD);
        payment.setTransactionRef("COD-" + order.getOrderCode());
        payment.setAmount(order.getTotalPrice());
        payment.setStatus(PaymentStatus.PENDING);
        paymentRepo.save(payment);
    }

    private void cancelOrder(Order order, String reason) {
        if (order.getStatus() == OrderStatus.CANCELLED) {
            return;
        }
        releaseInventory(order);
        couponService.releaseUsage(order);
        order.setStatus(OrderStatus.CANCELLED);
        order.setPaymentStatus(PaymentStatus.CANCELLED);
        order.setCancelledAt(LocalDateTime.now());
        order.setAdminMessage(reason);
        mirrorItemStatus(order, OrderStatus.CANCELLED);
        paymentRepo.findByOrderIdOrderByCreatedAtDesc(order.getId()).forEach(payment -> {
            if (payment.getStatus() == PaymentStatus.PENDING) {
                payment.setStatus(PaymentStatus.CANCELLED);
            }
        });
        orderRepo.save(order);
    }

    private void markDelivered(Order order) {
        for (OrderItem item : order.getOrderItemList()) {
            Product product = item.getProduct();
            product.setSoldCount(product.getSoldCount() + item.getQuantity());
        }
        if (order.getPaymentMethod() == PaymentMethod.COD) {
            order.setPaymentStatus(PaymentStatus.PAID);
            order.setPaidAt(LocalDateTime.now());
            paymentRepo.findByOrderIdOrderByCreatedAtDesc(order.getId()).stream()
                    .filter(payment -> payment.getProvider() == PaymentProvider.COD)
                    .findFirst()
                    .ifPresent(payment -> {
                        payment.setStatus(PaymentStatus.PAID);
                        payment.setPaidAt(LocalDateTime.now());
                    });
        }
    }

    private void processReturn(Order order) {
        for (OrderItem item : order.getOrderItemList()) {
            Product product = item.getProduct();
            product.setQuantity(product.getQuantity() + item.getQuantity());
            product.setSoldCount(Math.max(0, product.getSoldCount() - item.getQuantity()));
        }
        order.setPaymentStatus(PaymentStatus.REFUNDED);
    }

    private void releaseInventory(Order order) {
        List<OrderItem> sortedItems = order.getOrderItemList().stream()
                .sorted(Comparator.comparing(item -> item.getProduct().getId()))
                .toList();
        for (OrderItem item : sortedItems) {
            Product product = productRepo.findByIdForUpdate(item.getProduct().getId())
                    .orElseThrow(() -> new NotFoundException("Product not found"));
            product.setQuantity(product.getQuantity() + item.getQuantity());
        }
    }

    private void mirrorItemStatus(Order order, OrderStatus status) {
        order.getOrderItemList().forEach(item -> item.setStatus(status));
    }

    private Product lockProduct(Long productId) {
        return productRepo.findByIdForUpdate(productId)
                .orElseThrow(() -> new NotFoundException("Product not found"));
    }

    private void validatePurchasable(Product product, int quantity) {
        if (!product.isActive()) {
            throw BusinessException.conflict("PRODUCT_UNAVAILABLE", product.getName() + " is unavailable");
        }
        if (quantity < 1 || quantity > product.getQuantity()) {
            throw BusinessException.conflict(
                    "INSUFFICIENT_STOCK",
                    "Only " + product.getQuantity() + " unit(s) remain for " + product.getName()
            );
        }
    }

    private Order getDetailedOrder(Long orderId) {
        return orderRepo.findWithDetailsById(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found"));
    }

    private Order getOrderForUpdate(Long orderId) {
        return orderRepo.findForUpdateById(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found"));
    }

    private void ensureOwner(User user, Order order) {
        if (order.getUser() == null || !order.getUser().getId().equals(user.getId())) {
            throw BusinessException.forbidden("ORDER_FORBIDDEN", "Order does not belong to this user");
        }
    }

    private String generateOrderCode() {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        return "PET-" + date + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
