package com.saas.billing;

import com.saas.billing.domain.BillingCommand;
import com.saas.billing.domain.Customer;
import com.saas.billing.domain.InvoiceStatus;
import com.saas.billing.domain.Money;
import com.saas.billing.domain.PricingCalculator;
import com.saas.billing.port.CustomerRepository;
import com.saas.billing.port.InvoiceRepository;
import com.saas.billing.port.NotificationService;
import com.saas.billing.port.PaymentGateway;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Fachada de cobrança mensal: orquestra precificação, pagamento, fatura e notificação. */
public class SubscriptionBillingService {

    private final CustomerRepository customerRepository;
    private final InvoiceRepository invoiceRepository;
    private final PaymentGateway paymentGateway;
    private final NotificationService notificationService;
    private final Clock clock;
    private final PricingCalculator pricingCalculator = new PricingCalculator();

    public SubscriptionBillingService(CustomerRepository customerRepository,
                                      InvoiceRepository invoiceRepository,
                                      PaymentGateway paymentGateway,
                                      NotificationService notificationService,
                                      Clock clock) {
        this.customerRepository = Objects.requireNonNull(customerRepository);
        this.invoiceRepository = Objects.requireNonNull(invoiceRepository);
        this.paymentGateway = Objects.requireNonNull(paymentGateway);
        this.notificationService = Objects.requireNonNull(notificationService);
        this.clock = Objects.requireNonNull(clock);
    }

    public List<String> processMonthlyBilling(BillingCommand command) {
        Money amount = pricingCalculator.calculate(command, LocalDate.now(clock));
        Optional<Customer> customer = customerRepository.findById(command.customerId());
        if (customer.isEmpty()) {
            return new ArrayList<>();
        }
        if (!customer.get().isActive()) {
            return new ArrayList<>(List.of("Cliente inativo. Cobrança cancelada: " + command.customerId()));
        }
        return charge(customer.get(), command.paymentCardToken(), amount);
    }

    private List<String> charge(Customer customer, String paymentCardToken, Money amount) {
        List<String> logs = new ArrayList<>();
        boolean paid = paymentGateway.charge(paymentCardToken, amount);
        logs.add(paid
                ? "Pagamento processado com sucesso: " + amount
                : "Falha no pagamento do cartão para: " + customer.id());

        InvoiceStatus status = InvoiceStatus.fromPayment(paid);
        invoiceRepository.save(customer.id(), amount, status);
        notificationService.sendInvoice(customer, amount, status);
        logs.add("E-mail enviado para: " + customer.email());
        return logs;
    }
}
