package com.ulasakduman.vies_validator_service.service;

import com.ulasakduman.vies_validator_service.model.InvoiceRequest;
import com.ulasakduman.vies_validator_service.model.InvoiceValidationResponse;

public interface InvoiceValidationService {

    InvoiceValidationResponse validate(InvoiceRequest request);
}