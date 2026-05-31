package com.circleguard.identity.repository;

import com.circleguard.identity.model.IdentityMapping;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE;

/**
 * Verifica la persistencia y consulta por hash del IdentityMapping contra una
 * instancia real de PostgreSQL levantada con Testcontainers.
 * Las properties de vault son necesarias porque IdentityMapping usa un
 * AttributeConverter que las requiere para cifrar el campo realIdentity.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = NONE)
@Testcontainers
@TestPropertySource(properties = {
        "vault.secret=746573742d7365637265742d33322d63686172732d6c6f6e672d313233343536",
        "vault.salt=deadbeef",
        "vault.hash-salt=12345678"
})
class IdentityMappingRepositoryIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("circleguard_identity_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @Autowired
    private IdentityMappingRepository repository;

    @Test
    void savesAndRetrievesMappingByHash() {
        IdentityMapping mapping = IdentityMapping.builder()
                .realIdentity("student@university.edu")
                .identityHash("abc123hash")
                .salt("randomSalt")
                .build();

        IdentityMapping saved = repository.save(mapping);
        assertThat(saved.getAnonymousId()).isNotNull();

        Optional<IdentityMapping> found = repository.findByIdentityHash("abc123hash");
        assertThat(found).isPresent();
        assertThat(found.get().getRealIdentity()).isEqualTo("student@university.edu");
        assertThat(found.get().getSalt()).isEqualTo("randomSalt");
    }

    @Test
    void findByHashReturnsEmptyWhenNotPresent() {
        Optional<IdentityMapping> found = repository.findByIdentityHash("nonexistent-hash");
        assertThat(found).isEmpty();
    }
}
