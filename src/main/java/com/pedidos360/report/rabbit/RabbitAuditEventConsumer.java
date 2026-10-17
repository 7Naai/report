package com.pedidos360.report.rabbit;

import com.pedidos360.report.config.RabbitMQConfig;
import com.pedidos360.report.service.AuditEventService;
import com.pedidos360.report.service.InvalidAuditEventException;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/** Consume eventos de auditoría con ACK/NACK manual y reintentos acotados. */
@Component
public class RabbitAuditEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(RabbitAuditEventConsumer.class);
    private static final String RETRY_HEADER = "x-retry-count";
    private static final int MAX_RETRIES = 3;

    private final AuditEventService auditEventService;
    private final RabbitTemplate rabbitTemplate;

    public RabbitAuditEventConsumer(
            AuditEventService auditEventService,
            RabbitTemplate rabbitTemplate) {
        this.auditEventService = auditEventService;
        this.rabbitTemplate = rabbitTemplate;
    }

    @RabbitListener(
            queues = RabbitMQConfig.AUDIT_QUEUE,
            containerFactory = "manualAckRabbitListenerContainerFactory")
    public void consume(Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        String payload = new String(message.getBody(), StandardCharsets.UTF_8);

        try {
            auditEventService.saveFromJson(payload);
            channel.basicAck(deliveryTag, false);
            log.info("Evento RabbitMQ guardado en auditoría. routingKey={}, deliveryTag={}",
                    message.getMessageProperties().getReceivedRoutingKey(), deliveryTag);
        } catch (InvalidAuditEventException ex) {
            // JSON o timestamp inválido: no se arregla con reintentos.
            log.error("Evento de auditoría inválido; se enviará a la DLQ. payload={}", payload, ex);
            channel.basicNack(deliveryTag, false, false);
        } catch (DataAccessException ex) {
            // Error de persistencia: reintento con espera; después de 3 intentos, DLQ.
            int retriesAlreadyUsed = getRetryCount(message);
            if (retriesAlreadyUsed < MAX_RETRIES) {
                int nextRetry = retriesAlreadyUsed + 1;
                rabbitTemplate.convertAndSend(
                        RabbitMQConfig.EVENTS_EXCHANGE,
                        RabbitMQConfig.RETRY_ROUTING_KEY,
                        payload,
                        outgoing -> {
                            outgoing.getMessageProperties().setHeader(RETRY_HEADER, nextRetry);
                            return outgoing;
                        });
                channel.basicAck(deliveryTag, false);
                log.warn("Fallo de persistencia; reintento {}/{} programado para auditoría",
                        nextRetry, MAX_RETRIES, ex);
            } else {
                log.error("Persistencia falló tras {} reintentos; se enviará a la DLQ. payload={}",
                        MAX_RETRIES, payload, ex);
                channel.basicNack(deliveryTag, false, false);
            }
        } catch (Exception ex) {
            // Error no recuperable para este consumidor: enviar a DLQ, sin requeue.
            log.error("Error no recuperable procesando evento; se enviará a la DLQ. payload={}", payload, ex);
            channel.basicNack(deliveryTag, false, false);
        }
    }

    private int getRetryCount(Message message) {
        Object value = message.getMessageProperties().getHeaders().get(RETRY_HEADER);
        if (value instanceof Number number) {
            return Math.max(0, number.intValue());
        }
        if (value != null) {
            try {
                return Math.max(0, Integer.parseInt(value.toString()));
            } catch (NumberFormatException ignored) {
                log.warn("Header {} inválido; se tratará como primer intento", RETRY_HEADER);
            }
        }
        return 0;
    }
}
