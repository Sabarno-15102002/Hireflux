package com.sabarno.hireflux.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RedisConfigTest {

    private RedisConfig redisConfig;

    private RedisConnectionFactory connectionFactory;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {

        redisConfig = new RedisConfig();

        connectionFactory = mock(RedisConnectionFactory.class);

        objectMapper = new ObjectMapper();
    }

    @Test
    void cacheManager_shouldCreateRedisCacheManager() {

        RedisCacheManager cacheManager =
                redisConfig.cacheManager(
                        connectionFactory,
                        objectMapper);

        assertNotNull(cacheManager);
    }

    @Test
    void redisTemplate_shouldCreateRedisTemplate() {

        RedisTemplate<String, Object> template =
                redisConfig.redisTemplate(
                        connectionFactory,
                        objectMapper);

        assertNotNull(template);

        assertSame(
                connectionFactory,
                template.getConnectionFactory());

        assertTrue(
                template.getKeySerializer()
                        instanceof StringRedisSerializer);

        assertTrue(
                template.getHashKeySerializer()
                        instanceof StringRedisSerializer);

        assertTrue(
                template.getValueSerializer()
                        instanceof GenericJackson2JsonRedisSerializer);

        assertTrue(
                template.getHashValueSerializer()
                        instanceof GenericJackson2JsonRedisSerializer);
    }
}