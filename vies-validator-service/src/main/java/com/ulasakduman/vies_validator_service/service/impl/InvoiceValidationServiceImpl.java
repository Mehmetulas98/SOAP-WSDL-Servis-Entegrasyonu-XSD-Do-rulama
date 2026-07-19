package com.ulasakduman.vies_validator_service.service.impl;

import com.ulasakduman.vies_validator_service.model.InvoiceRequest;
import com.ulasakduman.vies_validator_service.model.InvoiceValidationResponse;
import com.ulasakduman.vies_validator_service.model.VatValidationResult;
import com.ulasakduman.vies_validator_service.service.InvoiceValidationService;
import com.ulasakduman.vies_validator_service.service.ViesClientService;
import eu.europa.ec.taxud.vies.services.checkvat.types.CheckVatResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class InvoiceValidationServiceImpl implements InvoiceValidationService {

    private static final Logger log = LoggerFactory.getLogger(InvoiceValidationServiceImpl.class);

    private final ViesClientService viesClientService;

    public InvoiceValidationServiceImpl(ViesClientService viesClientService) {
        this.viesClientService = viesClientService;
    }

    @Override
    public InvoiceValidationResponse validate(InvoiceRequest request) {
        // İşlem başlangıcı
        log.info("Fatura doğrulanıyor {} — Satıcı: {}/{}, Alıcı: {}/{}",
                request.invoiceNumber(),
                request.sellerCountryCode(), request.sellerVatNumber(),
                request.buyerCountryCode(), request.buyerVatNumber());

        // Satıcı için doğrulama
        VatValidationResult sellerResult = check(request.sellerCountryCode(), request.sellerVatNumber());
        // Alıcı için doğrulama
        VatValidationResult buyerResult = check(request.buyerCountryCode(), request.buyerVatNumber());

        InvoiceValidationResponse response = buildResponse(request.invoiceNumber(), sellerResult, buyerResult);
        if (response.invoiceIssuable()) {
            log.info("Fatura {} geçerli. Satıcı: {} [{}], Alıcı: {} [{}]",
                    request.invoiceNumber(),
                    sellerResult.name(), sellerResult.countryCode(),
                    buyerResult.name(), buyerResult.countryCode());
        } else {
            log.info("Fatura {} geçersiz: {}", request.invoiceNumber(), response.message());
        }

        return response;
    }

    // SOAP servise giden ve doğrulama yapan fonksiyon
    private VatValidationResult check(String countryCode, String vatNumber) {
        CheckVatResponse response = viesClientService.checkVat(countryCode, vatNumber);
        return new VatValidationResult(
                response.getCountryCode(),
                response.getVatNumber(),
                response.isValid(),
                response.getName() != null ? response.getName().getValue() : null,
                response.getAddress() != null ? response.getAddress().getValue() : null
        );
    }

    // Cevap bilgisini dönen fonksiyon. Alıcı ve satıcı kdv numaraları doğru ise fatura düzenlenebilir.
    // Aksi durumda olumsuz cevap dönülür
    private InvoiceValidationResponse buildResponse(String invoiceNumber,
                                                     VatValidationResult seller,
                                                     VatValidationResult buyer) {
        if (!seller.valid() && !buyer.valid()) {
            return new InvoiceValidationResponse(invoiceNumber, false,
                    "Fatura düzenlenemez. Satıcı ve Alıcı KDV numarası geçerli değildir.",
                    seller, buyer);
        }
        if (!seller.valid()) {
            return new InvoiceValidationResponse(invoiceNumber, false,
                    "Fatura düzenlenemez. Satıcı KDV numarası geçerli değildir.",
                    seller, buyer);
        }
        if (!buyer.valid()) {
            return new InvoiceValidationResponse(invoiceNumber, false,
                    "Fatura düzenlenemez. Alıcı KDV numarası geçerli değildir.",
                    seller, buyer);
        }
        return new InvoiceValidationResponse(invoiceNumber, true,
                "Fatura düzenlenebilir. Her iki KDV numarası da geçerlidir.",
                seller, buyer);
    }
}