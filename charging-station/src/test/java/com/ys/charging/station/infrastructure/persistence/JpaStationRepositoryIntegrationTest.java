package com.ys.charging.station.infrastructure.persistence;

import com.ys.charging.station.TestBase;
import com.ys.charging.station.TestDataFactory;
import com.ys.charging.station.config.TestConfig;
import com.ys.charging.station.domain.model.Station;
import com.ys.charging.station.domain.model.StationStatus;
import com.ys.charging.station.domain.repository.StationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JPA 充电站仓储集成测试
 * 
 * @author yang
 * @since 2025-06-23
 */
@DataJpaTest
@ActiveProfiles("test")
@Import(TestConfig.class)
@DisplayName("JPA 充电站仓储集成测试")
class JpaStationRepositoryIntegrationTest extends TestBase {
    
    @Autowired
    private StationRepository stationRepository;
    
    @Nested
    @DisplayName("基本 CRUD 操作测试")
    class BasicCrudTest {
        
        @Test
        @DisplayName("应该能够保存和查找充电站")
        void shouldSaveAndFindStation() {
            // Given
            Station station = TestDataFactory.createTestStation();
            
            // When
            Station savedStation = stationRepository.save(station);
            Optional<Station> foundStation = stationRepository.findById(savedStation.getId());
            
            // Then
            assertNotNull(savedStation.getId());
            assertTrue(foundStation.isPresent());
            assertEquals(savedStation.getId(), foundStation.get().getId());
            assertEquals(station.getStationInfo().name(), foundStation.get().getStationInfo().name());
            assertEquals(station.getLocation().longitude(), foundStation.get().getLocation().longitude());
        }
        
        @Test
        @DisplayName("应该能够更新充电站")
        void shouldUpdateStation() {
            // Given
            Station station = TestDataFactory.createTestStation();
            Station savedStation = stationRepository.save(station);
            
            // When
            savedStation.changeStatus(StationStatus.MAINTENANCE, "定期维护");
            Station updatedStation = stationRepository.save(savedStation);
            
            // Then
            assertEquals(StationStatus.MAINTENANCE, updatedStation.getStatus());
        }
        
        @Test
        @DisplayName("应该能够删除充电站")
        void shouldDeleteStation() {
            // Given
            Station station = TestDataFactory.createTestStation();
            Station savedStation = stationRepository.save(station);
            
            // When
            stationRepository.delete(savedStation);
            Optional<Station> foundStation = stationRepository.findById(savedStation.getId());
            
            // Then
            assertFalse(foundStation.isPresent());
        }
        
        @Test
        @DisplayName("应该能够检查充电站是否存在")
        void shouldCheckIfStationExists() {
            // Given
            Station station = TestDataFactory.createTestStation();
            Station savedStation = stationRepository.save(station);
            
            // When & Then
            assertTrue(stationRepository.existsById(savedStation.getId()));
            assertFalse(stationRepository.existsById(999L));
        }
    }
    
    @Nested
    @DisplayName("查询操作测试")
    class QueryOperationTest {
        
        @Test
        @DisplayName("应该能够根据名称查找充电站")
        void shouldFindStationsByName() {
            // Given
            String stationName = "测试充电站";
            Station station1 = TestDataFactory.createTestStation(stationName, "运营商1");
            Station station2 = TestDataFactory.createTestStation(stationName, "运营商2");
            Station station3 = TestDataFactory.createTestStation("其他充电站", "运营商3");
            
            stationRepository.save(station1);
            stationRepository.save(station2);
            stationRepository.save(station3);
            
            // When
            List<Station> result = stationRepository.findByName(stationName);
            
            // Then
            assertEquals(2, result.size());
            assertTrue(result.stream().allMatch(s -> s.getStationInfo().name().equals(stationName)));
        }
        
        @Test
        @DisplayName("应该能够根据运营商查找充电站")
        void shouldFindStationsByOperator() {
            // Given
            String operator = "测试运营商";
            Station station1 = TestDataFactory.createTestStation("充电站1", operator);
            Station station2 = TestDataFactory.createTestStation("充电站2", operator);
            Station station3 = TestDataFactory.createTestStation("充电站3", "其他运营商");
            
            stationRepository.save(station1);
            stationRepository.save(station2);
            stationRepository.save(station3);
            
            // When
            List<Station> result = stationRepository.findByOperator(operator);
            
            // Then
            assertEquals(2, result.size());
            assertTrue(result.stream().allMatch(s -> s.getStationInfo().operator().equals(operator)));
        }
        
