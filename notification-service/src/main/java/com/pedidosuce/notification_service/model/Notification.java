package com.pedidosuce.notification_service.model;

import java.time.Instant;

public record Notification(
        Long id,
        Long customerId,
        String message,
        Instant sentAt
) {
}