package com.washmycar.dto;

import com.washmycar.entity.StatutInscription;
import jakarta.validation.constraints.NotNull;

public record StatutInscriptionRequest(@NotNull(message = "Le statut est obligatoire") StatutInscription statut) {
}
