package com.pedidos360.report.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class RabbitMQConfig {

    public static final String EVENTS_EXCHANGE =
            "pedidos360.events.topic";

    public static final String DLX_EXCHANGE =
            "pedidos360.events.dlx";

    public static final String AUDIT_QUEUE =
            "report.audit.queue";

    public static final String AUDIT_DLQ =
            "report.audit.dlq";

    public static final String AUDIT_ROUTING_KEY =
            "orders.audit.#";

    public static final String DLQ_ROUTING_KEY =
            "report.audit.dead";

    @Bean
    public TopicExchange eventsExchange() {
        return new TopicExchange(EVENTS_EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(DLX_EXCHANGE, true, false);
    }
    

    @Bean
    public Queue auditQueue() {
        Map<String, Object> arguments = new HashMap<>();

        arguments.put("x-dead-letter-exchange", DLX_EXCHANGE);
        arguments.put("x-dead-letter-routing-key", DLQ_ROUTING_KEY);
        arguments.put("x-max-length", 10000);
        arguments.put("x-message-ttl", 86400000);

        return new Queue(
                AUDIT_QUEUE,
                true,
                false,
                false,
                arguments
        );
    }

    @Bean
    public Queue auditDlq() {
        return QueueBuilder.durable(AUDIT_DLQ)
                .withArgument("x-max-length", 50000)
                .withArgument("x-message-ttl", 604800000)
                .build();
    }

    @Bean
    public Binding auditBinding() {
        return BindingBuilder
                .bind(auditQueue())
                .to(eventsExchange())
                .with(AUDIT_ROUTING_KEY);
    }

    @Bean
    public Binding auditDlqBinding() {
        return BindingBuilder
                .bind(auditDlq())
                .to(deadLetterExchange())
                .with(DLQ_ROUTING_KEY);
    }
}