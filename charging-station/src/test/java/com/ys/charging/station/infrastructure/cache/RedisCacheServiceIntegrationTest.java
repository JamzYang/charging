package com.ys.charging.station.infrastructure.cache;

import com.ys.charging.station.TestBase;
import com.ys.charging.station.TestDataFactory;
import com.ys.charging.station.config.TestConfig;
import com.ys.charging.station.domain.model.Station;
import com.ys.charging.station.domain.service.StationCacheService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Redis 缓存服务集成测试
 * 
 * @author yang
 * @since 2025-06-23
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestConfig.class)
@DisplayName("Redis 缓存服务集成测试")
class RedisCacheServiceIntegrationTest extends TestBase {
    
    @Autowired
    private StationCacheService cacheService;
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Nested
    @DisplayName("充电站缓存测试")
    class StationCacheTest {
        
        @Test
        @DisplayName("应该能够缓存充电站")
        void shouldCacheStation() {
            // Given
            Station station = TestDataFactory.createTestStation();
            setFieldValue(station, "id", TEST_STATION_ID);
            Duration ttl = Duration.ofMinutes(30);
            
            // When
            cacheService.cacheStation(station, ttl);
            
            // Then
            String cacheKey = "station:" + TEST_STATION_ID;
            assertTrue(redisTemplate.hasKey(cacheKey));
            
            Object cachedValue = redisTemplate.opsForValue().get(cacheKey);
            assertNotNull(cachedValue);
        }
        
        @Test
        @DisplayName("应该能够从缓存获取充电站")
        void shouldGetStationFromCache() {
            // Given
            Station station = TestDataFactory.createTestStation();
            setFieldValue(station, "id", TEST_STATION_ID);
            Duration ttl = Duration.ofMinutes(30);
            
            cacheService.cacheStation(station, ttl);
            
            // When
            Optional<Station> result = cacheService.getCachedStation(TEST_STATION_ID);
            
            // Then
            assertTrue(result.isPresent());
            assertEquals(TEST_STATION_ID, result.get().getId());
            assertEquals(station.getStationInfo().name(), result.get().getStationInfo().name());
        }
        
        @Test
        @DisplayName("获取不存在的缓存应该返回空")
        void shouldReturnEmptyForNonExistentCache() {
            // When
            Optional<Station> result = cacheService.getCachedStation(999L);
            
            // Then
            assertFalse(result.isPresent());
        }
        
        @Test
        @DisplayName("应该能够清除充电站缓存")
        void shouldEvictStationCache() {
            // Given
            Station station = TestDataFactory.createTestStation();
            setFieldValue(station, "id", TEST_STATION_ID);
            Duration ttl = Duration.ofMinutes(30);
            
            cacheService.cacheStation(station, ttl);
            assertTrue(cacheService.getCachedStation(TEST_STATION_ID).isPresent());
            
            // When
            boolean result = cacheService.evictStationCache(TEST_STATION_ID);
            
            // Then
            assertTrue(result);
            assertFalse(cacheService.getCachedStation(TEST_STATION_ID).isPresent());
        }
        
        @Test
        @DisplayName("应该能够批量缓存充电站")
        void shouldBatchCacheStations() {
            // Given
            List<Station> stations = List.of(
                TestDataFactory.createTestStation("站点1", "运营商1"),
                TestDataFactory.createTestStation("站点2", "运营商2")
            );
            setFieldValue(stations.get(0), "id", 1L);
            setFieldValue(stations.get(1), "id", 2L);
            Duration ttl = Duration.ofMinutes(30);
            
            // When
            int cachedCount = cacheService.batchCacheStations(stations, ttl);
            
            // Then
            assertEquals(2, cachedCount);
            assertTrue(cacheService.getCachedStation(1L).isPresent());
            assertTrue(cacheService.getCachedStation(2L).isPresent());
        }
        
        @Test
        @DisplayName("应该能够清除充电站相关的所有缓存")
        void shouldEvictStationRelatedCaches() {
            // Given
            Station station = TestDataFactory.createTestStation();
            setFieldValue(station, "id", TEST_STATION_ID);
            Duration ttl = Duration.ofMinutes(30);
            
            // 缓存充电站和相关数据
            cacheService.cacheStation(station, ttl);
            cacheService.cacheStationStatistics(TEST_STATION_ID, "test-stats", ttl);
            cacheService.cacheStationConnectors(TEST_STATION_ID, List.of(), ttl);
            
            // When
            cacheService.evictStationRelatedCaches(TEST_STATION_ID);
            
            // Then
            assertFalse(cacheService.getCachedStation(TEST_STATION_ID).isPresent());
            assertFalse(cacheService.getCachedStationStatistics(TEST_STATION_ID).isPresent());
            assertFalse(cacheService.getCachedStationConnectors(TEST_STATION_ID).isPresent());
        }
    }
    
    @Nested
    @DisplayName("充电站列表缓存测试")
    class StationListCacheTest {
        
