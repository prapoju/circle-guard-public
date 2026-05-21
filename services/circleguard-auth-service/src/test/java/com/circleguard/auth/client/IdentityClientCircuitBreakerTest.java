package com.circleguard.auth.client;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.ResourceAccessException;

import java.time.Duration;
import java.util.UUID;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifica las transiciones de estado del circuit breaker hacia identity-service.
 * Usa una instancia programatica con valores reducidos (minimumNumberOfCalls=2,
 * waitDurationInOpenState=500ms) para correr en milisegundos sin tocar el
 * application.yml.
 */
class IdentityClientCircuitBreakerTest {

    private CircuitBreaker circuitBreaker;
    private Supplier<UUID> failingCall;
    private Supplier<UUID> successCall;

    @BeforeEach
    void setUp() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
            .failureRateThreshold(50)
            .slidingWindowSize(2)
            .minimumNumberOfCalls(2)
            .waitDurationInOpenState(Duration.ofMillis(500))
            .permittedNumberOfCallsInHalfOpenState(1)
            .build();

        CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(config);
        circuitBreaker = registry.circuitBreaker("identityService");

        failingCall = CircuitBreaker.decorateSupplier(circuitBreaker,
            () -> { throw new ResourceAccessException("identity-service down"); });

        successCall = CircuitBreaker.decorateSupplier(circuitBreaker, UUID::randomUUID);
    }

    @Test
    void cb_startsInClosedState() {
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
    }

    @Test
    void cb_transitionsToOpenAfterRepeatedFailures() {
        // Dos llamadas fallidas alcanzan minimumNumberOfCalls=2 con 100% de fallos
        assertThatThrownBy(failingCall::get).isInstanceOf(ResourceAccessException.class);
        assertThatThrownBy(failingCall::get).isInstanceOf(ResourceAccessException.class);

        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);

        // Las siguientes llamadas son cortadas sin invocar al downstream
        assertThatThrownBy(failingCall::get).isInstanceOf(CallNotPermittedException.class);
    }

    @Test
    void cb_recoversToClosedAfterHalfOpenSuccess() throws InterruptedException {
        // Forzar OPEN con dos fallos
        assertThatThrownBy(failingCall::get).isInstanceOf(ResourceAccessException.class);
        assertThatThrownBy(failingCall::get).isInstanceOf(ResourceAccessException.class);
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);

        // Esperar a que termine waitDurationInOpenState (500ms)
        Thread.sleep(600);

        // La primera llamada permitida pasa el CB a HALF_OPEN.
        // Como es exitosa, vuelve a CLOSED.
        UUID result = successCall.get();
        assertThat(result).isNotNull();
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
    }

    @Test
    void cb_returnsToOpenIfHalfOpenCallFails() throws InterruptedException {
        // Forzar OPEN
        assertThatThrownBy(failingCall::get).isInstanceOf(ResourceAccessException.class);
        assertThatThrownBy(failingCall::get).isInstanceOf(ResourceAccessException.class);

        Thread.sleep(600);

        // Llamada en HALF_OPEN que falla -> CB vuelve a OPEN
        assertThatThrownBy(failingCall::get).isInstanceOf(ResourceAccessException.class);
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
    }
}
