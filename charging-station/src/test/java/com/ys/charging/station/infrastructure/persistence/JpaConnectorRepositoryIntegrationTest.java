package com.ys.charging.station.infrastructure.persistence;

import com.ys.charging.station.TestBase;
import com.ys.charging.station.TestDataFactory;
import com.ys.charging.station.config.TestConfig;
import com.ys.charging.station.domain.model.*;
import com.ys.charging.station.domain.repository.ConnectorRepository;
import com.ys.charging.station.domain.repository.StationRepository;
import org.junit.jupiter.api.BeforeEach;
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
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JPA 充电桩仓储集成测试
 * 
 * @author yang
 * @since 2025-06-23
 */
@DataJpaTest
@ActiveProfiles("test")
@Import(TestConfig.class)
@DisplayName("JPA 充电桩仓储集成测试")
class JpaConnectorRepositoryIntegrationTest extends TestBase {
    
    @Autowired
    private ConnectorRepository connectorRepository;
    
    @Autowired
    private StationRepository stationRepository;
    
    private Station testStation;
    
    @BeforeEach
    void setUpTestStation() {
        testStation = TestDataFactory.createTestStation();
        testStation = stationRepository.save(testStation);
    }
    
    @Nested
    @DisplayName("基本 CRUD 操作测试")
    class BasicCrudTest {
        
        @Test
        @DisplayName("应该能够保存和查找充电桩")
        void shouldSaveAndFindConnector() {
            // Given
            Connector connector = TestDataFactory.createTestConnector(testStation.getId());
            
            // When
            Connector savedConnector = connectorRepository.save(connector);
            Optional<Connector> foundConnector = connectorRepository.findById(savedConnector.getId());
            
            // Then
            assertNotNull(savedConnector.getId());
            assertTrue(foundConnector.isPresent());
            assertEquals(savedConnector.getId(), foundConnector.get().getId());
            assertEquals(testStation.getId(), foundConnector.get().getStationId());
        }
        
        @Test
        @DisplayName("应该能够更新充电桩")
        void shouldUpdateConnector() {
            // Given
            Connector connector = TestDataFactory.createTestConnector(testStation.getId());
            Connector savedConnector = connectorRepository.save(connector);
            
            // When
            savedConnector.setFault("测试故障");
            Connector updatedConnector = connectorRepository.save(savedConnector);
            
            // Then
            assertEquals(ConnectorStatus.FAULT, updatedConnector.getStatus());
            assertEquals("测试故障", updatedConnector.getFaultReason());
        }
        
        @Test
        @DisplayName("应该能够删除充电桩")
        void shouldDeleteConnector() {
            // Given
            Connector connector = TestDataFactory.createTestConnector(testStation.getId());
            Connector savedConnector = connectorRepository.save(connector);
            
            // When
            connectorRepository.delete(savedConnector);
            Optional<Connector> foundConnector = connectorRepository.findById(savedConnector.getId());
            
            // Then
            assertFalse(foundConnector.isPresent());
        }
    }
    
    @Nested
    @DisplayName("查询操作测试")
    class QueryOperationTest {
        
        @Test
        @DisplayName("应该能够根据充电站ID查找充电桩")
        void shouldFindConnectorsByStationId() {
            // Given
            Connector connector1 = TestDataFactory.createTestConnector(testStation.getId(), "A01");
            Connector connector2 = TestDataFactory.createTestConnector(testStation.getId(), "A02");
            
            // 创建另一个充电站的充电桩
            Station anotherStation = TestDataFactory.createTestStation();
            anotherStation = stationRepository.save(anotherStation);
            Connector connector3 = TestDataFactory.createTestConnector(anotherStation.getId(), "B01");
            
            connectorRepository.save(connector1);
            connectorRepository.save(connector2);
            connectorRepository.save(connector3);
            
            // When
            List<Connector> result = connectorRepository.findByStationId(testStation.getId());
            
            // Then
            assertEquals(2, result.size());
            assertTrue(result.stream().allMatch(c -> c.getStationId().equals(testStation.getId())));
        }
        
