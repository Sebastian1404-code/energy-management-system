package distributedSystem.CustomerSupport.gemini;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import distributedSystem.CustomerSupport.config.GeminiProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class GeminiClient {

    private static final int MAX_OUTPUT_TOKENS = 100;

    private final GeminiProperties props;

    private final RestClient restClient = RestClient.builder()
            .baseUrl("https://generativelanguage.googleapis.com")
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
            .build();

    public Optional<String> generateReply(String userMessage, List<String> recentConversation) {
        String apiKey = props.apiKey();
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("Gemini disabled: gemini.api-key missing/blank");
            return Optional.empty();
        }

        String modelName = (props.model() == null || props.model().isBlank())
                ? "gemini-3-flash-preview"
                : props.model().trim();

        String context = buildTinyContext(recentConversation);
        String userText = context.isBlank()
                ? userMessage
                : ("Context:\n" + context + "\n\nUser: " + userMessage);

        // If you still want “system”, prepend it (most compatible):
        String system = (props.systemPrompt() == null || props.systemPrompt().isBlank())
                ? "You are a customer support assistant. Reply in at most 70 tokens. Be concise (1-3 sentences)."
                : props.systemPrompt();
        String finalPrompt = system + "\n\n" + userText;

        GeminiGenerateRequest req = GeminiGenerateRequest.of(finalPrompt, MAX_OUTPUT_TOKENS);

        log.info("Gemini request model={}", modelName);
        log.info("Gemini request uri(masked)=/v1beta/models/{}:generateContent?key={}",
                modelName, maskKey(apiKey));
        log.info("Gemini prompt chars={} (contextLines={})",
                finalPrompt.length(), recentConversation == null ? 0 : recentConversation.size());

        Instant start = Instant.now();
        try {
            String raw = restClient.post()
                    // IMPORTANT: keep "models/" in the path, pass only modelName to avoid %2F encoding
                    .uri(uriBuilder -> uriBuilder
                            .path("/v1beta/models/{model}:generateContent")
                            .queryParam("key", apiKey)
                            .build(modelName))
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(req)
                    .retrieve()
                    .body(String.class);

            long ms = Duration.between(start, Instant.now()).toMillis();
            log.info("Gemini HTTP success in {} ms (rawBodyChars={})", ms, raw == null ? 0 : raw.length());

            if (raw == null || raw.isBlank()) {
                log.warn("Gemini returned EMPTY HTTP body ({} ms)", ms);
                return Optional.empty();
            }

            // Helpful debug: keep short head in logs (don’t spam)
            log.debug("Gemini raw response head: {}", head(raw, 800));

            GeminiGenerateResponse resp = Json.MAPPER.readValue(raw, GeminiGenerateResponse.class);

            String text = (resp == null) ? null : resp.firstText();
            if (text == null || text.isBlank()) {
                log.warn("Gemini returned no candidate text ({} ms). Raw(head): {}", ms, head(raw, 800));
                return Optional.empty();
            }

            String trimmed = text.trim();
            log.info("Gemini reply extracted in {} ms (replyChars={})", ms, trimmed.length());
            return Optional.of(trimmed);

        } catch (RestClientResponseException e) {
            long ms = Duration.between(start, Instant.now()).toMillis();
            int status = e.getStatusCode().value();
            String body = e.getResponseBodyAsString();

            log.warn("Gemini HTTP {} after {} ms. Response(head): {}", status, ms, head(body, 1200));

            if (status == 429) {
                log.warn("Gemini quota/rate limited. Consider retry/backoff or switching model/quota.");
            }
            return Optional.empty();

        } catch (Exception e) {
            long ms = Duration.between(start, Instant.now()).toMillis();
            log.error("Gemini call failed after {} ms: {}", ms, e.toString(), e);
            return Optional.empty();
        }
    }

    private static String buildTinyContext(List<String> recentConversation) {
        if (recentConversation == null || recentConversation.isEmpty()) return "";
        int from = Math.max(0, recentConversation.size() - 4);
        return String.join("\n", recentConversation.subList(from, recentConversation.size()));
    }

    private static String head(String s, int maxChars) {
        if (s == null) return "(null)";
        if (s.length() <= maxChars) return s;
        return s.substring(0, maxChars) + "...(truncated)";
    }

    private static String maskKey(String key) {
        if (key == null) return "(null)";
        String k = key.trim();
        if (k.length() <= 8) return "****";
        return k.substring(0, 4) + "****" + k.substring(k.length() - 4);
    }

    // ---------------- Request DTOs (matches your working curl shape) ----------------

    public record GeminiGenerateRequest(List<ReqContent> contents, GenerationConfig generationConfig) {
        public static GeminiGenerateRequest of(String userText, int maxOutputTokens) {
            ReqContent user = new ReqContent(List.of(new ReqPart(userText)));
            return new GeminiGenerateRequest(
                    List.of(user),
                    new GenerationConfig(maxOutputTokens, 0.2)
            );
        }
    }

    public record ReqContent(List<ReqPart> parts) {}
    public record ReqPart(String text) {}
    public record GenerationConfig(Integer maxOutputTokens, Double temperature) {}

    // ---------------- Response DTOs (ignore unknown fields like thoughtSignature) ----------------

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GeminiGenerateResponse(List<Candidate> candidates) {
        public String firstText() {
            if (candidates == null || candidates.isEmpty()) return null;
            Candidate c = candidates.get(0);
            if (c == null || c.content() == null || c.content().parts() == null || c.content().parts().isEmpty()) return null;

            StringBuilder sb = new StringBuilder();
            for (RespPart p : c.content().parts()) {
                if (p != null && p.text() != null && !p.text().isBlank()) {
                    if (!sb.isEmpty()) sb.append('\n');
                    sb.append(p.text());
                }
            }
            return sb.isEmpty() ? null : sb.toString();
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Candidate(RespContent content) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RespContent(List<RespPart> parts, String role) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RespPart(String text) {
        // Gemini may include fields like thoughtSignature; we ignore them via @JsonIgnoreProperties
    }

    // ---------------- Minimal JSON helper ----------------
    static final class Json {
        static final com.fasterxml.jackson.databind.ObjectMapper MAPPER =
                new com.fasterxml.jackson.databind.ObjectMapper().findAndRegisterModules();
        private Json() {}
    }
}
