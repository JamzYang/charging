package com.ys.charging.station.interfaces.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ys.charging.station.TestBase;
import com.ys.charging.station.TestDataFactory;
import com.ys.charging.station.config.TestConfig;
import com.ys.charging.station.domain.model.Station;
import com.ys.charging.station.domain.model.StationStatus;
import com.ys.charging.station.domain.repository.StationRepository;
import com.ys.charging.station.interfaces.rest.dto.CreateStationRequest;
import com.ys.charging.station.interfaces.rest.dto.UpdateLocationRequest;
import com.ys.charging.station.interfaces.rest.dto.ChangeStatusRequest;
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
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalTime;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 充电站控制器集成测试
 * 
 * @author yang
 * @since 2025-06-23
 */
@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Import(TestConfig.class)
@Transactional
@DisplayName("充电站控制器集成测试")
class StationControllerIntegrationTest extends TestBase {
    
    @Autowired
    private WebApplicationContext webApplicationContext;
    
    @Autowired
    private StationRepository stationRepository;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    private MockMvc mockMvc;
    
    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }
    
    @Nested
    @DisplayName("充电站创建 API 测试")
    class StationCreationApiTest {
        
        @Test
        @DisplayName("应该能够创建充电站")
        void shouldCreateStation() throws Exception {
            // Given
            CreateStationRequest request = new CreateStationRequest(
                "测试充电站",
                "测试运营商",
                "010-12345678",
                "测试描述",
                "停车场,便利店",
                BigDecimal.valueOf(116.457),
                BigDecimal.valueOf(39.918),
                "北京市朝阳区建国门外大街1号",
                "北京市",
                "北京市",
                LocalTime.of(6, 0),
                LocalTime.of(22, 0),
                false
            );
            
            // When & Then
            mockMvc.perform(post("/api/v1/stations")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is("测试充电站")))
                .andExpect(jsonPath("$.operator", is("测试运营商")))
                .andExpect(jsonPath("$.longitude", is(116.457)))
                .andExpect(jsonPath("$.latitude", is(39.918)))
                .andExpect(jsonPath("$.status", is("OPERATING")))
                .andExpect(jsonPath("$.id", notNullValue()));
        }
        
        @Test
        @DisplayName("创建充电站时缺少必填字段应该返回400")
        void shouldReturn400WhenMissingRequiredFields() throws Exception {
            // Given - 缺少名称的请求
            CreateStationRequest request = new CreateStationRequest(
                null, // 缺少名称
                "测试运营商",
                "010-12345678",
                "测试描述",
                "停车场",
                BigDecimal.valueOf(116.457),
                BigDecimal.valueOf(39.918),
                "北京市朝阳区",
                "北京市",
                "北京市",
                LocalTime.of(6, 0),
                LocalTime.of(22, 0),
                false
            );
            
            // When & Then
            mockMvc.perform(post("/api/v1/stations")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("VALIDATION_ERROR")));
        }
        
        @Test
        @DisplayName("创建重复的充电站应该返回400")
        void shouldReturn400WhenCreatingDuplicateStation() throws Exception {
            // Given - 先创建一个充电站
            Station existingStation = TestDataFactory.createTestStation("重复充电站", "重复运营商");
            stationRepository.save(existingStation);
            
            CreateStationRequest request = new CreateStationRequest(
                "重复充电站",
                "重复运营商",
                "010-12345678",
                "测试描述",
                "停车场",
                BigDecimal.valueOf(116.457),
                BigDecimal.valueOf(39.918),
                "北京市朝阳区",
                "北京市",
                "北京市",
                LocalTime.of(6, 0),
                LocalTime.of(22, 0),
                false
            );
            
            // When & Then
            mockMvc.perform(post("/api/v1/stations")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("充电站已存在")));
        }
    }
    
    @Nested
    @DisplayName("充电站查询 API 测试")
    class StationQueryApiTest {
        
        @Test
        @DisplayName("应该能够根据ID获取充电站")
        void shouldGetStationById() throws Exception {
            // Given
            Station station = TestDataFactory.createTestStation();
            Station savedStation = stationRepository.save(station);
            
            // When & Then
            mockMvc.perform(get("/api/v1/stations/{stationId}", savedStation.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(savedStation.getId().intValue())))
                .andExpect(jsonPath("$.name", is(station.getStationInfo().name())))
                .andExpect(jsonPath("$.operator", is(station.getStationInfo().operator())));
        }
        
        @Test
        @DisplayName("获取不存在的充电站应该返回404")
        void shouldReturn404WhenStationNotFound() throws Exception {
            // When & Then
            mockMvc.perform(get("/api/v1/stations/{stationId}", 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("NOT_FOUND")));
        }
        
        @Test
        @DisplayName("应该能够分页查询充电站")
        void shouldGetStationsWithPagination() throws Exception {
            // Given - 创建多个充电站
            for (int i = 1; i <= 5; i++) {
                Station station = TestDataFactory.createTestStation("充电站" + i, "运营商" + i);
                stationRepository.save(station);
            }
            
            // When & Then
            mockMvc.perform(get("/api/v1/stations")
                    .param("page", "0")
                    .param("size", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(3)))
                .andExpect(jsonPath("$.totalElements", is(5)))
                .andExpect(jsonPath("$.totalPages", is(2)));
        }
        
        @Test
        @DisplayName("应该能够根据状态查询充电站")
        void shouldGetStationsByStatus() throws Exception {
            // Given
            Station operatingStation = TestDataFactory.createStationWithStatus(StationStatus.OPERATING);
            Station maintenanceStation = TestDataFactory.createStationWithStatus(StationStatus.MAINTENANCE);
            
            stationRepository.save(operatingStation);
            stationRepository.save(maintenanceStation);
            
            // When & Then
            mockMvc.perform(get("/api/v1/stations/by-status/{status}", "OPERATING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].status", is("OPERATING")));
        }
        
        @Test
        @DisplayName("应该能够搜索充电站")
        void shouldSearchStations() throws Exception {
            // Given
            Station station1 = TestDataFactory.createTestStation("国贸充电站", "国家电网");
            Station station2 = TestDataFactory.createTestStation("CBD充电站", "特来电");
            Station station3 = TestDataFactory.createTestStation("机场充电站", "星星充电");
            
            stationRepository.save(station1);
            stationRepository.save(station2);
            stationRepository.save(station3);
            
            // When & Then
            mockMvc.perform(get("/api/v1/stations/search")
                    .param("keyword", "国贸"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name", is("国贸充电站")));
        }
        
        @Test
        @DisplayName("应该能够获取可用充电站")
        void shouldGetAvailableStations() throws Exception {
            // Given
            Station stationWithConnectors = TestDataFactory.createStationWithConnectors(2);
            Station emptyStation = TestDataFactory.createTestStation();
            
            stationRepository.save(stationWithConnectors);
            stationRepository.save(emptyStation);
            
            // When & Then
            mockMvc.perform(get("/api/v1/stations/available"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1))) // 只有有充电桩的充电站
                .andExpect(jsonPath("$[0].availableConnectorCount", greaterThan(0)));
        }
    }
    
    @Nested
    @DisplayName("充电站更新 API 测试")
    class StationUpdateApiTest {
        
        @Test
        @DisplayName("应该能够更新充电站位置")
        void shouldUpdateStationLocation() throws Exception {
            // Given
            Station station = TestDataFactory.createTestStation();
            Station savedStation = stationRepository.save(station);
            
            UpdateLocationRequest request = new UpdateLocationRequest(
                BigDecimal.valueOf(121.505),
                BigDecimal.valueOf(31.245),
                "上海市浦东新区陆家嘴金融中心",
                "上海市",
                "上海市"
            );
            
            // When & Then
            mockMvc.perform(put("/api/v1/stations/{stationId}/location", savedStation.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.longitude", is(121.505)))
                .andExpect(jsonPath("$.latitude", is(31.245)))
                .andExpect(jsonPath("$.city", is("上海市")));
        }
        
        @Test
        @DisplayName("应该能够更改充电站状态")
        void shouldChangeStationStatus() throws Exception {
            // Given
            Station station = TestDataFactory.createTestStation();
            Station savedStation = stationRepository.save(station);
            
            ChangeStatusRequest request = new ChangeStatusRequest(
                "MAINTENANCE",
                "定期维护"
            );
            
            // When & Then
            mockMvc.perform(put("/api/v1/stations/{stationId}/status", savedStation.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("MAINTENANCE")));
        }
        
        @Test
        @DisplayName("更新不存在的充电站应该返回404")
        void shouldReturn404WhenUpdatingNonExistentStation() throws Exception {
            // Given
            UpdateLocationRequest request = new UpdateLocationRequest(
                BigDecimal.valueOf(121.505),
                BigDecimal.valueOf(31.245),
                "上海市浦东新区",
                "上海市",
                "上海市"
            );
            
            // When & Then
            mockMvc.perform(put("/api/v1/stations/{stationId}/location", 999L)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("充电站不存在")));
        }
    }
    
    @Nested
    @DisplayName("充电站删除 API 测试")
    class StationDeletionApiTest {
        
        @Test
        @DisplayName("应该能够删除没有充电桩的充电站")
        void shouldDeleteStationWithoutConnectors() throws Exception {
            // Given
            Station station = TestDataFactory.createTestStation();
            Station savedStation = stationRepository.save(station);
            
            // When & Then
            mockMvc.perform(delete("/api/v1/stations/{stationId}", savedStation.getId()))
                .andExpect(status().isNoContent());
            
            // 验证充电站已被删除
            mockMvc.perform(get("/api/v1/stations/{stationId}", savedStation.getId()))
                .andExpect(status().isNotFound());
        }
        
        @Test
        @DisplayName("删除有充电桩的充电站应该返回409")
        void shouldReturn409WhenDeletingStationWithConnectors() throws Exception {
            // Given
            Station stationWithConnectors = TestDataFactory.createStationWithConnectors(2);
            Station savedStation = stationRepository.save(stationWithConnectors);
            
            // When & Then
            mockMvc.perform(delete("/api/v1/stations/{stationId}", savedStation.getId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("CONFLICT")))
                .andExpect(jsonPath("$.message", containsString("还有可用充电桩")));
        }
        
        @Test
        @DisplayName("删除不存在的充电站应该返回400")
        void shouldReturn400WhenDeletingNonExistentStation() throws Exception {
            // When & Then
            mockMvc.perform(delete("/api/v1/stations/{stationId}", 999L))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("充电站不存在")));
        }
    }
    
    @Nested
    @DisplayName("充电站统计 API 测试")
    class StationStatisticsApiTest {
        
        @Test
        @DisplayName("应该能够获取充电站统计信息")
        void shouldGetStationStatistics() throws Exception {
            // Given - 创建一些测试数据
            for (int i = 1; i <= 3; i++) {
                Station station = TestDataFactory.createTestStation("充电站" + i, "运营商" + i);
                stationRepository.save(station);
            }
            
            // When & Then
            mockMvc.perform(get("/api/v1/stations/statistics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalStations", greaterThanOrEqualTo(3)))
                .andExpect(jsonPath("$.operatingStations", greaterThanOrEqualTo(3)));
        }
    }
    
    @Nested
    @DisplayName("API 错误处理测试")
    class ApiErrorHandlingTest {
        
        @Test
        @DisplayName("无效的JSON格式应该返回400")
        void shouldReturn400ForInvalidJson() throws Exception {
            // When & Then
            mockMvc.perform(post("/api/v1/stations")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{ invalid json }"))
                .andExpect(status().isBadRequest());
        }
        
        @Test
        @DisplayName("不支持的HTTP方法应该返回405")
        void shouldReturn405ForUnsupportedMethod() throws Exception {
            // When & Then
            mockMvc.perform(patch("/api/v1/stations/1"))
                .andExpect(status().isMethodNotAllowed());
        }
        
        @Test
        @DisplayName("无效的路径参数应该返回400")
        void shouldReturn400ForInvalidPathParameter() throws Exception {
            // When & Then
            mockMvc.perform(get("/api/v1/stations/{stationId}", "invalid-id"))
                .andExpect(status().isBadRequest());
        }
    }
    
    @Nested
    @DisplayName("API 性能测试")
    class ApiPerformanceTest {
        
        @Test
        @DisplayName("批量查询应该在合理时间内完成")
        void shouldCompleteQueryInReasonableTime() throws Exception {
            // Given - 创建大量测试数据
            for (int i = 1; i <= 50; i++) {
                Station station = TestDataFactory.createTestStation("充电站" + i, "运营商" + (i % 5));
                stationRepository.save(station);
            }
            
            // When & Then
            long startTime = System.currentTimeMillis();
            
            mockMvc.perform(get("/api/v1/stations")
                    .param("page", "0")
                    .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(20)));
            
            long endTime = System.currentTimeMillis();
            
            // 查询应该在2秒内完成
            assertTrue(endTime - startTime < 2000);
        }
    }
}
