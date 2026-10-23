package com.pedidosuce.delivery_service.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.pedidosuce.delivery_service.exception.ConflictException;
import com.pedidosuce.delivery_service.exception.ResourceNotFoundException;
import com.pedidosuce.delivery_service.exception.ServiceUnavailableException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

@Component
public class OrderClient {

    private final RestClient restClient;

    public OrderClient(@Value("${clients.order-url}") String baseUrl,
                       @Value("${clients.timeout-ms}") int timeoutMs) {
        this.restClient = RestClients.create(baseUrl, timeoutMs);
    }

    public OrderData changeStatus(Long orderId, String newStatus) {
        try {
            return restClient.patch()
                    .uri("/api/orders/{id}/status", orderId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new StatusRequest(newStatus))
                    .retrieve()
                    .body(OrderData.class);
        } catch (HttpClientErrorException.NotFound ex) {
            throw new ResourceNotFoundException("No existe el pedido con id " + orderId);
        } catch (HttpClientErrorException.Conflict ex) {
            throw new ConflictException(detailOf(ex, "El pedido no admite ese cambio de estado"));
        } catch (ResourceAccessException | HttpServerErrorException ex) {
            throw new ServiceUnavailableException(
                    "order-service no está disponible en este momento");
        }
    }

    private String detailOf(HttpClientErrorException ex, String fallback) {
        try {
            ErrorBody body = ex.getResponseBodyAs(ErrorBody.class);
            return (body != null && body.detail() != null) ? body.detail() : fallback;
        } catch (RuntimeException e) {
            return fallback;
        }
    }

    public record StatusRequest(String status) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record OrderData(Long id, Long customerId, String shippingAddress, String status) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ErrorBody(String detail) {
    }
}
