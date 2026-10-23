package com.pedidosuce.order_service.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.pedidosuce.order_service.model.NotificationStatus;
import com.pedidosuce.order_service.model.Order;
import com.pedidosuce.order_service.model.OrderStatus;

public record OrderResponse(
        Long id,
        Long customerId,
        String shippingAddress,
        BigDecimal total,
        OrderStatus status,
        NotificationStatus notificationStatus,
        Instant createdAt,
        List<ItemResponse> items
) {

    public record ItemResponse(
            Long productId,
            String productName,
            Integer quantity,
            BigDecimal unitPrice,
            BigDecimal subtotal
    ) {
    }

    public static OrderResponse from(Order order) {
        List<ItemResponse> items = order.getItems().stream()
                .map(item -> new ItemResponse(
                        item.getProductId(),
                        item.getProductName(),
                        item.getQuantity(),
                        item.getUnitPrice(),
                        item.getSubtotal()))
                .toList();

        return new OrderResponse(
                order.getId(),
                order.getCustomerId(),
                order.getShippingAddress(),
                order.getTotal(),
                order.getStatus(),
                order.getNotificationStatus(),
                order.getCreatedAt(),
                items);
    }
}