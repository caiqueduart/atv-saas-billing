package com.saas.billing.domain;

import java.util.Objects;

public record Customer(String id, String email, String status) {

    private static final String ACTIVE = "ACTIVE";

    public Customer {
        Objects.requireNonNull(id, "id");
    }

    public boolean isActive() {
        return ACTIVE.equalsIgnoreCase(status);
    }
}
