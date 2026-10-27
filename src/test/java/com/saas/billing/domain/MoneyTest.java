package com.saas.billing.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class MoneyTest {

    @Test
    void normalizaParaDuasCasasDecimais() {
        assertEquals(new BigDecimal("10.00"), Money.of("10").amount());
        assertEquals(Money.of("10.00"), Money.of("10"));
    }

    @Test
    void arredondaComHalfEven() {
        assertEquals(Money.of("0.12"), Money.of("0.125"));
        assertEquals(Money.of("0.14"), Money.of("0.135"));
    }

    @Test
    void operacoesSaoExatasEmCentavos() {
        // Em double: 0.1 + 0.2 = 0.30000000000000004
        assertEquals(Money.of("0.30"), Money.of("0.10").plus(Money.of("0.20")));
    }

    @Test
    void operacoesDevolvemNovaInstancia() {
        Money original = Money.of("100.00");
        Money result = original.minus(Money.of("1.00"));

        assertNotSame(original, result);
        assertEquals(Money.of("100.00"), original);
        assertEquals(Money.of("99.00"), result);
    }

    @Test
    void atLeastZeroLimitaValoresNegativos() {
        Money negative = Money.of("5.00").minus(Money.of("30.00"));

        assertTrue(negative.isNegative());
        assertSame(Money.ZERO, negative.atLeastZero());
    }

    @Test
    void formataEmReais() {
        assertEquals("R$ 84.99", Money.of("84.99").toString());
    }
}
