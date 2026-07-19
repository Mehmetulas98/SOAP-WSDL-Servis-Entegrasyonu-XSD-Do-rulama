package com.ulasakduman.vies_validator_service.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record InvoiceRequest(

        @NotBlank
        String invoiceNumber,

        @NotBlank
        @Pattern(regexp = "[A-Z]{2}", message = "Ülke kodu 2 büyük harften oluşmalıdır")
        String sellerCountryCode,

        @NotBlank
        @Pattern(regexp = "[0-9A-Za-z+*.]{2,12}", message = "KDV numarası 2-12 karakter arasında alfanumerik olmalıdır")
        String sellerVatNumber,

        @NotBlank
        @Pattern(regexp = "[A-Z]{2}", message = "Ülke kodu 2 büyük harften oluşmalıdır")
        String buyerCountryCode,

        @NotBlank
        @Pattern(regexp = "[0-9A-Za-z+*.]{2,12}", message = "KDV numarası 2-12 karakter arasında alfanumerik olmalıdır")
        String buyerVatNumber
) {}