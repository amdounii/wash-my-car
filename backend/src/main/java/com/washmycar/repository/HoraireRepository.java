package com.washmycar.repository;

import com.washmycar.entity.Horaire;
import com.washmycar.entity.JourSemaine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface HoraireRepository extends JpaRepository<Horaire, Long> {

    List<Horaire> findByStationId(Long stationId);

    Optional<Horaire> findByStationIdAndJourSemaine(Long stationId, JourSemaine jour);
}
