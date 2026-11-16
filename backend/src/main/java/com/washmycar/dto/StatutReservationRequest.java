package com.washmycar.dto;

import com.washmycar.entity.StatutReservation;
import jakarta.validation.constraints.NotNull;

public record StatutReservationRequest(@NotNull(message = "Le statut est obligatoire") StatutReservation statut) {
}
