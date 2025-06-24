package com.ys.charging.station.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Location 值对象单元测试
 * 
 * @author yang
 * @since 2025-06-23
 */
@DisplayName("地理位置值对象测试")
class LocationTest {
    
    @Nested
    @DisplayName("地理位置创建测试")
    class LocationCreationTest {
        
        @Test
        @DisplayName("应该能够创建有效的地理位置")
        void shouldCreateValidLocation() {
            // Given
            BigDecimal longitude = BigDecimal.valueOf(116.457);
            BigDecimal latitude = BigDecimal.valueOf(39.918);
            String address = "北京市朝阳区建国门外大街1号";
            String city = "北京市";
            String province = "北京市";
            
            // When
            Location location = Location.of(longitude, latitude, address, city, province);
            
            // Then
            assertNotNull(location);
            assertEquals(longitude, location.longitude());
            assertEquals(latitude, location.latitude());
            assertEquals(address, location.address());
            assertEquals(city, location.city());
            assertEquals(province, location.province());
        }
        
        @Test
        @DisplayName("创建地理位置时不能传入空的经度")
        void shouldThrowExceptionWhenLongitudeIsNull() {
            // When & Then
            assertThrows(IllegalArgumentException.class, () -> {
                Location.of(null, BigDecimal.valueOf(39.918), "地址", "城市", "省份");
            });
        }
        
        @Test
        @DisplayName("创建地理位置时不能传入空的纬度")
        void shouldThrowExceptionWhenLatitudeIsNull() {
            // When & Then
            assertThrows(IllegalArgumentException.class, () -> {
                Location.of(BigDecimal.valueOf(116.457), null, "地址", "城市", "省份");
            });
        }
        
        @Test
        @DisplayName("创建地理位置时不能传入空的地址")
        void shouldThrowExceptionWhenAddressIsNull() {
            // When & Then
            assertThrows(IllegalArgumentException.class, () -> {
                Location.of(BigDecimal.valueOf(116.457), BigDecimal.valueOf(39.918), null, "城市", "省份");
            });
        }
        
        @Test
        @DisplayName("经度必须在有效范围内")
        void shouldValidateLongitudeRange() {
            // When & Then
            assertThrows(IllegalArgumentException.class, () -> {
                Location.of(BigDecimal.valueOf(-181), BigDecimal.valueOf(39.918), "地址", "城市", "省份");
            });
            
            assertThrows(IllegalArgumentException.class, () -> {
                Location.of(BigDecimal.valueOf(181), BigDecimal.valueOf(39.918), "地址", "城市", "省份");
            });
        }
        
        @Test
        @DisplayName("纬度必须在有效范围内")
        void shouldValidateLatitudeRange() {
            // When & Then
            assertThrows(IllegalArgumentException.class, () -> {
                Location.of(BigDecimal.valueOf(116.457), BigDecimal.valueOf(-91), "地址", "城市", "省份");
            });
            
            assertThrows(IllegalArgumentException.class, () -> {
                Location.of(BigDecimal.valueOf(116.457), BigDecimal.valueOf(91), "地址", "城市", "省份");
            });
        }
    }
    
    @Nested
    @DisplayName("距离计算测试")
    class DistanceCalculationTest {
        
        @Test
        @DisplayName("应该能够计算两点之间的距离")
        void shouldCalculateDistanceBetweenTwoPoints() {
            // Given - 北京和上海的大致坐标
            Location beijing = Location.of(
                BigDecimal.valueOf(116.457), BigDecimal.valueOf(39.918),
                "北京市朝阳区", "北京市", "北京市"
            );
            Location shanghai = Location.of(
                BigDecimal.valueOf(121.505), BigDecimal.valueOf(31.245),
                "上海市浦东新区", "上海市", "上海市"
            );
            
            // When
            double distance = beijing.distanceTo(shanghai);
            
            // Then
            assertTrue(distance > 1000); // 北京到上海大约1000多公里
            assertTrue(distance < 1500); // 应该不超过1500公里
        }
        
        @Test
        @DisplayName("相同位置的距离应该为0")
        void shouldReturnZeroDistanceForSameLocation() {
            // Given
            Location location1 = Location.of(
                BigDecimal.valueOf(116.457), BigDecimal.valueOf(39.918),
                "北京市朝阳区", "北京市", "北京市"
            );
            Location location2 = Location.of(
                BigDecimal.valueOf(116.457), BigDecimal.valueOf(39.918),
                "北京市朝阳区", "北京市", "北京市"
            );
            
            // When
            double distance = location1.distanceTo(location2);
            
            // Then
            assertEquals(0.0, distance, 0.001); // 允许小的浮点误差
        }
        
        @Test
        @DisplayName("计算距离时不能传入空的目标位置")
        void shouldThrowExceptionWhenTargetLocationIsNull() {
            // Given
            Location location = Location.of(
                BigDecimal.valueOf(116.457), BigDecimal.valueOf(39.918),
                "北京市朝阳区", "北京市", "北京市"
            );
            
            // When & Then
            assertThrows(IllegalArgumentException.class, () -> {
                location.distanceTo(null);
            });
        }
        
