package com.ulasakduman.vies_validator_service.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.core.io.ClassPathResource;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.transport.http.HttpUrlConnectionMessageSender;

import java.time.Duration;

// VIES servisi için marshaller ve soap servis için template beanlerini tanımlar
@EnableRetry
@Configuration
public class WebServiceConfig {

    private static final Logger log = LoggerFactory.getLogger(WebServiceConfig.class);

    @Value("${vies.endpoint.url}")
    private String endpointUrl;

    @Value("${vies.connection-timeout:5000}")
    private int connectionTimeoutMs;

    @Value("${vies.read-timeout:10000}")
    private int readTimeoutMs;

    // XSD şemasına göre CheckVat tiplerini unmarshal eden marshaller
    @Bean
    public Jaxb2Marshaller viesMarshaller() {
        Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
        // şema ve template set edilir.
        marshaller.setPackagesToScan( "eu.europa.ec.taxud.vies.services.checkvat.types");
        marshaller.setSchemas(new ClassPathResource("wsdl/checkVatTypes.xsd"));
        marshaller.setValidationEventHandler(event -> {
            //log.warn("JAXB doğrulama olayı [önem={}]: {}", event.getSeverity(), event.getMessage());
            return true;
        });
        return marshaller;
    }

    // Timeout'ları ayarlanmış http sender ile vies'e istek atan template
    @Bean
    public WebServiceTemplate webServiceTemplate(Jaxb2Marshaller viesMarshaller) {
        HttpUrlConnectionMessageSender sender = new HttpUrlConnectionMessageSender();
        sender.setConnectionTimeout(Duration.ofMillis(connectionTimeoutMs));
        sender.setReadTimeout(Duration.ofMillis(readTimeoutMs));
        WebServiceTemplate template = new WebServiceTemplate();
        template.setMarshaller(viesMarshaller);
        template.setUnmarshaller(viesMarshaller);
        template.setDefaultUri(endpointUrl);
        template.setMessageSender(sender);
        return template;
    }
}