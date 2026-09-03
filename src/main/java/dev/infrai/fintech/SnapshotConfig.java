package dev.infrai.fintech;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public record SnapshotConfig(String bucket, int hourUtc) {
    public static SnapshotConfig load() throws IOException {
        Properties properties = new Properties();
        try (InputStream input = SnapshotConfig.class.getResourceAsStream("/application.properties")) {
            if (input == null) {
                throw new IOException("application.properties is missing");
            }
            properties.load(input);
        }
        String bucket = System.getenv().getOrDefault("SNAPSHOT_BUCKET", properties.getProperty("snapshot.bucket"));
        String hour = System.getenv().getOrDefault("SNAPSHOT_HOUR_UTC", properties.getProperty("snapshot.hour-utc"));
        int hourUtc = Integer.parseInt(hour);
        if (bucket == null || bucket.isBlank() || hourUtc < 0 || hourUtc > 23) {
            throw new IllegalArgumentException("Snapshot configuration is invalid");
        }
        return new SnapshotConfig(bucket, hourUtc);
    }
}

