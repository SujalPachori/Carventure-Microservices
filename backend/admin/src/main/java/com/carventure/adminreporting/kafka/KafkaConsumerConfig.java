package com.carventure.adminreporting.kafka;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;

import java.util.HashMap;
import java.util.Map;

@EnableKafka
@Configuration
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    // one consumer group for the whole Reporting service - every listener
    // below shares it so each event is processed exactly once per instance
    private static final String GROUP_ID = "reporting-service";

    @Bean
    public ConsumerFactory<String, Object> consumerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, GROUP_ID);
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        config.put(ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS, StringDeserializer.class);
        config.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JacksonJsonDeserializer.class);
        config.put(JacksonJsonDeserializer.TRUSTED_PACKAGES, "com.carventure.adminreporting.event");
        // each producer service serializes with its own event package in the __TypeId__ header
        // (e.g. com.carventure.carservice.event.CarListedEvent) since nobody shares a DTO
        // library - map each of those back to Reporting's own local copy of the same shape
        config.put(JacksonJsonDeserializer.TYPE_MAPPINGS,
            "com.carventure.car.carkafka.event.CarListedEvent:com.carventure.adminreporting.event.CarListedEvent,"
            + "com.autoplatform.payment.paymentkafka.event.OrderCreatedEvent:com.carventure.adminreporting.event.OrderCreatedEvent,"
            + "com.autoplatform.payment.paymentkafka.event.OrderCompletedEvent:com.carventure.adminreporting.event.OrderCompletedEvent,"
            + "com.cardrive.scheduling.schedulingkafka.event.TestDriveScheduledEvent:com.carventure.adminreporting.event.TestDriveScheduledEvent,"
            + "com.cardrive.scheduling.schedulingkafka.event.TestDriveCompletedEvent:com.carventure.adminreporting.event.TestDriveCompletedEvent,"
            + "com.carventure.usermanagement.userkafka.event.UserRegisteredEvent:com.carventure.adminreporting.event.UserRegisteredEvent");
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return new DefaultKafkaConsumerFactory<>(config);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        return factory;
    }
}
