package com.washmycar.controller;

import com.washmycar.dto.*;
import com.washmycar.entity.StatutInscription;
import com.washmycar.entity.StatutReservation;
import com.washmycar.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * Back-office administrateur : stations, clients, réservations, évaluations, tableau de bord.
 * Toutes les listes sont paginées (page commence à 0).
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Administration")
public class AdminController {

    private final DashboardService dashboardService;
    private final StationService stationService;
    private final UserService userService;
    private final ReservationService reservationService;
    private final EvaluationService evaluationService;

    @GetMapping("/dashboard")
    @Operation(summary = "Indicateurs du tableau de bord")
    public DashboardResponse dashboard() {
        return dashboardService.get();
    }

    // ================================================================== stations

    @GetMapping("/stations")
    @Operation(summary = "Rechercher les stations (statut et texte optionnels)")
    public PageResponse<StationResponse> stations(@RequestParam(required = false) StatutInscription statut,
                                                  @RequestParam(required = false) String q,
                                                  @RequestParam(defaultValue = "0") @Min(0) int page,
                                                  @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {
        return stationService.search(statut, q, page, size);
    }

    @GetMapping("/stations/{id}")
    @Operation(summary = "Détail d'une station")
    public StationResponse station(@PathVariable Long id) {
        return stationService.getById(id);
    }

    @GetMapping("/stations/{id}/details")
    @Operation(summary = "Horaires et tarifs d'une station")
    public StationDetailResponse stationDetails(@PathVariable Long id) {
        return stationService.getDetailAdmin(id);
    }

    @PostMapping("/stations")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Créer une station et le compte de son gérant")
    public StationResponse createStation(@Valid @RequestBody AdminStationRequest req) {
        return stationService.createByAdmin(req);
    }

    @PutMapping("/stations/{id}")
    @Operation(summary = "Modifier les informations d'une station")
    public StationResponse updateStation(@PathVariable Long id, @Valid @RequestBody StationInfoRequest req) {
        return stationService.updateByAdmin(id, req);
    }

    @PatchMapping("/stations/{id}/statut")
    @Operation(summary = "Valider ou refuser l'inscription d'une station")
    public StationResponse changerStatutStation(@PathVariable Long id,
                                                @Valid @RequestBody StatutInscriptionRequest req) {
        return stationService.changerStatut(id, req.statut());
    }

    @DeleteMapping("/stations/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Supprimer une station (et son compte gérant)")
    public void deleteStation(@PathVariable Long id) {
        stationService.deleteByAdmin(id);
    }

    // ================================================================== clients

    @GetMapping("/clients")
    @Operation(summary = "Rechercher les clients")
    public PageResponse<UserResponse> clients(@RequestParam(required = false) String q,
                                              @RequestParam(defaultValue = "0") @Min(0) int page,
                                              @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {
        return userService.searchClients(q, page, size);
    }

    @GetMapping("/clients/{id}")
    @Operation(summary = "Détail d'un client")
    public UserResponse client(@PathVariable Long id) {
        return userService.getClient(id);
    }

    @PostMapping("/clients")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Créer un client")
    public UserResponse createClient(@Valid @RequestBody ClientRequest req) {
        return userService.createClient(req);
    }

    @PutMapping("/clients/{id}")
    @Operation(summary = "Modifier un client (mot de passe vide = inchangé)")
    public UserResponse updateClient(@PathVariable Long id, @Valid @RequestBody ClientRequest req) {
        return userService.updateClient(id, req);
    }

    @DeleteMapping("/clients/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Supprimer un client")
    public void deleteClient(@PathVariable Long id) {
        userService.deleteClient(id);
    }

    // ================================================================== réservations

    @GetMapping("/reservations")
    @Operation(summary = "Rechercher les réservations")
    public PageResponse<ReservationResponse> reservations(@RequestParam(required = false) StatutReservation statut,
                                                          @RequestParam(required = false) Long stationId,
                                                          @RequestParam(required = false) String q,
                                                          @RequestParam(defaultValue = "0") @Min(0) int page,
                                                          @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {
        return reservationService.search(statut, stationId, q, page, size);
    }

    @PatchMapping("/reservations/{id}/statut")
    @Operation(summary = "Changer le statut d'une réservation")
    public ReservationResponse changerStatutReservation(@PathVariable Long id,
                                                        @Valid @RequestBody StatutReservationRequest req) {
        return reservationService.changerStatutParAdmin(id, req.statut());
    }

    @DeleteMapping("/reservations/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Supprimer une réservation")
    public void deleteReservation(@PathVariable Long id) {
        reservationService.deleteByAdmin(id);
    }

    // ================================================================== évaluations

    @GetMapping("/evaluations")
    @Operation(summary = "Rechercher les évaluations (modération)")
    public PageResponse<EvaluationResponse> evaluations(@RequestParam(required = false) Long stationId,
                                                        @RequestParam(required = false) @Min(1) @Max(5) Integer note,
                                                        @RequestParam(required = false) String q,
                                                        @RequestParam(defaultValue = "0") @Min(0) int page,
                                                        @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {
        return evaluationService.search(stationId, note, q, page, size);
    }

    @DeleteMapping("/evaluations/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Supprimer une évaluation (la note moyenne est recalculée)")
    public void deleteEvaluation(@PathVariable Long id) {
        evaluationService.deleteByAdmin(id);
    }
}
