package com.pedidosuce.order_service.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long customerId;

    @Column(nullable = false, length = 255)
    private String shippingAddress;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status = OrderStatus.CREATED;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationStatus notificationStatus = NotificationStatus.PENDING;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    protected Order() {
        // Constructor vacío requerido por JPA
    }

    public Order(Long customerId, String shippingAddress) {
        this.customerId = customerId;
        this.shippingAddress = shippingAddress;
    }

    public void addItem(OrderItem item) {
        item.assignTo(this);
        items.add(item);
        total = total.add(item.getSubtotal());
    }

    public void changeStatus(OrderStatus newStatus) {
        this.status = newStatus;
    }

    public void markNotificationSent() {
        this.notificationStatus = NotificationStatus.SENT;
    }

    public Long getId() { return id; }
    public Long getCustomerId() { return customerId; }
    public String getShippingAddress() { return shippingAddress; }
    public BigDecimal getTotal() { return total; }
    public OrderStatus getStatus() { return status; }
    public NotificationStatus getNotificationStatus() { return notificationStatus; }
    public Instant getCreatedAt() { return createdAt; }
    public List<OrderItem> getItems() { return items; }
}