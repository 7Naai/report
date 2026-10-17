package com.example.orders.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * EJEMPLO para el microservicio orders (no copiar dentro de report).
 * Publica el mismo contrato JSON que espera report.RabbitAuditEventConsumer.
 */
@Service
public class OrderAuditEventPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    private final String exchange;
    private final String routingKey;

    public OrderAuditEventPublisher(
            RabbitTemplate rabbitTemplate,
            ObjectMapper objectMapper,
            @Value("${app.rabbitmq.events-exchange:pedidos360.events.topic}") String exchange,
            @Value("${app.rabbitmq.audit-routing-key:orders.audit}") String routingKey) {
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
        this.exchange = exchange;
        this.routingKey = routingKey;
    }

    public void publish(String orderId, String eventType, String actor) {
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("eventId", UUID.randomUUID().toString());
        event.put("type", eventType);
        event.put("orderId", orderId);
        event.put("actor", actor == null || actor.isBlank() ? "orders-service" : actor);
        event.put("timestamp", LocalDateTime.now().toString());

        try {
            String json = objectMapper.writeValueAsString(event);
            rabbitTemplate.convertAndSend(exchange, routingKey, json);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("No se pudo serializar el evento de auditoría", ex);
        }
    }
}
