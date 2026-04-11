package com.sabarno.hireflux.service.util;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.sabarno.hireflux.exception.impl.BadRequestException;

@Service
public class OpenAIService {

    private static final String MODEL = "gpt-4.1-mini";
    private static final String CONTENT = "content";

    @Value("${openai.api.key}")
    private String apiKey;

    private String extractJson(String response) {
        int start = response.indexOf("{");
        int end = response.lastIndexOf("}");

        if (start == -1 || end == -1 || start > end) {
            throw new BadRequestException("Invalid JSON from AI");
        }

        return response.substring(start, end + 1);
    }

    private final WebClient webClient = WebClient.builder()
        .baseUrl("https://api.openai.com/v1")
        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .build();

    public String parseResume(String resumeText) {

        String prompt = buildPrompt(resumeText);

        Map<String, Object> request = Map.of(
            "model", MODEL,
            "messages", List.of(
                Map.of("role", "system", CONTENT, "You are a resume parser."),
                Map.of("role", "user", CONTENT, prompt)
            ),
            "temperature", 0,
            "response_format", Map.of("type", "json_object")
        );

        
        String text = webClient.post()
            .uri("/chat/completions")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
            .bodyValue(request)
            .retrieve()
            .bodyToMono(JsonNode.class)
            .map(res -> res.get("choices").get(0).get("message").get(CONTENT).asText())
            .block();
        return text != null ? extractJson(text) : null;
    }

    private String buildPrompt(String text) {
        return """
        Extract structured data from the following resume.

        Return ONLY valid JSON (no explanation).

        JSON format:
        {
          "fileName": "",
          "skills": [],
          "education": [
            {
              "degree": "",
              "college": "",
              "from": "",
              "to": ""
            }
          ],
          "experience": [
            {
              "role": "",
              "company": "",
              "location": "",
              "description": "",
              "from": "",
              "to": ""
            }
          ],
          "projects": [
            {
              "title": "",
              "description": "",
              "technologies": [],
              "link": "",
              "from": "",
              "to": ""
            }
          ]
        }

        Resume:
        """ + text;
    }
}
