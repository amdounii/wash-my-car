package com.washmycar.service;

import com.washmycar.dto.PageResponse;
import com.washmycar.dto.ReservationCreateRequest;
import com.washmycar.dto.ReservationResponse;
import com.washmycar.entity.*;
import com.washmycar.exception.ApiException;
import com.washmycar.mapper.DtoMapper;
import com.washmycar.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static com.washmycar.service.AuthService.blankToNull;

/**
 * Réservations de créneaux.
 * Règles : créneau dans les horaires d'ouverture, sans chevauchement avec une
 * réservation en attente ou acceptée de la même station.
 */
@Service
@RequiredArgsConstructor
public class ReservationService {

    private static final Set<StatutReservation> STATUTS_BLOQUANTS =
            EnumSet.of(StatutReservation.EN_ATTENTE, StatutReservation.ACCEPTEE);

    private final ReservationRepository reservationRepository;
    private final StationRepository stationRepository;
    private final TarifRepository tarifRepository;
    private final HoraireRepository horaireRepository;
    private final UserRepository userRepository;

    // ================================================================== client

    @Transactional
    public ReservationResponse create(Long clientId, ReservationCreateRequest req) {
        // Verrou sur la station : deux demandes simultanées ne peuvent pas obtenir le même créneau.
        Station station = stationRepository.findByIdForUpdate(req.stationId())
                .filter(s -> s.getStatutInscription() == StatutInscription.VALIDEE)
                .orElseThrow(() -> ApiException.notFound("Station introuvable"));
        Tarif tarif = tarifRepository.findByIdAndStationId(req.tarifId(), station.getId())
                .orElseThrow(() -> ApiException.badRequest("Ce service n'est pas proposé par cette station"));

        LocalDateTime debut = req.dateDebut().truncatedTo(ChronoUnit.MINUTES);
        LocalDateTime fin = debut.plusMinutes(tarif.getDureeMinutes());
        if (!debut.isAfter(LocalDateTime.now())) {
            throw ApiException.badRequest("Le créneau doit être dans le futur");
        }
        verifierHoraires(station, debut, fin);
        verifierDisponibilite(station.getId(), debut, fin, null);

        Reservation r = reservationRepository.save(Reservation.builder()
                .client(userRepository.getReferenceById(clientId))
                .station(station)
                .typeVehicule(tarif.getTypeVehicule())
                .service(tarif.getService())
                .prix(tarif.getPrix())
                .dateDebut(debut)
                .dateFin(fin)
                .statut(StatutReservation.EN_ATTENTE)
                .build());
        return DtoMapper.toReservation(reservationRepository.findById(r.getId()).orElseThrow());
    }

    @Transactional(readOnly = true)
    public List<ReservationResponse> mesReservations(Long clientId) {
        return reservationRepository.findByClientIdOrderByDateDebutDesc(clientId).stream()
                .map(DtoMapper::toReservation).toList();
    }

    /** Le client peut annuler une réservation en attente ou acceptée, tant qu'elle n'a pas commencé. */
    @Transactional
    public ReservationResponse annulerParClient(Long clientId, Long reservationId) {
        Reservation r = reservationRepository.findById(reservationId)
                .filter(x -> x.getClient().getId().equals(clientId))
                .orElseThrow(() -> ApiException.notFound("Réservation introuvable"));
        if (!r.getStatut().bloqueCreneau()) {
            throw ApiException.badRequest("Seule une réservation en attente ou acceptée peut être annulée");
        }
        if (!r.getDateDebut().isAfter(LocalDateTime.now())) {
            throw ApiException.badRequest("Impossible d'annuler une réservation déjà commencée ou passée");
        }
        r.setStatut(StatutReservation.ANNULEE);
        return DtoMapper.toReservation(r);
    }

    // ================================================================== station

    @Transactional(readOnly = true)
    public List<ReservationResponse> reservationsStation(Long stationUserId, StatutReservation statut) {
        Station s = stationDuUser(stationUserId);
        return reservationRepository.findByStation(s.getId(), statut).stream()
                .map(DtoMapper::toReservation).toList();
    }

