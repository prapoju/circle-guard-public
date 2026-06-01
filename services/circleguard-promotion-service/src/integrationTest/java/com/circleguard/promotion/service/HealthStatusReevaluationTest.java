package com.circleguard.promotion.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.kafka.core.KafkaTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@Tag("integration")
public class HealthStatusReevaluationTest {

    @Autowired
    private HealthStatusService healthStatusService;

    @Autowired
    private Neo4jClient neo4jClient;

    @MockBean
    private KafkaTemplate<String, Object> kafkaTemplate;

    @BeforeEach
    void setup() {
        neo4jClient.query("MATCH (n) DETACH DELETE n").run();
    }

    @Test
    void testSingleRelease() {
        createNode("A", "CONFIRMED");
        createNode("B", "SUSPECT");
        createRelationship("A", "B");

        healthStatusService.resolveStatus("A");

        assertEquals("ACTIVE", getStatus("B"));
    }

    @Test
    void testBlockedRelease() {
        createNode("A", "CONFIRMED");
        createNode("B", "SUSPECT");
        createNode("C", "CONFIRMED");
        createRelationship("A", "B");
        createRelationship("C", "B");

        healthStatusService.resolveStatus("A");

        assertEquals("SUSPECT", getStatus("B"));
    }

    @Test
    void testMultiHopRelease() {
        createNode("A", "CONFIRMED");
        createNode("B", "SUSPECT");
        createNode("C", "PROBABLE");
        createRelationship("A", "B");
        createRelationship("B", "C");

        healthStatusService.resolveStatus("A");

        assertEquals("ACTIVE", getStatus("B"));
        assertEquals("ACTIVE", getStatus("C"));
    }

    @Test
    void testPartialReleaseInMesh() {
        createNode("A", "CONFIRMED");
        createNode("B", "SUSPECT");
        createNode("C", "PROBABLE");
        createNode("D", "SUSPECT");
        createRelationship("A", "B");
        createRelationship("B", "C");
        createRelationship("D", "C");

        healthStatusService.resolveStatus("A");

        assertEquals("ACTIVE", getStatus("B"));
        assertEquals("PROBABLE", getStatus("C"));
    }

    private void createNode(String id, String status) {
        neo4jClient.query("CREATE (:User {anonymousId: $id, status: $status})")
                .bind(id).to("id")
                .bind(status).to("status")
                .run();
    }

    private void createRelationship(String id1, String id2) {
        neo4jClient.query("MATCH (u1:User {anonymousId: $id1}), (u2:User {anonymousId: $id2}) " +
                "CREATE (u1)-[:ENCOUNTERED {startTime: timestamp()}]->(u2)")
                .bind(id1).to("id1")
                .bind(id2).to("id2")
                .run();
    }

    private String getStatus(String id) {
        return neo4jClient.query("MATCH (u:User {anonymousId: $id}) RETURN u.status as status")
                .bind(id).to("id")
                .fetchAs(String.class).one().orElse("NOT_FOUND");
    }
}
