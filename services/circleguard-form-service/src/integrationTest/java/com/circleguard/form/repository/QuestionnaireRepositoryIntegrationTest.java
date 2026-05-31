package com.circleguard.form.repository;

import com.circleguard.form.model.Questionnaire;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE;

/**
 * Verifica el repositorio de cuestionarios contra una instancia real de
 * PostgreSQL levantada con Testcontainers, incluyendo la query custom
 * findFirstByIsActiveTrueOrderByVersionDesc.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = NONE)
@Testcontainers
class QuestionnaireRepositoryIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("circleguard_form_test")
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
    private QuestionnaireRepository repository;

    @Test
    void persistsAndRetrievesQuestionnaire() {
        Questionnaire q = Questionnaire.builder()
                .title("Encuesta diaria")
                .description("Sintomas de las ultimas 24h")
                .version(1)
                .isActive(true)
                .build();

        Questionnaire saved = repository.save(q);
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();

        Optional<Questionnaire> found = repository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getTitle()).isEqualTo("Encuesta diaria");
    }

    @Test
    void findFirstActiveReturnsHighestVersion() {
        repository.save(Questionnaire.builder()
                .title("v1").version(1).isActive(true).build());
        repository.save(Questionnaire.builder()
                .title("v2").version(2).isActive(true).build());
        repository.save(Questionnaire.builder()
                .title("v3-inactive").version(3).isActive(false).build());

        Optional<Questionnaire> active = repository.findFirstByIsActiveTrueOrderByVersionDesc();
        assertThat(active).isPresent();
        assertThat(active.get().getVersion()).isEqualTo(2);
        assertThat(active.get().getTitle()).isEqualTo("v2");
    }
}
