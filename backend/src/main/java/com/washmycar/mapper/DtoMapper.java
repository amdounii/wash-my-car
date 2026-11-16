package com.washmycar.mapper;

import com.washmycar.dto.*;
import com.washmycar.entity.*;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

/** Conversion manuelle Entité -> DTO (aucune entité JPA n'est exposée par l'API). */
public final class DtoMapper {

    private DtoMapper() {
    }

    public static UserResponse toUser(User u) {
        return new UserResponse(u.getId(), u.getNom(), u.getPrenom(), u.getEmail(), u.getTelephone(),
                u.getRole(), u.isEmailVerifie(), u.getDateCreation());
    }

    public static StationResponse toStation(Station s) {
        User u = s.getUser();
        return new StationResponse(s.getId(), s.getNom(), s.getAdresse(), s.getVille(), s.getLatitude(),
                s.getLongitude(), s.getTelephone(), s.getDescription(), s.getPhotoUrl(),
                s.getStatutInscription(), s.getNoteMoyenne(),
                new ProprietaireResponse(u.getId(), u.getNom(), u.getPrenom(), u.getEmail(), u.getTelephone()));
    }

    public static HoraireDto toHoraire(Horaire h) {
        return new HoraireDto(h.getJourSemaine(), h.getHeureOuverture(), h.getHeureFermeture(), h.isFerme());
    }

    /** Horaires triés du lundi au dimanche. */
    public static List<HoraireDto> toHoraires(List<Horaire> horaires) {
        return horaires.stream()
                .sorted(Comparator.comparing(Horaire::getJourSemaine))
                .map(DtoMapper::toHoraire)
                .toList();
    }

    public static TarifResponse toTarif(Tarif t) {
        return new TarifResponse(t.getId(), t.getTypeVehicule(), t.getService(), t.getPrix(), t.getDureeMinutes());
    }

    /** Tarifs triés par type de véhicule puis par prix. */
    public static List<TarifResponse> toTarifs(List<Tarif> tarifs) {
        return tarifs.stream()
                .sorted(Comparator.comparing(Tarif::getTypeVehicule).thenComparing(Tarif::getPrix))
                .map(DtoMapper::toTarif)
                .toList();
    }

    public static StationSummaryResponse toSummary(Station s, Double distanceKm, boolean ouverte) {
        BigDecimal prixMin = s.getTarifs().stream().map(Tarif::getPrix).min(BigDecimal::compareTo).orElse(null);
        return new StationSummaryResponse(s.getId(), s.getNom(), s.getAdresse(), s.getVille(), s.getLatitude(),
                s.getLongitude(), s.getPhotoUrl(), s.getNoteMoyenne(), distanceKm, ouverte, prixMin);
    }

    public static StationDetailResponse toDetail(Station s, long nbEvaluations, boolean ouverte) {
        return new StationDetailResponse(s.getId(), s.getNom(), s.getAdresse(), s.getVille(), s.getLatitude(),
                s.getLongitude(), s.getTelephone(), s.getDescription(), s.getPhotoUrl(), s.getNoteMoyenne(),
                nbEvaluations, ouverte, toHoraires(s.getHoraires()), toTarifs(s.getTarifs()));
    }

    public static ReservationResponse toReservation(Reservation r) {
        User c = r.getClient();
        Station s = r.getStation();
        return new ReservationResponse(r.getId(), c.getId(), c.getPrenom() + " " + c.getNom(), c.getEmail(),
                c.getTelephone(), s.getId(), s.getNom(), s.getAdresse() + ", " + s.getVille(),
                r.getTypeVehicule(), r.getService(), r.getDateDebut(), r.getDateFin(), r.getStatut(), r.getPrix(),
                r.getDateCreation(), r.getStatut() == StatutReservation.ACCEPTEE);
    }

    public static EvaluationResponse toEvaluation(Evaluation e) {
        User c = e.getClient();
        return new EvaluationResponse(e.getId(), c.getId(), c.getPrenom() + " " + c.getNom().charAt(0) + ".",
                e.getStation().getId(), e.getStation().getNom(), e.getNote(), e.getCommentaire(),
                e.getDateCreation());
    }
}
