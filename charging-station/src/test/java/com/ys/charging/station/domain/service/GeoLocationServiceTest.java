package com.ys.charging.station.domain.service;

import com.ys.charging.station.TestBase;
import com.ys.charging.station.TestDataFactory;
import com.ys.charging.station.domain.model.Location;
import com.ys.charging.station.domain.model.Station;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.GeoOperations;
import org.springframework.data.redis.core.RedisTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * GeoLocationService 单元测试
 * 
 * @author yang
 * @since 2025-06-23
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("地理位置服务测试")
class GeoLocationServiceTest extends TestBase {
    
    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    
    @Mock
    private GeoOperations<String, Object> geoOperations;
    
    private GeoLocationService geoLocationService;
    
    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForGeo()).thenReturn(geoOperations);
        geoLocationService = new RedisGeoLocationService(redisTemplate);
    }
    
    @Nested
    @DisplayName("充电站位置管理测试")
    class StationLocationManagementTest {
        
        @Test
        @DisplayName("应该能够添加充电站位置")
        void shouldAddStationLocation() {
            // Given
            Station station = TestDataFactory.createTestStation();
            setFieldValue(station, "id", TEST_STATION_ID);
            
            when(geoOperations.add(anyString(), any(), anyString())).thenReturn(1L);
            
            // When
            boolean result = geoLocationService.addStationLocation(station);
            
            // Then
            assertTrue(result);
            
            verify(geoOperations).add(eq("stations:geo"), 
                argThat(point -> point.getX() == station.getLocation().getLongitudeForGeo() &&
                               point.getY() == station.getLocation().getLatitudeForGeo()),
                eq("station:" + TEST_STATION_ID));
        }
        
        @Test
        @DisplayName("应该能够更新充电站位置")
        void shouldUpdateStationLocation() {
            // Given
            Location newLocation = Location.of(
                BigDecimal.valueOf(121.505),
                BigDecimal.valueOf(31.245),
                "上海市浦东新区", "上海市", "上海市"
            );
            
            when(geoOperations.remove(anyString(), anyString())).thenReturn(1L);
            when(geoOperations.add(anyString(), any(), anyString())).thenReturn(1L);
            
            // When
            boolean result = geoLocationService.updateStationLocation(TEST_STATION_ID, newLocation);
            
            // Then
            assertTrue(result);
            
            verify(geoOperations).remove("stations:geo", "station:" + TEST_STATION_ID);
            verify(geoOperations).add(eq("stations:geo"), 
                argThat(point -> point.getX() == newLocation.getLongitudeForGeo() &&
                               point.getY() == newLocation.getLatitudeForGeo()),
                eq("station:" + TEST_STATION_ID));
        }
        
        @Test
        @DisplayName("应该能够移除充电站位置")
        void shouldRemoveStationLocation() {
            // Given
            when(geoOperations.remove(anyString(), anyString())).thenReturn(1L);
            
            // When
            boolean result = geoLocationService.removeStationLocation(TEST_STATION_ID);
            
            // Then
            assertTrue(result);
            
            verify(geoOperations).remove("stations:geo", "station:" + TEST_STATION_ID);
        }
        
        @Test
        @DisplayName("应该能够批量添加充电站位置")
        void shouldBatchAddStationLocations() {
            // Given
            List<Station> stations = List.of(
                TestDataFactory.createTestStationInCity("北京市", 116.457, 39.918),
                TestDataFactory.createTestStationInCity("上海市", 121.505, 31.245)
            );
            setFieldValue(stations.get(0), "id", 1L);
            setFieldValue(stations.get(1), "id", 2L);
            
            when(geoOperations.add(anyString(), anyMap())).thenReturn(2L);
            
            // When
            int result = geoLocationService.batchAddStationLocations(stations);
            
            // Then
            assertEquals(2, result);
            
            verify(geoOperations).add(eq("stations:geo"), anyMap());
        }
        
        @Test
        @DisplayName("应该能够批量移除充电站位置")
        void shouldBatchRemoveStationLocations() {
            // Given
            List<Long> stationIds = List.of(1L, 2L, 3L);
            
            when(geoOperations.remove(anyString(), any(String[].class))).thenReturn(3L);
            
            // When
            int result = geoLocationService.batchRemoveStationLocations(stationIds);
            
            // Then
            assertEquals(3, result);
            
            verify(geoOperations).remove(eq("stations:geo"), 
                eq("station:1"), eq("station:2"), eq("station:3"));
        }
    }
    
    @Nested
    @DisplayName("地理搜索测试")
    class GeoSearchTest {
        
        @Test
        @DisplayName("应该能够搜索附近的充电站")
        void shouldSearchNearbyStations() {
            // Given
            GeoSearchCriteria criteria = TestDataFactory.createGeoSearchCriteria();
            
            // Mock Redis GEO 搜索结果
            when(geoOperations.radius(anyString(), any(), any())).thenReturn(mockGeoResults());
            
            // When
            List<GeoSearchResult> results = geoLocationService.searchNearbyStations(criteria);
            
            // Then
            assertNotNull(results);
            assertFalse(results.isEmpty());
            
            verify(geoOperations).radius(eq("stations:geo"), any(), any());
        }
        
        @Test
        @DisplayName("应该能够计算两个充电站之间的距离")
        void shouldCalculateDistanceBetweenStations() {
            // Given
            Long stationId1 = 1L;
            Long stationId2 = 2L;
            
            // Mock Redis GEO 距离计算
            when(geoOperations.distance(anyString(), anyString(), anyString(), any()))
                .thenReturn(org.springframework.data.geo.Distance.of(10.5, org.springframework.data.geo.Metrics.KILOMETERS));
            
            // When
            Optional<Double> result = geoLocationService.calculateDistance(stationId1, stationId2);
            
            // Then
            assertTrue(result.isPresent());
            assertEquals(10.5, result.get(), 0.01);
            
            verify(geoOperations).distance("stations:geo", "station:1", "station:2", 
                org.springframework.data.geo.Metrics.KILOMETERS);
        }
        
        @Test
        @DisplayName("应该能够计算充电站到指定位置的距离")
        void shouldCalculateDistanceToLocation() {
            // Given
            Long stationId = TEST_STATION_ID;
            Location targetLocation = Location.of(
                BigDecimal.valueOf(121.505),
                BigDecimal.valueOf(31.245),
                "上海市浦东新区", "上海市", "上海市"
            );
            
            // Mock Redis GEO 位置查询
            when(geoOperations.position(anyString(), anyString()))
                .thenReturn(List.of(new org.springframework.data.geo.Point(116.457, 39.918)));
            
            // When
            Optional<Double> result = geoLocationService.calculateDistanceToLocation(stationId, targetLocation);
            
            // Then
            assertTrue(result.isPresent());
            assertTrue(result.get() > 1000); // 北京到上海大约1000多公里
            
            verify(geoOperations).position("stations:geo", "station:" + stationId);
        }
        
        @Test
        @DisplayName("应该能够统计指定半径内的充电站数量")
        void shouldCountStationsWithinRadius() {
            // Given
            Location centerLocation = testLocation;
            double radiusKm = 5.0;
            
            // Mock Redis GEO 搜索结果
            when(geoOperations.radius(anyString(), any(), any())).thenReturn(mockGeoResults());
            
            // When
            long result = geoLocationService.countStationsWithinRadius(centerLocation, radiusKm);
            
            // Then
            assertEquals(2, result); // mockGeoResults 返回2个结果
            
            verify(geoOperations).radius(eq("stations:geo"), any(), any());
        }
    }
    
    @Nested
    @DisplayName("数据一致性检查测试")
    class DataConsistencyTest {
        
        @Test
        @DisplayName("应该能够检查地理位置数据一致性")
        void shouldCheckDataConsistency() {
            // Given
            // Mock Redis 中的数据
            when(geoOperations.radius(anyString(), any(), any())).thenReturn(mockGeoResults());
            
            // When
            GeoLocationService.GeoDataConsistencyReport report = geoLocationService.checkDataConsistency();
            
            // Then
            assertNotNull(report);
            assertNotNull(report.missingInGeo());
            assertNotNull(report.missingInDatabase());
            assertNotNull(report.getSummary());
        }
        
        @Test
        @DisplayName("数据一致时应该返回一致性报告")
        void shouldReturnConsistentReportWhenDataIsConsistent() {
            // Given
            // Mock 一致的数据
            when(geoOperations.radius(anyString(), any(), any())).thenReturn(List.of());
            
            // When
            GeoLocationService.GeoDataConsistencyReport report = geoLocationService.checkDataConsistency();
            
            // Then
            assertTrue(report.isConsistent());
            assertTrue(report.missingInGeo().isEmpty());
            assertTrue(report.missingInDatabase().isEmpty());
        }
    }
    
    @Nested
    @DisplayName("异常处理测试")
    class ExceptionHandlingTest {
        
        @Test
        @DisplayName("Redis 连接异常时应该优雅处理")
        void shouldHandleRedisConnectionException() {
            // Given
            when(geoOperations.add(anyString(), any(), anyString()))
                .thenThrow(new org.springframework.data.redis.RedisConnectionFailureException("Redis连接失败"));
            
            Station station = TestDataFactory.createTestStation();
            setFieldValue(station, "id", TEST_STATION_ID);
            
            // When & Then
            assertDoesNotThrow(() -> {
                boolean result = geoLocationService.addStationLocation(station);
                assertFalse(result); // 应该返回 false 表示操作失败
            });
        }
        
        @Test
        @DisplayName("无效的地理位置数据应该被拒绝")
        void shouldRejectInvalidGeoData() {
            // Given
            Station stationWithInvalidLocation = TestDataFactory.createTestStation();
            // 设置无效的位置（超出范围）
            Location invalidLocation = Location.of(
                BigDecimal.valueOf(200), // 无效经度
                BigDecimal.valueOf(100), // 无效纬度
                "无效地址", "无效城市", "无效省份"
            );
            setFieldValue(stationWithInvalidLocation, "location", invalidLocation);
            setFieldValue(stationWithInvalidLocation, "id", TEST_STATION_ID);
            
            // When & Then
            assertThrows(IllegalArgumentException.class, () -> {
                geoLocationService.addStationLocation(stationWithInvalidLocation);
            });
        }
    }
    
    // 辅助方法
    private List<org.springframework.data.redis.connection.RedisGeoCommands.GeoLocation<Object>> mockGeoResults() {
        return List.of(
            new org.springframework.data.redis.connection.RedisGeoCommands.GeoLocation<>("station:1", 
                new org.springframework.data.geo.Point(116.457, 39.918)),
            new org.springframework.data.redis.connection.RedisGeoCommands.GeoLocation<>("station:2", 
                new org.springframework.data.geo.Point(116.467, 39.928))
        );
    }
}
