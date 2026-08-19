package com.omnibid.identity.messaging;

import com.omnibid.identity.config.IdentityProperties;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
@RequiredArgsConstructor
public class IdentityKafkaConfig {

    private final IdentityProperties properties;

    @Bean
    NewTopic identityEventsTopic() {
        return TopicBuilder.name(properties.kafka().identityTopic())
                .partitions(3)
                .replicas(1)
                .build();
    }
}
