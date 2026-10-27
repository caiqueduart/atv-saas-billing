package com.saas.billing.domain;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Parâmetros de uma cobrança mensal. Substitui a lista de parâmetros soltos
 * do método legado (o {@code planType}, nunca utilizado, foi removido).
 */
public record BillingCommand(
        String customerId,
        Money basePrice,
        LocalDate signupDate,
        int overdueDays,
        String promoCode,
        boolean nonProfit,
        String paymentCardToken) {

    public BillingCommand {
        Objects.requireNonNull(customerId, "customerId");
        Objects.requireNonNull(basePrice, "basePrice");
        Objects.requireNonNull(signupDate, "signupDate");
    }

    public boolean hasPromoCode() {
        return promoCode != null && !promoCode.trim().isEmpty();
    }

    public boolean isOverdue() {
        return overdueDays > 0;
    }
}
