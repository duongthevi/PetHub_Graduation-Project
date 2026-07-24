package com.dtvi.Pet.Ecommerce.dto.statistics;

import java.math.BigDecimal;

public record DashboardResponse(
        BigDecimal paidRevenue,
        long totalOrders,
        long pendingOrders,
        long activeProducts,
        long lowStockProducts,
        long users
) {
}
