package com.saas.billing.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PricingCalculatorTest {

    private static final LocalDate OCTOBER = LocalDate.of(2026, 10, 15);
    private static final LocalDate NOVEMBER = LocalDate.of(2026, 11, 15);

    private final PricingCalculator calculator = new PricingCalculator();

    private static BillingCommand command(String basePrice, LocalDate signup, int overdueDays,
                                          String promoCode, boolean nonProfit) {
        return new BillingCommand("c1", Money.of(basePrice), signup, overdueDays, promoCode, nonProfit, "tok_x");
    }

    @Test
    void semDescontosCobraOPrecoBase() {
        Money price = calculator.calculate(command("100.00", OCTOBER.minusMonths(6), 0, null, false), OCTOBER);
        assertEquals(Money.of("100.00"), price);
    }

    @ParameterizedTest(name = "{0} meses -> {1}")
    @CsvSource({
            "12, 100.00", // exatamente 1 ano: ainda sem desconto
            "13, 90.00",  // > 12 meses: 10%
            "24, 90.00",
            "25, 85.00",  // > 24 meses: 15%
    })
    void descontoDeFidelidade(int months, String expected) {
        Money price = calculator.calculate(command("100.00", OCTOBER.minusMonths(months), 0, null, false), OCTOBER);
        assertEquals(Money.of(expected), price);
    }

    @Test
    void mesesAtivosIgnoramODia() {
        assertEquals(1, PricingCalculator.monthsActive(LocalDate.of(2026, 9, 30), LocalDate.of(2026, 10, 1)));
        assertEquals(0, PricingCalculator.monthsActive(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31)));
    }

    @Test
    void ongRecebe30PorCentoAposFidelidade() {
        Money price = calculator.calculate(command("100.00", OCTOBER.minusMonths(30), 0, null, true), OCTOBER);
        assertEquals(Money.of("59.50"), price); // 100 * 0.85 * 0.70
    }

    @Test
    void blackFridaySoValeEmNovembro() {
        LocalDate signup = OCTOBER.minusMonths(6);
        assertEquals(Money.of("70.00"),
                calculator.calculate(command("100.00", signup, 0, "BLACKFRIDAY", false), NOVEMBER));
        assertEquals(Money.of("100.00"),
                calculator.calculate(command("100.00", signup, 0, "BLACKFRIDAY", false), OCTOBER));
    }

    @Test
    void cupomIgnoraMaiusculasEMinusculas() {
        Money price = calculator.calculate(command("100.00", NOVEMBER.minusMonths(6), 0, "blackFriday", false), NOVEMBER);
        assertEquals(Money.of("70.00"), price);
    }

    @ParameterizedTest(name = "{0} meses -> {1}")
    @CsvSource({"0, 90.00", "1, 90.00", "2, 100.00"})
    void welcome10SoParaClientesNovos(int months, String expected) {
        Money price = calculator.calculate(command("100.00", OCTOBER.minusMonths(months), 0, "WELCOME10", false), OCTOBER);
        assertEquals(Money.of(expected), price);
    }

    @ParameterizedTest
    @CsvSource(value = {"''", "'   '", "INVALIDO"}, nullValues = "NULL")
    void cupomVazioOuDesconhecidoNaoAlteraOPreco(String promo) {
        Money price = calculator.calculate(command("100.00", NOVEMBER.minusMonths(6), 0, promo, false), NOVEMBER);
        assertEquals(Money.of("100.00"), price);
    }

    @Test
    void atrasoAplicaMulta2PorCentoEJurosDiarios() {
        Money price = calculator.calculate(command("100.00", OCTOBER.minusMonths(6), 10, null, false), OCTOBER);
        assertEquals(Money.of("103.00"), price); // 100 + 2.00 + (0.10 * 10)
    }

    @Test
    void atrasoNegativoOuZeroNaoGeraEncargos() {
        assertEquals(Money.of("100.00"),
                calculator.calculate(command("100.00", OCTOBER.minusMonths(6), -3, null, false), OCTOBER));
    }

    @Test
    void precoNuncaFicaNegativo() {
        Money price = calculator.calculate(command("20.00", NOVEMBER.minusMonths(6), 5, "BLACKFRIDAY", false), NOVEMBER);
        assertEquals(Money.ZERO, price);
    }

    @Test
    void regrasSaoAplicadasNaOrdemDoLegado() {
        // 200 -> fidelidade 15% = 170 -> ONG 30% = 119 -> Black Friday = 89
        // -> multa 1.78 + juros 89 * 0.001 * 5 = 0.445 (HALF_EVEN: 0.44) -> 91.22
        Money price = calculator.calculate(command("200.00", NOVEMBER.minusMonths(36), 5, "BLACKFRIDAY", true), NOVEMBER);
        assertEquals(Money.of("91.22"), price);
    }
}
