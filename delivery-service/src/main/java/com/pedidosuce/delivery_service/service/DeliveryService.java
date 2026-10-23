package com.pedidosuce.delivery_service.service;

import java.util.List;

import com.pedidosuce.delivery_service.client.NotificationClient;
import com.pedidosuce.delivery_service.client.OrderClient;
import com.pedidosuce.delivery_service.client.OrderClient.OrderData;
import com.pedidosuce.delivery_service.dto.CreateDeliveryRequest;
import com.pedidosuce.delivery_service.exception.ConflictException;
import com.pedidosuce.delivery_service.exception.ResourceNotFoundException;
import com.pedidosuce.delivery_service.model.Delivery;
import com.pedidosuce.delivery_service.model.DeliveryStatus;
import com.pedidosuce.delivery_service.repository.DeliveryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeliveryService {

    private final DeliveryRepository repository;
    private final OrderClient orderClient;
    private final NotificationClient notificationClient;

    public DeliveryService(DeliveryRepository repository,
                           OrderClient orderClient,
                           NotificationClient notificationClient) {
        this.repository = repository;
        this.orderClient = orderClient;
        this.notificationClient = notificationClient;
    }

    @Transactional(readOnly = true)
    public List<Delivery> findAll() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public Delivery findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No existe la entrega con id " + id));
    }

    @Transactional
    public Delivery create(CreateDeliveryRequest request) {
        // 1. Pedir a order-service que pase el pedido a IN_DELIVERY (dependencia CRÍTICA).
        //    order-service comprueba que el pedido exista y que esté en CREATED.
        OrderData order = orderClient.changeStatus(request.orderId(), "IN_DELIVERY");

        // 2. Registrar la entrega con la dirección y el cliente que devolvió order-service
        Delivery delivery = new Delivery(
                order.id(), order.customerId(), order.shippingAddress(), request.courier());
        return repository.save(delivery);
    }

    @Transactional
    public Delivery updateStatus(Long id, DeliveryStatus newStatus) {
        Delivery delivery = findById(id);
        if (!delivery.getStatus().canChangeTo(newStatus)) {
            throw new ConflictException("No se puede cambiar la entrega " + id
                    + " de " + delivery.getStatus() + " a " + newStatus);
        }

        // 1. Actualizar el pedido en order-service (CRÍTICO): si falla, la entrega no cambia
        orderClient.changeStatus(delivery.getOrderId(), "DELIVERED");

        // 2. Marcar la entrega como completada
        delivery.markDelivered();

        // 3. Avisar al cliente (NO crítico)
        notificationClient.send(delivery.getCustomerId(),
                "Tu pedido #" + delivery.getOrderId() + " fue entregado. ¡Gracias por tu compra!");

        return delivery;
    }
}