        @Test
        @DisplayName("应该能够根据状态查找充电站")
        void shouldFindStationsByStatus() {
            // Given
            Station operatingStation = TestDataFactory.createStationWithStatus(StationStatus.OPERATING);
            Station maintenanceStation = TestDataFactory.createStationWithStatus(StationStatus.MAINTENANCE);
            Station faultStation = TestDataFactory.createStationWithStatus(StationStatus.FAULT);
            
            stationRepository.save(operatingStation);
            stationRepository.save(maintenanceStation);
            stationRepository.save(faultStation);
            
            // When
            List<Station> operatingStations = stationRepository.findByStatus(StationStatus.OPERATING);
            List<Station> maintenanceStations = stationRepository.findByStatus(StationStatus.MAINTENANCE);
            
            // Then
            assertEquals(1, operatingStations.size());
            assertEquals(1, maintenanceStations.size());
            assertEquals(StationStatus.OPERATING, operatingStations.get(0).getStatus());
            assertEquals(StationStatus.MAINTENANCE, maintenanceStations.get(0).getStatus());
        }
        
        @Test
        @DisplayName("应该能够根据状态分页查找充电站")
        void shouldFindStationsByStatusWithPagination() {
            // Given
            for (int i = 0; i < 5; i++) {
                Station station = TestDataFactory.createStationWithStatus(StationStatus.OPERATING);
                stationRepository.save(station);
            }
            
            Pageable pageable = PageRequest.of(0, 3);
            
            // When
            Page<Station> result = stationRepository.findByStatus(StationStatus.OPERATING, pageable);
            
            // Then
            assertEquals(3, result.getContent().size());
            assertEquals(5, result.getTotalElements());
            assertEquals(2, result.getTotalPages());
        }
        
        @Test
        @DisplayName("应该能够根据城市查找充电站")
        void shouldFindStationsByCity() {
            // Given
            Station beijingStation = TestDataFactory.createTestStationInCity("北京市", 116.457, 39.918);
            Station shanghaiStation = TestDataFactory.createTestStationInCity("上海市", 121.505, 31.245);
            Station anotherBeijingStation = TestDataFactory.createTestStationInCity("北京市", 116.467, 39.928);
            
            stationRepository.save(beijingStation);
            stationRepository.save(shanghaiStation);
            stationRepository.save(anotherBeijingStation);
            
            // When
            List<Station> beijingStations = stationRepository.findByCity("北京市");
            List<Station> shanghaiStations = stationRepository.findByCity("上海市");
            
            // Then
            assertEquals(2, beijingStations.size());
            assertEquals(1, shanghaiStations.size());
        }
        
        @Test
        @DisplayName("应该能够根据地理位置范围查找充电站")
        void shouldFindStationsByLocationBounds() {
            // Given
            // 北京地区的充电站
            Station beijingStation = TestDataFactory.createTestStationInCity("北京市", 116.457, 39.918);
            // 上海地区的充电站
            Station shanghaiStation = TestDataFactory.createTestStationInCity("上海市", 121.505, 31.245);
            
            stationRepository.save(beijingStation);
            stationRepository.save(shanghaiStation);
            
            // 定义北京地区的范围
            BigDecimal minLng = BigDecimal.valueOf(116.0);
            BigDecimal maxLng = BigDecimal.valueOf(117.0);
            BigDecimal minLat = BigDecimal.valueOf(39.0);
            BigDecimal maxLat = BigDecimal.valueOf(40.0);
            
            // When
            List<Station> result = stationRepository.findByLocationBounds(minLng, maxLng, minLat, maxLat);
            
            // Then
            assertEquals(1, result.size());
            assertEquals("北京市", result.get(0).getLocation().city());
        }
    }
    
    @Nested
    @DisplayName("搜索操作测试")
    class SearchOperationTest {
        
        @Test
        @DisplayName("应该能够根据关键词搜索充电站")
        void shouldSearchStationsByKeyword() {
            // Given
            Station station1 = TestDataFactory.createTestStation("国贸充电站", "国家电网");
            Station station2 = TestDataFactory.createTestStation("CBD充电站", "特来电");
            Station station3 = TestDataFactory.createTestStation("机场充电站", "星星充电");
            
            stationRepository.save(station1);
            stationRepository.save(station2);
            stationRepository.save(station3);
            
            Pageable pageable = PageRequest.of(0, 10);
            
            // When
            Page<Station> result1 = stationRepository.searchByKeyword("国贸", pageable);
            Page<Station> result2 = stationRepository.searchByKeyword("电网", pageable);
            Page<Station> result3 = stationRepository.searchByKeyword("充电", pageable);
            
            // Then
            assertEquals(1, result1.getContent().size());
            assertEquals("国贸充电站", result1.getContent().get(0).getStationInfo().name());
            
            assertEquals(1, result2.getContent().size());
            assertEquals("国家电网", result2.getContent().get(0).getStationInfo().operator());
            
            assertEquals(3, result3.getContent().size()); // 所有充电站都包含"充电"
        }
    }
    
