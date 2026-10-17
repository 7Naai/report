package com.pedidos360.report.rabbit;

import com.pedidos360.report.config.RabbitMQConfig;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/** Registra en logs los mensajes que llegaron a la DLQ y confirma su recepción. */
@Component
public class RabbitDeadLetterConsumer {

    private static final Logger log = LoggerFactory.getLogger(RabbitDeadLetterConsumer.class);

    @RabbitListener(
            queues = RabbitMQConfig.AUDIT_DLQ,
            containerFactory = "manualAckRabbitListenerContainerFactory")
    public void logDeadLetter(Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        String payload = new String(message.getBody(), StandardCharsets.UTF_8);

        log.error("MENSAJE EN DLQ. queue={}, exchange={}, routingKey={}, payload={}, headers={}",
                RabbitMQConfig.AUDIT_DLQ,
                message.getMessageProperties().getReceivedExchange(),
                message.getMessageProperties().getReceivedRoutingKey(),
                payload,
                message.getMessageProperties().getHeaders());

        channel.basicAck(deliveryTag, false);
    }
}
