package com.pedidos360.report.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;

/**
 * Topología RabbitMQ centralizada para el dominio de auditoría de Report.
 * Los nombres deben coincidir con los usados por el microservicio productor.
 */
@Configuration
@EnableRabbit
public class RabbitMQConfig {

    public static final String EVENTS_EXCHANGE = "pedidos360.events.topic";
    public static final String DEAD_LETTER_EXCHANGE = "pedidos360.events.dlx";

    public static final String AUDIT_QUEUE = "report.audit.queue";
    public static final String AUDIT_RETRY_QUEUE = "report.audit.retry.queue";
    public static final String AUDIT_DLQ = "report.audit.dlq";

    public static final String AUDIT_ROUTING_KEY = "orders.audit";
    public static final String RETRY_ROUTING_KEY = "orders.audit.retry";
    public static final String DLQ_ROUTING_KEY = "report.audit.dead";

    private static final int MAIN_QUEUE_MAX_LENGTH = 10_000;
    private static final int RETRY_QUEUE_MAX_LENGTH = 10_000;
    private static final int DLQ_MAX_LENGTH = 50_000;

    @Bean
    public TopicExchange eventsExchange() {
        return new TopicExchange(EVENTS_EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(DEAD_LETTER_EXCHANGE, true, false);
    }

    @Bean
    public Queue auditQueue() {
        return QueueBuilder.durable(AUDIT_QUEUE)
                .withArgument("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", DLQ_ROUTING_KEY)
                .withArgument("x-max-length", MAIN_QUEUE_MAX_LENGTH)
                // Mensajes que no se procesan en 24 h pasan a la DLQ.
                .withArgument("x-message-ttl", 86_400_000)
                .build();
    }

    @Bean
    public Queue auditRetryQueue() {
        return QueueBuilder.durable(AUDIT_RETRY_QUEUE)
                // Espera 5 s antes de volver a la cola principal.
                .withArgument("x-message-ttl", 5_000)
                .withArgument("x-dead-letter-exchange", EVENTS_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", AUDIT_ROUTING_KEY)
                .withArgument("x-max-length", RETRY_QUEUE_MAX_LENGTH)
                .build();
    }

    @Bean
    public Queue auditDeadLetterQueue() {
        return QueueBuilder.durable(AUDIT_DLQ)
                .withArgument("x-max-length", DLQ_MAX_LENGTH)
                // Retención máxima de 7 días para mensajes en DLQ.
                .withArgument("x-message-ttl", 604_800_000)
                .build();
    }

    @Bean
    public Binding auditQueueBinding(
            @Qualifier("auditQueue") Queue auditQueue,
            @Qualifier("eventsExchange") TopicExchange eventsExchange) {
        return BindingBuilder.bind(auditQueue)
                .to(eventsExchange)
                .with(AUDIT_ROUTING_KEY);
    }

    @Bean
    public Binding auditRetryQueueBinding(
            @Qualifier("auditRetryQueue") Queue auditRetryQueue,
            @Qualifier("eventsExchange") TopicExchange eventsExchange) {
        return BindingBuilder.bind(auditRetryQueue)
                .to(eventsExchange)
                .with(RETRY_ROUTING_KEY);
    }

    @Bean
    public Binding auditDeadLetterQueueBinding(
            @Qualifier("auditDeadLetterQueue") Queue auditDeadLetterQueue,
            @Qualifier("deadLetterExchange") DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(auditDeadLetterQueue)
                .to(deadLetterExchange)
                .with(DLQ_ROUTING_KEY);
    }

    @Bean(name = "manualAckRabbitListenerContainerFactory")
    public SimpleRabbitListenerContainerFactory manualAckRabbitListenerContainerFactory(
            ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        factory.setPrefetchCount(10);
        // Evita la reentrega automática ilimitada; los listeners deciden ACK/NACK explícitamente.
        factory.setDefaultRequeueRejected(false);
        return factory;
    }
}
