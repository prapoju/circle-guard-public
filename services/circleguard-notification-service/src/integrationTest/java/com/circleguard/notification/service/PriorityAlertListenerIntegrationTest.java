package com.circleguard.notification.service;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.fail;

@SpringBootTest
@Tag("integration")
class PriorityAlertListenerIntegrationTest {

    private static final String TOPIC = "alert.priority";
    private static final String GROUP = "notification-priority-group";

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Test
    void handlePriorityAlert_consumesMessageFromKafka() throws Exception {
        String payload = "{\"eventType\":\"CONFIRMED_CASE\",\"affectedCount\":3}";

        // Enviar el mensaje y obtener el offset exacto donde fue escrito
        long sentOffset = kafkaTemplate
                .send(TOPIC, payload)
                .get()
                .getRecordMetadata()
                .offset();

        // Verificar que el consumer group avanza su offset más allá del mensaje enviado
        // Esto confirma que el listener consumió y procesó el mensaje
        TopicPartition tp = new TopicPartition(TOPIC, 0);

        try (AdminClient admin = AdminClient.create(Map.of(
                AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092"))) {

            long deadline = System.currentTimeMillis() + 15_000;

            while (System.currentTimeMillis() < deadline) {
                Map<TopicPartition, OffsetAndMetadata> offsets = admin
                        .listConsumerGroupOffsets(GROUP)
                        .partitionsToOffsetAndMetadata()
                        .get();

                OffsetAndMetadata committed = offsets.get(tp);
                if (committed != null && committed.offset() > sentOffset) {
                    return; // El listener consumió el mensaje
                }
                Thread.sleep(500);
            }
        }

        fail("El listener no consumió el mensaje en 15 segundos");
    }
}
