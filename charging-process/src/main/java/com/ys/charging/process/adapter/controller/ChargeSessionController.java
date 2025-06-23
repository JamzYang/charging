package com.ys.charging.process.adapter.controller;

import com.ys.charging.process.application.service.ChargeSessionService;
import com.ys.charging.process.domain.model.ChargeSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * 充电会话控制器
 * 
 * 提供充电会话相关的 REST API 接口，包括：
 * - 会话创建和查询
 * - 状态转换操作
 * - 充电流程控制
 * 
 * 设计原则：
 * - RESTful API 设计
 * - 统一的响应格式
 * - 适当的 HTTP 状态码
 * - 输入参数验证
 * - 异常处理
 * 
 * @author yang
 * @since 2025-06-23
 */
@RestController
@RequestMapping("/api/v1/charge-sessions")
public class ChargeSessionController {
    
    private final ChargeSessionService chargeSessionService;
    
    @Autowired
    public ChargeSessionController(ChargeSessionService chargeSessionService) {
        this.chargeSessionService = chargeSessionService;
    }
    
    /**
     * 创建新的充电会话
     * 
     * @param request 创建会话请求
     * @return 创建的充电会话
     */
    @PostMapping
    public ResponseEntity<ChargeSession> createSession(@RequestBody CreateSessionRequest request) {
        ChargeSession session = chargeSessionService.createSession(
            request.userId(),
            request.stationId(),
            request.connectorId()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(session);
    }
    
    /**
     * 根据ID获取充电会话
     * 
     * @param sessionId 会话ID
     * @return 充电会话信息
     */
    @GetMapping("/{sessionId}")
    public ResponseEntity<ChargeSession> getSession(@PathVariable Long sessionId) {
        ChargeSession session = chargeSessionService.getSessionById(sessionId);
        return ResponseEntity.ok(session);
    }
    
    /**
     * 获取用户的充电会话列表
     * 
     * @param userId 用户ID
     * @return 充电会话列表
     */
    @GetMapping
    public ResponseEntity<List<ChargeSession>> getUserSessions(@RequestParam Long userId) {
        List<ChargeSession> sessions = chargeSessionService.getUserSessions(userId);
        return ResponseEntity.ok(sessions);
    }
    
    /**
     * 获取用户活跃的充电会话
     * 
     * @param userId 用户ID
     * @return 活跃的充电会话列表
     */
    @GetMapping("/active")
    public ResponseEntity<List<ChargeSession>> getUserActiveSessions(@RequestParam Long userId) {
        List<ChargeSession> sessions = chargeSessionService.getUserActiveSessions(userId);
        return ResponseEntity.ok(sessions);
    }
    
    /**
     * 预约充电桩
     * 
     * @param sessionId 会话ID
     * @param request 预约请求
     * @return 操作结果
     */
    @PostMapping("/{sessionId}/reserve")
    public ResponseEntity<ApiResponse> reserveConnector(
            @PathVariable Long sessionId, 
            @RequestBody ReserveRequest request) {
        chargeSessionService.reserveConnector(sessionId, request.reservationId());
        return ResponseEntity.ok(new ApiResponse("预约成功"));
    }
    
    /**
     * 启动充电
     * 
     * @param sessionId 会话ID
     * @return 操作结果
     */
    @PostMapping("/{sessionId}/start")
    public ResponseEntity<ApiResponse> startCharging(@PathVariable Long sessionId) {
        chargeSessionService.startCharging(sessionId);
        return ResponseEntity.ok(new ApiResponse("充电启动请求已发送"));
    }
    
    /**
     * 确认充电启动
     * 
     * @param sessionId 会话ID
     * @return 操作结果
     */
    @PostMapping("/{sessionId}/confirm-start")
    public ResponseEntity<ApiResponse> confirmChargingStart(@PathVariable Long sessionId) {
        chargeSessionService.confirmChargingStart(sessionId);
        return ResponseEntity.ok(new ApiResponse("充电启动已确认"));
    }
    
    /**
     * 停止充电
     * 
     * @param sessionId 会话ID
     * @return 操作结果
     */
    @PostMapping("/{sessionId}/stop")
    public ResponseEntity<ApiResponse> stopCharging(@PathVariable Long sessionId) {
        chargeSessionService.stopCharging(sessionId);
        return ResponseEntity.ok(new ApiResponse("充电停止请求已发送"));
    }
    
    /**
     * 确认充电停止
     * 
     * @param sessionId 会话ID
     * @param request 停止确认请求
     * @return 操作结果
     */
    @PostMapping("/{sessionId}/confirm-stop")
    public ResponseEntity<ApiResponse> confirmChargingStop(
            @PathVariable Long sessionId, 
            @RequestBody ConfirmStopRequest request) {
        chargeSessionService.confirmChargingStop(sessionId, request.energyConsumed());
        return ResponseEntity.ok(new ApiResponse("充电停止已确认"));
    }
    
    /**
     * 确认支付
     * 
     * @param sessionId 会话ID
     * @param request 支付确认请求
     * @return 操作结果
     */
    @PostMapping("/{sessionId}/confirm-payment")
    public ResponseEntity<ApiResponse> confirmPayment(
            @PathVariable Long sessionId, 
            @RequestBody PaymentRequest request) {
        chargeSessionService.confirmPayment(sessionId, request.amount());
        return ResponseEntity.ok(new ApiResponse("支付已确认"));
    }
    
    /**
     * 插枪检测回调
     * 
     * @param connectorId 充电桩ID
     * @return 操作结果
     */
    @PostMapping("/connectors/{connectorId}/plug-detected")
    public ResponseEntity<ApiResponse> handlePlugDetected(@PathVariable Long connectorId) {
        chargeSessionService.handlePlugDetected(connectorId);
        return ResponseEntity.ok(new ApiResponse("插枪检测已处理"));
    }
    
    /**
     * 拔枪检测回调
     * 
     * @param connectorId 充电桩ID
     * @return 操作结果
     */
    @PostMapping("/connectors/{connectorId}/unplug-detected")
    public ResponseEntity<ApiResponse> handleUnplugDetected(@PathVariable Long connectorId) {
        chargeSessionService.handleUnplugDetected(connectorId);
        return ResponseEntity.ok(new ApiResponse("拔枪检测已处理"));
    }
    
    /**
     * 处理预约超时（管理接口）
     * 
     * @param timeoutMinutes 超时分钟数
     * @return 处理结果
     */
    @PostMapping("/admin/handle-timeouts")
    public ResponseEntity<ApiResponse> handleReservationTimeouts(@RequestParam int timeoutMinutes) {
        int count = chargeSessionService.handleReservationTimeouts(timeoutMinutes);
        return ResponseEntity.ok(new ApiResponse("已处理 " + count + " 个超时预约"));
    }
    
    /**
     * 全局异常处理
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(new ApiResponse(e.getMessage()));
    }
    
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse> handleIllegalState(IllegalStateException e) {
        return ResponseEntity.badRequest().body(new ApiResponse(e.getMessage()));
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse> handleGenericException(Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse("系统内部错误: " + e.getMessage()));
    }
    
    // 请求和响应 DTO 类

    public record CreateSessionRequest(
        Long userId,
        Long stationId,
        Long connectorId
    ) {}

    public record ReserveRequest(
        Long reservationId
    ) {}

    public record ConfirmStopRequest(
        BigDecimal energyConsumed
    ) {}

    public record PaymentRequest(
        BigDecimal amount
    ) {}

    public record ApiResponse(
        String message
    ) {}
}
