package com.circleguard.auth.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Lanzada cuando el circuit breaker hacia identity-service esta abierto
 * o la llamada subyacente fallo. Spring la mapea a HTTP 503.
 */
@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
public class IdentityServiceUnavailableException extends RuntimeException {
    public IdentityServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
