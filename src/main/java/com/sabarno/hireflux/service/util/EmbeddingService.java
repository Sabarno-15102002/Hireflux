package com.sabarno.hireflux.service.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.JsonNode;

@Service
public class EmbeddingService {

    private static final String MODEL = "text-embedding-3-small";

    @Value("${openai.api.key}")
    private String apiKey;

    private final WebClient webClient = WebClient.builder()
            .baseUrl("https://api.openai.com/v1")
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .build();

    public List<Double> createEmbedding(String text) {

        Map<String, Object> request = Map.of(
                "model", MODEL,
                "input", text
        );

        return webClient.post()
                .uri("/embeddings")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(res -> res.get("data").get(0).get("embedding"))
                .map(node -> {
                    List<Double> vector = new ArrayList<>();
                    node.forEach(n -> vector.add(n.asDouble()));
                    return vector;
                })
                .block();
    }
}