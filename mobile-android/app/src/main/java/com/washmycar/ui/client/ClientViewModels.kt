package com.washmycar.ui.client

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.washmycar.data.model.*
import com.washmycar.data.repository.ClientRepository
import com.washmycar.data.repository.StationRepository
import com.washmycar.util.Format
import com.washmycar.util.GeoPosition
import com.washmycar.util.LocationProvider
import com.washmycar.util.Slots
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

/** État générique d'un chargement. */
sealed interface Loadable<out T> {
    data object Loading : Loadable<Nothing>
    data class Error(val message: String) : Loadable<Nothing>
    data class Ready<T>(val data: T) : Loadable<T>
}

// ====================================================================== stations proches

@HiltViewModel
class NearbyViewModel @Inject constructor(
    private val stationRepository: StationRepository,
    private val locationProvider: LocationProvider,
) : ViewModel() {

    val rayons = listOf(5.0, 10.0, 25.0, 50.0, 100.0)

    var state by mutableStateOf<Loadable<List<StationSummary>>>(Loadable.Loading)
        private set
    var position by mutableStateOf<GeoPosition?>(null)
        private set

    /** Message affiché quand la position réelle n'est pas utilisée. */
    var positionInfo by mutableStateOf<String?>(null)
        private set
    var rayonKm by mutableStateOf(10.0)
        private set
    var showMap by mutableStateOf(false)

    val hasLocationPermission get() = locationProvider.hasPermission()

    /** Récupère la position (repli sur Tunis si indisponible ou hors de Tunisie) puis charge les stations. */
    fun locateAndLoad() {
        state = Loadable.Loading
        viewModelScope.launch {
            val p = locationProvider.current()
            position = when {
                p == null -> {
                    positionInfo = if (locationProvider.hasPermission()) "Position indisponible : recherche autour de Tunis"
                    else "Localisation refusée : recherche autour de Tunis"
                    LocationProvider.TUNIS
                }
                !LocationProvider.isInTunisia(p) -> {
                    positionInfo = "Vous êtes hors de Tunisie : recherche autour de Tunis"
                    LocationProvider.TUNIS
                }
                else -> {
                    positionInfo = null
                    p
                }
            }
            load()
        }
    }

    fun changeRayon(km: Double) {
        rayonKm = km
        load()
    }

    fun load() {
        val p = position ?: return locateAndLoad()
        state = Loadable.Loading
        viewModelScope.launch {
            state = stationRepository.nearby(p.latitude, p.longitude, rayonKm).fold(
                onSuccess = { Loadable.Ready(it) },
                onFailure = { Loadable.Error(it.message ?: "Erreur") },
            )
        }
    }
}

// ====================================================================== fiche station

data class StationPage(val detail: StationDetail, val evaluations: List<Evaluation>)

@HiltViewModel
class StationDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val stationRepository: StationRepository,
) : ViewModel() {

    private val stationId: Long = checkNotNull(savedStateHandle["stationId"])

    var state by mutableStateOf<Loadable<StationPage>>(Loadable.Loading)
        private set

    init {
        load()
    }

    fun load() {
        state = Loadable.Loading
        viewModelScope.launch {
            val detail = async { stationRepository.detail(stationId) }
            val evaluations = async { stationRepository.evaluations(stationId) }
            val d = detail.await()
            state = d.fold(
                onSuccess = { Loadable.Ready(StationPage(it, evaluations.await().getOrDefault(emptyList()))) },
                onFailure = { Loadable.Error(it.message ?: "Erreur") },
            )
        }
    }
}

// ====================================================================== réservation

