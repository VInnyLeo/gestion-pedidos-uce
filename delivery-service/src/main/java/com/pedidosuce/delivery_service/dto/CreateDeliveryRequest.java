package com.pedidosuce.delivery_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateDeliveryRequest(
        @NotNull(message = "El orderId es obligatorio")
        Long orderId,

        @NotBlank(message = "El nombre del repartidor es obligatorio")
        @Size(max = 100, message = "El nombre del repartidor admite máximo 100 caracteres")
        String courier
) {
}
