package com.ys.charging.station.domain.model;

import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/**
 * 充电桩信息值对象
 * 
 * 封装充电桩的技术规格和基本信息。
 * 使用 Java 21 record 特性，确保不可变性。
 * 
 * @param connectorNumber 充电桩编号
 * @param connectorType 充电桩类型
 * @param maxPower 最大功率（kW）
 * @param voltage 电压（V）
 * @param current 电流（A）
 * @param protocol 通信协议
 * 
 * @author yang
 * @since 2025-06-23
 */
@Embeddable
public record ConnectorInfo(
    @NotBlank(message = "充电桩编号不能为空")
    String connectorNumber,
    
    @NotNull(message = "充电桩类型不能为空")
    @Enumerated(EnumType.STRING)
    ConnectorType connectorType,
    
    @NotNull(message = "最大功率不能为空")
    @Positive(message = "最大功率必须大于0")
    BigDecimal maxPower,
    
    @NotNull(message = "电压不能为空")
    @Positive(message = "电压必须大于0")
    Integer voltage,
    
    @NotNull(message = "电流不能为空")
    @Positive(message = "电流必须大于0")
    Integer current,
    
    @NotNull(message = "通信协议不能为空")
    @Enumerated(EnumType.STRING)
    CommunicationProtocol protocol
) {
    
    /**
     * 充电桩类型枚举
     */
    public enum ConnectorType {
        /**
         * 国标直流快充
         */
        GB_T_DC("国标直流", "GB/T 20234.3"),
        
        /**
         * 国标交流慢充
         */
        GB_T_AC("国标交流", "GB/T 20234.2"),
        
        /**
         * 特斯拉超充
         */
        TESLA_SUPERCHARGER("特斯拉超充", "Tesla Supercharger"),
        
        /**
         * CCS欧标
         */
        CCS_COMBO("CCS欧标", "CCS Combo"),
        
        /**
         * CHAdeMO日标
         */
        CHADEMO("CHAdeMO日标", "CHAdeMO");
        
        private final String displayName;
        private final String standard;
        
        ConnectorType(String displayName, String standard) {
            this.displayName = displayName;
            this.standard = standard;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        public String getStandard() {
            return standard;
        }
        
        /**
         * 判断是否为直流快充
         */
        public boolean isDC() {
            return this == GB_T_DC || this == TESLA_SUPERCHARGER || 
                   this == CCS_COMBO || this == CHADEMO;
        }
        
        /**
         * 判断是否为交流慢充
         */
        public boolean isAC() {
            return this == GB_T_AC;
        }
    }
    
    /**
     * 通信协议枚举
     */
    public enum CommunicationProtocol {
        /**
         * OCPP 1.6
         */
        OCPP_16("OCPP 1.6"),
        
        /**
         * OCPP 2.0
         */
        OCPP_20("OCPP 2.0"),
        
        /**
         * 国网协议
         */
        SGCC("国网协议"),
        
        /**
         * 自定义协议
         */
        CUSTOM("自定义协议");
        
        private final String displayName;
        
        CommunicationProtocol(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    /**
     * 创建充电桩信息
     */
    public ConnectorInfo {
        // 验证功率与电压电流的匹配性
        if (maxPower != null && voltage != null && current != null) {
            // P = U * I / 1000 (kW)
            BigDecimal calculatedPower = BigDecimal.valueOf(voltage)
                .multiply(BigDecimal.valueOf(current))
                .divide(BigDecimal.valueOf(1000));
            
            // 允许10%的误差
            BigDecimal tolerance = maxPower.multiply(BigDecimal.valueOf(0.1));
            if (calculatedPower.subtract(maxPower).abs().compareTo(tolerance) > 0) {
                throw new IllegalArgumentException(
                    String.format("功率配置不匹配：计算功率 %.2f kW，声明功率 %.2f kW", 
                        calculatedPower.doubleValue(), maxPower.doubleValue()));
            }
        }
    }
    
    /**
     * 创建国标直流充电桩
     * 
     * @param connectorNumber 充电桩编号
     * @param maxPower 最大功率（kW）
     * @return ConnectorInfo 对象
     */
    public static ConnectorInfo createGBTDC(String connectorNumber, BigDecimal maxPower) {
        // 根据功率确定电压电流
        int voltage = maxPower.compareTo(BigDecimal.valueOf(60)) > 0 ? 750 : 500;
        int current = maxPower.multiply(BigDecimal.valueOf(1000))
            .divide(BigDecimal.valueOf(voltage), 0, BigDecimal.ROUND_UP).intValue();
        
        return new ConnectorInfo(
            connectorNumber,
            ConnectorType.GB_T_DC,
            maxPower,
            voltage,
            current,
            CommunicationProtocol.OCPP_16
        );
    }
    
    /**
     * 创建国标交流充电桩
     * 
     * @param connectorNumber 充电桩编号
     * @param maxPower 最大功率（kW）
     * @return ConnectorInfo 对象
     */
    public static ConnectorInfo createGBTAC(String connectorNumber, BigDecimal maxPower) {
        // 交流充电桩通常使用220V或380V
        int voltage = maxPower.compareTo(BigDecimal.valueOf(22)) > 0 ? 380 : 220;
        int current = maxPower.multiply(BigDecimal.valueOf(1000))
            .divide(BigDecimal.valueOf(voltage), 0, BigDecimal.ROUND_UP).intValue();
        
        return new ConnectorInfo(
            connectorNumber,
            ConnectorType.GB_T_AC,
            maxPower,
            voltage,
            current,
            CommunicationProtocol.OCPP_16
        );
    }
    
    /**
     * 获取充电桩类型描述
     * 
     * @return 类型描述字符串
     */
    public String getTypeDescription() {
        return String.format("%s (%s)", 
            connectorType.getDisplayName(), 
            connectorType.getStandard());
    }
    
    /**
     * 获取功率描述
     * 
     * @return 功率描述字符串
     */
    public String getPowerDescription() {
        return String.format("%.1f kW (%dV/%dA)", 
            maxPower.doubleValue(), voltage, current);
    }
    
    /**
     * 判断是否为快充
     * 
     * @return true 如果是快充（功率 >= 50kW）
     */
    public boolean isFastCharging() {
        return maxPower.compareTo(BigDecimal.valueOf(50)) >= 0;
    }
    
    /**
     * 判断是否为超充
     * 
     * @return true 如果是超充（功率 >= 150kW）
     */
    public boolean isSuperCharging() {
        return maxPower.compareTo(BigDecimal.valueOf(150)) >= 0;
    }
}
