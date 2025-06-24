package com.ys.charging.station.application.service;

import com.ys.charging.station.TestBase;
import com.ys.charging.station.TestDataFactory;
import com.ys.charging.station.domain.model.*;
import com.ys.charging.station.domain.repository.StationRepository;
import com.ys.charging.station.domain.service.GeoLocationService;
import com.ys.charging.station.domain.service.StationCacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * StationService 单元测试
 * 
 * @author yang
 * @since 2025-06-23
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("充电站应用服务测试")
class StationServiceTest extends TestBase {
    
    @Mock
    private StationRepository stationRepository;
    
    @Mock
    private GeoLocationService geoLocationService;
    
    @Mock
    private StationCacheService cacheService;
    
    private StationService stationService;
    
    @BeforeEach
    void setUp() {
        stationService = new StationService(stationRepository, geoLocationService, cacheService);
    }
    
    @Nested
    @DisplayName("充电站创建测试")
    class StationCreationTest {
        
        @Test
        @DisplayName("应该能够创建充电站")
        void shouldCreateStation() {
            // Given
            Station expectedStation = TestDataFactory.createTestStation();
            setFieldValue(expectedStation, "id", TEST_STATION_ID);
            
            when(stationRepository.existsByNameAndOperator(anyString(), anyString())).thenReturn(false);
            when(stationRepository.save(any(Station.class))).thenReturn(expectedStation);
            
            // When
            Station result = stationService.createStation(testStationInfo, testLocation, testBusinessHours);
            
            // Then
            assertNotNull(result);
            assertEquals(TEST_STATION_ID, result.getId());
            assertEquals(testStationInfo, result.getStationInfo());
            assertEquals(testLocation, result.getLocation());
            assertEquals(testBusinessHours, result.getBusinessHours());
            
            // 验证交互
            verify(stationRepository).existsByNameAndOperator(testStationInfo.name(), testStationInfo.operator());
            verify(stationRepository).save(any(Station.class));
            verify(geoLocationService).addStationLocation(expectedStation);
            verify(cacheService).cacheStation(expectedStation, Duration.ofHours(2));
        }
        
        @Test
        @DisplayName("创建重复名称和运营商的充电站应该抛出异常")
        void shouldThrowExceptionWhenCreatingDuplicateStation() {
            // Given
            when(stationRepository.existsByNameAndOperator(anyString(), anyString())).thenReturn(true);
            
            // When & Then
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
                stationService.createStation(testStationInfo, testLocation, testBusinessHours);
            });
            
            assertTrue(exception.getMessage().contains("充电站已存在"));
            