        @Test
        @DisplayName("应该能够根据充电站ID和状态查找充电桩")
        void shouldFindConnectorsByStationIdAndStatus() {
            // Given
            Connector idleConnector = TestDataFactory.createTestConnector(testStation.getId(), "A01");
            Connector reservedConnector = TestDataFactory.createTestConnector(testStation.getId(), "A02");
            reservedConnector.reserve(TEST_USER_ID, 30);
            
            connectorRepository.save(idleConnector);
            connectorRepository.save(reservedConnector);
            
            // When
            List<Connector> idleConnectors = connectorRepository.findByStationIdAndStatus(
                testStation.getId(), ConnectorStatus.IDLE);
            List<Connector> reservedConnectors = connectorRepository.findByStationIdAndStatus(
                testStation.getId(), ConnectorStatus.RESERVED);
            
            // Then
            assertEquals(1, idleConnectors.size());
            assertEquals(1, reservedConnectors.size());
            assertEquals(ConnectorStatus.IDLE, idleConnectors.get(0).getStatus());
            assertEquals(ConnectorStatus.RESERVED, reservedConnectors.get(0).getStatus());
        }
        
        @Test
        @DisplayName("应该能够根据状态查找充电桩")
        void shouldFindConnectorsByStatus() {
            // Given
            Connector idleConnector = TestDataFactory.createTestConnector(testStation.getId(), "A01");
            Connector faultConnector = TestDataFactory.createTestConnector(testStation.getId(), "A02");
            faultConnector.setFault("测试故障");
            
            connectorRepository.save(idleConnector);
            connectorRepository.save(faultConnector);
            
            // When
            List<Connector> idleConnectors = connectorRepository.findByStatus(ConnectorStatus.IDLE);
            List<Connector> faultConnectors = connectorRepository.findByStatus(ConnectorStatus.FAULT);
            
            // Then
            assertEquals(1, idleConnectors.size());
            assertEquals(1, faultConnectors.size());
        }
        
        @Test
        @DisplayName("应该能够根据状态分页查找充电桩")
        void shouldFindConnectorsByStatusWithPagination() {
            // Given
            for (int i = 1; i <= 5; i++) {
                Connector connector = TestDataFactory.createTestConnector(testStation.getId(), "A" + String.format("%02d", i));
                connectorRepository.save(connector);
            }
            
            Pageable pageable = PageRequest.of(0, 3);
            
            // When
            Page<Connector> result = connectorRepository.findByStatus(ConnectorStatus.IDLE, pageable);
            
            // Then
            assertEquals(3, result.getContent().size());
            assertEquals(5, result.getTotalElements());
            assertEquals(2, result.getTotalPages());
        }
        
        @Test
        @DisplayName("应该能够根据预约用户ID查找充电桩")
        void shouldFindConnectorsByReservedUserId() {
            // Given
            Connector connector1 = TestDataFactory.createTestConnector(testStation.getId(), "A01");
            Connector connector2 = TestDataFactory.createTestConnector(testStation.getId(), "A02");
            Connector connector3 = TestDataFactory.createTestConnector(testStation.getId(), "A03");
            
            connector1.reserve(TEST_USER_ID, 30);
            connector2.reserve(TEST_USER_ID + 1, 30);
            // connector3 保持空闲状态
            
            connectorRepository.save(connector1);
            connectorRepository.save(connector2);
            connectorRepository.save(connector3);
            
            // When
            List<Connector> result = connectorRepository.findByReservedByUserId(TEST_USER_ID);
            
            // Then
            assertEquals(1, result.size());
            assertEquals(TEST_USER_ID, result.get(0).getReservedByUserId());
        }
        
        @Test
        @DisplayName("应该能够根据充电桩类型查找")
        void shouldFindConnectorsByType() {
            // Given
            Connector dcConnector = TestDataFactory.createTestConnector(testStation.getId(), "A01", 
                ConnectorInfo.ConnectorType.GB_T_DC, BigDecimal.valueOf(60.0));
            Connector acConnector = TestDataFactory.createTestConnector(testStation.getId(), "A02", 
                ConnectorInfo.ConnectorType.GB_T_AC, BigDecimal.valueOf(7.0));
            
            connectorRepository.save(dcConnector);
            connectorRepository.save(acConnector);
            
            // When
            List<Connector> dcConnectors = connectorRepository.findByConnectorType(ConnectorInfo.ConnectorType.GB_T_DC);
            List<Connector> acConnectors = connectorRepository.findByConnectorType(ConnectorInfo.ConnectorType.GB_T_AC);
            
            // Then
            assertEquals(1, dcConnectors.size());
            assertEquals(1, acConnectors.size());
            assertEquals(ConnectorInfo.ConnectorType.GB_T_DC, dcConnectors.get(0).getConnectorInfo().connectorType());
            assertEquals(ConnectorInfo.ConnectorType.GB_T_AC, acConnectors.get(0).getConnectorInfo().connectorType());
        }
    }
    
