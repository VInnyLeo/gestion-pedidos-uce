package com.pedidosuce.delivery_service.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "deliveries")
public class Delivery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long orderId;

    @Column(nullable = false)
    private Long customerId;

    @Column(nullable = false, length = 255)
    private String address;

    @Column(nullable = false, length = 100)
    private String courier;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DeliveryStatus status = DeliveryStatus.IN_TRANSIT;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    private Instant deliveredAt;

    protected Delivery() {
        // Constructor vacío requerido por JPA
    }

    public Delivery(Long orderId, Long customerId, String address, String courier) {
        this.orderId = orderId;
        this.customerId = customerId;
        this.address = address;
        this.courier = courier;
    }

    public void markDelivered() {
        this.status = DeliveryStatus.DELIVERED;
        this.deliveredAt = Instant.now();
    }

    public Long getId() { return id; }
    public Long getOrderId() { return orderId; }
    public Long getCustomerId() { return customerId; }
    public String getAddress() { return address; }
    public String getCourier() { return courier; }
    public DeliveryStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getDeliveredAt() { return deliveredAt; }
}
