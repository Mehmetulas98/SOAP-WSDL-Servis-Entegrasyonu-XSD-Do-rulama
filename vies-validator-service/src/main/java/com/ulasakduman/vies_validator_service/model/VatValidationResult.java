package com.ulasakduman.vies_validator_service.model;

public record VatValidationResult(
        String countryCode,
        String vatNumber,
        boolean valid,
        String name,
        String address
) {}