package com.ys.charging.process.domain.service;

import com.ys.charging.process.domain.model.ChargeSessionEvent;
import com.ys.charging.process.domain.model.ChargeSessionStatus;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

/**
 * 充电会话状态转换表
 * 
 * 集中管理所有状态流转规则，定义从当前状态和触发事件到目标状态的映射关系。
 * 采用静态 Map 实现，便于维护和扩展。
 * 
 * 设计原则：
 * - 只定义状态转换的有效性和目标状态
 * - 不包含任何业务动作（Action）或副作用
 * - 支持后续扩展为配置驱动或数据库驱动
 * 
 * @author yang
 * @since 2025-06-23
 */
@Component
public class TransitionTable {
    
    /**
     * 状态转换映射表
     * 结构：Map<当前状态, Map<触发事件, 目标状态>>
     */
    private static final Map<ChargeSessionStatus, Map<ChargeSessionEvent, ChargeSessionStatus>> TRANSITION_MAP = Map.of(
        
        // INIT 状态的转换
        ChargeSessionStatus.INIT, Map.of(
            ChargeSessionEvent.RESERVE_CMD_SUCCESS, ChargeSessionStatus.RESERVED,
            ChargeSessionEvent.PLUG_DETECTED, ChargeSessionStatus.READY_TO_CHARGE
        ),
        
        // RESERVED 状态的转换
        ChargeSessionStatus.RESERVED, Map.of(
            ChargeSessionEvent.LOCK_READY, ChargeSessionStatus.LOCK_READY,
            ChargeSessionEvent.PLUG_DETECTED, ChargeSessionStatus.READY_TO_CHARGE,
            ChargeSessionEvent.RESERVATION_TIMEOUT, ChargeSessionStatus.END,
            ChargeSessionEvent.CANCEL_RESERVATION, ChargeSessionStatus.END
        ),
        
        // LOCK_READY 状态的转换
        ChargeSessionStatus.LOCK_READY, Map.of(
            ChargeSessionEvent.PLUG_DETECTED, ChargeSessionStatus.READY_TO_CHARGE,
            ChargeSessionEvent.RESERVATION_TIMEOUT, ChargeSessionStatus.END,
            ChargeSessionEvent.CANCEL_RESERVATION, ChargeSessionStatus.END
        ),
        
        // READY_TO_CHARGE 状态的转换
        ChargeSessionStatus.READY_TO_CHARGE, Map.of(
            ChargeSessionEvent.START_REQUESTED, ChargeSessionStatus.CHARGE_STARTING,
            ChargeSessionEvent.UNPLUG_DETECTED, ChargeSessionStatus.INIT
        ),
        
        // CHARGE_STARTING 状态的转换
        ChargeSessionStatus.CHARGE_STARTING, Map.of(
            ChargeSessionEvent.START_CONFIRMED, ChargeSessionStatus.CHARGING,
            ChargeSessionEvent.START_FAILED, ChargeSessionStatus.READY_TO_CHARGE
        ),
        
        // CHARGING 状态的转换
        ChargeSessionStatus.CHARGING, Map.of(
            ChargeSessionEvent.STOP_REQUESTED, ChargeSessionStatus.CHARGE_STOPPING,
            ChargeSessionEvent.STOP_CONFIRMED, ChargeSessionStatus.CHARGE_STOPPED
        ),
        
        // CHARGE_STOPPING 状态的转换
        ChargeSessionStatus.CHARGE_STOPPING, Map.of(
            ChargeSessionEvent.STOP_CONFIRMED, ChargeSessionStatus.CHARGE_STOPPED
        ),
        
        // CHARGE_STOPPED 状态的转换
        ChargeSessionStatus.CHARGE_STOPPED, Map.of(
            ChargeSessionEvent.PAYMENT_CONFIRMED, ChargeSessionStatus.PAID,
            ChargeSessionEvent.UNPLUG_DETECTED, ChargeSessionStatus.UNPAID,
            ChargeSessionEvent.PAYMENT_TIMEOUT, ChargeSessionStatus.UNPAID
        ),
        
        // PAID 状态的转换
        ChargeSessionStatus.PAID, Map.of(
            ChargeSessionEvent.UNPLUG_DETECTED, ChargeSessionStatus.END
        ),
        
        // UNPAID 状态的转换
        ChargeSessionStatus.UNPAID, Map.of(
            ChargeSessionEvent.PAYMENT_CONFIRMED, ChargeSessionStatus.END
        )
        
        // END 状态为终态，无转换
    );
    
    /**
     * 获取下一个状态
     * 
     * @param currentStatus 当前状态
     * @param event 触发事件
     * @return 目标状态，如果转换不合法则返回 Optional.empty()
     */
    public static Optional<ChargeSessionStatus> getNext(ChargeSessionStatus currentStatus, ChargeSessionEvent event) {
        return Optional.ofNullable(
            TRANSITION_MAP.getOrDefault(currentStatus, Map.of()).get(event)
        );
    }
    
    /**
     * 验证状态转换是否合法
     * 
     * @param currentStatus 当前状态
     * @param event 触发事件
     * @return true 如果转换合法，false 否则
     */
    public static boolean isValidTransition(ChargeSessionStatus currentStatus, ChargeSessionEvent event) {
        return getNext(currentStatus, event).isPresent();
    }
    
    /**
     * 获取指定状态的所有可能转换
     * 
     * @param status 状态
     * @return 该状态的所有可能事件和目标状态映射
     */
    public static Map<ChargeSessionEvent, ChargeSessionStatus> getPossibleTransitions(ChargeSessionStatus status) {
        return TRANSITION_MAP.getOrDefault(status, Map.of());
    }
}
