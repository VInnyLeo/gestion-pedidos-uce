package com.pedidosuce.notification_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record NotificationRequest(
        @NotNull(message = "El customerId es obligatorio")
        Long customerId,

        @NotBlank(message = "El mensaje es obligatorio")
        @Size(max = 500, message = "El mensaje admite máximo 500 caracteres")
        String message
) {
}