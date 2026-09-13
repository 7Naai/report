package com.pedidos360.report.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pedidos360.report.model.AuditEvent;
import com.pedidos360.report.repository.AuditEventRepository;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

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
            topics = "orders.events",
            groupId = "report-service-group"
    )
    public void consumeOrderEvent(String message) {

        try {

            JsonNode json = objectMapper.readTree(message);

            AuditEvent event = new AuditEvent();

            event.setEventId(
                    getText(json, "eventId")
            );

            event.setEventType(
                    getText(json, "type")
            );

            event.setOrderId(
                    getText(json, "orderId")
            );

            event.setActor(
                    getText(json, "actor")
            );

            event.setDetails(message);

            String timestamp =
                    getText(json, "timestamp");

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