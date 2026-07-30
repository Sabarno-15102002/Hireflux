package com.sabarno.hireflux.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.transaction.KafkaAwareTransactionManager;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class KafkaConfigTest {

        private KafkaConfig kafkaConfig;

        private ProducerFactory<String, Object> producerFactory;

        @SuppressWarnings("unchecked")
        @BeforeEach
        void setUp() {

                kafkaConfig = new KafkaConfig();

                producerFactory = mock(ProducerFactory.class);

                ReflectionTestUtils.setField(
                                kafkaConfig,
                                "resumeUploadTopicName",
                                "resume-uploaded");

                ReflectionTestUtils.setField(
                                kafkaConfig,
                                "resumeUploadDltTopicName",
                                "resume-uploaded-dlt");
        }

        @Test
        void kafkaTemplate_shouldCreateKafkaTemplate() {

                KafkaTemplate<String, Object> kafkaTemplate = kafkaConfig.kafkaTemplate(producerFactory);

                assertNotNull(kafkaTemplate);
        }

        @Test
        void kafkaTransactionManager_shouldCreateTransactionManager() {

                when(producerFactory.transactionCapable())
                                .thenReturn(true);
                KafkaAwareTransactionManager<String, Object> manager = kafkaConfig
                                .kafkaTransactionManager(producerFactory);

                assertNotNull(manager);
        }

        @Test
        void resumeUploadedTopic_shouldCreateTopicWithCorrectConfiguration() {

                NewTopic topic = kafkaConfig.resumeUploadedTopic();

                assertEquals(
                                "resume-uploaded",
                                topic.name());

                assertEquals(
                                3,
                                topic.numPartitions());

                assertEquals(
                                Short.valueOf((short) 1),
                                topic.replicationFactor());
        }

        @Test
        void resumeUploadedDltTopic_shouldCreateTopicWithCorrectConfiguration() {

                NewTopic topic = kafkaConfig.resumeUploadedDltTopic();

                assertEquals(
                                "resume-uploaded-dlt",
                                topic.name());

                assertEquals(
                                3,
                                topic.numPartitions());

                assertEquals(
                                Short.valueOf((short) 1),
                                topic.replicationFactor());
        }
}