            // 验证没有保存操作
            verify(stationRepository, never()).save(any(Station.class));
            verify(geoLocationService, never()).addStationLocation(any(Station.class));
        }
        
        @Test
        @DisplayName("创建充电站时不能传入(0,0)坐标")
        void shouldThrowExceptionWhenCreatingStationWithZeroCoordinates() {
            // Given
            Location invalidLocation = Location.of(
                java.math.BigDecimal.ZERO, java.math.BigDecimal.ZERO,
                "测试地址", "测试城市", "测试省份"
            );
            
            // When & Then
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
                stationService.createStation(testStationInfo, invalidLocation, testBusinessHours);
            });
            
            assertTrue(exception.getMessage().contains("地理位置不能为 (0, 0)"));
        }
    }
    
    @Nested
    @DisplayName("充电站查询测试")
    class StationQueryTest {
        
        @Test
        @DisplayName("应该能够根据ID获取充电站")
        void shouldGetStationById() {
            // Given
            Station expectedStation = TestDataFactory.createTestStation();
            setFieldValue(expectedStation, "id", TEST_STATION_ID);
            
            when(stationRepository.findById(TEST_STATION_ID)).thenReturn(Optional.of(expectedStation));
            
            // When
            Station result = stationService.getStationById(TEST_STATION_ID);
            
            // Then
            assertNotNull(result);
            assertEquals(TEST_STATION_ID, result.getId());
            
            verify(stationRepository).findById(TEST_STATION_ID);
        }
        
        @Test
        @DisplayName("获取不存在的充电站应该抛出异常")
        void shouldThrowExceptionWhenStationNotFound() {
            // Given
            when(stationRepository.findById(TEST_STATION_ID)).thenReturn(Optional.empty());
            
            // When & Then
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
                stationService.getStationById(TEST_STATION_ID);
            });
            
            assertTrue(exception.getMessage().contains("充电站不存在"));
        }
        
        @Test
        @DisplayName("应该能够根据状态分页查询充电站")
        void shouldGetStationsByStatus() {
            // Given
            List<Station> stations = List.of(
                TestDataFactory.createStationWithStatus(StationStatus.OPERATING),
                TestDataFactory.createStationWithStatus(StationStatus.OPERATING)
            );
            Page<Station> expectedPage = new PageImpl<>(stations);
            Pageable pageable = PageRequest.of(0, 10);
            
            when(stationRepository.findByStatus(StationStatus.OPERATING, pageable)).thenReturn(expectedPage);
            
            // When
            Page<Station> result = stationService.getStationsByStatus(StationStatus.OPERATING, pageable);
            
            // Then
            assertNotNull(result);
            assertEquals(2, result.getContent().size());
            
            verify(stationRepository).findByStatus(StationStatus.OPERATING, pageable);
        }
        
        @Test
        @DisplayName("应该能够搜索充电站")
        void shouldSearchStations() {
            // Given
            String keyword = "测试";
            List<Station> stations = List.of(TestDataFactory.createTestStation());
            Page<Station> expectedPage = new PageImpl<>(stations);
            Pageable pageable = PageRequest.of(0, 10);
            
            when(stationRepository.searchByKeyword(keyword, pageable)).thenReturn(expectedPage);
            
            // When
            Page<Station> result = stationService.searchStations(keyword, pageable);
            
            // Then
            assertNotNull(result);
            assertEquals(1, result.getContent().size());
            
            verify(stationRepository).searchByKeyword(keyword, pageable);
        }
        
        @Test
        @DisplayName("应该能够获取有可用充电桩的充电站")
        void shouldGetStationsWithAvailableConnectors() {
            // Given
            List<Station> expectedStations = List.of(
                TestDataFactory.createStationWithConnectors(2),
                TestDataFactory.createStationWithConnectors(3)
            );
            
            when(stationRepository.findStationsWithAvailableConnectors()).thenReturn(expectedStations);
            
            // When
            List<Station> result = stationService.getStationsWithAvailableConnectors();
            
            // Then
            assertNotNull(result);
            assertEquals(2, result.size());
            
            verify(stationRepository).findStationsWithAvailableConnectors();
        }
    }
    
    @Nested
    @DisplayName("充电站更新测试")
    class StationUpdateTest {
        
        @Test
        @DisplayName("应该能够更新充电站信息")
        void shouldUpdateStationInfo() {
            // Given
            Station existingStation = TestDataFactory.createTestStation();
            setFieldValue(existingStation, "id", TEST_STATION_ID);
            
            StationInfo newStationInfo = new StationInfo(
                "新充电站名称", "新运营商", "010-87654321", "新描述", "新设施"
            );
            
            Station updatedStation = new Station(newStationInfo, existingStation.getLocation(), existingStation.getBusinessHours());
            setFieldValue(updatedStation, "id", TEST_STATION_ID);
            
            when(stationRepository.findById(TEST_STATION_ID)).thenReturn(Optional.of(existingStation));
            when(stationRepository.save(any(Station.class))).thenReturn(updatedStation);
            
            // When
            Station result = stationService.updateStationInfo(TEST_STATION_ID, newStationInfo);
            
            // Then
            assertNotNull(result);
            assertEquals(newStationInfo, result.getStationInfo());
            
            verify(stationRepository).findById(TEST_STATION_ID);
            verify(stationRepository).save(any(Station.class));
            verify(cacheService).cacheStation(updatedStation, Duration.ofHours(2));
        }
        
        @Test
        @DisplayName("应该能够更新充电站地理位置")
        void shouldUpdateStationLocation() {
            // Given
            Station existingStation = TestDataFactory.createTestStation();
            setFieldValue(existingStation, "id", TEST_STATION_ID);
            
            Location newLocation = Location.of(
                java.math.BigDecimal.valueOf(121.505),
                java.math.BigDecimal.valueOf(31.245),
                "上海市浦东新区", "上海市", "上海市"
            );
            
            Station updatedStation = new Station(existingStation.getStationInfo(), newLocation, existingStation.getBusinessHours());
            setFieldValue(updatedStation, "id", TEST_STATION_ID);
            
            when(stationRepository.findById(TEST_STATION_ID)).thenReturn(Optional.of(existingStation));
            when(stationRepository.save(any(Station.class))).thenReturn(updatedStation);
            
            // When
            Station result = stationService.updateStationLocation(TEST_STATION_ID, newLocation);
            
            // Then
            assertNotNull(result);
            assertEquals(newLocation, result.getLocation());
            
            verify(stationRepository).findById(TEST_STATION_ID);
            verify(stationRepository).save(any(Station.class));
            verify(geoLocationService).updateStationLocation(TEST_STATION_ID, newLocation);
            verify(cacheService).cacheStation(updatedStation, Duration.ofHours(2));
        }
        
        @Test
        @DisplayName("应该能够更改充电站状态")
        void shouldChangeStationStatus() {
            // Given
            Station existingStation = TestDataFactory.createTestStation();
            setFieldValue(existingStation, "id", TEST_STATION_ID);
            
            when(stationRepository.findById(TEST_STATION_ID)).thenReturn(Optional.of(existingStation));
            when(stationRepository.save(any(Station.class))).thenReturn(existingStation);
            
            // When
            Station result = stationService.changeStationStatus(TEST_STATION_ID, StationStatus.MAINTENANCE, "定期维护");
            
            // Then
            assertNotNull(result);
            
            verify(stationRepository).findById(TEST_STATION_ID);
            verify(stationRepository).save(existingStation);
            verify(cacheService).cacheStation(existingStation, Duration.ofHours(2));
        }
        
        @Test
        @DisplayName("关闭充电站时应该从地理位置服务中移除")
        void shouldRemoveFromGeoServiceWhenClosingStation() {
            // Given
            Station existingStation = TestDataFactory.createTestStation();
            setFieldValue(existingStation, "id", TEST_STATION_ID);
            
            when(stationRepository.findById(TEST_STATION_ID)).thenReturn(Optional.of(existingStation));
            when(stationRepository.save(any(Station.class))).thenReturn(existingStation);
            
            // When
            stationService.changeStationStatus(TEST_STATION_ID, StationStatus.CLOSED, "永久关闭");
            
            // Then
            verify(geoLocationService).removeStationLocation(TEST_STATION_ID);
        }
    }
    
    @Nested
    @DisplayName("充电桩管理测试")
    class ConnectorManagementTest {
        
        @Test
        @DisplayName("应该能够添加充电桩到充电站")
        void shouldAddConnectorToStation() {
            // Given
            Station existingStation = TestDataFactory.createTestStation();
            setFieldValue(existingStation, "id", TEST_STATION_ID);
            
            when(stationRepository.findById(TEST_STATION_ID)).thenReturn(Optional.of(existingStation));
            when(stationRepository.save(any(Station.class))).thenReturn(existingStation);
            
            // When
            Station result = stationService.addConnectorToStation(TEST_STATION_ID, testConnectorInfo, testParkingSpot);
            
            // Then
            assertNotNull(result);
            
            verify(stationRepository).findById(TEST_STATION_ID);
            verify(stationRepository).save(existingStation);
            verify(cacheService).cacheStation(existingStation, Duration.ofHours(2));
        }
        
        @Test
        @DisplayName("应该能够从充电站移除充电桩")
        void shouldRemoveConnectorFromStation() {
            // Given
            Station existingStation = TestDataFactory.createStationWithConnectors(2);
            setFieldValue(existingStation, "id", TEST_STATION_ID);
            Long connectorId = existingStation.getConnectors().get(0).getId();
            
            when(stationRepository.findById(TEST_STATION_ID)).thenReturn(Optional.of(existingStation));
            when(stationRepository.save(any(Station.class))).thenReturn(existingStation);
            
            // When
            Station result = stationService.removeConnectorFromStation(TEST_STATION_ID, connectorId);
            
            // Then
            assertNotNull(result);
            
            verify(stationRepository).findById(TEST_STATION_ID);
            verify(stationRepository).save(existingStation);
            verify(cacheService).cacheStation(existingStation, Duration.ofHours(2));
        }
    }
    
    @Nested
    @DisplayName("充电站删除测试")
    class StationDeletionTest {
        
        @Test
        @DisplayName("应该能够删除没有可用充电桩的充电站")
        void shouldDeleteStationWithoutAvailableConnectors() {
            // Given
            Station existingStation = TestDataFactory.createTestStation();
            setFieldValue(existingStation, "id", TEST_STATION_ID);
            
            when(stationRepository.findById(TEST_STATION_ID)).thenReturn(Optional.of(existingStation));
            
            // When
            stationService.deleteStation(TEST_STATION_ID);
            
            // Then
            verify(stationRepository).findById(TEST_STATION_ID);
            verify(geoLocationService).removeStationLocation(TEST_STATION_ID);
            verify(cacheService).evictStationRelatedCaches(TEST_STATION_ID);
            verify(stationRepository).delete(existingStation);
        }
        
        @Test
        @DisplayName("删除有可用充电桩的充电站应该抛出异常")
        void shouldThrowExceptionWhenDeletingStationWithAvailableConnectors() {
            // Given
            Station existingStation = TestDataFactory.createStationWithConnectors(2);
            setFieldValue(existingStation, "id", TEST_STATION_ID);
            
            when(stationRepository.findById(TEST_STATION_ID)).thenReturn(Optional.of(existingStation));
            
            // When & Then
            IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
                stationService.deleteStation(TEST_STATION_ID);
            });
            
            assertTrue(exception.getMessage().contains("还有可用充电桩"));
            
            // 验证没有删除操作
            verify(stationRepository, never()).delete(any(Station.class));
        }
    }
    
    @Nested
    @DisplayName("批量操作测试")
    class BatchOperationTest {
        
        @Test
        @DisplayName("应该能够批量创建充电站")
        void shouldBatchCreateStations() {
            // Given
            List<Station> stations = List.of(
                TestDataFactory.createTestStation("站点1", "运营商1"),
                TestDataFactory.createTestStation("站点2", "运营商2")
            );
            
            when(stationRepository.existsByNameAndOperator(anyString(), anyString())).thenReturn(false);
            when(stationRepository.saveAll(anyList())).thenReturn(stations);
            
            // When
            List<Station> result = stationService.batchCreateStations(stations);
            
            // Then
            assertNotNull(result);
            assertEquals(2, result.size());
            
            verify(stationRepository, times(2)).existsByNameAndOperator(anyString(), anyString());
            verify(stationRepository).saveAll(stations);
            verify(geoLocationService).batchAddStationLocations(stations);
            verify(cacheService).batchCacheStations(stations, Duration.ofHours(2));
        }
    }
    
    @Nested
    @DisplayName("统计信息测试")
    class StatisticsTest {
        
        @Test
        @DisplayName("应该能够获取充电站统计信息")
        void shouldGetStationStatistics() {
            // Given
            StationRepository.StationStatistics expectedStats = new StationRepository.StationStatistics(
                100L, 80L, 10L, 5L, 5L, 500L, 400L, 5.0, 80.0
            );
            
            when(stationRepository.getStatistics()).thenReturn(expectedStats);
            
            // When
            StationRepository.StationStatistics result = stationService.getStationStatistics();
            
            // Then
            assertNotNull(result);
            assertEquals(100L, result.totalStations());
            assertEquals(80L, result.operatingStations());
            
            verify(stationRepository).getStatistics();
        }
    }
}
