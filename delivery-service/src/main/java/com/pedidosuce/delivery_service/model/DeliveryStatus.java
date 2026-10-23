package com.pedidosuce.delivery_service.model;

public enum DeliveryStatus {
    IN_TRANSIT,
    DELIVERED;

    public boolean canChangeTo(DeliveryStatus next) {
        return this == IN_TRANSIT && next == DELIVERED;
    }
}
