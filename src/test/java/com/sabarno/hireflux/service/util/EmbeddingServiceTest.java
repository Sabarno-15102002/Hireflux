package com.sabarno.hireflux.service.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class EmbeddingServiceTest {
 
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
 
    private MeterRegistry meterRegistry;
 
    private EmbeddingService embeddingService;
 
    private static final String API_KEY = "test-api-key-123";
    private final ObjectMapper objectMapper = new ObjectMapper();
 
    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        embeddingService = new EmbeddingService(meterRegistry);
        ReflectionTestUtils.setField(embeddingService, "webClient", webClient);
        ReflectionTestUtils.setField(embeddingService, "apiKey", API_KEY);
 
        lenient().when(webClient.post()).thenReturn(requestBodyUriSpec);
        lenient().when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        lenient().when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        lenient().when(requestBodySpec.bodyValue(any()))
                .thenReturn((WebClient.RequestHeadersSpec) requestHeadersSpec);
        lenient().when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
    }
 
    private JsonNode buildEmbeddingResponse(List<Double> embedding) throws Exception {
        String json = """
                {
                  "data": [
                    { "embedding": %s }
                  ]
                }
                """.formatted(objectMapper.writeValueAsString(embedding));
        return objectMapper.readTree(json);
    }
 
    @Test
    void testCreateEmbedding_shouldReturnParsedVector_whenResponseIsValid() throws Exception {
        List<Double> expectedEmbedding = List.of(0.1, 0.2, 0.3);
        JsonNode response = buildEmbeddingResponse(expectedEmbedding);
        when(responseSpec.bodyToMono(JsonNode.class)).thenReturn(Mono.just(response));
 
        List<Double> result = embeddingService.createEmbedding("some job text");
 
        assertEquals(expectedEmbedding, result);
    }
 
    @Test
    void testCreateEmbedding_shouldNotIncrementFailureCounter_onSuccess() throws Exception {
        JsonNode response = buildEmbeddingResponse(List.of(0.5));
        when(responseSpec.bodyToMono(JsonNode.class)).thenReturn(Mono.just(response));
 
        embeddingService.createEmbedding("some job text");
 
        assertEquals(0.0, meterRegistry.counter("embedding.failures").count());
    }
 
    @Test
    void testCreateEmbedding_shouldHandleLargeEmbeddingVector() throws Exception {
        List<Double> largeVector = java.util.stream.IntStream.range(0, 1536)
                .mapToObj(i -> i * 0.001)
                .toList();
        JsonNode response = buildEmbeddingResponse(largeVector);
        when(responseSpec.bodyToMono(JsonNode.class)).thenReturn(Mono.just(response));
 
        List<Double> result = embeddingService.createEmbedding("some job text");
 
        assertEquals(1536, result.size());
        assertEquals(largeVector, result);
    }
 
    @Test
    void testCreateEmbedding_shouldIncrementFailureCounter_whenWebClientCallFails() {
        RuntimeException apiFailure = new RuntimeException("OpenAI API unavailable");
        when(responseSpec.bodyToMono(JsonNode.class)).thenReturn(Mono.error(apiFailure));
 
        assertThrows(RuntimeException.class,
                () -> embeddingService.createEmbedding("some job text"));
 
        assertEquals(1.0, meterRegistry.counter("embedding.failures").count());
    }
 
    @Test
    void testCreateEmbedding_shouldPropagateOriginalException_afterRecordingFailure() {
        RuntimeException apiFailure = new RuntimeException("Rate limit exceeded");
        when(responseSpec.bodyToMono(JsonNode.class)).thenReturn(Mono.error(apiFailure));
 
        RuntimeException thrown = assertThrows(RuntimeException.class,
                () -> embeddingService.createEmbedding("some job text"));
 
        assertEquals("Rate limit exceeded", thrown.getMessage());
    }
 
    @Test
    void testCreateEmbedding_shouldThrowNullPointerException_whenResponseHasNoDataField() throws Exception {
        JsonNode malformedResponse = objectMapper.readTree("{}");
        when(responseSpec.bodyToMono(JsonNode.class)).thenReturn(Mono.just(malformedResponse));
 
        assertThrows(NullPointerException.class,
                () -> embeddingService.createEmbedding("some job text"));
        assertEquals(1.0, meterRegistry.counter("embedding.failures").count());
    }
 
    @Test
    void testCreateEmbedding_shouldReturnNull_whenReactiveResponseCompletesEmpty() {
        when(responseSpec.bodyToMono(JsonNode.class)).thenReturn(Mono.empty());
 
        List<Double> result = embeddingService.createEmbedding("some job text");
 
        assertNull(result);
    }
 
    @Test
    void testCreateEmbedding_shouldPostToEmbeddingsEndpoint() throws Exception {
        JsonNode response = buildEmbeddingResponse(List.of(0.1));
        when(responseSpec.bodyToMono(JsonNode.class)).thenReturn(Mono.just(response));
 
        embeddingService.createEmbedding("some job text");
 
        verify(requestBodyUriSpec).uri("/embeddings");
    }
 
    @Test
    void testCreateEmbedding_shouldSendBearerTokenWithConfiguredApiKey() throws Exception {
        JsonNode response = buildEmbeddingResponse(List.of(0.1));
        when(responseSpec.bodyToMono(JsonNode.class)).thenReturn(Mono.just(response));
 
        embeddingService.createEmbedding("some job text");
 
        verify(requestBodySpec).header(HttpHeaders.AUTHORIZATION, "Bearer " + API_KEY);
    }
 
    @SuppressWarnings("unchecked")
    @Test
    void testCreateEmbedding_shouldSendCorrectModelAndInputText() throws Exception {
        JsonNode response = buildEmbeddingResponse(List.of(0.1));
        when(responseSpec.bodyToMono(JsonNode.class)).thenReturn(Mono.just(response));
 
        ArgumentCaptor<Object> bodyCaptor = ArgumentCaptor.forClass(Object.class);
        embeddingService.createEmbedding("Backend Engineer role description");
 
        verify(requestBodySpec).bodyValue(bodyCaptor.capture());
        Map<String, Object> requestBody = (Map<String, Object>) bodyCaptor.getValue();
 
        assertEquals("text-embedding-3-small", requestBody.get("model"));
        assertEquals("Backend Engineer role description", requestBody.get("input"));
    }
}