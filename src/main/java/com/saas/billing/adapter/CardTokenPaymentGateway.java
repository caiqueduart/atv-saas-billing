package com.saas.billing.adapter;

import com.saas.billing.domain.Money;
import com.saas.billing.port.PaymentGateway;

/** Mantém a regra do legado: tokens válidos começam com "tok_". */
public class CardTokenPaymentGateway implements PaymentGateway {

    private static final String VALID_TOKEN_PREFIX = "tok_";

    @Override
    public boolean charge(String paymentCardToken, Money amount) {
        return paymentCardToken != null && paymentCardToken.startsWith(VALID_TOKEN_PREFIX);
    }
}
