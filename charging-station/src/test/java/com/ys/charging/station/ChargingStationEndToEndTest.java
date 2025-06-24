package com.ys.charging.station;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ys.charging.station.config.TestConfig;
import com.ys.charging.station.domain.model.*;
import com.ys.charging.station.domain.repository.ConnectorRepository;
import com.ys.charging.station.domain.repository.StationRepository;
import com.ys.charging.station.interfaces.rest.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 充电站管理系统端到端测试
 * 
 * 测试完整的业务流程，从充电站创建到充电会话完成的全过程。
 * 
 * @author yang
 * @since 2025-06-23
 */
@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Import(TestConfig.class)
@Transactional
@DisplayName("充电站管理系统端到端测试")
class ChargingStationEndToEndTest extends TestBase {
    
    @Autowired
    private WebApplicationContext webApplicationContext;
    
    @Autowired
    private StationRepository stationRepository;
    
    @Autowired
    private ConnectorRepository connectorRepository;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    private MockMvc mockMvc;
    
    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }
    
    @Nested
    @DisplayName("完整充电站生命周期测试")
    class CompleteStationLifecycleTest {
        
        @Test
        @DisplayName("应该能够完成充电站从创建到删除的完整生命周期")
        void shouldCompleteFullStationLifecycle() throws Exception {
            // 1. 创建充电站
            CreateStationRequest createRequest = new CreateStationRequest(
                "端到端测试充电站",
                "测试运营商",
                "010-12345678",
                "端到端测试描述",
                "停车场,便利店,洗手间",
                BigDecimal.valueOf(116.457),
                BigDecimal.valueOf(39.918),
                "北京市朝阳区建国门外大街1号",
                "北京市",
                "北京市",
                LocalTime.of(6, 0),
                LocalTime.of(22, 0),
                false
            );
            
            MvcResult createResult = mockMvc.perform(post("/api/v1/stations")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is("端到端测试充电站")))
                .andExpect(jsonPath("$.status", is("OPERATING")))
                .andReturn();
            
            StationResponse createdStation = objectMapper.readValue(
                createResult.getResponse().getContentAsString(), StationResponse.class);
            Long stationId = createdStation.id();
            
            // 2. 验证充电站已创建
            mockMvc.perform(get("/api/v1/stations/{stationId}", stationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(stationId.intValue())))
                .andExpect(jsonPath("$.name", is("端到端测试充电站")));
            
            // 3. 更新充电站信息
            UpdateStationRequest updateRequest = new UpdateStationRequest(
                "更新后的充电站名称",
                "更新后的运营商",
                "010-87654321",
                "更新后的描述",
                "停车场,便利店,洗手间,餐厅"
            );
            
            mockMvc.perform(put("/api/v1/stations/{stationId}", stationId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("更新后的充电站名称")))
                .andExpect(jsonPath("$.operator", is("更新后的运营商")));
            
            // 4. 更改充电站状态
            ChangeStatusRequest statusRequest = new ChangeStatusRequest("MAINTENANCE", "定期维护");
            
            mockMvc.perform(put("/api/v1/stations/{stationId}/status", stationId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(statusRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("MAINTENANCE")));
            
            // 5. 恢复运营状态
            ChangeStatusRequest operatingRequest = new ChangeStatusRequest("OPERATING", "维护完成");
            
            mockMvc.perform(put("/api/v1/stations/{stationId}/status", stationId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(operatingRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("OPERATING")));
            
            // 6. 删除充电站
            mockMvc.perform(delete("/api/v1/stations/{stationId}", stationId))
                .andExpect(status().isNoContent());
            
            // 7. 验证充电站已删除
            mockMvc.perform(get("/api/v1/stations/{stationId}", stationId))
                .andExpect(status().isNotFound());
        }
    }
    
    @Nested
    @DisplayName("完整充电会话流程测试")
    class CompleteChargingSessionTest {
        
        @Test
        @DisplayName("应该能够完成从预约到充电结束的完整流程")
        void shouldCompleteFullChargingSession() throws Exception {
            // 准备：创建充电站和充电桩
            Station station = TestDataFactory.createStationWithConnectors(2);
            Station savedStation = stationRepository.save(station);
            Long stationId = savedStation.getId();
            Long connectorId = savedStation.getConnectors().get(0).getId();
            Long userId = 1001L;
            
            // 1. 预约充电桩
            ReserveConnectorRequest reserveRequest = new ReserveConnectorRequest(userId, 30);
            
            mockMvc.perform(post("/api/v1/connectors/{connectorId}/reserve", connectorId)
                    .param("stationId", stationId.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(reserveRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("RESERVED")))
                .andExpect(jsonPath("$.reservedByUserId", is(userId.intValue())));
            
            // 2. 验证预约状态
            mockMvc.perform(get("/api/v1/connectors/{connectorId}", connectorId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("RESERVED")))
                .andExpect(jsonPath("$.reservedByUserId", is(userId.intValue())));
            
            // 3. 用户到达，占用充电桩
            OccupyConnectorRequest occupyRequest = new OccupyConnectorRequest(userId);
            
            mockMvc.perform(post("/api/v1/connectors/{connectorId}/occupy", connectorId)
                    .param("stationId", stationId.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(occupyRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("OCCUPIED")));
            
            // 4. 开始充电
            mockMvc.perform(post("/api/v1/connectors/{connectorId}/start-charging", connectorId)
                    .param("stationId", stationId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("CHARGING")));
            
            // 5. 更新心跳（模拟充电过程中的心跳）
            mockMvc.perform(post("/api/v1/connectors/{connectorId}/heartbeat", connectorId))
                .andExpect(status().isOk());
            
            // 6. 停止充电
            mockMvc.perform(post("/api/v1/connectors/{connectorId}/stop-charging", connectorId)
                    .param("stationId", stationId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("OCCUPIED")));
            
            // 7. 释放充电桩
            mockMvc.perform(post("/api/v1/connectors/{connectorId}/release", connectorId)
                    .param("stationId", stationId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("IDLE")));
            
            // 8. 验证充电桩已恢复空闲状态
            mockMvc.perform(get("/api/v1/connectors/{connectorId}", connectorId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("IDLE")))
                .andExpect(jsonPath("$.reservedByUserId", nullValue()));
        }
        
        @Test
        @DisplayName("应该能够处理预约取消流程")
        void shouldHandleReservationCancellation() throws Exception {
            // 准备：创建充电站和充电桩
            Station station = TestDataFactory.createStationWithConnectors(1);
            Station savedStation = stationRepository.save(station);
            Long stationId = savedStation.getId();
            Long connectorId = savedStation.getConnectors().get(0).getId();
            Long userId = 1001L;
            
            // 1. 预约充电桩
            ReserveConnectorRequest reserveRequest = new ReserveConnectorRequest(userId, 30);
            
            mockMvc.perform(post("/api/v1/connectors/{connectorId}/reserve", connectorId)
                    .param("stationId", stationId.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(reserveRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("RESERVED")));
            
            // 2. 取消预约
            CancelReservationRequest cancelRequest = new CancelReservationRequest(userId, "用户主动取消");
            
            mockMvc.perform(post("/api/v1/connectors/{connectorId}/cancel-reservation", connectorId)
                    .param("stationId", stationId.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(cancelRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("IDLE")))
                .andExpect(jsonPath("$.reservedByUserId", nullValue()));
            
            // 3. 验证充电桩已恢复空闲状态
            mockMvc.perform(get("/api/v1/connectors/{connectorId}", connectorId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("IDLE")));
        }
    }
    
    @Nested
    @DisplayName("故障处理流程测试")
    class FaultHandlingTest {
        
        @Test
        @DisplayName("应该能够处理充电桩故障和修复流程")
        void shouldHandleConnectorFaultAndRepair() throws Exception {
            // 准备：创建充电站和充电桩
            Station station = TestDataFactory.createStationWithConnectors(1);
            Station savedStation = stationRepository.save(station);
            Long connectorId = savedStation.getConnectors().get(0).getId();
            
            // 1. 设置充电桩故障
            SetFaultRequest faultRequest = new SetFaultRequest("电源模块故障");
            
            mockMvc.perform(post("/api/v1/connectors/{connectorId}/fault", connectorId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(faultRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("FAULT")))
                .andExpect(jsonPath("$.faultReason", is("电源模块故障")));
            
            // 2. 验证故障状态
            mockMvc.perform(get("/api/v1/connectors/{connectorId}", connectorId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("FAULT")))
                .andExpect(jsonPath("$.faultReason", is("电源模块故障")));
            
            // 3. 修复充电桩
            mockMvc.perform(post("/api/v1/connectors/{connectorId}/repair", connectorId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("IDLE")))
                .andExpect(jsonPath("$.faultReason", nullValue()));
            
            // 4. 验证修复后状态
            mockMvc.perform(get("/api/v1/connectors/{connectorId}", connectorId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("IDLE")))
                .andExpect(jsonPath("$.faultReason", nullValue()));
        }
    }
    
    @Nested
    @DisplayName("地理搜索流程测试")
    class GeoSearchTest {
        
        @Test
        @DisplayName("应该能够完成地理位置搜索流程")
        void shouldCompleteGeoSearchFlow() throws Exception {
            // 准备：创建不同位置的充电站
            Station beijingStation = TestDataFactory.createTestStationInCity("北京市", 116.457, 39.918);
            Station shanghaiStation = TestDataFactory.createTestStationInCity("上海市", 121.505, 31.245);
            
            stationRepository.save(beijingStation);
            stationRepository.save(shanghaiStation);
            
            // 1. 搜索北京附近的充电站
            GeoSearchRequest searchRequest = new GeoSearchRequest(
                116.457, 39.918, 10.0, 20, null, null, null, null, null
            );
            
            mockMvc.perform(post("/api/v1/geo/search/nearby")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(searchRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));
            
            // 2. 简单的附近搜索
            mockMvc.perform(get("/api/v1/geo/search/nearby")
                    .param("longitude", "116.457")
                    .param("latitude", "39.918")
                    .param("radiusKm", "5.0")
                    .param("maxResults", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));
            
            // 3. 获取最近的充电站
            mockMvc.perform(get("/api/v1/geo/nearest")
                    .param("longitude", "116.457")
                    .param("latitude", "39.918")
                    .param("maxResults", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));
            
            // 4. 统计指定半径内的充电站数量
            mockMvc.perform(get("/api/v1/geo/count")
                    .param("longitude", "116.457")
                    .param("latitude", "39.918")
                    .param("radiusKm", "50.0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count", greaterThanOrEqualTo(1)));
        }
    }
    
    @Nested
    @DisplayName("系统集成测试")
    class SystemIntegrationTest {
        
        @Test
        @DisplayName("应该能够处理并发充电会话")
        void shouldHandleConcurrentChargingSessions() throws Exception {
            // 准备：创建有多个充电桩的充电站
            Station station = TestDataFactory.createStationWithConnectors(3);
            Station savedStation = stationRepository.save(station);
            Long stationId = savedStation.getId();
            
            List<Long> connectorIds = savedStation.getConnectors().stream()
                .map(Connector::getId)
                .toList();
            
            // 并发预约多个充电桩
            for (int i = 0; i < connectorIds.size(); i++) {
                Long connectorId = connectorIds.get(i);
                Long userId = 1001L + i;
                
                ReserveConnectorRequest reserveRequest = new ReserveConnectorRequest(userId, 30);
                
                mockMvc.perform(post("/api/v1/connectors/{connectorId}/reserve", connectorId)
                        .param("stationId", stationId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reserveRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status", is("RESERVED")))
                    .andExpected(jsonPath("$.reservedByUserId", is(userId.intValue())));
            }
            
            // 验证充电站状态
            mockMvc.perform(get("/api/v1/stations/{stationId}/with-connectors", stationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.connectors", hasSize(3)))
                .andExpect(jsonPath("$.availableConnectors", is(0))); // 所有充电桩都被预约
        }
        
        @Test
        @DisplayName("应该能够处理系统统计和监控")
        void shouldHandleSystemStatisticsAndMonitoring() throws Exception {
            // 准备：创建测试数据
            for (int i = 1; i <= 3; i++) {
                Station station = TestDataFactory.createStationWithConnectors(2);
                stationRepository.save(station);
            }
            
            // 1. 获取充电站统计
            mockMvc.perform(get("/api/v1/stations/statistics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalStations", greaterThanOrEqualTo(3)))
                .andExpect(jsonPath("$.operatingStations", greaterThanOrEqualTo(3)));
            
            // 2. 获取充电桩统计
            mockMvc.perform(get("/api/v1/connectors/statistics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalConnectors", greaterThanOrEqualTo(6)))
                .andExpect(jsonPath("$.idleConnectors", greaterThanOrEqualTo(6)));
            
            // 3. 获取可用充电桩
            mockMvc.perform(get("/api/v1/connectors/available"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(6))));
            
            // 4. 获取运营中的充电站
            mockMvc.perform(get("/api/v1/stations/operating"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(3))));
        }
    }
}
