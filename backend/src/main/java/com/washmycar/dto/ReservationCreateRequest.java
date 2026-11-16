package com.washmycar.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;

import java.time.LocalDateTime;

/** Demande de réservation : la fin du créneau est calculée à partir de la durée du tarif. */
public record ReservationCreateRequest(
        @NotNull(message = "La station est obligatoire") Long stationId,
        @NotNull(message = "Le tarif (service) est obligatoire") Long tarifId,
        @NotNull(message = "La date de début est obligatoire")
        @Future(message = "Le créneau doit être dans le futur")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm[:ss]") LocalDateTime dateDebut
) {
}
