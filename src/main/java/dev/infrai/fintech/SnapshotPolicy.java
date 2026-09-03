package dev.infrai.fintech;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class SnapshotPolicy {
    public enum PaymentState { PENDING, SETTLED }
    public enum Action { PAYMENT, REFUND, ACCOUNT_CHANGE }

    public record PaymentEvent(String eventId, Action action, PaymentState state,
                               BigDecimal amount, int riskScore, Instant occurredAt) {}
    public record AuditNotification(String eventId, Action action, String reason, Instant occurredAt) {}
    public record Decision(List<PaymentEvent> archived, List<AuditNotification> notifications) {}

    private final int highRiskThreshold;

    public SnapshotPolicy(int highRiskThreshold) {
        this.highRiskThreshold = highRiskThreshold;
    }

    public Decision classify(List<PaymentEvent> events) {
        List<PaymentEvent> archived = new ArrayList<>();
        List<AuditNotification> notifications = new ArrayList<>();
        for (PaymentEvent event : events) {
            if (event.riskScore() >= highRiskThreshold) {
                notifications.add(new AuditNotification(event.eventId(), event.action(),
                        "risk score requires review", event.occurredAt()));
            } else if (event.state() == PaymentState.SETTLED) {
                archived.add(event);
            }
        }
        return new Decision(List.copyOf(archived), List.copyOf(notifications));
    }
}

