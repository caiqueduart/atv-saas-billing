package com.saas.billing.adapter;

import com.saas.billing.domain.Customer;
import com.saas.billing.domain.InvoiceStatus;
import com.saas.billing.domain.Money;
import com.saas.billing.port.InfrastructureException;
import com.saas.billing.port.NotificationService;
import java.util.Objects;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.SimpleEmail;

public class SmtpNotificationService implements NotificationService {

    private static final String SUBJECT = "Sua Fatura Mensal";

    private final String hostName;
    private final int smtpPort;
    private final String from;

    public SmtpNotificationService(String hostName, int smtpPort, String from) {
        this.hostName = Objects.requireNonNull(hostName);
        this.smtpPort = smtpPort;
        this.from = Objects.requireNonNull(from);
    }

    @Override
    public void sendInvoice(Customer customer, Money amount, InvoiceStatus status) {
        try {
            SimpleEmail email = new SimpleEmail();
            email.setHostName(hostName);
            email.setSmtpPort(smtpPort);
            email.setFrom(from);
            email.setSubject(SUBJECT);
            email.setMsg(messageFor(amount, status));
            email.addTo(customer.email());
            email.send();
        } catch (EmailException e) {
            throw new InfrastructureException("Erro ao enviar e-mail para " + customer.email(), e);
        }
    }

    static String messageFor(Money amount, InvoiceStatus status) {
        return switch (status) {
            case PAID -> "Olá! Sua fatura no valor de " + amount + " foi paga com sucesso.";
            case FAILED -> "Olá! Não conseguimos cobrar sua fatura no valor de " + amount
                    + ". Verifique os dados do seu cartão.";
        };
    }
}
