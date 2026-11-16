package com.washmycar.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.washmycar.entity.JourSemaine;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

/** Horaire d'un jour (heures au format HH:mm). Si ferme = true, les heures sont ignorées. */
public record HoraireDto(
        @NotNull(message = "Le jour est obligatoire") JourSemaine jourSemaine,
        @JsonFormat(pattern = "HH:mm") LocalTime heureOuverture,
        @JsonFormat(pattern = "HH:mm") LocalTime heureFermeture,
        boolean ferme
) {
}
