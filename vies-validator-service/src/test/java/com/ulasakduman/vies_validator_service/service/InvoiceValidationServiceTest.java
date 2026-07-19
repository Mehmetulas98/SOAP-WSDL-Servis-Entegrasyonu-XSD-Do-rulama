package com.ulasakduman.vies_validator_service.service;

import com.ulasakduman.vies_validator_service.exception.ViesServiceException;
import com.ulasakduman.vies_validator_service.model.InvoiceRequest;
import com.ulasakduman.vies_validator_service.model.InvoiceValidationResponse;
import com.ulasakduman.vies_validator_service.service.impl.InvoiceValidationServiceImpl;
import eu.europa.ec.taxud.vies.services.checkvat.types.CheckVatResponse;
import eu.europa.ec.taxud.vies.services.checkvat.types.ObjectFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvoiceValidationServiceTest {

    @Mock
    private ViesClientService viesClientService;

    private InvoiceValidationService service;
    private final ObjectFactory objectFactory = new ObjectFactory();

    @BeforeEach
    void setUp() {
        service = new InvoiceValidationServiceImpl(viesClientService);
    }

    @Test
    void validate_bothValid_invoiceIsIssuable() {
        when(viesClientService.checkVat("DE", "129273398")).thenReturn(validResponse("DE", "129273398", "Seller GmbH", "Berlin"));
        when(viesClientService.checkVat("FR", "00300076965")).thenReturn(validResponse("FR", "00300076965", "Buyer SARL", "Paris"));

        InvoiceValidationResponse response = service.validate(invoiceRequest());

        assertThat(response.invoiceIssuable()).isTrue();
        assertThat(response.sellerVat().valid()).isTrue();
        assertThat(response.buyerVat().valid()).isTrue();
        assertThat(response.sellerVat().name()).isEqualTo("Seller GmbH");
        assertThat(response.buyerVat().name()).isEqualTo("Buyer SARL");
    }

    @Test
    void validate_sellerInvalid_invoiceNotIssuable() {
        when(viesClientService.checkVat("DE", "129273398")).thenReturn(invalidResponse("DE", "129273398"));
        when(viesClientService.checkVat("FR", "00300076965")).thenReturn(validResponse("FR", "00300076965", "Buyer SARL", "Paris"));

        InvoiceValidationResponse response = service.validate(invoiceRequest());

        assertThat(response.invoiceIssuable()).isFalse();
        assertThat(response.message()).contains("Satıcı");
        assertThat(response.sellerVat().valid()).isFalse();
        assertThat(response.buyerVat().valid()).isTrue();
    }

    @Test
    void validate_buyerInvalid_invoiceNotIssuable() {
        when(viesClientService.checkVat("DE", "129273398")).thenReturn(validResponse("DE", "129273398", "Seller GmbH", "Berlin"));
        when(viesClientService.checkVat("FR", "00300076965")).thenReturn(invalidResponse("FR", "00300076965"));

        InvoiceValidationResponse response = service.validate(invoiceRequest());

        assertThat(response.invoiceIssuable()).isFalse();
        assertThat(response.message()).contains("Alıcı");
        assertThat(response.buyerVat().valid()).isFalse();
    }

    @Test
    void validate_bothInvalid_invoiceNotIssuable() {
        when(viesClientService.checkVat("DE", "129273398")).thenReturn(invalidResponse("DE", "129273398"));
        when(viesClientService.checkVat("FR", "00300076965")).thenReturn(invalidResponse("FR", "00300076965"));

        InvoiceValidationResponse response = service.validate(invoiceRequest());

        assertThat(response.invoiceIssuable()).isFalse();
        assertThat(response.message()).contains("Satıcı").contains("Alıcı");
    }

    @Test
    void validate_viesThrows_exceptionPropagates() {
        when(viesClientService.checkVat("DE", "129273398"))
                .thenThrow(new ViesServiceException("Service unavailable", "SERVICE_UNAVAILABLE"));

        assertThatThrownBy(() -> service.validate(invoiceRequest()))
                .isInstanceOf(ViesServiceException.class)
                .hasMessageContaining("unavailable");
    }

    // --- helpers ---

    private InvoiceRequest invoiceRequest() {
        return new InvoiceRequest("INV-001", "DE", "129273398", "FR", "00300076965");
    }

    private CheckVatResponse validResponse(String country, String vatNumber, String name, String address) {
        CheckVatResponse r = new CheckVatResponse();
        r.setCountryCode(country);
        r.setVatNumber(vatNumber);
        r.setValid(true);
        r.setName(objectFactory.createCheckVatResponseName(name));
        r.setAddress(objectFactory.createCheckVatResponseAddress(address));
        return r;
    }

    private CheckVatResponse invalidResponse(String country, String vatNumber) {
        CheckVatResponse r = new CheckVatResponse();
        r.setCountryCode(country);
        r.setVatNumber(vatNumber);
        r.setValid(false);
        return r;
    }
}