    @Nested
    @DisplayName("统计操作测试")
    class StatisticsOperationTest {
        
        @Test
        @DisplayName("应该能够统计充电站数量")
        void shouldCountStations() {
            // Given
            for (int i = 0; i < 3; i++) {
                Station station = TestDataFactory.createTestStation();
                stationRepository.save(station);
            }
            
            // When
            long totalCount = stationRepository.count();
            long operatingCount = stationRepository.countByStatus(StationStatus.OPERATING);
            
            // Then
            assertEquals(3, totalCount);
            assertEquals(3, operatingCount); // 默认创建的都是运营状态
        }
        
        @Test
        @DisplayName("应该能够根据运营商统计数量")
        void shouldCountStationsByOperator() {
            // Given
            String operator1 = "运营商1";
            String operator2 = "运营商2";
            
            stationRepository.save(TestDataFactory.createTestStation("站点1", operator1));
            stationRepository.save(TestDataFactory.createTestStation("站点2", operator1));
            stationRepository.save(TestDataFactory.createTestStation("站点3", operator2));
            
            // When
            long count1 = stationRepository.countByOperator(operator1);
            long count2 = stationRepository.countByOperator(operator2);
            
            // Then
            assertEquals(2, count1);
            assertEquals(1, count2);
        }
        
        @Test
        @DisplayName("应该能够根据城市统计数量")
        void shouldCountStationsByCity() {
            // Given
            String city1 = "北京市";
            String city2 = "上海市";
            
            stationRepository.save(TestDataFactory.createTestStationInCity(city1, 116.457, 39.918));
            stationRepository.save(TestDataFactory.createTestStationInCity(city1, 116.467, 39.928));
            stationRepository.save(TestDataFactory.createTestStationInCity(city2, 121.505, 31.245));
            
            // When
            long beijingCount = stationRepository.countByCity(city1);
            long shanghaiCount = stationRepository.countByCity(city2);
            
            // Then
            assertEquals(2, beijingCount);
            assertEquals(1, shanghaiCount);
        }
        
        @Test
        @DisplayName("应该能够检查名称和运营商的唯一性")
        void shouldCheckNameAndOperatorUniqueness() {
            // Given
            String name = "测试充电站";
            String operator = "测试运营商";
            
            Station station = TestDataFactory.createTestStation(name, operator);
            stationRepository.save(station);
            
            // When & Then
            assertTrue(stationRepository.existsByNameAndOperator(name, operator));
            assertFalse(stationRepository.existsByNameAndOperator(name, "其他运营商"));
            assertFalse(stationRepository.existsByNameAndOperator("其他名称", operator));
        }
    }
    
    @Nested
    @DisplayName("批量操作测试")
    class BatchOperationTest {
        
        @Test
        @DisplayName("应该能够批量保存充电站")
        void shouldBatchSaveStations() {
            // Given
            List<Station> stations = List.of(
                TestDataFactory.createTestStation("站点1", "运营商1"),
                TestDataFactory.createTestStation("站点2", "运营商2"),
                TestDataFactory.createTestStation("站点3", "运营商3")
            );
            
            // When
            List<Station> savedStations = stationRepository.saveAll(stations);
            
            // Then
            assertEquals(3, savedStations.size());
            assertTrue(savedStations.stream().allMatch(s -> s.getId() != null));
        }
        
        @Test
        @DisplayName("应该能够批量查找充电站")
        void shouldBatchFindStations() {
            // Given
            List<Station> stations = List.of(
                TestDataFactory.createTestStation("站点1", "运营商1"),
                TestDataFactory.createTestStation("站点2", "运营商2")
            );
            List<Station> savedStations = stationRepository.saveAll(stations);
            List<Long> ids = savedStations.stream().map(Station::getId).toList();
            
            // When
            List<Station> foundStations = stationRepository.findAllById(ids);
            
            // Then
            assertEquals(2, foundStations.size());
            assertEquals(ids.size(), foundStations.size());
        }
    }
}