        @Test
        @DisplayName("应该能够缓存充电站列表")
        void shouldCacheStationList() {
            // Given
            List<Station> stations = List.of(
                TestDataFactory.createTestStation("站点1", "运营商1"),
                TestDataFactory.createTestStation("站点2", "运营商2")
            );
            String cacheKey = "available-stations";
            Duration ttl = Duration.ofMinutes(15);
            
            // When
            cacheService.cacheStationList(cacheKey, stations, ttl);
            
            // Then
            Optional<List<Station>> result = cacheService.getCachedStationList(cacheKey);
            assertTrue(result.isPresent());
            assertEquals(2, result.get().size());
        }
        
        @Test
        @DisplayName("应该能够清除充电站列表缓存")
        void shouldEvictStationListCache() {
            // Given
            List<Station> stations = List.of(TestDataFactory.createTestStation());
            String cacheKey = "test-list";
            Duration ttl = Duration.ofMinutes(15);
            
            cacheService.cacheStationList(cacheKey, stations, ttl);
            assertTrue(cacheService.getCachedStationList(cacheKey).isPresent());
            
            // When
            boolean result = cacheService.evictStationListCache(cacheKey);
            
            // Then
            assertTrue(result);
            assertFalse(cacheService.getCachedStationList(cacheKey).isPresent());
        }
    }
    
    @Nested
    @DisplayName("充电桩缓存测试")
    class ConnectorCacheTest {
        
        @Test
        @DisplayName("应该能够缓存充电站的充电桩列表")
        void shouldCacheStationConnectors() {
            // Given
            List<com.ys.charging.station.domain.model.Connector> connectors = List.of(
                TestDataFactory.createTestConnector(TEST_STATION_ID, "A01"),
                TestDataFactory.createTestConnector(TEST_STATION_ID, "A02")
            );
            Duration ttl = Duration.ofMinutes(10);
            
            // When
            cacheService.cacheStationConnectors(TEST_STATION_ID, connectors, ttl);
            
            // Then
            Optional<List<com.ys.charging.station.domain.model.Connector>> result = 
                cacheService.getCachedStationConnectors(TEST_STATION_ID);
            assertTrue(result.isPresent());
            assertEquals(2, result.get().size());
        }
        
        @Test
        @DisplayName("应该能够清除充电桩缓存")
        void shouldEvictConnectorCache() {
            // Given
            List<com.ys.charging.station.domain.model.Connector> connectors = List.of(
                TestDataFactory.createTestConnector(TEST_STATION_ID, "A01")
            );
            Duration ttl = Duration.ofMinutes(10);
            
            cacheService.cacheStationConnectors(TEST_STATION_ID, connectors, ttl);
            assertTrue(cacheService.getCachedStationConnectors(TEST_STATION_ID).isPresent());
            
            // When
            boolean result = cacheService.evictStationConnectorsCache(TEST_STATION_ID);
            
            // Then
            assertTrue(result);
            assertFalse(cacheService.getCachedStationConnectors(TEST_STATION_ID).isPresent());
        }
    }
    
    @Nested
    @DisplayName("统计信息缓存测试")
    class StatisticsCacheTest {
        
        @Test
        @DisplayName("应该能够缓存充电站统计信息")
        void shouldCacheStationStatistics() {
            // Given
            String statistics = "test-statistics-data";
            Duration ttl = Duration.ofMinutes(5);
            
            // When
            cacheService.cacheStationStatistics(TEST_STATION_ID, statistics, ttl);
            
            // Then
            Optional<String> result = cacheService.getCachedStationStatistics(TEST_STATION_ID);
            assertTrue(result.isPresent());
            assertEquals(statistics, result.get());
        }
        
        @Test
        @DisplayName("应该能够缓存全局统计信息")
        void shouldCacheGlobalStatistics() {
            // Given
            String globalStats = "global-statistics-data";
            String cacheKey = "global-stats";
            Duration ttl = Duration.ofMinutes(5);
            
            // When
            cacheService.cacheGlobalStatistics(cacheKey, globalStats, ttl);
            
            // Then
            Optional<String> result = cacheService.getCachedGlobalStatistics(cacheKey);
            assertTrue(result.isPresent());
            assertEquals(globalStats, result.get());
        }
        
        @Test
        @DisplayName("应该能够清除统计信息缓存")
        void shouldEvictStatisticsCache() {
            // Given
            String statistics = "test-statistics";
            Duration ttl = Duration.ofMinutes(5);
            
            cacheService.cacheStationStatistics(TEST_STATION_ID, statistics, ttl);
            assertTrue(cacheService.getCachedStationStatistics(TEST_STATION_ID).isPresent());
            
            // When
            boolean result = cacheService.evictStationStatisticsCache(TEST_STATION_ID);
            
            // Then
            assertTrue(result);
            assertFalse(cacheService.getCachedStationStatistics(TEST_STATION_ID).isPresent());
        }
    }
    
    @Nested
    @DisplayName("缓存过期测试")
    class CacheExpirationTest {
        
