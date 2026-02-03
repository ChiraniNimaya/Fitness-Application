package com.fitness.aiservice.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Map;

@Service
@Slf4j
public class GeminiService {
    private final WebClient webClient;

    @Value("${gemini.api.base-url}")
    private String baseUrl;

    @Value("${gemini.api.model}")
    private String model;

    @Value("${gemini.api.key}")
    private String apiKey;

    public GeminiService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    @PostConstruct
    public void init() {
        log.info("GeminiService initialized:");
        log.info("  Base URL: {}", baseUrl);
        log.info("  Model: {}", model);
        log.info("  API Key: {}***", apiKey != null && apiKey.length() > 4 ? apiKey.substring(0, 4) : "null");
        log.info("  Full URL will be: {}/v1beta/models/{}:generateContent", baseUrl, model);
    }

    public String getAnswer(String question) {
        Map<String, Object> requestBody = Map.of(
                "contents", new Object[] {
                        Map.of("parts", new Object[]{
                                Map.of("text", question)
                        })
                }
        );

        String endpoint = String.format("/v1beta/models/%s:generateContent", model);
        String fullUrl = baseUrl + endpoint;

        log.info("Calling Gemini API: {}", fullUrl);

        try {
            String response = webClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .scheme("https")
                            .host(baseUrl.replace("https://", "").replace("http://", ""))
                            .path(endpoint)
                            .queryParam("key", apiKey)
                            .build())
                    .header("Content-Type", "application/json")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            log.info("Successfully received response from Gemini API");
            return response;

        } catch (WebClientResponseException e) {
            log.error("Gemini API error: Status={}, Response Body={}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            return String.format("{\"error\": \"Gemini API error: %s - %s\"}",
                    e.getStatusCode(), e.getMessage());
        } catch (Exception e) {
            log.error("Error calling Gemini API: ", e);
            return String.format("{\"error\": \"Failed to call Gemini API: %s\"}", e.getMessage());
        }
    }
}