package com.pedidosuce.order_service.dto;

import com.pedidosuce.order_service.model.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateStatusRequest(
        @NotNull(message = "El estado es obligatorio")
        OrderStatus status
) {
}