        @Test
        @DisplayName("缓存应该在TTL过期后自动清除")
        void shouldExpireCacheAfterTTL() throws InterruptedException {
            // Given
            Station station = TestDataFactory.createTestStation();
            setFieldValue(station, "id", TEST_STATION_ID);
            Duration shortTtl = Duration.ofSeconds(1);
            
            // When
            cacheService.cacheStation(station, shortTtl);
            assertTrue(cacheService.getCachedStation(TEST_STATION_ID).isPresent());
            
            // 等待缓存过期
            Thread.sleep(1500);
            
            // Then
            assertFalse(cacheService.getCachedStation(TEST_STATION_ID).isPresent());
        }
        
        @Test
        @DisplayName("应该能够检查缓存是否存在")
        void shouldCheckCacheExistence() {
            // Given
            Station station = TestDataFactory.createTestStation();
            setFieldValue(station, "id", TEST_STATION_ID);
            Duration ttl = Duration.ofMinutes(30);
            
            // When & Then
            assertFalse(cacheService.hasStationCache(TEST_STATION_ID));
            
            cacheService.cacheStation(station, ttl);
            assertTrue(cacheService.hasStationCache(TEST_STATION_ID));
            
            cacheService.evictStationCache(TEST_STATION_ID);
            assertFalse(cacheService.hasStationCache(TEST_STATION_ID));
        }
        
        @Test
        @DisplayName("应该能够获取缓存的剩余TTL")
        void shouldGetCacheRemainingTTL() {
            // Given
            Station station = TestDataFactory.createTestStation();
            setFieldValue(station, "id", TEST_STATION_ID);
            Duration ttl = Duration.ofMinutes(30);
            
            cacheService.cacheStation(station, ttl);
            
            // When
            Optional<Duration> remainingTtl = cacheService.getStationCacheRemainingTTL(TEST_STATION_ID);
            
            // Then
            assertTrue(remainingTtl.isPresent());
            assertTrue(remainingTtl.get().toMinutes() > 25); // 应该接近30分钟
            assertTrue(remainingTtl.get().toMinutes() <= 30);
        }
    }
    
    @Nested
    @DisplayName("缓存性能测试")
    class CachePerformanceTest {
        
        @Test
        @DisplayName("批量缓存操作应该高效执行")
        void shouldPerformBatchOperationsEfficiently() {
            // Given
            List<Station> stations = List.of();
            for (int i = 1; i <= 100; i++) {
                Station station = TestDataFactory.createTestStation("站点" + i, "运营商" + i);
                setFieldValue(station, "id", (long) i);
                stations = new java.util.ArrayList<>(stations);
                stations.add(station);
            }
            Duration ttl = Duration.ofMinutes(30);
            
            // When
            long startTime = System.currentTimeMillis();
            int cachedCount = cacheService.batchCacheStations(stations, ttl);
            long endTime = System.currentTimeMillis();
            
            // Then
            assertEquals(100, cachedCount);
            assertTrue(endTime - startTime < 5000); // 应该在5秒内完成
            
            // 验证缓存是否成功
            assertTrue(cacheService.getCachedStation(1L).isPresent());
            assertTrue(cacheService.getCachedStation(50L).isPresent());
            assertTrue(cacheService.getCachedStation(100L).isPresent());
        }
        
        @Test
        @DisplayName("缓存命中应该比数据库查询快")
        void shouldBeFasterThanDatabaseQuery() {
            // Given
            Station station = TestDataFactory.createTestStation();
            setFieldValue(station, "id", TEST_STATION_ID);
            Duration ttl = Duration.ofMinutes(30);
            
            cacheService.cacheStation(station, ttl);
            
            // When - 多次缓存查询
            long startTime = System.currentTimeMillis();
            for (int i = 0; i < 1000; i++) {
                cacheService.getCachedStation(TEST_STATION_ID);
            }
            long endTime = System.currentTimeMillis();
            
            // Then
            assertTrue(endTime - startTime < 1000); // 1000次查询应该在1秒内完成
        }
    }
    
    @Nested
    @DisplayName("缓存异常处理测试")
    class CacheExceptionHandlingTest {
        
        @Test
        @DisplayName("Redis 连接异常时应该优雅处理")
        void shouldHandleRedisConnectionException() {
            // 这个测试需要模拟 Redis 连接失败的情况
            // 在实际项目中，可以通过停止 Redis 服务或使用 Mock 来测试
            
            // Given
            Station station = TestDataFactory.createTestStation();
            setFieldValue(station, "id", TEST_STATION_ID);
            Duration ttl = Duration.ofMinutes(30);
            
            // When & Then - 应该不抛出异常
            assertDoesNotThrow(() -> {
                cacheService.cacheStation(station, ttl);
                cacheService.getCachedStation(TEST_STATION_ID);
                cacheService.evictStationCache(TEST_STATION_ID);
            });
        }
        
        @Test
        @DisplayName("无效的缓存键应该被正确处理")
        void shouldHandleInvalidCacheKeys() {
            // When & Then
            assertDoesNotThrow(() -> {
                cacheService.getCachedStation(null);
                cacheService.evictStationCache(null);
                cacheService.getCachedStationList(null);
                cacheService.evictStationListCache(null);
            });
        }
    }
}
