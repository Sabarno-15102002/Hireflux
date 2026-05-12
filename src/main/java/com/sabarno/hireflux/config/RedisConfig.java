package com.sabarno.hireflux.config;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@Configuration
@EnableCaching
public class RedisConfig {

        @Bean
        RedisCacheManager cacheManager(
                RedisConnectionFactory connectionFactory,
                ObjectMapper objectMapper
        ) {

                ObjectMapper redisObjectMapper = objectMapper.copy();
                redisObjectMapper.registerModule(new JavaTimeModule());
                redisObjectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

                GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(
                                redisObjectMapper);

                RedisSerializationContext.SerializationPair<Object> pair = RedisSerializationContext.SerializationPair
                                .fromSerializer(serializer);

                RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                                .serializeValuesWith(pair)
                                .disableCachingNullValues()
                                .entryTtl(Duration.ofMinutes(10));

                Map<String, RedisCacheConfiguration> cacheConfigs = new HashMap<>();

                cacheConfigs.put("jobs",
                                defaultConfig.entryTtl(Duration.ofMinutes(5)));

                cacheConfigs.put("job",
                                defaultConfig.entryTtl(Duration.ofMinutes(10)));

                cacheConfigs.put("users",
                                defaultConfig.entryTtl(Duration.ofMinutes(15)));

                cacheConfigs.put("resume",
                                defaultConfig.entryTtl(Duration.ofMinutes(3)));

                cacheConfigs.put("user_resumes",
                                defaultConfig.entryTtl(Duration.ofMinutes(3)));

                return RedisCacheManager.builder(connectionFactory)
                                .cacheDefaults(defaultConfig)
                                .withInitialCacheConfigurations(cacheConfigs)
                                .transactionAware()
                                .build();
        }

        @Bean
        RedisTemplate<String, Object> redisTemplate(
                        RedisConnectionFactory connectionFactory,
                        ObjectMapper objectMapper) {

                ObjectMapper redisObjectMapper = objectMapper.copy();
                redisObjectMapper.registerModule(new JavaTimeModule());
                redisObjectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

                GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(
                                redisObjectMapper);

                RedisTemplate<String, Object> template = new RedisTemplate<>();
                template.setConnectionFactory(connectionFactory);

                template.setKeySerializer(new StringRedisSerializer());
                template.setHashKeySerializer(new StringRedisSerializer());

                template.setValueSerializer(serializer);
                template.setHashValueSerializer(serializer);

                template.afterPropertiesSet();
                return template;
        }
}