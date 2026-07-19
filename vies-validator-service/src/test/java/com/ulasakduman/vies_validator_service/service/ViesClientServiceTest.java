package com.ulasakduman.vies_validator_service.service;

import com.ulasakduman.vies_validator_service.exception.ViesServiceException;
import com.ulasakduman.vies_validator_service.service.impl.ViesClientServiceImpl;
import eu.europa.ec.taxud.vies.services.checkvat.types.CheckVat;
import eu.europa.ec.taxud.vies.services.checkvat.types.CheckVatResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ws.client.WebServiceIOException;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.soap.client.SoapFaultClientException;

import javax.xml.namespace.QName;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ViesClientServiceTest {

    @Mock
    private WebServiceTemplate webServiceTemplate;

    private ViesClientServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ViesClientServiceImpl(webServiceTemplate);
    }

    @Test
    void checkVat_success_returnsResponse() {
        CheckVatResponse mockResponse = new CheckVatResponse();
        mockResponse.setCountryCode("DE");
        mockResponse.setVatNumber("129273398");
        mockResponse.setValid(true);

        when(webServiceTemplate.marshalSendAndReceive(any())).thenReturn(mockResponse);

        CheckVatResponse result = service.checkVat("DE", "129273398");

        assertThat(result.isValid()).isTrue();
        assertThat(result.getCountryCode()).isEqualTo("DE");
    }

    @Test
    void checkVat_sendsCorrectRequest() {
        CheckVatResponse mockResponse = new CheckVatResponse();
        when(webServiceTemplate.marshalSendAndReceive(any())).thenReturn(mockResponse);

        service.checkVat("DE", "129273398");

        ArgumentCaptor<CheckVat> captor = ArgumentCaptor.forClass(CheckVat.class);
        verify(webServiceTemplate).marshalSendAndReceive(captor.capture());
        assertThat(captor.getValue().getCountryCode()).isEqualTo("DE");
        assertThat(captor.getValue().getVatNumber()).isEqualTo("129273398");
    }

    @Test
    void checkVat_soapFault_throwsViesServiceException() {
        SoapFaultClientException soapFault = mockSoapFault("INVALID_INPUT");
        when(webServiceTemplate.marshalSendAndReceive(any())).thenThrow(soapFault);

        assertThatThrownBy(() -> service.checkVat("XX", "123"))
                .isInstanceOf(ViesServiceException.class)
                .satisfies(ex -> {
                    ViesServiceException vex = (ViesServiceException) ex;
                    assertThat(vex.getFaultCode()).isEqualTo("INVALID_INPUT");
                });
    }

    @Test
    void checkVat_networkError_throwsWebServiceIOException() {
        when(webServiceTemplate.marshalSendAndReceive(any()))
                .thenThrow(new WebServiceIOException("Connection refused"));

        assertThatThrownBy(() -> service.checkVat("DE", "129273398"))
                .isInstanceOf(WebServiceIOException.class);
    }

    // --- helper ---

    private SoapFaultClientException mockSoapFault(String faultString) {
        SoapFaultClientException ex = mock(SoapFaultClientException.class);
        when(ex.getFaultStringOrReason()).thenReturn(faultString);
        return ex;
    }
}