@HiltViewModel
class BookingViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val stationRepository: StationRepository,
    private val clientRepository: ClientRepository,
) : ViewModel() {

    private val stationId: Long = checkNotNull(savedStateHandle["stationId"])

    /** Les 14 prochains jours proposés à la réservation. */
    val jours: List<LocalDate> = (0L until 14L).map { Format.today().plusDays(it) }

    var station by mutableStateOf<Loadable<StationDetail>>(Loadable.Loading)
        private set
    var typeVehicule by mutableStateOf<String?>(null)
        private set
    var tarif by mutableStateOf<Tarif?>(null)
        private set
    var date by mutableStateOf(Format.today())
        private set
    var slots by mutableStateOf<Loadable<List<LocalTime>>>(Loadable.Ready(emptyList()))
        private set
    var heure by mutableStateOf<LocalTime?>(null)
    var submitting by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var confirmation by mutableStateOf<Reservation?>(null)
        private set

    init {
        loadStation()
    }

    fun loadStation() {
        station = Loadable.Loading
        viewModelScope.launch {
            station = stationRepository.detail(stationId).fold(
                onSuccess = { d ->
                    typeVehicule = d.tarifs.firstOrNull()?.typeVehicule
                    // Premier jour ouvert par défaut.
                    date = jours.firstOrNull { horaireDu(d, it)?.ferme == false } ?: jours.first()
                    Loadable.Ready(d)
                },
                onFailure = { Loadable.Error(it.message ?: "Erreur") },
            )
        }
    }

    fun typesDisponibles(d: StationDetail) =
        Format.TYPES_VEHICULE.keys.filter { type -> d.tarifs.any { it.typeVehicule == type } }

    fun selectType(type: String) {
        typeVehicule = type
        tarif = null
        heure = null
        slots = Loadable.Ready(emptyList())
    }

    fun selectTarif(t: Tarif) {
        tarif = t
        refreshSlots()
    }

    fun selectDate(d: LocalDate) {
        date = d
        refreshSlots()
    }

    fun horaireDu(d: StationDetail, jour: LocalDate) = d.horaires.firstOrNull { it.jourSemaine == Format.jourOf(jour) }

    /** Recharge les créneaux occupés du jour puis calcule les créneaux libres. */
    private fun refreshSlots() {
        val d = (station as? Loadable.Ready)?.data ?: return
        val t = tarif ?: return
        heure = null
        slots = Loadable.Loading
        viewModelScope.launch {
            slots = stationRepository.occupied(stationId, date).fold(
                onSuccess = { occupes -> Loadable.Ready(Slots.compute(date, horaireDu(d, date), t.dureeMinutes, occupes)) },
                onFailure = { Loadable.Error(it.message ?: "Erreur") },
            )
        }
    }

    fun reserve() {
        val t = tarif ?: return
        val h = heure ?: return
        submitting = true
        error = null
        viewModelScope.launch {
            clientRepository.reserve(stationId, t.id, date.atTime(h))
                .onSuccess { confirmation = it }
                .onFailure {
                    error = it.message
                    refreshSlots() // le créneau vient peut-être d'être pris
                }
            submitting = false
        }
    }
}

// ====================================================================== mes réservations

@HiltViewModel
class MyReservationsViewModel @Inject constructor(private val clientRepository: ClientRepository) : ViewModel() {

    var state by mutableStateOf<Loadable<List<Reservation>>>(Loadable.Loading)
        private set
    var message by mutableStateOf<String?>(null)
    var busy by mutableStateOf(false)
        private set

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            if (state !is Loadable.Ready) state = Loadable.Loading
            state = clientRepository.myReservations().fold(
                onSuccess = { Loadable.Ready(it) },
                onFailure = { Loadable.Error(it.message ?: "Erreur") },
            )
        }
    }

    fun cancel(r: Reservation) = act {
        clientRepository.cancel(r.id).map { "Réservation annulée" }
    }

    fun evaluate(stationId: Long, note: Int, commentaire: String) = act {
        clientRepository.evaluate(stationId, note, commentaire).map { "Merci pour votre avis !" }
    }

    private fun act(block: suspend () -> Result<String>) {
        busy = true
        viewModelScope.launch {
            message = block().fold(onSuccess = { it }, onFailure = { it.message })
            busy = false
            load()
        }
    }
}
