package com.washmycar.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Mise à jour des horaires de la semaine (un élément par jour, 7 au maximum). */
public record HorairesRequest(
        @NotEmpty(message = "Au moins un jour doit être renseigné")
        @Size(max = 7) List<@Valid HoraireDto> horaires
) {
}
