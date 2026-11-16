package com.washmycar.ui.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.washmycar.data.model.RegisterClientRequest
import com.washmycar.data.model.RegisterStationRequest
import com.washmycar.data.model.StationInfo
import com.washmycar.data.remote.ApiException
import com.washmycar.data.repository.AuthRepository
import com.washmycar.util.LocationProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

private val EMAIL_REGEX = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")
private val PHONE_REGEX = Regex("^(\\+216)?[2-9]\\d{7}$")

/** Vérifications communes aux formulaires (le backend revalide tout). */
internal object Validators {
    fun required(v: String) = if (v.isBlank()) "Champ obligatoire" else null
    fun email(v: String) = required(v) ?: if (!EMAIL_REGEX.matches(v.trim())) "Email invalide" else null
    fun password(v: String) = if (v.length < 8) "8 caractères minimum" else null
    fun phone(v: String) = if (v.isNotBlank() && !PHONE_REGEX.matches(v.trim())) "Numéro tunisien à 8 chiffres" else null
}

// ====================================================================== connexion

@HiltViewModel
class LoginViewModel @Inject constructor(private val authRepository: AuthRepository) : ViewModel() {

    var email by mutableStateOf("")
    var motDePasse by mutableStateOf("")
    var loading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var info by mutableStateOf<String?>(null)
        private set

    /** Vrai lorsque la connexion est refusée faute de validation de l'email. */
    var canResendVerification by mutableStateOf(false)
        private set

    fun login() {
        if (email.isBlank() || motDePasse.isBlank()) {
            error = "Saisissez votre email et votre mot de passe"
            return
        }
        loading = true
        error = null
        info = null
        viewModelScope.launch {
            // En cas de succès, la session est enregistrée et la navigation bascule automatiquement.
            authRepository.login(email, motDePasse).onFailure { e ->
                error = e.message
                canResendVerification = (e as? ApiException)?.code == 403 && e.message.orEmpty().contains("email")
            }
            loading = false
        }
    }

    fun resendVerification() {
        viewModelScope.launch {
            authRepository.resendVerification(email)
                .onSuccess { info = it.message; error = null; canResendVerification = false }
                .onFailure { error = it.message }
        }
    }
}

// ====================================================================== inscription client

@HiltViewModel
class RegisterClientViewModel @Inject constructor(private val authRepository: AuthRepository) : ViewModel() {

    var nom by mutableStateOf("")
    var prenom by mutableStateOf("")
    var email by mutableStateOf("")
    var telephone by mutableStateOf("")
    var motDePasse by mutableStateOf("")
    var confirmation by mutableStateOf("")

    var errors by mutableStateOf<Map<String, String>>(emptyMap())
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var loading by mutableStateOf(false)
        private set
    var successMessage by mutableStateOf<String?>(null)
        private set

    fun submit() {
        val local = buildMap {
            Validators.required(nom)?.let { put("nom", it) }
            Validators.required(prenom)?.let { put("prenom", it) }
            Validators.email(email)?.let { put("email", it) }
            Validators.phone(telephone)?.let { put("telephone", it) }
            Validators.password(motDePasse)?.let { put("motDePasse", it) }
            if (confirmation != motDePasse) put("confirmation", "Les mots de passe ne correspondent pas")
        }
        errors = local
        if (local.isNotEmpty()) return

        loading = true
        error = null
        viewModelScope.launch {
            authRepository.registerClient(
                RegisterClientRequest(nom.trim(), prenom.trim(), email.trim(), motDePasse, telephone.trim().ifBlank { null })
            ).onSuccess { successMessage = it.message }
                .onFailure { e ->
                    error = e.message
                    errors = (e as? ApiException)?.fieldErrors.orEmpty()
                }
            loading = false
        }
    }
}

// ====================================================================== inscription station

@HiltViewModel
class RegisterStationViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val locationProvider: LocationProvider,
) : ViewModel() {

    // Gérant
    var nom by mutableStateOf("")
    var prenom by mutableStateOf("")
    var email by mutableStateOf("")
    var telephone by mutableStateOf("")
    var motDePasse by mutableStateOf("")
    var confirmation by mutableStateOf("")

    // Station
    var stationNom by mutableStateOf("")
    var adresse by mutableStateOf("")
    var ville by mutableStateOf("")
    var stationTelephone by mutableStateOf("")
    var description by mutableStateOf("")
    var latitude by mutableStateOf<Double?>(null)
    var longitude by mutableStateOf<Double?>(null)

    var errors by mutableStateOf<Map<String, String>>(emptyMap())
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var loading by mutableStateOf(false)
        private set
    var locating by mutableStateOf(false)
        private set
    var successMessage by mutableStateOf<String?>(null)
        private set

    fun setPosition(lat: Double, lng: Double) {
        latitude = lat
        longitude = lng
    }

    /** Place la station à la position actuelle du téléphone (permission demandée par l'écran). */
    fun useMyLocation() {
        locating = true
        viewModelScope.launch {
            val p = locationProvider.current()
            if (p != null) setPosition(p.latitude, p.longitude)
            else error = "Position indisponible : touchez la carte pour placer la station."
            locating = false
        }
    }

    fun submit() {
        val local = buildMap {
            Validators.required(nom)?.let { put("nom", it) }
            Validators.required(prenom)?.let { put("prenom", it) }
            Validators.email(email)?.let { put("email", it) }
            Validators.phone(telephone)?.let { put("telephone", it) }
            Validators.password(motDePasse)?.let { put("motDePasse", it) }
            if (confirmation != motDePasse) put("confirmation", "Les mots de passe ne correspondent pas")
            Validators.required(stationNom)?.let { put("station.nom", it) }
            Validators.required(adresse)?.let { put("station.adresse", it) }
            Validators.required(ville)?.let { put("station.ville", it) }
            Validators.phone(stationTelephone)?.let { put("station.telephone", it) }
            if (latitude == null || longitude == null) put("position", "Indiquez l'emplacement sur la carte")
        }
        errors = local
        if (local.isNotEmpty()) {
            error = "Certains champs sont invalides"
            return
        }

        loading = true
        error = null
        viewModelScope.launch {
            val station = StationInfo(
                nom = stationNom.trim(),
                adresse = adresse.trim(),
                ville = ville.trim(),
                latitude = latitude!!,
                longitude = longitude!!,
                telephone = stationTelephone.trim().ifBlank { null },
                description = description.trim().ifBlank { null },
                photoUrl = null,
            )
            authRepository.registerStation(
                RegisterStationRequest(nom.trim(), prenom.trim(), email.trim(), motDePasse, telephone.trim().ifBlank { null }, station)
            ).onSuccess { successMessage = it.message }
                .onFailure { e ->
                    error = e.message
                    errors = (e as? ApiException)?.fieldErrors.orEmpty()
                }
            loading = false
        }
    }
}