    @Nested
    @DisplayName("复杂查询测试")
    class ComplexQueryTest {
        
        @Test
        @DisplayName("应该能够查找使用中的充电桩")
        void shouldFindInUseConnectors() {
            // Given
            Connector idleConnector = TestDataFactory.createTestConnector(testStation.getId(), "A01");
            Connector reservedConnector = TestDataFactory.createTestConnector(testStation.getId(), "A02");
            Connector occupiedConnector = TestDataFactory.createTestConnector(testStation.getId(), "A03");
            Connector chargingConnector = TestDataFactory.createTestConnector(testStation.getId(), "A04");
            
            reservedConnector.reserve(TEST_USER_ID, 30);
            occupiedConnector.occupy(TEST_USER_ID);
            chargingConnector.occupy(TEST_USER_ID);
            chargingConnector.startCharging();
            
            connectorRepository.save(idleConnector);
            connectorRepository.save(reservedConnector);
            connectorRepository.save(occupiedConnector);
            connectorRepository.save(chargingConnector);
            
            // When
            List<Connector> result = connectorRepository.findInUseConnectors(testStation.getId());
            
            // Then
            assertEquals(3, result.size()); // RESERVED, OCCUPIED, CHARGING
            assertTrue(result.stream().noneMatch(c -> c.getStatus() == ConnectorStatus.IDLE));
        }
        
        @Test
        @DisplayName("应该能够查找过期预约的充电桩")
        void shouldFindExpiredReservations() {
            // Given
            Connector expiredConnector = TestDataFactory.createTestConnector(testStation.getId(), "A01");
            Connector validConnector = TestDataFactory.createTestConnector(testStation.getId(), "A02");
            
            expiredConnector.reserve(TEST_USER_ID, 30);
            validConnector.reserve(TEST_USER_ID + 1, 30);
            
            // 设置过期时间
            setFieldValue(expiredConnector, "reservationExpiresAt", Instant.now().minusSeconds(3600));
            
            connectorRepository.save(expiredConnector);
            connectorRepository.save(validConnector);
            
            // When
            List<Connector> result = connectorRepository.findExpiredReservations(Instant.now());
            
            // Then
            assertEquals(1, result.size());
            assertEquals(expiredConnector.getId(), result.get(0).getId());
        }
        
        @Test
        @DisplayName("应该能够查找心跳超时的充电桩")
        void shouldFindHeartbeatTimeoutConnectors() {
            // Given
            Connector timeoutConnector = TestDataFactory.createTestConnector(testStation.getId(), "A01");
            Connector normalConnector = TestDataFactory.createTestConnector(testStation.getId(), "A02");
            
            timeoutConnector.updateHeartbeat();
            normalConnector.updateHeartbeat();
            
            // 设置超时的心跳时间
            setFieldValue(timeoutConnector, "lastHeartbeatAt", Instant.now().minusSeconds(1200)); // 20分钟前
            
            connectorRepository.save(timeoutConnector);
            connectorRepository.save(normalConnector);
            
            Instant timeoutThreshold = Instant.now().minusSeconds(600); // 10分钟前
            
            // When
            List<Connector> result = connectorRepository.findHeartbeatTimeoutConnectors(timeoutThreshold);
            
            // Then
            assertEquals(1, result.size());
            assertEquals(timeoutConnector.getId(), result.get(0).getId());
        }
        
