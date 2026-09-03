package dev.infrai.fintech;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

public final class FintechSnapshotApplication {
    private FintechSnapshotApplication() {}

    public static void main(String[] args) throws Exception {
        SnapshotConfig config = SnapshotConfig.load();
        NightlySnapshotService service = new NightlySnapshotService(
                config, new SnapshotPolicy(80), new InfraiStorageClient());
        LocalDate date = LocalDate.now(ZoneOffset.UTC);
        Instant lessonTime = date.atStartOfDay(ZoneOffset.UTC).toInstant();
        List<SnapshotPolicy.PaymentEvent> events = List.of(
                new SnapshotPolicy.PaymentEvent("pay-1001", SnapshotPolicy.Action.PAYMENT,
                        SnapshotPolicy.PaymentState.SETTLED, new BigDecimal("42.50"), 12, lessonTime),
                new SnapshotPolicy.PaymentEvent("refund-1002", SnapshotPolicy.Action.REFUND,
                        SnapshotPolicy.PaymentState.SETTLED, new BigDecimal("300.00"), 91, lessonTime.plusSeconds(60)),
                new SnapshotPolicy.PaymentEvent("pay-1003", SnapshotPolicy.Action.PAYMENT,
                        SnapshotPolicy.PaymentState.PENDING, new BigDecimal("18.25"), 10, lessonTime.plusSeconds(120)));
        NightlySnapshotService.SnapshotResult result = service.run(date, events);
        System.out.printf("Stored %s with %d payment; emitted %d audit notification.%n",
                result.objectKey(), result.archivedCount(), result.notifications().size());
    }
}

