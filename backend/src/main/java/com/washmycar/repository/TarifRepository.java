package com.washmycar.repository;

import com.washmycar.entity.Tarif;
import com.washmycar.entity.TypeVehicule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TarifRepository extends JpaRepository<Tarif, Long> {

    List<Tarif> findByStationIdOrderByTypeVehiculeAscPrixAsc(Long stationId);

    Optional<Tarif> findByIdAndStationId(Long id, Long stationId);

    boolean existsByStationIdAndTypeVehiculeAndServiceIgnoreCase(Long stationId, TypeVehicule type, String service);

    boolean existsByStationIdAndTypeVehiculeAndServiceIgnoreCaseAndIdNot(Long stationId, TypeVehicule type,
                                                                          String service, Long id);
}
