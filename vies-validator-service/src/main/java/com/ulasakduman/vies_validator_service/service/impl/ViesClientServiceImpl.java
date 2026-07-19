package com.ulasakduman.vies_validator_service.service.impl;

import com.ulasakduman.vies_validator_service.exception.RetryableViesException;
import com.ulasakduman.vies_validator_service.exception.ViesServiceException;
import com.ulasakduman.vies_validator_service.service.ViesClientService;
import eu.europa.ec.taxud.vies.services.checkvat.types.CheckVat;
import eu.europa.ec.taxud.vies.services.checkvat.types.CheckVatResponse;
import jakarta.xml.bind.UnmarshalException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.oxm.UnmarshallingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.ws.client.WebServiceIOException;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.soap.client.SoapFaultClientException;

import java.util.Set;

@Service
public class ViesClientServiceImpl implements ViesClientService {

    private static final Logger log = LoggerFactory.getLogger(ViesClientServiceImpl.class);

    // Retry olması gereken hata kodları
    private static final Set<String> RETRYABLE_FAULT_CODES = Set.of(
            "MS_UNAVAILABLE", "TIMEOUT", "MS_MAX_CONCURRENT_REQ",
            "GLOBAL_MAX_CONCURRENT_REQ", "SERVICE_UNAVAILABLE"
    );

    private final WebServiceTemplate webServiceTemplate;

    public ViesClientServiceImpl(WebServiceTemplate webServiceTemplate) {
        this.webServiceTemplate = webServiceTemplate;
    }

    @Override
    // Spring boot 4.1 ile gelen retry mekanizması. Çağrım max 3 defa yapılıyor.
    @Retryable(
            retryFor = {RetryableViesException.class, WebServiceIOException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000)
    )
    public CheckVatResponse checkVat(String countryCode, String vatNumber) {
        log.debug("VIES checkVat çağrılıyor — countryCode={}, vatNumber={}", countryCode, vatNumber);
        CheckVat request = new CheckVat();
        request.setCountryCode(countryCode);
        request.setVatNumber(vatNumber);

        try {
            return (CheckVatResponse) webServiceTemplate.marshalSendAndReceive(request);
        } catch (SoapFaultClientException ex) {
            String faultCode = ex.getFaultStringOrReason();
            log.warn("VIES SOAP Fault {}/{} için: {}", countryCode, vatNumber, faultCode);
            throw toException(faultCode, ex);
        } catch (UnmarshallingFailureException ex) {
            // Servis bazen soap fault kodunu http 200 olarak dönüyor.  Body unmarshal fakat exceptiona sebep olur.
            String faultCode = extractFaultCode(ex);
            log.warn("VIES HTTP 200 ile SOAP Fault döndürdü {}/{}: {}", countryCode, vatNumber, faultCode);
            throw toException(faultCode, ex);
        } catch (WebServiceIOException ex) {
            log.warn("VIES ağ hatası {}/{}: {}", countryCode, vatNumber, ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            log.error("Beklenmeyen VIES hatası {}/{}: {}", countryCode, vatNumber, ex.getMessage(), ex);
            throw new ViesServiceException("VIES servisinden beklenmeyen hata alındı.", "SERVICE_UNAVAILABLE", ex);
        }
    }

    @Recover
    public CheckVatResponse recover(RetryableViesException ex, String countryCode, String vatNumber) {
        log.error("Tüm denemeler tükendi — VIES checkVat {}/{}: {}", countryCode, vatNumber, ex.getMessage());
        throw new ViesServiceException(
                "VIES servisine şu an ulaşılamıyor. Lütfen daha sonra tekrar deneyiniz.",
                "SERVICE_UNAVAILABLE",
                ex
        );
    }

    @Recover
    public CheckVatResponse recover(WebServiceIOException ex, String countryCode, String vatNumber) {
        log.error("Tüm denemeler tükendi (ağ hatası) — VIES checkVat {}/{}: {}", countryCode, vatNumber, ex.getMessage());
        throw new ViesServiceException(
                "VIES servisine şu an ulaşılamıyor. Lütfen daha sonra tekrar deneyiniz.",
                "SERVICE_UNAVAILABLE",
                ex
        );
    }

    private ViesServiceException toException(String faultCode, Throwable cause) {
        if (RETRYABLE_FAULT_CODES.contains(faultCode)) {
            return new RetryableViesException(faultCode, faultCode, cause);
        }
        return new ViesServiceException(faultCode, faultCode, cause);
    }

    private String extractFaultCode(UnmarshallingFailureException ex) {
        if (ex.getCause() instanceof UnmarshalException ue && ue.getMessage() != null) {
            String msg = ue.getMessage();
            for (String code : RETRYABLE_FAULT_CODES) {
                if (msg.contains(code)) return code;
            }
            if (msg.contains("INVALID_INPUT")) return "INVALID_INPUT";
        }
        return "SERVICE_UNAVAILABLE";
    }
}