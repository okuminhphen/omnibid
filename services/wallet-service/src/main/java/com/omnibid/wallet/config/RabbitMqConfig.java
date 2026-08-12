package com.omnibid.wallet.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    @Bean
    DirectExchange refundExchange(@Value("${omnibid.rabbitmq.refund-exchange}") String name) {
        return new DirectExchange(name, true, false);
    }

    @Bean
    DirectExchange refundDeadLetterExchange(@Value("${omnibid.rabbitmq.refund-dlx}") String name) {
        return new DirectExchange(name, true, false);
    }

    @Bean
    Queue refundQueue(
            @Value("${omnibid.rabbitmq.refund-queue}") String queue,
            @Value("${omnibid.rabbitmq.refund-dlx}") String deadLetterExchange,
            @Value("${omnibid.rabbitmq.refund-dead-letter-routing-key}") String deadLetterRoutingKey
    ) {
        return QueueBuilder.durable(queue)
                .deadLetterExchange(deadLetterExchange)
                .deadLetterRoutingKey(deadLetterRoutingKey)
                .build();
    }

    @Bean
    Queue refundDeadLetterQueue(@Value("${omnibid.rabbitmq.refund-dlq}") String queue) {
        return QueueBuilder.durable(queue).build();
    }

    @Bean
    Binding refundBinding(
            Queue refundQueue,
            DirectExchange refundExchange,
            @Value("${omnibid.rabbitmq.refund-routing-key}") String routingKey
    ) {
        return BindingBuilder.bind(refundQueue).to(refundExchange).with(routingKey);
    }

    @Bean
    Binding refundDeadLetterBinding(
            Queue refundDeadLetterQueue,
            DirectExchange refundDeadLetterExchange,
            @Value("${omnibid.rabbitmq.refund-dead-letter-routing-key}") String deadLetterRoutingKey
    ) {
        return BindingBuilder.bind(refundDeadLetterQueue)
                .to(refundDeadLetterExchange)
                .with(deadLetterRoutingKey);
    }

    @Bean
    Jackson2JsonMessageConverter rabbitJsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