        @Test
        @DisplayName("应该能够检查是否在指定半径内")
        void shouldCheckIfWithinRadius() {
            // Given
            Location center = Location.of(
                BigDecimal.valueOf(116.457), BigDecimal.valueOf(39.918),
                "北京市朝阳区", "北京市", "北京市"
            );
            Location nearby = Location.of(
                BigDecimal.valueOf(116.467), BigDecimal.valueOf(39.928),
                "北京市朝阳区", "北京市", "北京市"
            );
            Location faraway = Location.of(
                BigDecimal.valueOf(121.505), BigDecimal.valueOf(31.245),
                "上海市浦东新区", "上海市", "上海市"
            );
            
            // When & Then
            assertTrue(center.isWithinRadius(nearby, 5.0)); // 5公里内
            assertFalse(center.isWithinRadius(faraway, 5.0)); // 5公里外
        }
    }
    
    @Nested
    @DisplayName("地理位置格式化测试")
    class LocationFormattingTest {
        
        @Test
        @DisplayName("应该能够获取地理位置的字符串表示")
        void shouldGetLocationString() {
            // Given
            Location location = Location.of(
                BigDecimal.valueOf(116.457), BigDecimal.valueOf(39.918),
                "北京市朝阳区建国门外大街1号", "北京市", "北京市"
            );
            
            // When
            String locationString = location.toString();
            
            // Then
            assertNotNull(locationString);
            assertTrue(locationString.contains("116.457"));
            assertTrue(locationString.contains("39.918"));
            assertTrue(locationString.contains("北京市朝阳区建国门外大街1号"));
        }
        
        @Test
        @DisplayName("应该能够获取坐标字符串")
        void shouldGetCoordinatesString() {
            // Given
            Location location = Location.of(
                BigDecimal.valueOf(116.457), BigDecimal.valueOf(39.918),
                "北京市朝阳区建国门外大街1号", "北京市", "北京市"
            );
            
            // When
            String coordinates = location.getCoordinatesString();
            
            // Then
            assertEquals("116.457,39.918", coordinates);
        }
        
        @Test
        @DisplayName("应该能够获取完整地址")
        void shouldGetFullAddress() {
            // Given
            Location location = Location.of(
                BigDecimal.valueOf(116.457), BigDecimal.valueOf(39.918),
                "朝阳区建国门外大街1号", "北京市", "北京市"
            );
            
            // When
            String fullAddress = location.getFullAddress();
            
            // Then
            assertEquals("北京市北京市朝阳区建国门外大街1号", fullAddress);
        }
    }
    
    @Nested
    @DisplayName("值对象相等性测试")
    class LocationEqualityTest {
        
        @Test
        @DisplayName("相同属性的地理位置应该相等")
        void shouldBeEqualForSameProperties() {
            // Given
            Location location1 = Location.of(
                BigDecimal.valueOf(116.457), BigDecimal.valueOf(39.918),
                "北京市朝阳区", "北京市", "北京市"
            );
            Location location2 = Location.of(
                BigDecimal.valueOf(116.457), BigDecimal.valueOf(39.918),
                "北京市朝阳区", "北京市", "北京市"
            );
            
            // When & Then
            assertEquals(location1, location2);
            assertEquals(location1.hashCode(), location2.hashCode());
        }
        
        @Test
        @DisplayName("不同属性的地理位置应该不相等")
        void shouldNotBeEqualForDifferentProperties() {
            // Given
            Location location1 = Location.of(
                BigDecimal.valueOf(116.457), BigDecimal.valueOf(39.918),
                "北京市朝阳区", "北京市", "北京市"
            );
            Location location2 = Location.of(
                BigDecimal.valueOf(121.505), BigDecimal.valueOf(31.245),
                "上海市浦东新区", "上海市", "上海市"
            );
            
            // When & Then
            assertNotEquals(location1, location2);
        }
        
        @Test
        @DisplayName("地理位置不应该等于null或其他类型对象")
        void shouldNotEqualNullOrOtherTypes() {
            // Given
            Location location = Location.of(
                BigDecimal.valueOf(116.457), BigDecimal.valueOf(39.918),
                "北京市朝阳区", "北京市", "北京市"
            );
            
            // When & Then
            assertNotEquals(location, null);
            assertNotEquals(location, "字符串");
            assertNotEquals(location, 123);
        }
    }
    
    @Nested
    @DisplayName("地理位置工具方法测试")
    class LocationUtilityTest {
        
        @Test
        @DisplayName("应该能够获取用于地理搜索的经度")
        void shouldGetLongitudeForGeo() {
            // Given
            Location location = Location.of(
                BigDecimal.valueOf(116.457), BigDecimal.valueOf(39.918),
                "北京市朝阳区", "北京市", "北京市"
            );
            
            // When
            double longitude = location.getLongitudeForGeo();
            
            // Then
            assertEquals(116.457, longitude, 0.000001);
        }
        
        @Test
        @DisplayName("应该能够获取用于地理搜索的纬度")
        void shouldGetLatitudeForGeo() {
            // Given
            Location location = Location.of(
                BigDecimal.valueOf(116.457), BigDecimal.valueOf(39.918),
                "北京市朝阳区", "北京市", "北京市"
            );
            
            // When
            double latitude = location.getLatitudeForGeo();
            
            // Then
            assertEquals(39.918, latitude, 0.000001);
        }
        
        @Test
        @DisplayName("应该能够检查地理位置是否有效")
        void shouldCheckIfLocationIsValid() {
            // Given
            Location validLocation = Location.of(
                BigDecimal.valueOf(116.457), BigDecimal.valueOf(39.918),
                "北京市朝阳区", "北京市", "北京市"
            );
            
            // When & Then
            assertTrue(validLocation.isValid());
        }
    }
}