        @Test
        @DisplayName("应该能够根据功率范围查找充电桩")
        void shouldFindConnectorsByPowerRange() {
            // Given
            Connector lowPowerConnector = TestDataFactory.createTestConnector(testStation.getId(), "A01", 
                ConnectorInfo.ConnectorType.GB_T_AC, BigDecimal.valueOf(7.0));
            Connector mediumPowerConnector = TestDataFactory.createTestConnector(testStation.getId(), "A02", 
                ConnectorInfo.ConnectorType.GB_T_DC, BigDecimal.valueOf(60.0));
            Connector highPowerConnector = TestDataFactory.createTestConnector(testStation.getId(), "A03", 
                ConnectorInfo.ConnectorType.TESLA_SUPERCHARGER, BigDecimal.valueOf(250.0));
            
            connectorRepository.save(lowPowerConnector);
            connectorRepository.save(mediumPowerConnector);
            connectorRepository.save(highPowerConnector);
            
            // When
            List<Connector> fastChargers = connectorRepository.findByPowerRange(
                BigDecimal.valueOf(50.0), BigDecimal.valueOf(100.0));
            List<Connector> superChargers = connectorRepository.findByPowerRange(
                BigDecimal.valueOf(150.0), BigDecimal.valueOf(300.0));
            
            // Then
            assertEquals(1, fastChargers.size());
            assertEquals(1, superChargers.size());
            assertEquals(BigDecimal.valueOf(60.0), fastChargers.get(0).getConnectorInfo().maxPower());
            assertEquals(BigDecimal.valueOf(250.0), superChargers.get(0).getConnectorInfo().maxPower());
        }
        
        @Test
        @DisplayName("应该能够查找快充桩")
        void shouldFindFastChargers() {
            // Given
            Connector slowCharger = TestDataFactory.createTestConnector(testStation.getId(), "A01", 
                ConnectorInfo.ConnectorType.GB_T_AC, BigDecimal.valueOf(7.0));
            Connector fastCharger = TestDataFactory.createTestConnector(testStation.getId(), "A02", 
                ConnectorInfo.ConnectorType.GB_T_DC, BigDecimal.valueOf(60.0));
            Connector superCharger = TestDataFactory.createTestConnector(testStation.getId(), "A03", 
                ConnectorInfo.ConnectorType.TESLA_SUPERCHARGER, BigDecimal.valueOf(250.0));
            
            connectorRepository.save(slowCharger);
            connectorRepository.save(fastCharger);
            connectorRepository.save(superCharger);
            
            // When
            List<Connector> result = connectorRepository.findFastChargers(testStation.getId());
            
            // Then
            assertEquals(2, result.size()); // 功率 >= 50kW 的充电桩
            assertTrue(result.stream().allMatch(c -> c.getConnectorInfo().maxPower().compareTo(BigDecimal.valueOf(50)) >= 0));
        }
        
        @Test
        @DisplayName("应该能够查找超充桩")
        void shouldFindSuperChargers() {
            // Given
            Connector fastCharger = TestDataFactory.createTestConnector(testStation.getId(), "A01", 
                ConnectorInfo.ConnectorType.GB_T_DC, BigDecimal.valueOf(60.0));
            Connector superCharger = TestDataFactory.createTestConnector(testStation.getId(), "A02", 
                ConnectorInfo.ConnectorType.TESLA_SUPERCHARGER, BigDecimal.valueOf(250.0));
            
            connectorRepository.save(fastCharger);
            connectorRepository.save(superCharger);
            
            // When
            List<Connector> result = connectorRepository.findSuperChargers(testStation.getId());
            
            // Then
            assertEquals(1, result.size()); // 功率 >= 150kW 的充电桩
            assertTrue(result.stream().allMatch(c -> c.getConnectorInfo().maxPower().compareTo(BigDecimal.valueOf(150)) >= 0));
        }
    }
    
    @Nested
    @DisplayName("统计操作测试")
    class StatisticsOperationTest {
        
        @Test
        @DisplayName("应该能够统计充电桩数量")
        void shouldCountConnectors() {
            // Given
            for (int i = 1; i <= 3; i++) {
                Connector connector = TestDataFactory.createTestConnector(testStation.getId(), "A" + String.format("%02d", i));
                connectorRepository.save(connector);
            }
            
            // When
            long totalCount = connectorRepository.count();
            long stationCount = connectorRepository.countByStationId(testStation.getId());
            long idleCount = connectorRepository.countByStatus(ConnectorStatus.IDLE);
            
            // Then
            assertEquals(3, totalCount);
            assertEquals(3, stationCount);
            assertEquals(3, idleCount);
        }
        
