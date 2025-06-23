package com.ys.charging.station.domain.model;

import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * 营业时间值对象
 * 
 * 封装充电站的营业时间信息，支持营业时间验证和查询。
 * 使用 Java 21 record 特性，确保不可变性。
 * 
 * @param openTime 开始营业时间
 * @param closeTime 结束营业时间
 * @param is24Hours 是否24小时营业
 * 
 * @author yang
 * @since 2025-06-23
 */
@Embeddable
public record BusinessHours(
    @NotNull
    LocalTime openTime,
    
    @NotNull
    LocalTime closeTime,
    
    boolean is24Hours
) {
    
    /**
     * 创建营业时间对象
     */
    public BusinessHours {
        // 如果是24小时营业，设置标准时间
        if (is24Hours) {
            openTime = LocalTime.of(0, 0);
            closeTime = LocalTime.of(23, 59);
        } else {
            // 验证营业时间的合理性
            if (openTime != null && closeTime != null) {
                if (openTime.equals(closeTime)) {
                    throw new IllegalArgumentException("开始时间和结束时间不能相同");
                }
            }
        }
    }
    
    /**
     * 创建24小时营业的营业时间
     * 
     * @return 24小时营业的 BusinessHours
     */
    public static BusinessHours always() {
        return new BusinessHours(LocalTime.of(0, 0), LocalTime.of(23, 59), true);
    }
    
    /**
     * 创建指定时间段的营业时间
     * 
     * @param openTime 开始时间
     * @param closeTime 结束时间
     * @return BusinessHours 对象
     */
    public static BusinessHours of(LocalTime openTime, LocalTime closeTime) {
        return new BusinessHours(openTime, closeTime, false);
    }
    
    /**
     * 创建指定时间段的营业时间（字符串格式）
     * 
     * @param openTime 开始时间（HH:mm 格式）
     * @param closeTime 结束时间（HH:mm 格式）
     * @return BusinessHours 对象
     */
    public static BusinessHours of(String openTime, String closeTime) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        return new BusinessHours(
            LocalTime.parse(openTime, formatter),
            LocalTime.parse(closeTime, formatter),
            false
        );
    }
    
    /**
     * 判断当前时间是否在营业时间内
     * 
     * @return true 如果当前时间在营业时间内
     */
    public boolean isOpenNow() {
        return isOpenAt(LocalTime.now());
    }
    
    /**
     * 判断指定时间是否在营业时间内
     * 
     * @param time 指定时间
     * @return true 如果指定时间在营业时间内
     */
    public boolean isOpenAt(LocalTime time) {
        if (is24Hours) {
            return true;
        }
        
        if (time == null) {
            return false;
        }
        
        // 处理跨天的情况（如 22:00 - 06:00）
        if (closeTime.isBefore(openTime)) {
            // 跨天营业：当前时间在开始时间之后，或在结束时间之前
            return time.isAfter(openTime) || time.equals(openTime) || 
                   time.isBefore(closeTime) || time.equals(closeTime);
        } else {
            // 同天营业：当前时间在开始和结束时间之间
            return (time.isAfter(openTime) || time.equals(openTime)) && 
                   (time.isBefore(closeTime) || time.equals(closeTime));
        }
    }
    
    /**
     * 获取营业时间描述
     * 
     * @return 营业时间的字符串描述
     */
    public String getDescription() {
        if (is24Hours) {
            return "24小时营业";
        }
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        return String.format("%s - %s", 
            openTime.format(formatter), 
            closeTime.format(formatter));
    }
    
    /**
     * 计算营业时长（小时）
     * 
     * @return 营业时长
     */
    public double getBusinessHours() {
        if (is24Hours) {
            return 24.0;
        }
        
        if (closeTime.isBefore(openTime)) {
            // 跨天营业
            long minutesToMidnight = 24 * 60 - (openTime.getHour() * 60 + openTime.getMinute());
            long minutesFromMidnight = closeTime.getHour() * 60 + closeTime.getMinute();
            return (minutesToMidnight + minutesFromMidnight) / 60.0;
        } else {
            // 同天营业
            long totalMinutes = (closeTime.getHour() * 60 + closeTime.getMinute()) - 
                               (openTime.getHour() * 60 + openTime.getMinute());
            return totalMinutes / 60.0;
        }
    }
    
    /**
     * 判断是否为跨天营业
     * 
     * @return true 如果跨天营业
     */
    public boolean isCrossDay() {
        return !is24Hours && closeTime.isBefore(openTime);
    }
}
