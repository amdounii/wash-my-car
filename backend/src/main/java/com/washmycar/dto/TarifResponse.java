package com.washmycar.dto;

import com.washmycar.entity.TypeVehicule;

import java.math.BigDecimal;

public record TarifResponse(Long id, TypeVehicule typeVehicule, String service, BigDecimal prix, Integer dureeMinutes) {
}