        @Test
        @DisplayName("应该能够统计可用充电桩数量")
        void shouldCountAvailableConnectors() {
            // Given
            Connector idleConnector = TestDataFactory.createTestConnector(testStation.getId(), "A01");
            Connector reservedConnector = TestDataFactory.createTestConnector(testStation.getId(), "A02");
            Connector faultConnector = TestDataFactory.createTestConnector(testStation.getId(), "A03");
            
            reservedConnector.reserve(TEST_USER_ID, 30);
            faultConnector.setFault("测试故障");
            
            connectorRepository.save(idleConnector);
            connectorRepository.save(reservedConnector);
            connectorRepository.save(faultConnector);
            
            // When
            long availableCount = connectorRepository.countAvailableConnectors(testStation.getId());
            
            // Then
            assertEquals(1, availableCount); // 只有 IDLE 状态的充电桩可用
        }
        
        @Test
        @DisplayName("应该能够统计使用中充电桩数量")
        void shouldCountInUseConnectors() {
            // Given
            Connector idleConnector = TestDataFactory.createTestConnector(testStation.getId(), "A01");
            Connector reservedConnector = TestDataFactory.createTestConnector(testStation.getId(), "A02");
            Connector chargingConnector = TestDataFactory.createTestConnector(testStation.getId(), "A03");
            
            reservedConnector.reserve(TEST_USER_ID, 30);
            chargingConnector.occupy(TEST_USER_ID);
            chargingConnector.startCharging();
            
            connectorRepository.save(idleConnector);
            connectorRepository.save(reservedConnector);
            connectorRepository.save(chargingConnector);
            
            // When
            long inUseCount = connectorRepository.countInUseConnectors(testStation.getId());
            
            // Then
            assertEquals(2, inUseCount); // RESERVED 和 CHARGING 状态
        }
        
        @Test
        @DisplayName("应该能够检查充电桩编号的唯一性")
        void shouldCheckConnectorNumberUniqueness() {
            // Given
            String connectorNumber = "A01";
            Connector connector = TestDataFactory.createTestConnector(testStation.getId(), connectorNumber);
            connectorRepository.save(connector);
            
            // When & Then
            assertTrue(connectorRepository.existsByStationIdAndConnectorNumber(testStation.getId(), connectorNumber));
            assertFalse(connectorRepository.existsByStationIdAndConnectorNumber(testStation.getId(), "A02"));
        }
    }
    
    @Nested
    @DisplayName("批量操作测试")
    class BatchOperationTest {
        
        @Test
        @DisplayName("应该能够批量更新充电桩状态")
        void shouldBatchUpdateConnectorStatus() {
            // Given
            List<Connector> connectors = List.of(
                TestDataFactory.createTestConnector(testStation.getId(), "A01"),
                TestDataFactory.createTestConnector(testStation.getId(), "A02"),
                TestDataFactory.createTestConnector(testStation.getId(), "A03")
            );
            
            List<Connector> savedConnectors = connectorRepository.saveAll(connectors);
            List<Long> connectorIds = savedConnectors.stream().map(Connector::getId).toList();
            
            // When
            int updatedCount = connectorRepository.batchUpdateStatus(connectorIds, ConnectorStatus.MAINTENANCE);
            
            // Then
            assertEquals(3, updatedCount);
            
            // 验证状态已更新
            List<Connector> updatedConnectors = connectorRepository.findAllById(connectorIds);
            assertTrue(updatedConnectors.stream().allMatch(c -> c.getStatus() == ConnectorStatus.MAINTENANCE));
        }
        
        @Test
        @DisplayName("应该能够根据充电站ID删除所有充电桩")
        void shouldDeleteConnectorsByStationId() {
            // Given
            for (int i = 1; i <= 3; i++) {
                Connector connector = TestDataFactory.createTestConnector(testStation.getId(), "A" + String.format("%02d", i));
                connectorRepository.save(connector);
            }
            
            // When
            connectorRepository.deleteByStationId(testStation.getId());
            
            // Then
            List<Connector> remainingConnectors = connectorRepository.findByStationId(testStation.getId());
            assertTrue(remainingConnectors.isEmpty());
        }
    }
}
