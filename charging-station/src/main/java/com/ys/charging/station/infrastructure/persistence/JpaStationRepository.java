package com.ys.charging.station.infrastructure.persistence;

import com.ys.charging.station.domain.model.Station;
import com.ys.charging.station.domain.model.StationStatus;
import com.ys.charging.station.domain.repository.StationRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * JPA 充电站仓储实现
 * 
 * 基于 Spring Data JPA 实现充电站的数据访问操作。
 * 提供高性能的查询和统计功能。
 * 
 * @author yang
 * @since 2025-06-23
 */
@Repository
public class JpaStationRepository implements StationRepository {
    
    private final StationJpaRepository jpaRepository;
    
    @PersistenceContext
    private EntityManager entityManager;
    
    public JpaStationRepository(StationJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }
    
    @Override
    public Station save(Station station) {
        return jpaRepository.save(station);
    }
    
    @Override
    public Optional<Station> findById(Long id) {
        return jpaRepository.findById(id);
    }
    
    @Override
    public Optional<Station> findByIdWithConnectors(Long id) {
        return jpaRepository.findByIdWithConnectors(id);
    }
    
    @Override
    public List<Station> findByName(String name) {
        return jpaRepository.findByStationInfoName(name);
    }
    
    @Override
    public List<Station> findByOperator(String operator) {
        return jpaRepository.findByStationInfoOperator(operator);
    }
    
    @Override
    public List<Station> findByStatus(StationStatus status) {
        return jpaRepository.findByStatus(status);
    }
    
    @Override
    public Page<Station> findByStatus(StationStatus status, Pageable pageable) {
        return jpaRepository.findByStatus(status, pageable);
    }
    
    @Override
    public List<Station> findStationsWithAvailableConnectors() {
        return jpaRepository.findStationsWithAvailableConnectors();
    }
    
    @Override
    public List<Station> findByLocationBounds(BigDecimal minLongitude, BigDecimal maxLongitude,
                                             BigDecimal minLatitude, BigDecimal maxLatitude) {
        return jpaRepository.findByLocationBounds(minLongitude, maxLongitude, minLatitude, maxLatitude);
    }
    
    @Override
    public List<Station> findByCity(String city) {
        return jpaRepository.findByLocationCity(city);
    }
    
    @Override
    public List<Station> findByProvince(String province) {
        return jpaRepository.findByLocationProvince(province);
    }
    
    @Override
    public List<Station> findOperatingStations() {
        return jpaRepository.findByStatus(StationStatus.OPERATING);
    }
    
    @Override
    public Page<Station> findAll(Pageable pageable) {
        return jpaRepository.findAll(pageable);
    }
    
    @Override
    public Page<Station> searchByKeyword(String keyword, Pageable pageable) {
        return jpaRepository.searchByKeyword(keyword, pageable);
    }
    
    @Override
    public long count() {
        return jpaRepository.count();
    }
    
    @Override
    public long countByStatus(StationStatus status) {
        return jpaRepository.countByStatus(status);
    }
    
    @Override
    public long countByOperator(String operator) {
        return jpaRepository.countByStationInfoOperator(operator);
    }
    
    @Override
    public long countByCity(String city) {
        return jpaRepository.countByLocationCity(city);
    }
    
    @Override
    public boolean existsById(Long id) {
        return jpaRepository.existsById(id);
    }
    
    @Override
    public boolean existsByNameAndOperator(String name, String operator) {
        return jpaRepository.existsByStationInfoNameAndStationInfoOperator(name, operator);
    }
    
    @Override
    public void delete(Station station) {
        jpaRepository.delete(station);
    }
    
