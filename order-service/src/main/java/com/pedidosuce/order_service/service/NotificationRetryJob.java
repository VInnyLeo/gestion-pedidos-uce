package com.pedidosuce.order_service.service;

import java.util.List;

import com.pedidosuce.order_service.client.NotificationClient;
import com.pedidosuce.order_service.model.NotificationStatus;
import com.pedidosuce.order_service.model.Order;
import com.pedidosuce.order_service.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class NotificationRetryJob {

    private static final Logger log = LoggerFactory.getLogger(NotificationRetryJob.class);

    private final OrderRepository repository;
    private final NotificationClient notificationClient;

    public NotificationRetryJob(OrderRepository repository, NotificationClient notificationClient) {
        this.repository = repository;
        this.notificationClient = notificationClient;
    }

    @Scheduled(fixedDelayString = "${notifications.retry-interval-ms}")
    @Transactional
    public void retryPendingNotifications() {
        List<Order> pending = repository.findByNotificationStatus(NotificationStatus.PENDING);
        if (pending.isEmpty()) {
            return;
        }

        log.info("Reintentando {} aviso(s) pendiente(s)", pending.size());

        for (Order order : pending) {
            String message = "Tu pedido #" + order.getId()
                    + " fue registrado por un total de $" + order.getTotal();

            if (notificationClient.send(order.getCustomerId(), message)) {
                order.markNotificationSent();
                log.info("Aviso del pedido {} enviado en el reintento", order.getId());
            } else {
                log.warn("notification-service sigue sin responder; se reintentará más tarde");
                break;
            }
        }
    }
}