package com.washmycar.dto;

import jakarta.validation.constraints.*;

public record EvaluationRequest(
        @NotNull(message = "La station est obligatoire") Long stationId,
        @NotNull(message = "La note est obligatoire")
        @Min(value = 1, message = "La note minimale est 1")
        @Max(value = 5, message = "La note maximale est 5") Integer note,
        @Size(max = 1000, message = "Commentaire limité à 1000 caractères") String commentaire
) {
}
