package com.saas.billing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.saas.billing.adapter.CardTokenPaymentGateway;
import com.saas.billing.domain.BillingCommand;
import com.saas.billing.domain.Customer;
import com.saas.billing.domain.InvoiceStatus;
import com.saas.billing.domain.Money;
import com.saas.billing.port.CustomerRepository;
import com.saas.billing.port.InfrastructureException;
import com.saas.billing.port.InvoiceRepository;
import com.saas.billing.port.NotificationService;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class SubscriptionBillingServiceTest {

    private static final Clock BLACK_FRIDAY_2026 =
            Clock.fixed(LocalDate.of(2026, 11, 27).atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

    private final FakeCustomerRepository customers = new FakeCustomerRepository();
    private final FakeInvoiceRepository invoices = new FakeInvoiceRepository();
    private final FakeNotificationService notifications = new FakeNotificationService();

    private final SubscriptionBillingService service = new SubscriptionBillingService(
            customers, invoices, new CardTokenPaymentGateway(), notifications, BLACK_FRIDAY_2026);

    private static BillingCommand command(String customerId, String promo, String token) {
        return new BillingCommand(customerId, Money.of("100.00"), LocalDate.of(2026, 5, 10), 0, promo, false, token);
    }

    @Test
    void cobrancaAprovadaGravaFaturaPagaENotifica() {
        customers.add(new Customer("c1", "c1@cliente.com", "ACTIVE"));

        List<String> logs = service.processMonthlyBilling(command("c1", "BLACKFRIDAY", "tok_123"));

        assertEquals(List.of(
                "Pagamento processado com sucesso: R$ 70.00",
                "E-mail enviado para: c1@cliente.com"), logs);
        assertEquals(List.of(new Invoice("c1", Money.of("70.00"), InvoiceStatus.PAID)), invoices.saved);
        assertEquals(List.of(new Invoice("c1", Money.of("70.00"), InvoiceStatus.PAID)), notifications.sent);
    }

    @Test
    void cobrancaRecusadaGravaFaturaComFalha() {
        customers.add(new Customer("c1", "c1@cliente.com", "active"));

        List<String> logs = service.processMonthlyBilling(command("c1", null, "cartao_invalido"));

        assertEquals("Falha no pagamento do cartão para: c1", logs.get(0));
        assertEquals(List.of(new Invoice("c1", Money.of("100.00"), InvoiceStatus.FAILED)), invoices.saved);
        assertEquals(InvoiceStatus.FAILED, notifications.sent.get(0).status());
    }

    @Test
    void tokenNuloEhTratadoComoFalha() {
        customers.add(new Customer("c1", "c1@cliente.com", "ACTIVE"));

        service.processMonthlyBilling(command("c1", null, null));

        assertEquals(InvoiceStatus.FAILED, invoices.saved.get(0).status());
    }

    @Test
    void clienteInativoNaoECobrado() {
        customers.add(new Customer("c2", "c2@cliente.com", "SUSPENDED"));

        List<String> logs = service.processMonthlyBilling(command("c2", null, "tok_123"));

        assertEquals(List.of("Cliente inativo. Cobrança cancelada: c2"), logs);
        assertTrue(invoices.saved.isEmpty());
        assertTrue(notifications.sent.isEmpty());
    }

    @Test
    void clienteInexistenteNaoGeraEfeitos() {
        List<String> logs = service.processMonthlyBilling(command("nao-existe", null, "tok_123"));

        assertTrue(logs.isEmpty());
        assertTrue(invoices.saved.isEmpty());
        assertTrue(notifications.sent.isEmpty());
    }

    @Test
    void falhaDeInfraestruturaNaoEhMascarada() {
        InfrastructureException failure = new InfrastructureException("banco fora", null);
        CustomerRepository broken = id -> { throw failure; };
        SubscriptionBillingService brokenService = new SubscriptionBillingService(
                broken, invoices, new CardTokenPaymentGateway(), notifications, BLACK_FRIDAY_2026);

        InfrastructureException thrown = assertThrows(InfrastructureException.class,
                () -> brokenService.processMonthlyBilling(command("c1", null, "tok_123")));
        assertSame(failure, thrown);
    }

    private record Invoice(String customerId, Money amount, InvoiceStatus status) {
    }

    private static class FakeCustomerRepository implements CustomerRepository {
        private final Map<String, Customer> byId = new HashMap<>();

        void add(Customer customer) {
            byId.put(customer.id(), customer);
        }

        @Override
        public Optional<Customer> findById(String customerId) {
            return Optional.ofNullable(byId.get(customerId));
        }
    }

    private static class FakeInvoiceRepository implements InvoiceRepository {
        final List<Invoice> saved = new ArrayList<>();

        @Override
        public void save(String customerId, Money amount, InvoiceStatus status) {
            saved.add(new Invoice(customerId, amount, status));
        }
    }

    private static class FakeNotificationService implements NotificationService {
        final List<Invoice> sent = new ArrayList<>();

        @Override
        public void sendInvoice(Customer customer, Money amount, InvoiceStatus status) {
            sent.add(new Invoice(customer.id(), amount, status));
        }
    }
}
