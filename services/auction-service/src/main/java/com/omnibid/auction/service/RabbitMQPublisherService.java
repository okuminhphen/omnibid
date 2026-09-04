package com.omnibid.auction.service;

import com.omnibid.auction.messaging.RefundCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

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
        CorrelationData correlationData = new CorrelationData(command.transactionId().toString());
        refundRabbitTemplate.convertAndSend(
                refundExchange,
                refundRoutingKey,
                command,
                correlationData
        );
        try {
            CorrelationData.Confirm confirm = correlationData.getFuture().get(5, TimeUnit.SECONDS);
            if (!confirm.isAck()) {
                throw new IllegalStateException("RabbitMQ rejected refund command: " + confirm.getReason());
            }
            if (correlationData.getReturned() != null) {
                throw new IllegalStateException("RabbitMQ returned unroutable refund command");
            }
            log.info(
                    "Published refund command transactionId={} userId={} auctionId={}",
                    command.transactionId(),
                    command.userId(),
                    command.auctionId()
            );
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("RabbitMQ publish was interrupted", exception);
        } catch (ExecutionException | TimeoutException exception) {
            throw new IllegalStateException(
                    "Could not confirm refund command " + command.transactionId(),
                    exception
            );
        }
    }
}
