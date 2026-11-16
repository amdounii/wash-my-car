package com.washmycar.ui.station

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.washmycar.data.model.*
import com.washmycar.data.remote.ApiException
import com.washmycar.data.repository.StationSpaceRepository
import com.washmycar.ui.client.Loadable
import com.washmycar.util.Format
import com.washmycar.util.LocationProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

private fun <T> Result<T>.toLoadable(): Loadable<T> =
    fold(onSuccess = { Loadable.Ready(it) }, onFailure = { Loadable.Error(it.message ?: "Erreur") })

// ====================================================================== réservations reçues

@HiltViewModel
class StationReservationsViewModel @Inject constructor(private val repo: StationSpaceRepository) : ViewModel() {

    /** null = toutes les réservations. */
    var filtre by mutableStateOf<String?>("EN_ATTENTE")
        private set
    var state by mutableStateOf<Loadable<List<Reservation>>>(Loadable.Loading)
        private set
    var busyId by mutableStateOf<Long?>(null)
        private set
    var message by mutableStateOf<String?>(null)

    init {
        load()
    }

    fun selectFiltre(statut: String?) {
        filtre = statut
        load()
    }

    fun load() {
        state = Loadable.Loading
        viewModelScope.launch { state = repo.reservations(filtre).toLoadable() }
    }

    /** Accepter, refuser ou annuler (transitions vérifiées par le backend). */
    fun changeStatut(r: Reservation, statut: String) {
        busyId = r.id
        viewModelScope.launch {
            message = repo.changeStatut(r.id, statut).fold(
                onSuccess = { "Réservation n°${it.id} : ${Format.STATUT_RESERVATION[it.statut]?.lowercase()}" },
                onFailure = { it.message },
            )
            busyId = null
            load()
        }
    }
}

// ====================================================================== horaires

@HiltViewModel
class HorairesViewModel @Inject constructor(private val repo: StationSpaceRepository) : ViewModel() {

    var state by mutableStateOf<Loadable<Unit>>(Loadable.Loading)
        private set

    /** Les 7 jours, dans l'ordre, modifiables localement avant enregistrement. */
    var horaires by mutableStateOf<List<Horaire>>(emptyList())
        private set
    var saving by mutableStateOf(false)
        private set
    var message by mutableStateOf<String?>(null)

    init {
        load()
    }

    fun load() {
        state = Loadable.Loading
        viewModelScope.launch {
            repo.horaires()
                .onSuccess { list ->
                    horaires = Format.JOURS.keys.map { jour ->
                        list.firstOrNull { it.jourSemaine == jour } ?: Horaire(jour, "08:00", "18:00", false)
                    }
                    state = Loadable.Ready(Unit)
                }
                .onFailure { state = Loadable.Error(it.message ?: "Erreur") }
        }
    }

    fun update(jour: String, transform: (Horaire) -> Horaire) {
        horaires = horaires.map { if (it.jourSemaine == jour) transform(it) else it }
    }

    fun toggleOuvert(jour: String, ouvert: Boolean) = update(jour) { h ->
        h.copy(
            ferme = !ouvert,
            heureOuverture = h.heureOuverture ?: "08:00",
            heureFermeture = h.heureFermeture ?: "18:00",
        )
    }

    fun save() {
        val invalide = horaires.firstOrNull { !it.ferme && Format.hour(it.heureOuverture) >= Format.hour(it.heureFermeture) }
        if (invalide != null) {
            message = "${Format.JOURS[invalide.jourSemaine]} : l'ouverture doit précéder la fermeture"
            return
        }
        saving = true
        viewModelScope.launch {
            val payload = horaires.map {
                if (it.ferme) it.copy(heureOuverture = null, heureFermeture = null)
                else it.copy(heureOuverture = Format.hour(it.heureOuverture), heureFermeture = Format.hour(it.heureFermeture))
            }
            message = repo.updateHoraires(payload).fold(
                onSuccess = { "Horaires enregistrés" },
                onFailure = { it.message },
            )
            saving = false
        }
    }
}

// ====================================================================== tarifs

@HiltViewModel
class TarifsViewModel @Inject constructor(private val repo: StationSpaceRepository) : ViewModel() {

