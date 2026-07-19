package com.ulasakduman.vies_validator_service.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.ws.client.WebServiceIOException;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        problem.setTitle("Geçersiz istek");
        return problem;
    }

    @ExceptionHandler(ViesServiceException.class)
    public ProblemDetail handleViesService(ViesServiceException ex) {
        HttpStatus status = isClientFault(ex.getFaultCode())
                ? HttpStatus.BAD_REQUEST
                : HttpStatus.SERVICE_UNAVAILABLE;
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, ex.getMessage());
        problem.setTitle("VIES servis hatası");
        problem.setProperty("faultCode", ex.getFaultCode());
        return problem;
    }

    @ExceptionHandler(WebServiceIOException.class)
    public ProblemDetail handleWebServiceIO(WebServiceIOException ex) {
        log.error("VIES ağ hatası: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.SERVICE_UNAVAILABLE, "VIES servisine ulaşılamıyor. Lütfen tekrar deneyin.");
        problem.setTitle("VIES servis hatası");
        problem.setProperty("faultCode", "SERVICE_UNAVAILABLE");
        return problem;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneral(Exception ex) {
        log.error("Beklenmeyen hata: {}", ex.getMessage(), ex);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, "Beklenmeyen bir hata oluştu.");
        problem.setTitle("Sunucu hatası");
        return problem;
    }

    private boolean isClientFault(String faultCode) {
        return "INVALID_INPUT".equals(faultCode);
    }
}