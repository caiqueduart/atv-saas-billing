package com.saas.billing.domain;

public enum InvoiceStatus {
    PAID,
    FAILED;

    public static InvoiceStatus fromPayment(boolean paymentSuccess) {
        return paymentSuccess ? PAID : FAILED;
    }
}
