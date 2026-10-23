package com.pedidosuce.notification_service.controller;

import java.util.List;

import com.pedidosuce.notification_service.dto.NotificationRequest;
import com.pedidosuce.notification_service.model.Notification;
import com.pedidosuce.notification_service.service.NotificationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService service;

    public NotificationController(NotificationService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Notification send(@Valid @RequestBody NotificationRequest request) {
        return service.send(request);
    }

    @GetMapping
    public List<Notification> findAll() {
        return service.findAll();
    }
}