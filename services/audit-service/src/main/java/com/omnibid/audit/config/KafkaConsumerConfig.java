package com.omnibid.audit.config;

import com.omnibid.audit.messaging.BidPlacedEvent;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableKafka
public class KafkaConsumerConfig {

    @Bean
    ConsumerFactory<String, BidPlacedEvent> bidConsumerFactory(KafkaProperties kafkaProperties) {
        Map<String, Object> properties = new HashMap<>(kafkaProperties.buildConsumerProperties(null));
        JsonDeserializer<BidPlacedEvent> valueDeserializer =
                new JsonDeserializer<>(BidPlacedEvent.class, false);
        valueDeserializer.addTrustedPackages("com.omnibid.audit.messaging");
        return new DefaultKafkaConsumerFactory<>(
                properties,
                new StringDeserializer(),
                valueDeserializer
        );
    }

    @Bean
    ConcurrentKafkaListenerContainerFactory<String, BidPlacedEvent> bidKafkaListenerContainerFactory(
            ConsumerFactory<String, BidPlacedEvent> bidConsumerFactory
    ) {
        ConcurrentKafkaListenerContainerFactory<String, BidPlacedEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(bidConsumerFactory);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);
        return factory;
    }
}
