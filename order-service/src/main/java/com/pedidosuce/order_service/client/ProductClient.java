package com.pedidosuce.order_service.client;

import java.math.BigDecimal;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.pedidosuce.order_service.dto.OrderItemRequest;
import com.pedidosuce.order_service.exception.ConflictException;
import com.pedidosuce.order_service.exception.ResourceNotFoundException;
import com.pedidosuce.order_service.exception.ServiceUnavailableException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

@Component
public class ProductClient {

    private final RestClient restClient;

    public ProductClient(@Value("${clients.product-url}") String baseUrl,
                         @Value("${clients.timeout-ms}") int timeoutMs) {
        this.restClient = RestClients.create(baseUrl, timeoutMs);
    }

    public List<ProductData> decreaseStock(List<OrderItemRequest> items) {
        try {
            ProductData[] products = restClient.post()
                    .uri("/api/products/decrease-stock")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new DecreaseStockRequest(items))
                    .retrieve()
                    .body(ProductData[].class);
            return List.of(products);
        } catch (HttpClientErrorException.NotFound ex) {
            throw new ResourceNotFoundException(detailOf(ex, "Uno de los productos no existe"));
        } catch (HttpClientErrorException.Conflict ex) {
            throw new ConflictException(detailOf(ex, "No hay stock suficiente"));
        } catch (ResourceAccessException | HttpServerErrorException ex) {
            throw new ServiceUnavailableException(
                    "product-service no está disponible en este momento");
        }
    }

    // Recupera el mensaje "detail" que envió product-service, para mostrárselo al cliente
    private String detailOf(HttpClientErrorException ex, String fallback) {
        try {
            ErrorBody body = ex.getResponseBodyAs(ErrorBody.class);
            return (body != null && body.detail() != null) ? body.detail() : fallback;
        } catch (RuntimeException e) {
            return fallback;
        }
    }

    public record DecreaseStockRequest(List<OrderItemRequest> items) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ProductData(Long id, String name, BigDecimal price) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ErrorBody(String detail) {
    }
}