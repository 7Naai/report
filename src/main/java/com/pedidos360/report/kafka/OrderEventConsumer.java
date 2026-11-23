package com.pedidos360.report.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pedidos360.report.model.AuditEvent;
import com.pedidos360.report.repository.AuditEventRepository;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
public class OrderEventConsumer {

    private final AuditEventRepository auditEventRepository;
    private final ObjectMapper objectMapper;

    public OrderEventConsumer(
            AuditEventRepository auditEventRepository,
            ObjectMapper objectMapper) {
        this.auditEventRepository = auditEventRepository;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(
            topics = "orders.audit.v1",
            groupId = "report-service-group"
    )
    public void consumeOrderEvent(String message) {

        try {

            JsonNode json = objectMapper.readTree(message);

            AuditEvent event = new AuditEvent();

            String eventId = getText(json, "eventId");
            event.setEventId(eventId != null ? eventId : UUID.randomUUID().toString());

            String status = getText(json, "status");
            String type = getText(json, "type");
            event.setEventType(type != null ? type : (status != null ? "ORDER_" + status : "ORDER_EVENT"));

            String orderId = getText(json, "id");
            event.setOrderId(orderId != null ? orderId : getText(json, "orderId"));

            String actor = getText(json, "userId");
            if (actor == null) {
                actor = getText(json, "customerId");
            }
            if (actor == null) {
                actor = getText(json, "actor");
            }
            event.setActor(actor);

            event.setDetails(message);

            String timestamp = getText(json, "createdAt");
            if (timestamp == null) {
                timestamp = getText(json, "timestamp");
            }

            if (timestamp != null && !timestamp.isBlank()) {
                event.setTimestamp(
                        LocalDateTime.parse(timestamp)
                );
            } else {
                event.setTimestamp(
                        LocalDateTime.now()
                );
            }

            auditEventRepository.save(event);
            System.out.println("Evento guardado en report para Order ID: " + event.getOrderId());

        } catch (Exception e) {

            System.err.println(
                    "Error procesando evento Kafka: "
                            + e.getMessage()
            );
        }
    }

    private String getText(
            JsonNode json,
            String field) {

        JsonNode node = json.get(field);

        return node != null && !node.isNull()
                ? node.asText()
                : null;
    }
}