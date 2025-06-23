package com.ys.charging.station.infrastructure.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 缓存配置类
 * 
 * 配置多级缓存策略：
 * 1. Caffeine 本地缓存 - 高频访问数据，毫秒级响应
 * 2. Redis 分布式缓存 - 共享数据，支持集群
 * 
 * 缓存策略：
 * - 充电站基本信息：本地缓存30分钟，Redis缓存2小时
 * - 充电桩状态：本地缓存5分钟，Redis缓存1小时
 * - 地理位置数据：Redis缓存24小时
 * - 统计数据：本地缓存10分钟，Redis缓存30分钟
 * 
 * @author yang
 * @since 2025-06-23
 */
@Configuration
@EnableCaching
public class CacheConfig {
    
    /**
     * Caffeine 本地缓存管理器
     */
    @Bean
    @Primary
    public CacheManager caffeineCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        
        // 设置默认缓存配置
        cacheManager.setCaffeine(Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(30, TimeUnit.MINUTES)
            .expireAfterAccess(10, TimeUnit.MINUTES)
            .recordStats());
        
        // 设置缓存名称
        cacheManager.setCacheNames(
            "stations",           // 充电站信息
            "connectors",         // 充电桩信息
            "connector-status",   // 充电桩状态
            "station-statistics", // 充电站统计
            "geo-search-results"  // 地理搜索结果
        );
        
        return cacheManager;
    }
    
    /**
     * Redis 分布式缓存管理器
     */
    @Bean
    public CacheManager redisCacheManager(RedisConnectionFactory redisConnectionFactory) {
        // 默认缓存配置
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofHours(2))
            .serializeKeysWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new GenericJackson2JsonRedisSerializer()))
            .disableCachingNullValues();
        
        // 特定缓存配置
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        
        // 充电站信息 - 2小时
        cacheConfigurations.put("redis:stations", defaultConfig
            .entryTtl(Duration.ofHours(2)));
        
        // 充电桩状态 - 1小时
        cacheConfigurations.put("redis:connector-status", defaultConfig
            .entryTtl(Duration.ofHours(1)));
        
        // 地理位置数据 - 24小时
        cacheConfigurations.put("redis:geo-locations", defaultConfig
            .entryTtl(Duration.ofHours(24)));
        
        // 统计数据 - 30分钟
        cacheConfigurations.put("redis:statistics", defaultConfig
            .entryTtl(Duration.ofMinutes(30)));
        
        // 搜索结果 - 10分钟
        cacheConfigurations.put("redis:search-results", defaultConfig
            .entryTtl(Duration.ofMinutes(10)));
        
        return RedisCacheManager.builder(redisConnectionFactory)
            .cacheDefaults(defaultConfig)
            .withInitialCacheConfigurations(cacheConfigurations)
            .build();
    }
    
    /**
     * Redis 模板配置
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);
        
        // 设置序列化器
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer();
        
        // Key 序列化
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        
        // Value 序列化
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);
        
        template.afterPropertiesSet();
        return template;
    }
    
    /**
     * 缓存键生成器
     */
    @Bean
    public org.springframework.cache.interceptor.KeyGenerator customKeyGenerator() {
        return (target, method, params) -> {
            StringBuilder key = new StringBuilder();
            key.append(target.getClass().getSimpleName()).append(":");
            key.append(method.getName()).append(":");
            
            for (Object param : params) {
                if (param != null) {
                    key.append(param.toString()).append(":");
                }
            }
            
            // 移除最后的冒号
            if (key.length() > 0 && key.charAt(key.length() - 1) == ':') {
                key.setLength(key.length() - 1);
            }
            
            return key.toString();
        };
    }
    
    /**
     * 缓存统计信息
     */
    public static class CacheStatistics {
        
        /**
         * 获取 Caffeine 缓存统计
         */
        public static Map<String, Object> getCaffeineStats(CacheManager cacheManager) {
            Map<String, Object> stats = new HashMap<>();
            
            if (cacheManager instanceof CaffeineCacheManager caffeineCacheManager) {
                for (String cacheName : caffeineCacheManager.getCacheNames()) {
                    org.springframework.cache.Cache cache = caffeineCacheManager.getCache(cacheName);
                    if (cache instanceof org.springframework.cache.caffeine.CaffeineCache caffeineCache) {
                        com.github.benmanes.caffeine.cache.stats.CacheStats cacheStats = 
                            caffeineCache.getNativeCache().stats();
                        
                        Map<String, Object> cacheStatMap = new HashMap<>();
                        cacheStatMap.put("hitCount", cacheStats.hitCount());
                        cacheStatMap.put("missCount", cacheStats.missCount());
                        cacheStatMap.put("hitRate", cacheStats.hitRate());
                        cacheStatMap.put("evictionCount", cacheStats.evictionCount());
                        cacheStatMap.put("estimatedSize", caffeineCache.getNativeCache().estimatedSize());
                        
                        stats.put(cacheName, cacheStatMap);
                    }
                }
            }
            
            return stats;
        }
        
        /**
         * 获取缓存使用摘要
         */
        public static String getCacheSummary(CacheManager cacheManager) {
            Map<String, Object> stats = getCaffeineStats(cacheManager);
            
            if (stats.isEmpty()) {
                return "无缓存统计信息";
            }
            
            StringBuilder summary = new StringBuilder("缓存统计:\n");
            
            for (Map.Entry<String, Object> entry : stats.entrySet()) {
                String cacheName = entry.getKey();
                @SuppressWarnings("unchecked")
                Map<String, Object> cacheStats = (Map<String, Object>) entry.getValue();
                
                summary.append(String.format("  %s: 命中率=%.2f%%, 大小=%d, 驱逐=%d\n",
                    cacheName,
                    (Double) cacheStats.get("hitRate") * 100,
                    (Long) cacheStats.get("estimatedSize"),
                    (Long) cacheStats.get("evictionCount")
                ));
            }
            
            return summary.toString();
        }
    }
    
    /**
     * 缓存预热配置
     */
    public static class CacheWarmup {
        
        /**
         * 预热充电站缓存
         */
        public static void warmupStationCache() {
            // 这里可以在应用启动时预加载热点数据
            // 例如：加载所有运营中的充电站信息
        }
        
        /**
         * 预热地理位置缓存
         */
        public static void warmupGeoCache() {
            // 预加载地理位置数据到 Redis GEO
        }
    }
}
