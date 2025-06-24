package com.ys.charging.station.interfaces.rest.exception;

/**
 * 冲突异常
 * 
 * @author yang
 * @since 2025-06-23
 */
public class ConflictException extends RuntimeException {
    
    public ConflictException(String message) {
        super(message);
    }
    
    public ConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}
