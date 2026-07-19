package com.ulasakduman.vies_validator_service.model;

public record InvoiceValidationResponse(
        String invoiceNumber,
        boolean invoiceIssuable,
        String message,
        VatValidationResult sellerVat,
        VatValidationResult buyerVat
) {}