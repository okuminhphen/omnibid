package com.omnibid.auction.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqPublisherConfig {

    @Bean
    DirectExchange auctionExchange(@Value("${omnibid.rabbitmq.refund-exchange}") String name) {
        return new DirectExchange(name, true, false);
    }

    @Bean
    Jackson2JsonMessageConverter auctionRabbitMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    RabbitTemplate refundRabbitTemplate(
            ConnectionFactory connectionFactory,
            Jackson2JsonMessageConverter auctionRabbitMessageConverter
    ) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(auctionRabbitMessageConverter);
        template.setMandatory(true);
        return template;
    }
}
