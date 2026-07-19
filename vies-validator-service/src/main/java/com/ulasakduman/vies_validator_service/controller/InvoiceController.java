package com.ulasakduman.vies_validator_service.controller;

import com.ulasakduman.vies_validator_service.model.InvoiceRequest;
import com.ulasakduman.vies_validator_service.model.InvoiceValidationResponse;
import com.ulasakduman.vies_validator_service.service.InvoiceValidationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/invoices")
public class InvoiceController {

    private final InvoiceValidationService invoiceValidationService;

    public InvoiceController(InvoiceValidationService invoiceValidationService) {
        this.invoiceValidationService = invoiceValidationService;
    }

    @PostMapping("/validate")
    public ResponseEntity<InvoiceValidationResponse> validate(@RequestBody @Valid InvoiceRequest request) {
        return ResponseEntity.ok(invoiceValidationService.validate(request));
    }
}