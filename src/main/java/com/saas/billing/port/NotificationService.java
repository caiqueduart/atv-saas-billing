package com.saas.billing.port;

import com.saas.billing.domain.Customer;
import com.saas.billing.domain.InvoiceStatus;
import com.saas.billing.domain.Money;

public interface NotificationService {

    void sendInvoice(Customer customer, Money amount, InvoiceStatus status);
}
