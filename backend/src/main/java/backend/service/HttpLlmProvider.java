package backend.service;

import backend.concierge.LlmProvider;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;

/**
 * Calls an Anthropic-style Messages API from the SERVER. Enabled only when both an API key and a
 * model name are configured; otherwise the Concierge runs on its built-in rules.
 * NOTE: written against the public Messages API shape but not exercised against the live service
 * in this repository's automated checks.
 */
@Component
public class HttpLlmProvider implements LlmProvider {

    private static final Logger log = LoggerFactory.getLogger(HttpLlmProvider.class);

    private final String apiKey;
    private final String model;
    private final String baseUrl;
    private final int timeoutMs;
    private final ObjectMapper mapper;
    private final HttpClient client;

    public HttpLlmProvider(@Value("${app.concierge.llm.api-key:}") String apiKey,
                           @Value("${app.concierge.llm.model:}") String model,
                           @Value("${app.concierge.llm.base-url:https://api.anthropic.com}") String baseUrl,
                           @Value("${app.concierge.llm.timeout-ms:8000}") int timeoutMs,
                           ObjectMapper mapper) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.model = model == null ? "" : model.trim();
        this.baseUrl = (baseUrl == null || baseUrl.isBlank() ? "https://api.anthropic.com" : baseUrl).replaceAll("/+$", "");
        this.timeoutMs = Math.max(1000, timeoutMs);
        this.mapper = mapper;
        this.client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
    }

    @Override
    public boolean enabled() {
        return !apiKey.isEmpty() && !model.isEmpty();
    }

    @Override
    public Optional<String> rewrite(String systemPrompt, String userPrompt) {
        if (!enabled()) {
            return Optional.empty();
        }
        try {
            ObjectNode body = mapper.createObjectNode();
            body.put("model", model);
            body.put("max_tokens", 300);
            body.put("system", systemPrompt);
            ArrayNode msgs = body.putArray("messages");
            ObjectNode m = msgs.addObject();
            m.put("role", "user");
            m.put("content", userPrompt);

            HttpRequest req = HttpRequest.newBuilder(URI.create(baseUrl + "/v1/messages"))
                    .timeout(Duration.ofMillis(timeoutMs))
                    .header("content-type", "application/json")
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", "2023-06-01")
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                    .build();
            HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() / 100 != 2) {
                log.warn("Concierge LLM returned HTTP {}", res.statusCode()); // never log bodies or keys
                return Optional.empty();
            }
            JsonNode text = mapper.readTree(res.body()).path("content").path(0).path("text");
            return text.isTextual() ? Optional.of(text.asText()) : Optional.empty();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        } catch (Exception e) {
            log.warn("Concierge LLM call failed: {}", e.getClass().getSimpleName());
            return Optional.empty();
        }
    }
}
