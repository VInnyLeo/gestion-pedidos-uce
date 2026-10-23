package com.pedidosuce.order_service.repository;

import java.util.List;

import com.pedidosuce.order_service.model.NotificationStatus;
import com.pedidosuce.order_service.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByNotificationStatus(NotificationStatus notificationStatus);
}