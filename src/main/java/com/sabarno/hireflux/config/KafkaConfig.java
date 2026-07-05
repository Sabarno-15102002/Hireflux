package com.sabarno.hireflux.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.annotation.EnableKafkaRetryTopic;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.retrytopic.RetryTopicConfiguration;
import org.springframework.kafka.retrytopic.RetryTopicConfigurationBuilder;
import org.springframework.kafka.transaction.KafkaTransactionManager;

import com.sabarno.hireflux.exception.NonRetryableProcessingException;


@Configuration
@EnableKafka
@EnableKafkaRetryTopic
public class KafkaConfig {

    @Value("${kafka.topic.resume-uploaded.name}")
    private String resumeUploadTopicName;
    @Value("${kafka.topic.resume-uploaded-dlt.name}")
    private String resumeUploadDltTopicName;
    private static final Integer TOPIC_REPLICATION_FACTOR = 1;
    private static final Integer TOPIC_PARTITION_COUNT = 3;

    @Bean
    KafkaTemplate<String, Object> kafkaTemplate(ProducerFactory<String, Object> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }

    @Bean
    KafkaTransactionManager<String, Object> kafkaTransactionManager(
            ProducerFactory<String, Object> producerFactory) {

        return new KafkaTransactionManager<>(producerFactory);
    }

    @Bean
    NewTopic resumeUploadedTopic(){
        return TopicBuilder.name(resumeUploadTopicName)
                .partitions(TOPIC_PARTITION_COUNT)
                .replicas(TOPIC_REPLICATION_FACTOR)
                .build();
    }

    @Bean
    NewTopic resumeUploadedDltTopic(){
        return TopicBuilder.name(resumeUploadDltTopicName)
                .partitions(TOPIC_PARTITION_COUNT)
                .replicas(TOPIC_REPLICATION_FACTOR)
                .build();
    }
}
