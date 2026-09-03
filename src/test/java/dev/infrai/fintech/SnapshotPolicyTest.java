package dev.infrai.fintech;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class SnapshotPolicyTest {
    public static void main(String[] args) {
        Instant now = Instant.parse("2026-08-30T02:00:00Z");
        SnapshotPolicy.PaymentEvent settled = event("pay-1", SnapshotPolicy.Action.PAYMENT,
                SnapshotPolicy.PaymentState.SETTLED, 20, now);
        SnapshotPolicy.PaymentEvent risky = event("refund-2", SnapshotPolicy.Action.REFUND,
                SnapshotPolicy.PaymentState.SETTLED, 90, now.plusSeconds(1));
        SnapshotPolicy.PaymentEvent pending = event("pay-3", SnapshotPolicy.Action.PAYMENT,
                SnapshotPolicy.PaymentState.PENDING, 10, now.plusSeconds(2));

        SnapshotPolicy.Decision decision = new SnapshotPolicy(80).classify(List.of(settled, risky, pending));

        require(decision.archived().equals(List.of(settled)), "only the settled low-risk payment is archived");
        require(decision.notifications().size() == 1, "one risk notification is emitted");
        SnapshotPolicy.AuditNotification notice = decision.notifications().get(0);
        require(notice.eventId().equals("refund-2") && notice.action() == SnapshotPolicy.Action.REFUND,
                "the notification identifies the reviewed refund");
        System.out.println("PASS: risk-first snapshot partition");
    }

    private static SnapshotPolicy.PaymentEvent event(String id, SnapshotPolicy.Action action,
            SnapshotPolicy.PaymentState state, int risk, Instant time) {
        return new SnapshotPolicy.PaymentEvent(id, action, state, new BigDecimal("10.00"), risk, time);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}

