package com.ys.charging.station.interfaces.rest.exception;

/**
 * 内部服务器错误异常
 * 
 * @author yang
 * @since 2025-06-23
 */
public class InternalServerErrorException extends RuntimeException {
    
    public InternalServerErrorException(String message) {
        super(message);
    }
    
    public InternalServerErrorException(String message, Throwable cause) {
        super(message, cause);
    }
}
