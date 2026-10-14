package com.project.tracker.config;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.tracker.dto.HabitLogResponseDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.lang.Nullable;

import java.time.Duration;
import java.util.List;

@Configuration
@EnableCaching
public class CacheConfig implements CachingConfigurer {
    public static final String LOGS_BY_MONTH = "logsByMonth";
    public static final Logger logger = LoggerFactory.getLogger(CacheConfig.class);

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory, ObjectMapper objectMapper) {
        JavaType logList=objectMapper.getTypeFactory().constructCollectionType(List.class, HabitLogResponseDTO.class);
        RedisCacheConfiguration logsByMonth = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(1)).disableCachingNullValues().serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer( new Jackson2JsonRedisSerializer<Object>(objectMapper,logList)));

        return RedisCacheManager.builder(connectionFactory).withCacheConfiguration(LOGS_BY_MONTH,logsByMonth).build();
    }

    private static void warn(String op, Cache cache, Object key, RuntimeException e) {
        logger.warn("Redis cache {} failed for {}::{}, falling back to Postgres: {}", op, cache.getName(), key, e.getMessage());
    }
    @Override
    public CacheErrorHandler errorHandler() {
        return new CacheErrorHandler() {
            public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
                warn("GET",cache,key,exception);
            }

            @Override
            public void handleCachePutError(RuntimeException exception, Cache cache, Object key, @Nullable Object value) {
                warn("PUT",cache,key,exception);
            }

            @Override
            public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
                warn("EVICT",cache,key,exception);
            }

            @Override
            public void handleCacheClearError(RuntimeException exception, Cache cache) {
                warn("CLEAR",cache,"*",exception);
            }
        };
    }
}
