package com.saas.billing.adapter;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.saas.billing.domain.Money;
import org.junit.jupiter.api.Test;

class CardTokenPaymentGatewayTest {

    private final CardTokenPaymentGateway gateway = new CardTokenPaymentGateway();

    @Test
    void aceitaTokensComPrefixoTok() {
        assertTrue(gateway.charge("tok_abc", Money.of("10.00")));
    }

    @Test
    void recusaTokenNuloOuInvalido() {
        assertFalse(gateway.charge(null, Money.of("10.00")));
        assertFalse(gateway.charge("card_abc", Money.of("10.00")));
    }
}
