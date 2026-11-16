package com.washmycar.ui.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.washmycar.ui.auth.BackTopBar
import com.washmycar.ui.components.*

/**
 * Profil du compte connecté. [onBack] est null quand l'écran est un onglet (client),
 * renseigné quand il est ouvert depuis le menu de la station.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(onBack: (() -> Unit)? = null, vm: ProfileViewModel = hiltViewModel()) {
    var confirmLogout by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(vm.message) {
        vm.message?.let {
            snackbar.showSnackbar(it)
            vm.message = null
        }
    }
    if (confirmLogout) {
        ConfirmDialog("Déconnexion", "Voulez-vous vous déconnecter ?", "Se déconnecter",
            onConfirm = { confirmLogout = false; vm.logout() }, onDismiss = { confirmLogout = false })
    }

    Scaffold(
        topBar = {
            if (onBack != null) BackTopBar("Mon compte", onBack)
            else TopAppBar(title = { Text("Mon profil") })
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        when {
            vm.loading -> LoadingBox(Modifier.padding(padding))
            vm.loadError != null -> ErrorBox(vm.loadError!!, vm::load, Modifier.padding(padding))
            else -> Column(
                Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                ErrorBanner(vm.error)
                FormField(vm.email, {}, "Email", enabled = false, leadingIcon = Icons.Filled.Email,
                    supportingText = "L'email ne peut pas être modifié")
                FormField(vm.nom, { vm.nom = it }, "Nom", error = vm.errors["nom"])
                FormField(vm.prenom, { vm.prenom = it }, "Prénom", error = vm.errors["prenom"])
                FormField(vm.telephone, { vm.telephone = it }, "Téléphone", error = vm.errors["telephone"],
                    keyboardType = KeyboardType.Phone, leadingIcon = Icons.Filled.Phone)

                SectionTitle("Changer de mot de passe (optionnel)")
                PasswordField(vm.ancienMotDePasse, { vm.ancienMotDePasse = it }, "Mot de passe actuel", vm.errors["ancienMotDePasse"])
                PasswordField(vm.nouveauMotDePasse, { vm.nouveauMotDePasse = it }, "Nouveau mot de passe", vm.errors["nouveauMotDePasse"])

                Spacer(Modifier.height(16.dp))
                PrimaryButton("Enregistrer", vm::save, loading = vm.saving)
                Spacer(Modifier.height(24.dp))
                OutlinedButton(
                    onClick = { confirmLogout = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) {
                    Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null)
                    Text("  Se déconnecter")
                }
            }
        }
    }
}
