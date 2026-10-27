package com.saas.billing.adapter;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.saas.billing.domain.InvoiceStatus;
import com.saas.billing.domain.Money;
import org.junit.jupiter.api.Test;

class SmtpNotificationServiceTest {

    @Test
    void mensagemRefleteOStatusDaFatura() {
        assertTrue(SmtpNotificationService.messageFor(Money.of("50.00"), InvoiceStatus.PAID)
                .contains("R$ 50.00 foi paga"));
        assertTrue(SmtpNotificationService.messageFor(Money.of("50.00"), InvoiceStatus.FAILED)
                .contains("Não conseguimos cobrar"));
    }
}
