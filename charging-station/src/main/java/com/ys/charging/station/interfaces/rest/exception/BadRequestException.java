package com.ys.charging.station.interfaces.rest.exception;

/**
 * 错误请求异常
 * 
 * @author yang
 * @since 2025-06-23
 */
public class BadRequestException extends RuntimeException {
    
    public BadRequestException(String message) {
        super(message);
    }
    
    public BadRequestException(String message, Throwable cause) {
        super(message, cause);
    }
}
