package com.ulasakduman.vies_validator_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ulasakduman.vies_validator_service.exception.ViesServiceException;
import com.ulasakduman.vies_validator_service.model.InvoiceRequest;
import com.ulasakduman.vies_validator_service.model.InvoiceValidationResponse;
import com.ulasakduman.vies_validator_service.model.VatValidationResult;
import com.ulasakduman.vies_validator_service.service.InvoiceValidationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(InvoiceController.class)
class InvoiceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private InvoiceValidationService invoiceValidationService;

    @Test
    void validate_validRequest_returns200() throws Exception {
        InvoiceValidationResponse response = issuableResponse();
        when(invoiceValidationService.validate(any())).thenReturn(response);

        mockMvc.perform(post("/api/invoices/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.invoiceIssuable").value(true))
                .andExpect(jsonPath("$.invoiceNumber").value("INV-001"))
                .andExpect(jsonPath("$.sellerVat.valid").value(true))
                .andExpect(jsonPath("$.buyerVat.valid").value(true));
    }

    @Test
    void validate_invoiceNotIssuable_returns200WithFalseFlag() throws Exception {
        InvoiceValidationResponse response = notIssuableResponse("Invoice cannot be issued: buyer VAT number is invalid.");
        when(invoiceValidationService.validate(any())).thenReturn(response);

        mockMvc.perform(post("/api/invoices/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.invoiceIssuable").value(false))
                .andExpect(jsonPath("$.message").value("Invoice cannot be issued: buyer VAT number is invalid."));
    }

    @Test
    void validate_invalidCountryCode_returns400() throws Exception {
        InvoiceRequest badRequest = new InvoiceRequest("INV-001", "de", "129273398", "FR", "00300076965");

        mockMvc.perform(post("/api/invoices/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void validate_missingInvoiceNumber_returns400() throws Exception {
        InvoiceRequest badRequest = new InvoiceRequest("", "DE", "129273398", "FR", "00300076965");

        mockMvc.perform(post("/api/invoices/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void validate_viesInvalidInput_returns400() throws Exception {
        when(invoiceValidationService.validate(any()))
                .thenThrow(new ViesServiceException("INVALID_INPUT", "INVALID_INPUT"));

        mockMvc.perform(post("/api/invoices/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.faultCode").value("INVALID_INPUT"));
    }

    @Test
    void validate_viesServiceUnavailable_returns503() throws Exception {
        when(invoiceValidationService.validate(any()))
                .thenThrow(new ViesServiceException("VIES service is currently unavailable.", "SERVICE_UNAVAILABLE"));

        mockMvc.perform(post("/api/invoices/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.faultCode").value("SERVICE_UNAVAILABLE"));
    }

    // --- helpers ---

    private InvoiceRequest validRequest() {
        return new InvoiceRequest("INV-001", "DE", "129273398", "FR", "00300076965");
    }

    private InvoiceValidationResponse issuableResponse() {
        VatValidationResult seller = new VatValidationResult("DE", "129273398", true, "Seller GmbH", "Berlin");
        VatValidationResult buyer = new VatValidationResult("FR", "00300076965", true, "Buyer SARL", "Paris");
        return new InvoiceValidationResponse("INV-001", true, "Invoice can be issued. Both VAT numbers are valid.", seller, buyer);
    }

    private InvoiceValidationResponse notIssuableResponse(String message) {
        VatValidationResult seller = new VatValidationResult("DE", "129273398", true, "Seller GmbH", "Berlin");
        VatValidationResult buyer = new VatValidationResult("FR", "00300076965", false, null, null);
        return new InvoiceValidationResponse("INV-001", false, message, seller, buyer);
    }
}