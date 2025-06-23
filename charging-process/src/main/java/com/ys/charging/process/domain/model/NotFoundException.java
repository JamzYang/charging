package com.ys.charging.process.domain.model;

/**
 * 资源未找到异常
 * 
 * 当请求的资源不存在时抛出此异常。
 * 通常用于根据ID查找聚合根或实体时。
 * 
 * 使用场景：
 * - 根据ID查找充电会话不存在
 * - 查找用户、充电站、充电桩等资源不存在
 * - 仓储查询返回空结果
 * 
 * @author yang
 * @since 2025-06-23
 */
public class NotFoundException extends RuntimeException {
    
    private final String resourceType;
    private final Object resourceId;
    
    /**
     * 构造函数
     * 
     * @param message 错误消息
     */
    public NotFoundException(String message) {
        super(message);
        this.resourceType = null;
        this.resourceId = null;
    }
    
    /**
     * 构造函数
     * 
     * @param resourceType 资源类型
     * @param resourceId 资源ID
     */
    public NotFoundException(String resourceType, Object resourceId) {
        super(String.format("%s 不存在: %s", resourceType, resourceId));
        this.resourceType = resourceType;
        this.resourceId = resourceId;
    }
    
    /**
     * 构造函数
     * 
     * @param resourceType 资源类型
     * @param resourceId 资源ID
     * @param message 自定义错误消息
     */
    public NotFoundException(String resourceType, Object resourceId, String message) {
        super(message);
        this.resourceType = resourceType;
        this.resourceId = resourceId;
    }
    
    /**
     * 构造函数
     * 
     * @param message 错误消息
     * @param cause 原因异常
     */
    public NotFoundException(String message, Throwable cause) {
        super(message, cause);
        this.resourceType = null;
        this.resourceId = null;
    }
    
    /**
     * 获取资源类型
     * 
     * @return 资源类型
     */
    public String getResourceType() {
        return resourceType;
    }
    
    /**
     * 获取资源ID
     * 
     * @return 资源ID
     */
    public Object getResourceId() {
        return resourceId;
    }
    
    /**
     * 创建充电会话未找到异常
     * 
     * @param sessionId 会话ID
     * @return NotFoundException 实例
     */
    public static NotFoundException chargeSession(Long sessionId) {
        return new NotFoundException("充电会话", sessionId);
    }
    
    /**
     * 创建用户未找到异常
     * 
     * @param userId 用户ID
     * @return NotFoundException 实例
     */
    public static NotFoundException user(Long userId) {
        return new NotFoundException("用户", userId);
    }
    
    /**
     * 创建充电站未找到异常
     * 
     * @param stationId 充电站ID
     * @return NotFoundException 实例
     */
    public static NotFoundException station(Long stationId) {
        return new NotFoundException("充电站", stationId);
    }
    
    /**
     * 创建充电桩未找到异常
     * 
     * @param connectorId 充电桩ID
     * @return NotFoundException 实例
     */
    public static NotFoundException connector(Long connectorId) {
        return new NotFoundException("充电桩", connectorId);
    }
}
