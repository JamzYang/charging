package com.ys.charging.station.domain.repository;

import com.ys.charging.station.domain.model.Station;
import com.ys.charging.station.domain.model.StationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * 充电站仓储接口
 * 
 * 定义充电站聚合根的持久化操作契约。
 * 遵循 DDD 仓储模式，封装数据访问逻辑。
 * 
 * @author yang
 * @since 2025-06-23
 */
public interface StationRepository {
    
    /**
     * 保存充电站
     * 
     * @param station 充电站聚合根
     * @return 保存后的充电站
     */
    Station save(Station station);
    
    /**
     * 根据ID查找充电站
     * 
     * @param id 充电站ID
     * @return 充电站，如果不存在则返回 empty
     */
    Optional<Station> findById(Long id);
    
    /**
     * 根据ID查找充电站（包含充电桩信息）
     * 
     * @param id 充电站ID
     * @return 充电站，如果不存在则返回 empty
     */
    Optional<Station> findByIdWithConnectors(Long id);
    
    /**
     * 根据名称查找充电站
     * 
     * @param name 充电站名称
     * @return 充电站列表
     */
    List<Station> findByName(String name);
    
    /**
     * 根据运营商查找充电站
     * 
     * @param operator 运营商
     * @return 充电站列表
     */
    List<Station> findByOperator(String operator);
    
    /**
     * 根据状态查找充电站
     * 
     * @param status 充电站状态
     * @return 充电站列表
     */
    List<Station> findByStatus(StationStatus status);
    
    /**
     * 根据状态分页查找充电站
     * 
     * @param status 充电站状态
     * @param pageable 分页参数
     * @return 分页的充电站列表
     */
    Page<Station> findByStatus(StationStatus status, Pageable pageable);
    
    /**
     * 查找有可用充电桩的充电站
     * 
     * @return 有可用充电桩的充电站列表
     */
    List<Station> findStationsWithAvailableConnectors();
    
    /**
     * 根据地理位置范围查找充电站
     * 
     * @param minLongitude 最小经度
     * @param maxLongitude 最大经度
     * @param minLatitude 最小纬度
     * @param maxLatitude 最大纬度
     * @return 指定范围内的充电站列表
     */
    List<Station> findByLocationBounds(BigDecimal minLongitude, BigDecimal maxLongitude,
                                      BigDecimal minLatitude, BigDecimal maxLatitude);
    
    /**
     * 根据城市查找充电站
     * 
     * @param city 城市名称
     * @return 指定城市的充电站列表
     */
    List<Station> findByCity(String city);
    
    /**
     * 根据省份查找充电站
     * 
     * @param province 省份名称
     * @return 指定省份的充电站列表
     */
    List<Station> findByProvince(String province);
    
    /**
     * 查找所有运营中的充电站
     * 
     * @return 运营中的充电站列表
     */
    List<Station> findOperatingStations();
    
    /**
     * 分页查找所有充电站
     * 
     * @param pageable 分页参数
     * @return 分页的充电站列表
     */
    Page<Station> findAll(Pageable pageable);
    
    /**
     * 根据关键词搜索充电站
     * 
     * @param keyword 搜索关键词（名称、地址、运营商）
     * @param pageable 分页参数
     * @return 匹配的充电站列表
     */
    Page<Station> searchByKeyword(String keyword, Pageable pageable);
    
    /**
     * 统计充电站总数
     * 
     * @return 充电站总数
     */
    long count();
    
    /**
     * 根据状态统计充电站数量
     * 
     * @param status 充电站状态
     * @return 指定状态的充电站数量
     */
    long countByStatus(StationStatus status);
    
    /**
     * 统计运营商的充电站数量
     * 
     * @param operator 运营商
     * @return 指定运营商的充电站数量
     */
    long countByOperator(String operator);
    
    /**
     * 统计城市的充电站数量
     * 
     * @param city 城市名称
     * @return 指定城市的充电站数量
     */
    long countByCity(String city);
    
    /**
     * 检查充电站是否存在
     * 
     * @param id 充电站ID
     * @return true 如果存在
     */
    boolean existsById(Long id);
    
    /**
     * 检查指定名称和运营商的充电站是否存在
     * 
     * @param name 充电站名称
     * @param operator 运营商
     * @return true 如果存在
     */
    boolean existsByNameAndOperator(String name, String operator);
    
    /**
     * 删除充电站
     * 
     * @param station 充电站聚合根
     */
    void delete(Station station);
    
    /**
     * 根据ID删除充电站
     * 
     * @param id 充电站ID
     */
    void deleteById(Long id);
    
    /**
     * 批量保存充电站
     * 
     * @param stations 充电站列表
     * @return 保存后的充电站列表
     */
    List<Station> saveAll(List<Station> stations);
    
    /**
     * 批量查找充电站
     * 
     * @param ids 充电站ID列表
     * @return 充电站列表
     */
    List<Station> findAllById(List<Long> ids);
    
    /**
     * 刷新充电站实体
     * 
     * @param station 充电站实体
     */
    void refresh(Station station);
    
    /**
     * 获取充电站统计信息
     * 
     * @return 统计信息
     */
    StationStatistics getStatistics();
    
    /**
     * 充电站统计信息
     */
    record StationStatistics(
        long totalStations,
        long operatingStations,
        long maintenanceStations,
        long faultStations,
        long closedStations,
        long totalConnectors,
        long availableConnectors,
        double averageConnectorsPerStation,
        double operatingRate
    ) {
        
        /**
         * 获取不可用充电站数量
         */
        public long getUnavailableStations() {
            return maintenanceStations + faultStations + closedStations;
        }
        
        /**
         * 计算运营率
         */
        public double calculateOperatingRate() {
            if (totalStations == 0) {
                return 0.0;
            }
            return (double) operatingStations / totalStations * 100;
        }
        
        /**
         * 计算充电桩可用率
         */
        public double calculateConnectorAvailabilityRate() {
            if (totalConnectors == 0) {
                return 0.0;
            }
            return (double) availableConnectors / totalConnectors * 100;
        }
        
        /**
         * 获取统计摘要
         */
        public String getSummary() {
            return String.format(
                "充电站总数: %d, 运营中: %d (%.1f%%), 充电桩总数: %d, 可用: %d (%.1f%%), 平均每站: %.1f个",
                totalStations, operatingStations, operatingRate,
                totalConnectors, availableConnectors, calculateConnectorAvailabilityRate(),
                averageConnectorsPerStation
            );
        }
    }
}
