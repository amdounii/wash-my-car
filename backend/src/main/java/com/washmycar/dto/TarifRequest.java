package com.washmycar.dto;

import com.washmycar.entity.TypeVehicule;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record TarifRequest(
        @NotNull(message = "Le type de véhicule est obligatoire") TypeVehicule typeVehicule,
        @NotBlank(message = "Le service est obligatoire") @Size(max = 100) String service,
        @NotNull(message = "Le prix est obligatoire")
        @DecimalMin(value = "0.0", message = "Le prix doit être positif")
        @Digits(integer = 5, fraction = 3, message = "Prix invalide (3 décimales maximum)") BigDecimal prix,
        @NotNull(message = "La durée est obligatoire")
        @Min(value = 5, message = "Durée minimale : 5 minutes")
        @Max(value = 480, message = "Durée maximale : 480 minutes") Integer dureeMinutes
) {
}
