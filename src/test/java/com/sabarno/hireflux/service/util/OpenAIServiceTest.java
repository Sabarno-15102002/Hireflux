package com.sabarno.hireflux.service.util;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sabarno.hireflux.exception.impl.OpenAIException;

import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OpenAIServiceTest {
 
    @Mock
    private WebClient webClient;
 
    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriSpec;
 
    @Mock
    private WebClient.RequestBodySpec requestBodySpec;
 
    @Mock
    private WebClient.RequestHeadersSpec<?> requestHeadersSpec;
 
    @Mock
    private WebClient.ResponseSpec responseSpec;
 
    private OpenAIService openAIService;
 
    private static final String API_KEY = "test-api-key-123";
    private final ObjectMapper objectMapper = new ObjectMapper();
 
    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        openAIService = new OpenAIService();
        ReflectionTestUtils.setField(openAIService, "webClient", webClient);
        ReflectionTestUtils.setField(openAIService, "apiKey", API_KEY);
 
        lenient().when(webClient.post()).thenReturn(requestBodyUriSpec);
        lenient().when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        lenient().when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        lenient().when(requestBodySpec.bodyValue(any()))
                .thenReturn((WebClient.RequestHeadersSpec) requestHeadersSpec);
        lenient().when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
    }
 
    private JsonNode buildChatCompletionResponse(String content) throws Exception {
        String json = """
                {
                  "choices": [
                    { "message": { "content": %s } }
                  ]
                }
                """.formatted(objectMapper.writeValueAsString(content));
        return objectMapper.readTree(json);
    }
 
    @Test
    void testParseResume_shouldReturnExtractedJson_whenResponseWrapsJsonInExtraText() throws Exception {
        String aiText = "Sure, here is the result: {\"skills\":[\"java\"]} Hope that helps!";
        JsonNode response = buildChatCompletionResponse(aiText);
        when(responseSpec.bodyToMono(JsonNode.class)).thenReturn(Mono.just(response));
 
        String result = openAIService.parseResume("Some resume text");
 
        assertEquals("{\"skills\":[\"java\"]}", result);
    }
 
    @Test
    void testParseResume_shouldReturnJsonAsIs_whenResponseIsPureJson() throws Exception {
        String aiText = "{\"fileName\":\"resume.pdf\",\"skills\":[]}";
        JsonNode response = buildChatCompletionResponse(aiText);
        when(responseSpec.bodyToMono(JsonNode.class)).thenReturn(Mono.just(response));
 
        String result = openAIService.parseResume("Some resume text");
 
        assertEquals(aiText, result);
    }
 
    @Test
    void testParseResume_shouldThrowOpenAIException_whenResponseHasNoJsonBraces() throws Exception {
        String aiText = "I could not parse this resume.";
        JsonNode response = buildChatCompletionResponse(aiText);
        when(responseSpec.bodyToMono(JsonNode.class)).thenReturn(Mono.just(response));
 
        OpenAIException exception = assertThrows(
                OpenAIException.class,
                () -> openAIService.parseResume("Some resume text")
        );
        assertEquals("Invalid JSON from AI", exception.getMessage());
    }

    @Test
    void testParseResume_shouldThrowOpenAIException_whenOpeningBraceExistsButNoClosingBrace() throws Exception {
        String aiText = "{ this JSON never closes";
        JsonNode response = buildChatCompletionResponse(aiText);
        when(responseSpec.bodyToMono(JsonNode.class)).thenReturn(Mono.just(response));

        OpenAIException exception = assertThrows(
                OpenAIException.class,
                () -> openAIService.parseResume("Some resume text")
        );
        assertEquals("Invalid JSON from AI", exception.getMessage());
    }
 
    @Test
    void testParseResume_shouldThrowOpenAIException_whenClosingBraceComesBeforeOpeningBrace() throws Exception {
        // start > end -> also invalid per the extractJson guard
        String aiText = "} some malformed text {";
        JsonNode response = buildChatCompletionResponse(aiText);
        when(responseSpec.bodyToMono(JsonNode.class)).thenReturn(Mono.just(response));
 
        assertThrows(OpenAIException.class, () -> openAIService.parseResume("Some resume text"));
    }
 
    @Test
    void testParseResume_shouldReturnNull_whenReactiveResponseCompletesEmpty() {
        when(responseSpec.bodyToMono(JsonNode.class)).thenReturn(Mono.empty());
 
        String result = openAIService.parseResume("Some resume text");
 
        assertNull(result);
    }
 
    @Test
    void testParseResume_shouldPropagateException_whenWebClientCallFails() {
        RuntimeException apiFailure = new RuntimeException("OpenAI API unavailable");
        when(responseSpec.bodyToMono(JsonNode.class)).thenReturn(Mono.error(apiFailure));
 
        RuntimeException thrown = assertThrows(
                RuntimeException.class,
                () -> openAIService.parseResume("Some resume text")
        );
        assertEquals("OpenAI API unavailable", thrown.getMessage());
    }
 
    @Test
    void testParseResume_shouldSendBearerTokenWithConfiguredApiKey() throws Exception {
        JsonNode response = buildChatCompletionResponse("{}");
        when(responseSpec.bodyToMono(JsonNode.class)).thenReturn(Mono.just(response));
 
        openAIService.parseResume("Some resume text");
 
        verify(requestBodySpec).header(HttpHeaders.AUTHORIZATION, "Bearer " + API_KEY);
    }
 
    @Test
    void testParseResume_shouldPostToChatCompletionsEndpoint() throws Exception {
        JsonNode response = buildChatCompletionResponse("{}");
        when(responseSpec.bodyToMono(JsonNode.class)).thenReturn(Mono.just(response));
 
        openAIService.parseResume("Some resume text");
 
        verify(requestBodyUriSpec).uri("/chat/completions");
    }
 
    @SuppressWarnings("unchecked")
    @Test
    void testParseResume_shouldBuildRequestBody_withCorrectModelAndMessages() throws Exception {
        JsonNode response = buildChatCompletionResponse("{}");
        when(responseSpec.bodyToMono(JsonNode.class)).thenReturn(Mono.just(response));
 
        ArgumentCaptor<Object> bodyCaptor = ArgumentCaptor.forClass(Object.class);
        openAIService.parseResume("Experienced Java developer");
 
        verify(requestBodySpec).bodyValue(bodyCaptor.capture());
        Map<String, Object> requestBody = (Map<String, Object>) bodyCaptor.getValue();
 
        assertEquals("gpt-4.1-mini", requestBody.get("model"));
        assertEquals(0, requestBody.get("temperature"));
        assertEquals(Map.of("type", "json_object"), requestBody.get("response_format"));
 
        List<Map<String, String>> messages = (List<Map<String, String>>) requestBody.get("messages");
        assertEquals(2, messages.size());
        assertEquals("system", messages.get(0).get("role"));
        assertEquals("You are a resume parser.", messages.get(0).get("content"));
        assertEquals("user", messages.get(1).get("role"));
        assertTrue(messages.get(1).get("content").contains("Experienced Java developer"));
        assertTrue(messages.get(1).get("content").contains("Extract structured data"));
    }
 
    @Test
    void testParseResume_shouldIncludeResumeTextVerbatim_inPrompt() throws Exception {
        JsonNode response = buildChatCompletionResponse("{}");
        when(responseSpec.bodyToMono(JsonNode.class)).thenReturn(Mono.just(response));
 
        ArgumentCaptor<Object> bodyCaptor = ArgumentCaptor.forClass(Object.class);
        String resumeText = "UNIQUE_MARKER_12345 - Senior Backend Engineer";
 
        openAIService.parseResume(resumeText);
 
        verify(requestBodySpec).bodyValue(bodyCaptor.capture());
        @SuppressWarnings("unchecked")
        Map<String, Object> requestBody = (Map<String, Object>) bodyCaptor.getValue();
        @SuppressWarnings("unchecked")
        List<Map<String, String>> messages = (List<Map<String, String>>) requestBody.get("messages");
 
        assertTrue(messages.get(1).get("content").contains(resumeText));
    }
}