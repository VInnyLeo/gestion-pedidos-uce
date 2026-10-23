package com.pedidosuce.order_service.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.pedidosuce.order_service.exception.ResourceNotFoundException;
import com.pedidosuce.order_service.exception.ServiceUnavailableException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

@Component
public class CustomerClient {

    private final RestClient restClient;

    public CustomerClient(@Value("${clients.customer-url}") String baseUrl,
                          @Value("${clients.timeout-ms}") int timeoutMs) {
        this.restClient = RestClients.create(baseUrl, timeoutMs);
    }

    public CustomerData getCustomer(Long customerId) {
        try {
            return restClient.get()
                    .uri("/api/customers/{id}", customerId)
                    .retrieve()
                    .body(CustomerData.class);
        } catch (HttpClientErrorException.NotFound ex) {
            throw new ResourceNotFoundException("No existe el cliente con id " + customerId);
        } catch (ResourceAccessException | HttpServerErrorException ex) {
            throw new ServiceUnavailableException(
                    "customer-service no está disponible en este momento");
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CustomerData(Long id, String name, String email, String address) {
    }
}