    @Override
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
    }
    
    @Override
    public List<Station> saveAll(List<Station> stations) {
        return jpaRepository.saveAll(stations);
    }
    
    @Override
    public List<Station> findAllById(List<Long> ids) {
        return jpaRepository.findAllById(ids);
    }
    
    @Override
    public void refresh(Station station) {
        entityManager.refresh(station);
    }
    
    @Override
    public StationStatistics getStatistics() {
        return jpaRepository.getStationStatistics();
    }
    
    /**
     * Spring Data JPA 仓储接口
     */
    interface StationJpaRepository extends JpaRepository<Station, Long> {
        
        /**
         * 根据名称查找充电站
         */
        List<Station> findByStationInfoName(String name);
        
        /**
         * 根据运营商查找充电站
         */
        List<Station> findByStationInfoOperator(String operator);
        
        /**
         * 根据状态查找充电站
         */
        List<Station> findByStatus(StationStatus status);
        
        /**
         * 根据状态分页查找充电站
         */
        Page<Station> findByStatus(StationStatus status, Pageable pageable);
        
        /**
         * 根据城市查找充电站
         */
        List<Station> findByLocationCity(String city);
        
        /**
         * 根据省份查找充电站
         */
        List<Station> findByLocationProvince(String province);
        
        /**
         * 根据状态统计数量
         */
        long countByStatus(StationStatus status);
        
        /**
         * 根据运营商统计数量
         */
        long countByStationInfoOperator(String operator);
        
        /**
         * 根据城市统计数量
         */
        long countByLocationCity(String city);
        
        /**
         * 检查名称和运营商是否存在
         */
        boolean existsByStationInfoNameAndStationInfoOperator(String name, String operator);
        
        /**
         * 查找包含充电桩信息的充电站
         */
        @Query("SELECT s FROM Station s LEFT JOIN FETCH s.connectors WHERE s.id = :id")
        Optional<Station> findByIdWithConnectors(@Param("id") Long id);
        
        /**
         * 查找有可用充电桩的充电站
         */
        @Query("SELECT DISTINCT s FROM Station s WHERE s.availableConnectors > 0 AND s.status = 'OPERATING'")
        List<Station> findStationsWithAvailableConnectors();
        
        /**
         * 根据地理位置范围查找充电站
         */
        @Query("SELECT s FROM Station s WHERE " +
               "s.location.longitude BETWEEN :minLng AND :maxLng AND " +
               "s.location.latitude BETWEEN :minLat AND :maxLat")
        List<Station> findByLocationBounds(@Param("minLng") BigDecimal minLongitude,
                                          @Param("maxLng") BigDecimal maxLongitude,
                                          @Param("minLat") BigDecimal minLatitude,
                                          @Param("maxLat") BigDecimal maxLatitude);
        
        /**
         * 根据关键词搜索充电站
         */
        @Query("SELECT s FROM Station s WHERE " +
               "LOWER(s.stationInfo.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
               "LOWER(s.stationInfo.operator) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
               "LOWER(s.location.address) LIKE LOWER(CONCAT('%', :keyword, '%'))")
        Page<Station> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);
        
        /**
         * 获取充电站统计信息
         */
        @Query("SELECT new com.ys.charging.station.domain.repository.StationRepository$StationStatistics(" +
               "COUNT(s), " +
               "SUM(CASE WHEN s.status = 'OPERATING' THEN 1 ELSE 0 END), " +
               "SUM(CASE WHEN s.status = 'MAINTENANCE' THEN 1 ELSE 0 END), " +
               "SUM(CASE WHEN s.status = 'FAULT' THEN 1 ELSE 0 END), " +
               "SUM(CASE WHEN s.status = 'CLOSED' THEN 1 ELSE 0 END), " +
               "COALESCE(SUM(s.totalConnectors), 0), " +
               "COALESCE(SUM(s.availableConnectors), 0), " +
               "COALESCE(AVG(CAST(s.totalConnectors AS double)), 0.0), " +
               "CASE WHEN COUNT(s) > 0 THEN " +
               "CAST(SUM(CASE WHEN s.status = 'OPERATING' THEN 1 ELSE 0 END) AS double) / COUNT(s) * 100 " +
               "ELSE 0.0 END" +
               ") FROM Station s")
        StationStatistics getStationStatistics();
    }
}
