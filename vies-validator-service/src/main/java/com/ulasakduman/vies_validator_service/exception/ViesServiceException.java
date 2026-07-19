package com.ulasakduman.vies_validator_service.exception;

public class ViesServiceException extends RuntimeException {

    private final String faultCode;

    public ViesServiceException(String message, String faultCode) {
        super(message);
        this.faultCode = faultCode;
    }

    public ViesServiceException(String message, String faultCode, Throwable cause) {
        super(message, cause);
        this.faultCode = faultCode;
    }

    public String getFaultCode() {
        return faultCode;
    }
}