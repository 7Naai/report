package com.pedidos360.report.rabbit;

import com.pedidos360.report.config.RabbitMQConfig;
import com.rabbitmq.client.Channel;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class RabbitDeadLetterConsumer {

    @RabbitListener(queues = RabbitMQConfig.AUDIT_DLQ)
    public void receiveDeadLetter(
            Message message,
            Channel channel) throws IOException {

        long deliveryTag =
                message.getMessageProperties().getDeliveryTag();

        String body = new String(
                message.getBody(),
                StandardCharsets.UTF_8
        );

        System.err.println(
                "[RabbitMQ][DLQ] Mensaje fallido: " + body
        );

        channel.basicAck(deliveryTag, false);
    }
}