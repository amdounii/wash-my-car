package com.washmycar.controller;

import com.washmycar.dto.*;
import com.washmycar.entity.StatutReservation;
import com.washmycar.security.AuthUser;
import com.washmycar.service.EvaluationService;
import com.washmycar.service.ReservationService;
import com.washmycar.service.StationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Espace de la station connectée : profil, horaires, tarifs, réservations reçues, avis. */
@RestController
@RequestMapping("/api/station")
@RequiredArgsConstructor
@PreAuthorize("hasRole('STATION')")
@Tag(name = "Station", description = "Gestion de la station connectée")
public class StationSpaceController {

    private final StationService stationService;
    private final ReservationService reservationService;
    private final EvaluationService evaluationService;

    // ------------------------------------------------------------------ profil

    @GetMapping("/profil")
    @Operation(summary = "Informations de ma station")
    public StationResponse profil(@AuthenticationPrincipal AuthUser me) {
        return stationService.getMaStation(me.id());
    }

    @PutMapping("/profil")
    @Operation(summary = "Mettre à jour les informations de ma station")
    public StationResponse updateProfil(@AuthenticationPrincipal AuthUser me,
                                        @Valid @RequestBody StationInfoRequest req) {
        return stationService.updateMaStation(me.id(), req);
    }

    // ------------------------------------------------------------------ horaires

    @GetMapping("/horaires")
    @Operation(summary = "Mes horaires de la semaine")
    public List<HoraireDto> horaires(@AuthenticationPrincipal AuthUser me) {
        return stationService.getMesHoraires(me.id());
    }

    @PutMapping("/horaires")
    @Operation(summary = "Mettre à jour mes horaires (jours fournis uniquement)")
    public List<HoraireDto> updateHoraires(@AuthenticationPrincipal AuthUser me,
                                           @Valid @RequestBody HorairesRequest req) {
        return stationService.updateMesHoraires(me.id(), req);
    }

    // ------------------------------------------------------------------ tarifs

    @GetMapping("/tarifs")
    @Operation(summary = "Mes tarifs")
    public List<TarifResponse> tarifs(@AuthenticationPrincipal AuthUser me) {
        return stationService.getMesTarifs(me.id());
    }

    @PostMapping("/tarifs")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Ajouter un tarif")
    public TarifResponse addTarif(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody TarifRequest req) {
        return stationService.addTarif(me.id(), req);
    }

    @PutMapping("/tarifs/{id}")
    @Operation(summary = "Modifier un tarif")
    public TarifResponse updateTarif(@AuthenticationPrincipal AuthUser me, @PathVariable Long id,
                                     @Valid @RequestBody TarifRequest req) {
        return stationService.updateTarif(me.id(), id, req);
    }

    @DeleteMapping("/tarifs/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Supprimer un tarif")
    public void deleteTarif(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) {
        stationService.deleteTarif(me.id(), id);
    }

    // ------------------------------------------------------------------ réservations

    @GetMapping("/reservations")
    @Operation(summary = "Réservations reçues (filtre optionnel par statut)")
    public List<ReservationResponse> reservations(@AuthenticationPrincipal AuthUser me,
                                                  @RequestParam(required = false) StatutReservation statut) {
        return reservationService.reservationsStation(me.id(), statut);
    }

    @PatchMapping("/reservations/{id}/statut")
    @Operation(summary = "Accepter, refuser ou annuler une réservation")
    public ReservationResponse changerStatut(@AuthenticationPrincipal AuthUser me, @PathVariable Long id,
                                             @Valid @RequestBody StatutReservationRequest req) {
        return reservationService.changerStatutParStation(me.id(), id, req.statut());
    }

    // ------------------------------------------------------------------ évaluations

    @GetMapping("/evaluations")
    @Operation(summary = "Évaluations reçues")
    public List<EvaluationResponse> evaluations(@AuthenticationPrincipal AuthUser me) {
        return evaluationService.parStationDuUser(me.id());
    }
}
