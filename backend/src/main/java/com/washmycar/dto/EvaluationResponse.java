package com.washmycar.dto;

import java.time.LocalDateTime;

public record EvaluationResponse(
        Long id,
        Long clientId,
        String clientNom,
        Long stationId,
        String stationNom,
        Integer note,
        String commentaire,
        LocalDateTime dateCreation
) {
}
