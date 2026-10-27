package com.saas.billing.port;

/** Falha de infraestrutura (banco, SMTP, gateway) propagada sem ser mascarada. */
public class InfrastructureException extends RuntimeException {

    public InfrastructureException(String message, Throwable cause) {
        super(message, cause);
    }
}
