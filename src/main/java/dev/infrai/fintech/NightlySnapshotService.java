package dev.infrai.fintech;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class NightlySnapshotService {
    public record SnapshotResult(String objectKey, int archivedCount,
                                 List<SnapshotPolicy.AuditNotification> notifications) {}

    private final SnapshotConfig config;
    private final SnapshotPolicy policy;
    private final InfraiStorageClient storage;

    public NightlySnapshotService(SnapshotConfig config, SnapshotPolicy policy, InfraiStorageClient storage) {
        this.config = config;
        this.policy = policy;
        this.storage = storage;
    }

    public SnapshotResult run(LocalDate date, List<SnapshotPolicy.PaymentEvent> events)
            throws IOException, InterruptedException {
        SnapshotPolicy.Decision decision = policy.classify(events);
        String key = "snapshots/" + date + ".json";
        storage.createBucket(config.bucket());
        String document = snapshotJson(date, decision.archived());
        String url = storage.presignPut(config.bucket(), key, "nightly-snapshot-" + date);
        storage.upload(url, document);
        return new SnapshotResult(key, decision.archived().size(), decision.notifications());
    }

    private static String snapshotJson(LocalDate date, List<SnapshotPolicy.PaymentEvent> events) {
        List<Map<String, Object>> rows = events.stream().map(event -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("event_id", event.eventId());
            row.put("action", event.action().name());
            row.put("state", event.state().name());
            row.put("amount", event.amount().toPlainString());
            row.put("risk_score", event.riskScore());
            row.put("occurred_at", event.occurredAt().toString());
            return row;
        }).toList();
        return Json.write(Map.of("snapshot_date", date.toString(), "generated_at", date.atStartOfDay(ZoneOffset.UTC).toString(), "payments", rows));
    }
}

