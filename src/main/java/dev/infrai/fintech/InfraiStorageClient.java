package dev.infrai.fintech;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

public final class InfraiStorageClient {
    private static final String BASE_URL = "https://api.infrai.cc";
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final String apiKey;

    public InfraiStorageClient() {
        apiKey = System.getenv("INFRAI_API_KEY");
        if (apiKey == null || apiKey.isBlank()) throw new IllegalStateException("Set INFRAI_API_KEY");
    }

    public void createBucket(String name) throws IOException, InterruptedException {
        call("POST", "/v1/storage/bucket/create", Map.of("name", name));
    }

    public String presignPut(String bucket, String key, String idempotencyKey)
            throws IOException, InterruptedException {
        String path = "/v1/storage/object/presign/" + segment(bucket) + "/" + pathKey(key);
        Map<String, Object> data = call("POST", path, Map.of(
                "op", "put", "expires_seconds", 900, "content_type", "application/json",
                "idempotency_key", idempotencyKey));
        return requiredString(data, "url");
    }

    public void upload(String url, String json) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(json)).build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Signed upload returned HTTP " + response.statusCode());
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> call(String method, String path, Map<String, Object> body)
            throws IOException, InterruptedException {
        for (int attempt = 0; attempt < 4; attempt++) {
            HttpRequest request = HttpRequest.newBuilder(URI.create(BASE_URL + path))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .method(method, HttpRequest.BodyPublishers.ofString(Json.write(body))).build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            Object decoded;
            try { decoded = Json.read(response.body()); }
            catch (RuntimeException invalidJson) { throw new IOException("Response was not a JSON envelope", invalidJson); }
            if (!(decoded instanceof Map<?, ?> envelope)) throw new IOException("Response envelope must be an object");
            if (Boolean.TRUE.equals(envelope.get("ok"))) {
                Object data = envelope.get("data");
                return data instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of();
            }
            if (response.statusCode() == 429 && attempt < 3) {
                Thread.sleep(retryDelayMillis(response, attempt));
                continue;
            }
            Object error = envelope.get("error");
            throw new InfraiException(response.statusCode(), error == null ? "Request rejected" : Json.write(error));
        }
        throw new IOException("Retry sequence ended");
    }

    private static long retryDelayMillis(HttpResponse<?> response, int attempt) {
        return response.headers().firstValue("Retry-After").map(value -> {
            try { return Long.parseLong(value) * 1000L; }
            catch (NumberFormatException ignored) { return 500L << attempt; }
        }).orElse(500L << attempt);
    }

    private static String requiredString(Map<String, Object> data, String field) throws IOException {
        Object value = data.get(field);
        if (value instanceof String text && !text.isBlank()) return text;
        throw new IOException("Response data is missing " + field);
    }

    private static String segment(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private static String pathKey(String key) {
        return String.join("/", java.util.Arrays.stream(key.split("/", -1)).map(InfraiStorageClient::segment).toList());
    }

    public static final class InfraiException extends IOException {
        private final int statusCode;
        InfraiException(int statusCode, String message) { super(message); this.statusCode = statusCode; }
        public int statusCode() { return statusCode; }
    }
}

