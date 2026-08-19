package com.omnibid.auction.service;

import com.omnibid.auction.messaging.RefundCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class RabbitMQPublisherService {

    private final RabbitTemplate refundRabbitTemplate;

    @Value("${omnibid.rabbitmq.refund-exchange}")
    private String refundExchange;

    @Value("${omnibid.rabbitmq.refund-routing-key}")
    private String refundRoutingKey;

    public void sendRefundCommand(RefundCommand command) {
        refundRabbitTemplate.convertAndSend(refundExchange, refundRoutingKey, command);
        log.info(
                "Published refund command transactionId={} userId={} auctionId={}",
                command.transactionId(),
                command.userId(),
                command.auctionId()
        );
    }
}
