package com.pedidos360.report.rabbit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pedidos360.report.model.AuditEvent;
import com.pedidos360.report.repository.AuditEventRepository;
import com.pedidos360.report.config.RabbitMQConfig;

import com.rabbitmq.client.Channel;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

@Component
public class RabbitAuditEventConsumer {

    private final AuditEventRepository repository;
    private final ObjectMapper objectMapper;

    public RabbitAuditEventConsumer(
            AuditEventRepository repository,
            ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @RabbitListener(queues = RabbitMQConfig.AUDIT_QUEUE)
    public void receive(Message message, Channel channel)
            throws IOException {

        long deliveryTag =
                message.getMessageProperties().getDeliveryTag();

        String body = new String(
                message.getBody(),
                StandardCharsets.UTF_8
        );

        try {
            JsonNode json = objectMapper.readTree(body);

            String eventId = getText(json, "eventId");
            String type = getText(json, "type");
            String orderId = getText(json, "orderId");

            if (eventId == null || eventId.isBlank()
                    || type == null || type.isBlank()
                    || orderId == null || orderId.isBlank()) {
                throw new IllegalArgumentException(
                        "Faltan eventId, type u orderId"
                );
            }

            AuditEvent event = new AuditEvent();

            event.setEventId(eventId);
            event.setEventType(type);
            event.setOrderId(orderId);
            event.setActor(getText(json, "actor"));
            event.setDetails(body);

            String timestamp = getText(json, "timestamp");

            if (timestamp == null || timestamp.isBlank()) {
                event.setTimestamp(LocalDateTime.now());
            } else {
                event.setTimestamp(LocalDateTime.parse(timestamp));
            }

            repository.save(event);

            channel.basicAck(deliveryTag, false);

            System.out.println(
                    "[RabbitMQ][ACK] Auditoría guardada: " + body
            );

        } catch (Exception e) {

            System.err.println(
                    "[RabbitMQ][NACK] Mensaje enviado a DLQ: "
                            + e.getMessage()
            );

            channel.basicNack(deliveryTag, false, false);
        }
    }

    private String getText(JsonNode json, String field) {
        JsonNode node = json.get(field);

        return node == null || node.isNull()
                ? null
                : node.asText();
    }
}