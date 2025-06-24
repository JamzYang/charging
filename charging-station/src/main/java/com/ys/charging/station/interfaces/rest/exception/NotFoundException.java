package com.ys.charging.station.interfaces.rest.exception;

/**
 * 资源未找到异常
 * 
 * @author yang
 * @since 2025-06-23
 */
public class NotFoundException extends RuntimeException {
    
    public NotFoundException(String message) {
        super(message);
    }
    
    public NotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
