package com.circleguard.promotion.repository.jpa;

import com.circleguard.promotion.model.jpa.SystemSettings;
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
 * Verifica la persistencia de SystemSettings contra una instancia real de
 * PostgreSQL levantada con Testcontainers, sin mocks de Hibernate.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = NONE)
@Testcontainers
class SystemSettingsRepositoryIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("circleguard_promotion_test")
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
    private SystemSettingsRepository repository;

    @Test
    void savesAndRetrievesSettings() {
        SystemSettings settings = SystemSettings.builder()
                .encounterWindowDays(14)
                .mandatoryFenceDays(14)
                .unconfirmedFencingEnabled(true)
                .autoThresholdSeconds(3600L)
                .build();

        SystemSettings saved = repository.save(settings);
        assertThat(saved.getId()).isNotNull();

        Optional<SystemSettings> found = repository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getEncounterWindowDays()).isEqualTo(14);
        assertThat(found.get().getMandatoryFenceDays()).isEqualTo(14);
        assertThat(found.get().getUnconfirmedFencingEnabled()).isTrue();
        assertThat(found.get().getAutoThresholdSeconds()).isEqualTo(3600L);
    }

    @Test
    void getSettingsReturnsFirstRowWhenPresent() {
        SystemSettings settings = SystemSettings.builder()
                .encounterWindowDays(7)
                .mandatoryFenceDays(7)
                .unconfirmedFencingEnabled(false)
                .autoThresholdSeconds(1800L)
                .build();
        repository.save(settings);

        Optional<SystemSettings> result = repository.getSettings();
        assertThat(result).isPresent();
        assertThat(result.get().getEncounterWindowDays()).isEqualTo(7);
    }
}
