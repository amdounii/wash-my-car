package com.washmycar.ui.profile

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.washmycar.data.model.ProfileUpdateRequest
import com.washmycar.data.remote.ApiException
import com.washmycar.data.repository.AuthRepository
import com.washmycar.data.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Profil du compte connecté (client ou gérant de station). */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    var loading by mutableStateOf(true)
        private set
    var loadError by mutableStateOf<String?>(null)
        private set

    var email by mutableStateOf("")
        private set
    var nom by mutableStateOf("")
    var prenom by mutableStateOf("")
    var telephone by mutableStateOf("")
    var ancienMotDePasse by mutableStateOf("")
    var nouveauMotDePasse by mutableStateOf("")

    var errors by mutableStateOf<Map<String, String>>(emptyMap())
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var saving by mutableStateOf(false)
        private set
    var message by mutableStateOf<String?>(null)

    init {
        load()
    }

    fun load() {
        loading = true
        loadError = null
        viewModelScope.launch {
            profileRepository.get()
                .onSuccess {
                    email = it.email
                    nom = it.nom
                    prenom = it.prenom
                    telephone = it.telephone.orEmpty()
                }
                .onFailure { loadError = it.message }
            loading = false
        }
    }

    fun save() {
        val changePassword = nouveauMotDePasse.isNotBlank()
        val local = buildMap {
            if (nom.isBlank()) put("nom", "Champ obligatoire")
            if (prenom.isBlank()) put("prenom", "Champ obligatoire")
            if (changePassword && nouveauMotDePasse.length < 8) put("nouveauMotDePasse", "8 caractères minimum")
            if (changePassword && ancienMotDePasse.isBlank()) put("ancienMotDePasse", "Saisissez votre mot de passe actuel")
        }
        errors = local
        if (local.isNotEmpty()) return

        saving = true
        error = null
        viewModelScope.launch {
            profileRepository.update(
                ProfileUpdateRequest(
                    nom = nom.trim(),
                    prenom = prenom.trim(),
                    telephone = telephone.trim(),
                    ancienMotDePasse = ancienMotDePasse.takeIf { changePassword },
                    nouveauMotDePasse = nouveauMotDePasse.takeIf { changePassword },
                )
            ).onSuccess {
                message = "Profil mis à jour"
                ancienMotDePasse = ""
                nouveauMotDePasse = ""
            }.onFailure { e ->
                error = e.message
                errors = (e as? ApiException)?.fieldErrors.orEmpty()
            }
            saving = false
        }
    }

    fun logout() {
        viewModelScope.launch { authRepository.logout() }
    }
}
