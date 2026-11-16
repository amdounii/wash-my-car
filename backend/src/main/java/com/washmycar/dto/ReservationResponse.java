package com.washmycar.dto;

import com.washmycar.entity.StatutReservation;
import com.washmycar.entity.TypeVehicule;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ReservationResponse(
        Long id,
        Long clientId,
        String clientNom,
        String clientEmail,
        String clientTelephone,
        Long stationId,
        String stationNom,
        String stationAdresse,
        TypeVehicule typeVehicule,
        String service,
        LocalDateTime dateDebut,
        LocalDateTime dateFin,
        StatutReservation statut,
        BigDecimal prix,
        LocalDateTime dateCreation,
        boolean evaluable
) {
}
