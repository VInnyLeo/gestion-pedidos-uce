package com.pedidosuce.order_service.model;

public enum OrderStatus {
    CREATED,
    IN_DELIVERY,
    DELIVERED;

    public boolean canChangeTo(OrderStatus next) {
        return (this == CREATED && next == IN_DELIVERY)
                || (this == IN_DELIVERY && next == DELIVERED);
    }
}