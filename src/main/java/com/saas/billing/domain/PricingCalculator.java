package com.saas.billing.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;

/**
 * Regras de precificação da assinatura mensal. Classe pura: não acessa
 * relógio, banco ou rede; a data de referência é recebida como parâmetro.
 */
public final class PricingCalculator {

    static final int LOYALTY_TIER_1_MONTHS = 12;
    static final int LOYALTY_TIER_2_MONTHS = 24;
    static final BigDecimal LOYALTY_TIER_1_DISCOUNT = new BigDecimal("0.10");
    static final BigDecimal LOYALTY_TIER_2_DISCOUNT = new BigDecimal("0.15");

    static final BigDecimal NON_PROFIT_FACTOR = new BigDecimal("0.70");

    static final String BLACK_FRIDAY_CODE = "BLACKFRIDAY";
    static final Month BLACK_FRIDAY_MONTH = Month.NOVEMBER;
    static final Money BLACK_FRIDAY_DISCOUNT = Money.of("30.00");

    static final String WELCOME_CODE = "WELCOME10";
    static final int WELCOME_MAX_MONTHS = 1;
    static final BigDecimal WELCOME_FACTOR = new BigDecimal("0.90");

    static final BigDecimal LATE_PENALTY_RATE = new BigDecimal("0.02");
    static final BigDecimal DAILY_INTEREST_RATE = new BigDecimal("0.001");

    public Money calculate(BillingCommand command, LocalDate today) {
        int monthsActive = monthsActive(command.signupDate(), today);

        Money price = applyLoyaltyDiscount(command.basePrice(), monthsActive);
        price = applyNonProfitDiscount(price, command.nonProfit());
        price = applyPromoCode(price, command, monthsActive, today);
        price = applyOverdueCharges(price, command.overdueDays());
        return price.atLeastZero();
    }

    /** Diferença em meses de calendário, ignorando o dia (regra do legado). */
    static int monthsActive(LocalDate signupDate, LocalDate today) {
        return (today.getYear() - signupDate.getYear()) * 12
                + (today.getMonthValue() - signupDate.getMonthValue());
    }

    private Money applyLoyaltyDiscount(Money basePrice, int monthsActive) {
        if (monthsActive > LOYALTY_TIER_2_MONTHS) {
            return basePrice.minus(basePrice.times(LOYALTY_TIER_2_DISCOUNT));
        }
        if (monthsActive > LOYALTY_TIER_1_MONTHS) {
            return basePrice.minus(basePrice.times(LOYALTY_TIER_1_DISCOUNT));
        }
        return basePrice;
    }

    private Money applyNonProfitDiscount(Money price, boolean nonProfit) {
        return nonProfit ? price.times(NON_PROFIT_FACTOR) : price;
    }

    private Money applyPromoCode(Money price, BillingCommand command, int monthsActive, LocalDate today) {
        if (!command.hasPromoCode()) {
            return price;
        }
        String code = command.promoCode();
        if (code.equalsIgnoreCase(BLACK_FRIDAY_CODE) && today.getMonth() == BLACK_FRIDAY_MONTH) {
            return price.minus(BLACK_FRIDAY_DISCOUNT);
        }
        if (code.equalsIgnoreCase(WELCOME_CODE) && monthsActive <= WELCOME_MAX_MONTHS) {
            return price.times(WELCOME_FACTOR);
        }
        return price;
    }

    private Money applyOverdueCharges(Money price, int overdueDays) {
        if (overdueDays <= 0) {
            return price;
        }
        Money penalty = price.times(LATE_PENALTY_RATE);
        Money interest = price.times(DAILY_INTEREST_RATE, overdueDays);
        return price.plus(penalty).plus(interest);
    }
}
