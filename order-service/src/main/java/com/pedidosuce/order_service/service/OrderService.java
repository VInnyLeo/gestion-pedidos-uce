package com.pedidosuce.order_service.service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.pedidosuce.order_service.client.CustomerClient;
import com.pedidosuce.order_service.client.CustomerClient.CustomerData;
import com.pedidosuce.order_service.client.NotificationClient;
import com.pedidosuce.order_service.client.ProductClient;
import com.pedidosuce.order_service.client.ProductClient.ProductData;
import com.pedidosuce.order_service.dto.CreateOrderRequest;
import com.pedidosuce.order_service.dto.OrderItemRequest;
import com.pedidosuce.order_service.dto.OrderResponse;
import com.pedidosuce.order_service.exception.ConflictException;
import com.pedidosuce.order_service.exception.ResourceNotFoundException;
import com.pedidosuce.order_service.model.Order;
import com.pedidosuce.order_service.model.OrderItem;
import com.pedidosuce.order_service.model.OrderStatus;
import com.pedidosuce.order_service.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

    private final OrderRepository repository;
    private final CustomerClient customerClient;
    private final ProductClient productClient;
    private final NotificationClient notificationClient;

    public OrderService(OrderRepository repository,
                        CustomerClient customerClient,
                        ProductClient productClient,
                        NotificationClient notificationClient) {
        this.repository = repository;
        this.customerClient = customerClient;
        this.productClient = productClient;
        this.notificationClient = notificationClient;
    }

    @Transactional
    public OrderResponse create(CreateOrderRequest request) {
        // 1. Validar que el cliente existe (dependencia CRÍTICA)
        CustomerData customer = customerClient.getCustomer(request.customerId());

        // 2. Descontar el stock de todos los productos (dependencia CRÍTICA, todo o nada)
        List<ProductData> products = productClient.decreaseStock(request.items());
        Map<Long, ProductData> productsById = products.stream()
                .collect(Collectors.toMap(ProductData::id, Function.identity(), (a, b) -> a));

        // 3. Armar el pedido con los nombres y precios reales, y guardarlo
        Order order = new Order(request.customerId(), customer.address());
        for (OrderItemRequest item : request.items()) {
            ProductData product = productsById.get(item.productId());
            order.addItem(new OrderItem(
                    product.id(), product.name(), item.quantity(), product.price()));
        }
        Order saved = repository.save(order);

        // 4. Avisar al cliente (dependencia NO crítica: si falla, el pedido sigue)
        String message = "Tu pedido #" + saved.getId()
                + " fue registrado por un total de $" + saved.getTotal();
        if (notificationClient.send(saved.getCustomerId(), message)) {
            saved.markNotificationSent();
        }

        return OrderResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> findAll() {
        return repository.findAll().stream()
                .map(OrderResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderResponse findById(Long id) {
        return OrderResponse.from(getOrder(id));
    }

    @Transactional
    public OrderResponse updateStatus(Long id, OrderStatus newStatus) {
        Order order = getOrder(id);
        if (!order.getStatus().canChangeTo(newStatus)) {
            throw new ConflictException("No se puede cambiar el pedido " + id
                    + " de " + order.getStatus() + " a " + newStatus);
        }
        order.changeStatus(newStatus);
        return OrderResponse.from(order);
    }

    private Order getOrder(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No existe el pedido con id " + id));
    }
}