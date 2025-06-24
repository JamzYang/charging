package com.ys.charging.station.domain.model;

import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 充电站信息值对象
 * 
 * 封装充电站的基本信息，包括名称、运营商、联系方式等。
 * 使用 Java 21 record 特性，确保不可变性。
 * 
 * @param name 充电站名称
 * @param operator 运营商
 * @param contactPhone 联系电话
 * @param description 描述信息
 * @param facilities 配套设施
 * 
 * @author yang
 * @since 2025-06-23
 */
@Embeddable
public record StationInfo(
    @NotBlank(message = "充电站名称不能为空")
    @Size(max = 100, message = "充电站名称长度不能超过100个字符")
    String name,
    
    @NotBlank(message = "运营商不能为空")
    @Size(max = 50, message = "运营商名称长度不能超过50个字符")
    String operator,
    
    @Pattern(regexp = "^1[3-9]\\d{9}$|^0\\d{2,3}-\\d{7,8}$", 
             message = "联系电话格式不正确")
    String contactPhone,
    
    @Size(max = 500, message = "描述信息长度不能超过500个字符")
    String description,
    
    @Size(max = 200, message = "配套设施信息长度不能超过200个字符")
    String facilities
) {
    
    /**
     * 创建充电站信息
     */
    public StationInfo {
        // 清理和标准化数据
        if (name != null) {
            name = name.trim();
        }
        if (operator != null) {
            operator = operator.trim();
        }
        if (contactPhone != null) {
            contactPhone = contactPhone.trim();
        }
        if (description != null) {
            description = description.trim();
        }
        if (facilities != null) {
            facilities = facilities.trim();
        }
    }
    
    /**
     * 创建基本充电站信息
     * 
     * @param name 充电站名称
     * @param operator 运营商
     * @return StationInfo 对象
     */
    public static StationInfo of(String name, String operator) {
        return new StationInfo(name, operator, null, null, null);
    }
    
    /**
     * 创建包含联系方式的充电站信息
     * 
     * @param name 充电站名称
     * @param operator 运营商
     * @param contactPhone 联系电话
     * @return StationInfo 对象
     */
    public static StationInfo of(String name, String operator, String contactPhone) {
        return new StationInfo(name, operator, contactPhone, null, null);
    }
    
    /**
     * 创建完整的充电站信息
     * 
     * @param name 充电站名称
     * @param operator 运营商
     * @param contactPhone 联系电话
     * @param description 描述信息
     * @param facilities 配套设施
     * @return StationInfo 对象
     */
    public static StationInfo of(String name, String operator, String contactPhone, 
                                String description, String facilities) {
        return new StationInfo(name, operator, contactPhone, description, facilities);
    }
    
    /**
     * 获取显示名称（包含运营商信息）
     * 
     * @return 格式化的显示名称
     */
    public String getDisplayName() {
        return String.format("%s (%s)", name, operator);
    }
    
    /**
     * 获取联系电话（别名方法，用于 DTO 转换）
     *
     * @return 联系电话
     */
    public String phone() {
        return contactPhone;
    }

    /**
     * 判断是否有联系电话
     *
     * @return true 如果有联系电话
     */
    public boolean hasContactPhone() {
        return contactPhone != null && !contactPhone.trim().isEmpty();
    }
    
    /**
     * 判断是否有描述信息
     * 
     * @return true 如果有描述信息
     */
    public boolean hasDescription() {
        return description != null && !description.trim().isEmpty();
    }
    
    /**
     * 判断是否有配套设施信息
     * 
     * @return true 如果有配套设施信息
     */
    public boolean hasFacilities() {
        return facilities != null && !facilities.trim().isEmpty();
    }
    
    /**
     * 获取格式化的联系电话
     * 
     * @return 格式化的电话号码，如果没有则返回 "暂无"
     */
    public String getFormattedContactPhone() {
        if (!hasContactPhone()) {
            return "暂无";
        }
        
        // 手机号码格式化：138****1234
        if (contactPhone.matches("^1[3-9]\\d{9}$")) {
            return contactPhone.substring(0, 3) + "****" + contactPhone.substring(7);
        }
        
        // 固定电话保持原样
        return contactPhone;
    }
    
    /**
     * 获取简短描述（限制长度）
     * 
     * @param maxLength 最大长度
     * @return 简短描述
     */
    public String getShortDescription(int maxLength) {
        if (!hasDescription()) {
            return "";
        }
        
        if (description.length() <= maxLength) {
            return description;
        }
        
        return description.substring(0, maxLength - 3) + "...";
    }
    
    /**
     * 更新联系电话
     * 
     * @param newContactPhone 新的联系电话
     * @return 新的 StationInfo 对象
     */
    public StationInfo withContactPhone(String newContactPhone) {
        return new StationInfo(name, operator, newContactPhone, description, facilities);
    }
    
    /**
     * 更新描述信息
     * 
     * @param newDescription 新的描述信息
     * @return 新的 StationInfo 对象
     */
    public StationInfo withDescription(String newDescription) {
        return new StationInfo(name, operator, contactPhone, newDescription, facilities);
    }
    
    /**
     * 更新配套设施信息
     * 
     * @param newFacilities 新的配套设施信息
     * @return 新的 StationInfo 对象
     */
    public StationInfo withFacilities(String newFacilities) {
        return new StationInfo(name, operator, contactPhone, description, newFacilities);
    }
}
