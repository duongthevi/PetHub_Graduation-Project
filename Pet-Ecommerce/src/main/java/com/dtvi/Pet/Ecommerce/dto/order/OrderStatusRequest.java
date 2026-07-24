package com.dtvi.Pet.Ecommerce.dto.order;

import com.dtvi.Pet.Ecommerce.enums.OrderStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record OrderStatusRequest(
        @NotNull OrderStatus status,
        @Size(max = 1000) String adminMessage
) {
}
