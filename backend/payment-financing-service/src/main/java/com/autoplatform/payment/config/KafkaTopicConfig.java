package com.autoplatform.payment.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Value("${payment.kafka.topic.order-events}")
    private String orderEventsTopic;

    @Value("${payment.kafka.topic.car-sold-events}")
    private String carSoldEventsTopic;

    @Bean
    public NewTopic orderEventsTopic() {
        return TopicBuilder.name(orderEventsTopic).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic carSoldEventsTopic() {
        return TopicBuilder.name(carSoldEventsTopic).partitions(3).replicas(1).build();
    }
}
