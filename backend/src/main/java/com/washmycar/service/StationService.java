package com.washmycar.service;

import com.washmycar.dto.*;
import com.washmycar.entity.*;
import com.washmycar.exception.ApiException;
import com.washmycar.mapper.DtoMapper;
import com.washmycar.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.washmycar.service.AuthService.blankToNull;
import static com.washmycar.service.AuthService.normalizeEmail;

/**
 * Stations de lavage : recherche géographique, fiche publique,
 * gestion par la station (profil, horaires, tarifs) et par l'administrateur.
 */
@Service
@RequiredArgsConstructor
public class StationService {

    /** Rayon de la Terre utilisé pour convertir les km en degrés (pré-filtre). */
    private static final double KM_PAR_DEGRE = 111.045;
    private static final double RAYON_MAX_KM = 200;

    private final StationRepository stationRepository;
    private final UserRepository userRepository;
    private final TarifRepository tarifRepository;
    private final ReservationRepository reservationRepository;
    private final EvaluationRepository evaluationRepository;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;

    // ================================================================== recherche publique

    /** Stations validées dans le rayon demandé, de la plus proche à la plus éloignée. */
    @Transactional(readOnly = true)
    public List<StationSummaryResponse> findProches(double lat, double lng, double rayonKm, int limite) {
        if (lat < -90 || lat > 90 || lng < -180 || lng > 180) {
            throw ApiException.badRequest("Coordonnées GPS invalides");
        }
        double rayon = Math.min(Math.max(rayonKm, 0.1), RAYON_MAX_KM);
        double dLat = rayon / KM_PAR_DEGRE;
        double dLng = rayon / (KM_PAR_DEGRE * Math.max(Math.cos(Math.toRadians(lat)), 0.01));

        List<StationRepository.StationDistance> resultats = stationRepository.findProches(lat, lng, rayon,
                lat - dLat, lat + dLat, lng - dLng, lng + dLng, Math.min(Math.max(limite, 1), 100));

        Map<Long, Station> stations = stationRepository.findAllById(
                        resultats.stream().map(StationRepository.StationDistance::getId).toList())
                .stream().collect(Collectors.toMap(Station::getId, Function.identity()));

        LocalDateTime maintenant = LocalDateTime.now();
        return resultats.stream()
                .filter(r -> stations.containsKey(r.getId()))
                .map(r -> {
                    Station s = stations.get(r.getId());
                    double distance = Math.round(r.getDistance() * 100.0) / 100.0;
                    return DtoMapper.toSummary(s, distance, estOuverte(s.getHoraires(), maintenant));
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public StationDetailResponse getDetailPublic(Long id) {
        Station s = getStationValidee(id);
        return DtoMapper.toDetail(s, evaluationRepository.countByStationId(id),
                estOuverte(s.getHoraires(), LocalDateTime.now()));
    }

    /** Créneaux déjà pris un jour donné (disponibilité affichée au client). */
    @Transactional(readOnly = true)
    public List<CreneauOccupeResponse> getCreneauxOccupes(Long stationId, LocalDate date) {
        getStationValidee(stationId);
        return reservationRepository.findOccupees(stationId, date.atStartOfDay(), date.plusDays(1).atStartOfDay(),
                        List.of(StatutReservation.EN_ATTENTE, StatutReservation.ACCEPTEE))
                .stream()
                .map(r -> new CreneauOccupeResponse(r.getDateDebut(), r.getDateFin()))
                .toList();
    }

    // ================================================================== espace station

    @Transactional(readOnly = true)
    public StationResponse getMaStation(Long userId) {
        return DtoMapper.toStation(getStationDuUser(userId));
    }

    @Transactional
    public StationResponse updateMaStation(Long userId, StationInfoRequest req) {
        Station s = getStationDuUser(userId);
        appliquerInfos(s, req);
        return DtoMapper.toStation(s);
    }

    @Transactional(readOnly = true)
    public List<HoraireDto> getMesHoraires(Long userId) {
        return DtoMapper.toHoraires(getStationDuUser(userId).getHoraires());
    }

    /** Met à jour les jours fournis ; les jours non fournis restent inchangés. */
    @Transactional
    public List<HoraireDto> updateMesHoraires(Long userId, HorairesRequest req) {
        Station s = getStationDuUser(userId);
        Set<JourSemaine> vus = EnumSet.noneOf(JourSemaine.class);
        for (HoraireDto dto : req.horaires()) {
            if (!vus.add(dto.jourSemaine())) {
                throw ApiException.badRequest("Le jour " + dto.jourSemaine() + " est renseigné plusieurs fois");
            }
            if (!dto.ferme()) {
                if (dto.heureOuverture() == null || dto.heureFermeture() == null) {
                    throw ApiException.badRequest("Heures d'ouverture et de fermeture obligatoires pour " + dto.jourSemaine());
                }
                if (!dto.heureOuverture().isBefore(dto.heureFermeture())) {
                    throw ApiException.badRequest("L'heure d'ouverture doit précéder l'heure de fermeture ("
                            + dto.jourSemaine() + ")");
                }
            }
            Horaire h = s.getHoraires().stream()
                    .filter(x -> x.getJourSemaine() == dto.jourSemaine())
                    .findFirst()
                    .orElseGet(() -> {
                        Horaire nouveau = Horaire.builder().station(s).jourSemaine(dto.jourSemaine()).build();
                        s.getHoraires().add(nouveau);
                        return nouveau;
                    });
            h.setFerme(dto.ferme());
            h.setHeureOuverture(dto.ferme() ? null : dto.heureOuverture());
            h.setHeureFermeture(dto.ferme() ? null : dto.heureFermeture());
        }
        return DtoMapper.toHoraires(s.getHoraires());
    }

    @Transactional(readOnly = true)
    public List<TarifResponse> getMesTarifs(Long userId) {
        Station s = getStationDuUser(userId);
        return tarifRepository.findByStationIdOrderByTypeVehiculeAscPrixAsc(s.getId()).stream()
                .map(DtoMapper::toTarif).toList();
    }

    @Transactional
    public TarifResponse addTarif(Long userId, TarifRequest req) {
        Station s = getStationDuUser(userId);
        String service = req.service().trim();
        if (tarifRepository.existsByStationIdAndTypeVehiculeAndServiceIgnoreCase(s.getId(), req.typeVehicule(), service)) {
            throw ApiException.conflict("Ce service existe déjà pour ce type de véhicule");
        }
        Tarif t = tarifRepository.save(Tarif.builder()
                .station(s).typeVehicule(req.typeVehicule()).service(service)
                .prix(req.prix()).dureeMinutes(req.dureeMinutes()).build());
        return DtoMapper.toTarif(t);
    }

    @Transactional
    public TarifResponse updateTarif(Long userId, Long tarifId, TarifRequest req) {
        Station s = getStationDuUser(userId);
        Tarif t = tarifRepository.findByIdAndStationId(tarifId, s.getId())
                .orElseThrow(() -> ApiException.notFound("Tarif introuvable"));
        String service = req.service().trim();
        if (tarifRepository.existsByStationIdAndTypeVehiculeAndServiceIgnoreCaseAndIdNot(
                s.getId(), req.typeVehicule(), service, tarifId)) {
            throw ApiException.conflict("Ce service existe déjà pour ce type de véhicule");
        }
        t.setTypeVehicule(req.typeVehicule());
        t.setService(service);
        t.setPrix(req.prix());
        t.setDureeMinutes(req.dureeMinutes());
        return DtoMapper.toTarif(t);
    }

    /** Les réservations existantes conservent une copie du service et du prix. */
    @Transactional
    public void deleteTarif(Long userId, Long tarifId) {
        Station s = getStationDuUser(userId);
        Tarif t = tarifRepository.findByIdAndStationId(tarifId, s.getId())
                .orElseThrow(() -> ApiException.notFound("Tarif introuvable"));
        s.getTarifs().remove(t);
        tarifRepository.delete(t);
    }

    // ================================================================== administration

    @Transactional(readOnly = true)
    public PageResponse<StationResponse> search(StatutInscription statut, String q, int page, int size) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        return PageResponse.of(stationRepository.search(statut, blankToNull(q), pageable), DtoMapper::toStation);
    }

    @Transactional(readOnly = true)
    public StationResponse getById(Long id) {
        return DtoMapper.toStation(getStation(id));
    }

    /** Fiche détaillée (horaires, tarifs) quel que soit le statut, pour l'administrateur. */
    @Transactional(readOnly = true)
    public StationDetailResponse getDetailAdmin(Long id) {
        Station s = getStation(id);
        return DtoMapper.toDetail(s, evaluationRepository.countByStationId(id),
                estOuverte(s.getHoraires(), LocalDateTime.now()));
    }

    @Transactional
    public StationResponse createByAdmin(AdminStationRequest req) {
        String email = normalizeEmail(req.email());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw ApiException.conflict("Un compte existe déjà avec cet email");
        }
        User user = userRepository.save(User.builder()
                .nom(req.nom().trim())
                .prenom(req.prenom().trim())
                .email(email)
                .motDePasse(passwordEncoder.encode(req.motDePasse()))
                .telephone(blankToNull(req.telephone()))
                .role(Role.STATION)
                .emailVerifie(true)
                .build());
        StatutInscription statut = req.statutInscription() != null ? req.statutInscription() : StatutInscription.VALIDEE;
        return DtoMapper.toStation(creerStation(user, req.station(), statut));
    }

    @Transactional
    public StationResponse updateByAdmin(Long id, StationInfoRequest req) {
        Station s = getStation(id);
        appliquerInfos(s, req);
        return DtoMapper.toStation(s);
    }

    /** Validation / refus d'une inscription ; la station est prévenue par email. */
    @Transactional
    public StationResponse changerStatut(Long id, StatutInscription statut) {
        Station s = getStation(id);
        if (s.getStatutInscription() != statut) {
            s.setStatutInscription(statut);
            if (statut != StatutInscription.EN_ATTENTE) {
                mailService.sendStatutStation(s);
            }
        }
        return DtoMapper.toStation(s);
    }

    /** Supprime la station et le compte du gérant (réservations, horaires… en cascade). */
    @Transactional
    public void deleteByAdmin(Long id) {
        Station s = getStation(id);
        userRepository.hardDelete(s.getUser().getId());
    }

    // ================================================================== utilitaires

    /** Crée une station avec des horaires par défaut (lun.-sam. 08:00-18:00, dimanche fermé). */
    @Transactional
    public Station creerStation(User owner, StationInfoRequest info, StatutInscription statut) {
        Station s = Station.builder().user(owner).statutInscription(statut).build();
        appliquerInfos(s, info);
        for (JourSemaine jour : JourSemaine.values()) {
            boolean dimanche = jour == JourSemaine.DIMANCHE;
            s.getHoraires().add(Horaire.builder()
                    .station(s)
                    .jourSemaine(jour)
                    .ferme(dimanche)
                    .heureOuverture(dimanche ? null : LocalTime.of(8, 0))
                    .heureFermeture(dimanche ? null : LocalTime.of(18, 0))
                    .build());
        }
        return stationRepository.save(s);
    }

    private void appliquerInfos(Station s, StationInfoRequest info) {
        s.setNom(info.nom().trim());
        s.setAdresse(info.adresse().trim());
        s.setVille(info.ville().trim());
        s.setLatitude(info.latitude());
        s.setLongitude(info.longitude());
        s.setTelephone(blankToNull(info.telephone()));
        s.setDescription(blankToNull(info.description()));
        s.setPhotoUrl(blankToNull(info.photoUrl()));
    }

    /** Indique si la station est ouverte à l'instant donné d'après ses horaires. */
    static boolean estOuverte(List<Horaire> horaires, LocalDateTime instant) {
        JourSemaine jour = JourSemaine.from(instant.getDayOfWeek());
        LocalTime heure = instant.toLocalTime();
        return horaires.stream()
                .filter(h -> h.getJourSemaine() == jour && !h.isFerme())
                .anyMatch(h -> h.getHeureOuverture() != null && h.getHeureFermeture() != null
                        && !heure.isBefore(h.getHeureOuverture()) && heure.isBefore(h.getHeureFermeture()));
    }

    Station getStation(Long id) {
        return stationRepository.findById(id).orElseThrow(() -> ApiException.notFound("Station introuvable"));
    }

    /** Une station non validée est invisible pour le public : on répond 404. */
    Station getStationValidee(Long id) {
        return stationRepository.findById(id)
                .filter(s -> s.getStatutInscription() == StatutInscription.VALIDEE)
                .orElseThrow(() -> ApiException.notFound("Station introuvable"));
    }

    Station getStationDuUser(Long userId) {
        return stationRepository.findByUserId(userId)
                .orElseThrow(() -> ApiException.notFound("Aucune station associée à ce compte"));
    }
}