    /**
     * Transitions autorisées pour la station :
     * EN_ATTENTE -> ACCEPTEE | REFUSEE | ANNULEE, ACCEPTEE -> ANNULEE.
     */
    @Transactional
    public ReservationResponse changerStatutParStation(Long stationUserId, Long reservationId,
                                                       StatutReservation nouveau) {
        Station s = stationDuUser(stationUserId);
        Reservation r = reservationRepository.findById(reservationId)
                .filter(x -> x.getStation().getId().equals(s.getId()))
                .orElseThrow(() -> ApiException.notFound("Réservation introuvable"));

        boolean autorise = switch (r.getStatut()) {
            case EN_ATTENTE -> nouveau != StatutReservation.EN_ATTENTE;
            case ACCEPTEE -> nouveau == StatutReservation.ANNULEE;
            default -> false;
        };
        if (!autorise) {
            throw ApiException.badRequest("Transition impossible : " + r.getStatut() + " -> " + nouveau);
        }
        r.setStatut(nouveau);
        return DtoMapper.toReservation(r);
    }

    // ================================================================== administration

    @Transactional(readOnly = true)
    public PageResponse<ReservationResponse> search(StatutReservation statut, Long stationId, String q,
                                                    int page, int size) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "dateDebut"));
        return PageResponse.of(reservationRepository.search(statut, stationId, blankToNull(q), pageable),
                DtoMapper::toReservation);
    }

    /**
     * L'administrateur peut forcer n'importe quel statut. S'il réactive une réservation
     * (refusée/annulée -> en attente/acceptée), on revérifie que le créneau est libre.
     */
    @Transactional
    public ReservationResponse changerStatutParAdmin(Long reservationId, StatutReservation nouveau) {
        Reservation r = reservationRepository.findById(reservationId)
                .orElseThrow(() -> ApiException.notFound("Réservation introuvable"));
        if (nouveau.bloqueCreneau() && !r.getStatut().bloqueCreneau()) {
            stationRepository.findByIdForUpdate(r.getStation().getId());
            verifierDisponibilite(r.getStation().getId(), r.getDateDebut(), r.getDateFin(), r.getId());
        }
        r.setStatut(nouveau);
        return DtoMapper.toReservation(r);
    }

    @Transactional
    public void deleteByAdmin(Long reservationId) {
        if (!reservationRepository.existsById(reservationId)) {
            throw ApiException.notFound("Réservation introuvable");
        }
        reservationRepository.deleteById(reservationId);
    }

    // ================================================================== règles métier

    /** Le créneau doit tenir entièrement dans les horaires du jour (pas de passage à minuit). */
    private void verifierHoraires(Station station, LocalDateTime debut, LocalDateTime fin) {
        if (!fin.toLocalDate().equals(debut.toLocalDate())) {
            throw ApiException.badRequest("Le créneau doit se terminer le même jour");
        }
        JourSemaine jour = JourSemaine.from(debut.getDayOfWeek());
        Horaire h = horaireRepository.findByStationIdAndJourSemaine(station.getId(), jour)
                .filter(x -> !x.isFerme() && x.getHeureOuverture() != null && x.getHeureFermeture() != null)
                .orElseThrow(() -> ApiException.badRequest("La station est fermée le " + jour.name().toLowerCase()));
        if (debut.toLocalTime().isBefore(h.getHeureOuverture()) || fin.toLocalTime().isAfter(h.getHeureFermeture())) {
            throw ApiException.badRequest("Le créneau doit être compris entre " + h.getHeureOuverture()
                    + " et " + h.getHeureFermeture() + " le " + jour.name().toLowerCase());
        }
    }

    private void verifierDisponibilite(Long stationId, LocalDateTime debut, LocalDateTime fin, Long exclureId) {
        if (reservationRepository.existsChevauchement(stationId, debut, fin, STATUTS_BLOQUANTS, exclureId)) {
            throw ApiException.conflict("Ce créneau est déjà réservé. Veuillez choisir un autre horaire.");
        }
    }

    private Station stationDuUser(Long userId) {
        return stationRepository.findByUserId(userId)
                .orElseThrow(() -> ApiException.notFound("Aucune station associée à ce compte"));
    }
}
