package com.ulasakduman.vies_validator_service.service;

import eu.europa.ec.taxud.vies.services.checkvat.types.CheckVatResponse;

public interface ViesClientService {

    CheckVatResponse checkVat(String countryCode, String vatNumber);
}