package com.washmycar.dto;

import java.time.LocalDateTime;

/** Intervalle déjà réservé (aucune information personnelle exposée). */
public record CreneauOccupeResponse(LocalDateTime debut, LocalDateTime fin) {
}
