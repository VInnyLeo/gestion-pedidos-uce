package com.pedidosuce.order_service.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class NotificationClient {

    private static final Logger log = LoggerFactory.getLogger(NotificationClient.class);

    private final RestClient restClient;

    public NotificationClient(@Value("${clients.notification-url}") String baseUrl,
                              @Value("${clients.timeout-ms}") int timeoutMs) {
        this.restClient = RestClients.create(baseUrl, timeoutMs);
    }

    /**
     * Intenta enviar un aviso. Nunca lanza excepciones: si falla, devuelve false,
     * porque las notificaciones NO son una dependencia crítica.
     */
    public boolean send(Long customerId, String message) {
        try {
            restClient.post()
                    .uri("/api/notifications")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new NotificationRequest(customerId, message))
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (RestClientException ex) {
            log.warn("No se pudo enviar el aviso al cliente {}: {}", customerId, ex.getMessage());
            return false;
        }
    }

    public record NotificationRequest(Long customerId, String message) {
    }
}
