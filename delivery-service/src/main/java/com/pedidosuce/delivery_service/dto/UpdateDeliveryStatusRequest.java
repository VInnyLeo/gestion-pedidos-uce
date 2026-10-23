package com.pedidosuce.delivery_service.dto;

import com.pedidosuce.delivery_service.model.DeliveryStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateDeliveryStatusRequest(
        @NotNull(message = "El estado es obligatorio")
        DeliveryStatus status
) {
}
