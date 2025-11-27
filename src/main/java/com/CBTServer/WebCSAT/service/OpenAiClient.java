package com.CBTServer.WebCSAT.service;

import io.netty.channel.ChannelOption;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import reactor.netty.http.client.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
public class OpenAiClient {

    private final WebClient webClient;

    @Value("${openai.api.key}")   // ✅ application.properties에서 자동 주입
    private String apiKey;

    public OpenAiClient(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl("https://api.openai.com/v1").build();
    }

    public Mono<String> generateTextAsync(String prompt) {
        System.out.println("[OpenAiClient] Loaded API key prefix: " + (apiKey != null ? apiKey.substring(0, 10) : "NULL"));

        return webClient.post()
                .uri("/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of(
                        "model", "gpt-4o-mini",
                        "messages", new Object[]{
                                Map.of("role", "system", "content", "You are a teacher analyzing student exam scores."),
                                Map.of("role", "user", "content", prompt)
                        },
                        "max_tokens", 500,
                        "temperature", 0.7
                ))
                .retrieve()
                .bodyToMono(Map.class)
                // ✅ 429 TooManyRequests → 최대 3회 재시도 (5초 간격)
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(5))
                        .filter(t -> t instanceof WebClientResponseException.TooManyRequests)
                        .onRetryExhaustedThrow((spec, signal) -> signal.failure()))
                // ✅ 정상 응답 파싱
                .map(response -> {
                    List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
                    Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                    return message.get("content").toString();
                })
                // ✅ 오류 처리 (HTTP 상태별 분기)
                .onErrorResume(WebClientResponseException.class, e -> {
                    HttpStatus status = (HttpStatus) e.getStatusCode();

                    System.err.println("🔴 [OpenAiClient] API Error");
                    System.err.println("Status: " + status);
                    System.err.println("Body: " + e.getResponseBodyAsString());

                    if (status == HttpStatus.UNAUTHORIZED) {
                        return Mono.error(new RuntimeException("401 Unauthorized: Invalid or missing OpenAI API key."));
                    } else if (status == HttpStatus.TOO_MANY_REQUESTS) {
                        return Mono.error(new RuntimeException("429 Too Many Requests: Rate limit exceeded. Try again later."));
                    } else if (status == HttpStatus.SERVICE_UNAVAILABLE) {
                        return Mono.error(new RuntimeException("503 Service Unavailable: OpenAI servers are temporarily overloaded."));
                    } else if (status.is4xxClientError()) {
                        return Mono.error(new RuntimeException("4xx Client Error: Check your API key or request parameters."));
                    } else if (status.is5xxServerError()) {
                        return Mono.error(new RuntimeException("5xx Server Error: OpenAI service unavailable. Please retry later."));
                    } else {
                        return Mono.error(new RuntimeException("Unexpected HTTP Error: " + status + " - " + e.getMessage()));
                    }
                })
                // ✅ 그 외 예외 처리 (네트워크, 타임아웃 등)
                .onErrorResume(Exception.class, e -> {
                    System.err.println("⚠️ [OpenAiClient] Unexpected Exception: " + e.getClass().getSimpleName());
                    e.printStackTrace();
                    return Mono.error(new RuntimeException("Unexpected AI request error: " + e.getMessage()));
                });
    }
}
