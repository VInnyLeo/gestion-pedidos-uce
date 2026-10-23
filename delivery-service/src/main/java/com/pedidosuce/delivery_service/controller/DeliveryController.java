package com.pedidosuce.delivery_service.controller;

import java.util.List;

import com.pedidosuce.delivery_service.dto.CreateDeliveryRequest;
import com.pedidosuce.delivery_service.dto.UpdateDeliveryStatusRequest;
import com.pedidosuce.delivery_service.model.Delivery;
import com.pedidosuce.delivery_service.service.DeliveryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/deliveries")
public class DeliveryController {

    private final DeliveryService service;

    public DeliveryController(DeliveryService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Delivery create(@Valid @RequestBody CreateDeliveryRequest request) {
        return service.create(request);
    }

    @GetMapping
    public List<Delivery> findAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public Delivery findById(@PathVariable Long id) {
        return service.findById(id);
    }

    @PatchMapping("/{id}/status")
    public Delivery updateStatus(@PathVariable Long id,
                                 @Valid @RequestBody UpdateDeliveryStatusRequest request) {
        return service.updateStatus(id, request.status());
    }
}
