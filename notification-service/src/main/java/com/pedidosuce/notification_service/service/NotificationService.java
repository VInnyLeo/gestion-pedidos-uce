package com.pedidosuce.notification_service.service;

import java.time.Instant;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicLong;

import com.pedidosuce.notification_service.dto.NotificationRequest;
import com.pedidosuce.notification_service.model.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    private static final int MAX_STORED = 100;

    private final AtomicLong idGenerator = new AtomicLong();
    private final Deque<Notification> notifications = new ConcurrentLinkedDeque<>();

    public Notification send(NotificationRequest request) {
        Notification notification = new Notification(
                idGenerator.incrementAndGet(),
                request.customerId(),
                request.message(),
                Instant.now());

        // Simulación del envío: un servicio real enviaría un correo o un SMS
        log.info("Aviso enviado al cliente {}: {}", notification.customerId(), notification.message());

        notifications.addFirst(notification);
        while (notifications.size() > MAX_STORED) {
            notifications.pollLast();
        }
        return notification;
    }

    public List<Notification> findAll() {
        return List.copyOf(notifications);
    }
}