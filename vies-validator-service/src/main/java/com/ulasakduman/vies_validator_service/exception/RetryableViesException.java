package com.ulasakduman.vies_validator_service.exception;

public class RetryableViesException extends ViesServiceException {

    public RetryableViesException(String message, String faultCode, Throwable cause) {
        super(message, faultCode, cause);
    }
}