    var state by mutableStateOf<Loadable<List<Tarif>>>(Loadable.Loading)
        private set
    var saving by mutableStateOf(false)
        private set
    var formError by mutableStateOf<String?>(null)
        private set
    var message by mutableStateOf<String?>(null)

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            if (state !is Loadable.Ready) state = Loadable.Loading
            state = repo.tarifs().toLoadable()
        }
    }

    fun clearFormError() {
        formError = null
    }

    /** Création (id null) ou modification ; [onDone] ferme le dialogue en cas de succès. */
    fun save(id: Long?, req: TarifRequest, onDone: () -> Unit) {
        saving = true
        formError = null
        viewModelScope.launch {
            val result = if (id == null) repo.addTarif(req) else repo.updateTarif(id, req)
            result.onSuccess {
                message = if (id == null) "Tarif ajouté" else "Tarif modifié"
                onDone()
                load()
            }.onFailure { e ->
                formError = (e as? ApiException)?.fieldErrors?.values?.firstOrNull() ?: e.message
            }
            saving = false
        }
    }

    fun delete(t: Tarif) {
        viewModelScope.launch {
            message = repo.deleteTarif(t.id).fold(onSuccess = { "Tarif supprimé" }, onFailure = { it.message })
            load()
        }
    }
}

// ====================================================================== avis

@HiltViewModel
class StationEvaluationsViewModel @Inject constructor(private val repo: StationSpaceRepository) : ViewModel() {

    var state by mutableStateOf<Loadable<List<Evaluation>>>(Loadable.Loading)
        private set

    init {
        load()
    }

    fun load() {
        state = Loadable.Loading
        viewModelScope.launch { state = repo.evaluations().toLoadable() }
    }
}

// ====================================================================== fiche de la station

@HiltViewModel
class StationInfoViewModel @Inject constructor(
    private val repo: StationSpaceRepository,
    private val locationProvider: LocationProvider,
) : ViewModel() {

    var state by mutableStateOf<Loadable<StationProfile>>(Loadable.Loading)
        private set

    var nom by mutableStateOf("")
    var adresse by mutableStateOf("")
    var ville by mutableStateOf("")
    var telephone by mutableStateOf("")
    var description by mutableStateOf("")
    var photoUrl by mutableStateOf("")
    var latitude by mutableStateOf<Double?>(null)
    var longitude by mutableStateOf<Double?>(null)

    var errors by mutableStateOf<Map<String, String>>(emptyMap())
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var saving by mutableStateOf(false)
        private set
    var locating by mutableStateOf(false)
        private set
    var message by mutableStateOf<String?>(null)

    init {
        load()
    }

    fun load() {
        state = Loadable.Loading
        viewModelScope.launch {
            state = repo.profile().onSuccess(::fill).toLoadable()
        }
    }

    private fun fill(p: StationProfile) {
        nom = p.nom
        adresse = p.adresse
        ville = p.ville
        telephone = p.telephone.orEmpty()
        description = p.description.orEmpty()
        photoUrl = p.photoUrl.orEmpty()
        latitude = p.latitude
        longitude = p.longitude
    }

    fun setPosition(lat: Double, lng: Double) {
        latitude = lat
        longitude = lng
    }

    fun useMyLocation() {
        locating = true
        viewModelScope.launch {
            locationProvider.current()?.let { setPosition(it.latitude, it.longitude) }
                ?: run { error = "Position indisponible : touchez la carte." }
            locating = false
        }
    }

    fun save() {
        val lat = latitude
        val lng = longitude
        val local = buildMap {
            if (nom.isBlank()) put("nom", "Champ obligatoire")
            if (adresse.isBlank()) put("adresse", "Champ obligatoire")
            if (ville.isBlank()) put("ville", "Champ obligatoire")
            if (lat == null || lng == null) put("position", "Indiquez l'emplacement")
        }
        errors = local
        if (local.isNotEmpty() || lat == null || lng == null) return

        saving = true
        error = null
        viewModelScope.launch {
            repo.updateProfile(
                StationInfo(
                    nom = nom.trim(),
                    adresse = adresse.trim(),
                    ville = ville.trim(),
                    latitude = lat,
                    longitude = lng,
                    telephone = telephone.trim().ifBlank { null },
                    description = description.trim().ifBlank { null },
                    photoUrl = photoUrl.trim().ifBlank { null },
                )
            ).onSuccess {
                state = Loadable.Ready(it)
                message = "Informations de la station enregistrées"
            }.onFailure { e ->
                error = e.message
                errors = (e as? ApiException)?.fieldErrors.orEmpty()
            }
            saving = false
        }
    }
}
