package com.washmycar.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.washmycar.ui.components.*
import com.washmycar.util.LocationProvider

// ====================================================================== connexion

@Composable
fun LoginScreen(onRegisterClient: () -> Unit, onRegisterStation: () -> Unit, vm: LoginViewModel = hiltViewModel()) {
    Scaffold { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(32.dp))
            Text("🚿", style = MaterialTheme.typography.displayMedium)
            Text("Wash My Car", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary)
            Text("Trouvez et réservez votre lavage", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(32.dp))

            ErrorBanner(vm.error)
            vm.info?.let {
                Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(8.dp)) {
                    Text(it, Modifier.padding(12.dp))
                }
            }
            if (vm.canResendVerification) {
                TextButton(onClick = vm::resendVerification) { Text("Renvoyer l'email de validation") }
            }
            Spacer(Modifier.height(8.dp))

            FormField(vm.email, { vm.email = it }, "Email", keyboardType = KeyboardType.Email, leadingIcon = Icons.Filled.Email)
            PasswordField(vm.motDePasse, { vm.motDePasse = it })
            Spacer(Modifier.height(16.dp))
            PrimaryButton("Se connecter", vm::login, loading = vm.loading)

            Spacer(Modifier.height(32.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))
            Text("Pas encore de compte ?", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onRegisterClient, modifier = Modifier.fillMaxWidth()) { Text("Créer un compte client") }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onRegisterStation, modifier = Modifier.fillMaxWidth()) { Text("Inscrire ma station de lavage") }
        }
    }
}

// ====================================================================== inscription client

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterClientScreen(onBack: () -> Unit, vm: RegisterClientViewModel = hiltViewModel()) {
    vm.successMessage?.let {
        InfoDialog("Inscription réussie", "$it\n\nCliquez sur le lien reçu pour activer votre compte, puis connectez-vous.", onBack)
    }
    Scaffold(topBar = { BackTopBar("Créer un compte client", onBack) }) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            ErrorBanner(vm.error)
            FormField(vm.nom, { vm.nom = it }, "Nom", error = vm.errors["nom"], leadingIcon = Icons.Filled.Person)
            FormField(vm.prenom, { vm.prenom = it }, "Prénom", error = vm.errors["prenom"])
            FormField(vm.email, { vm.email = it }, "Email", error = vm.errors["email"], keyboardType = KeyboardType.Email,
                leadingIcon = Icons.Filled.Email)
            FormField(vm.telephone, { vm.telephone = it }, "Téléphone (optionnel)", error = vm.errors["telephone"],
                keyboardType = KeyboardType.Phone, leadingIcon = Icons.Filled.Phone)
            PasswordField(vm.motDePasse, { vm.motDePasse = it }, error = vm.errors["motDePasse"])
            PasswordField(vm.confirmation, { vm.confirmation = it }, "Confirmer le mot de passe", vm.errors["confirmation"])
            Spacer(Modifier.height(8.dp))
            PrimaryButton("Créer mon compte", vm::submit, loading = vm.loading)
            Text(
                "Un email de validation vous sera envoyé.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
        }
    }
}

// ====================================================================== inscription station

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterStationScreen(onBack: () -> Unit, vm: RegisterStationViewModel = hiltViewModel()) {
    val requestPermission = rememberLocationPermissionRequest { granted -> if (granted) vm.useMyLocation() }

    vm.successMessage?.let {
        InfoDialog("Inscription enregistrée", "$it\n\nVous pourrez vous connecter dès la validation.", onBack)
    }
    Scaffold(topBar = { BackTopBar("Inscrire ma station", onBack) }) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            ErrorBanner(vm.error)

            SectionTitle("Le gérant")
            FormField(vm.nom, { vm.nom = it }, "Nom", error = vm.errors["nom"])
            FormField(vm.prenom, { vm.prenom = it }, "Prénom", error = vm.errors["prenom"])
            FormField(vm.email, { vm.email = it }, "Email", error = vm.errors["email"], keyboardType = KeyboardType.Email)
            FormField(vm.telephone, { vm.telephone = it }, "Téléphone", error = vm.errors["telephone"], keyboardType = KeyboardType.Phone)
            PasswordField(vm.motDePasse, { vm.motDePasse = it }, error = vm.errors["motDePasse"])
            PasswordField(vm.confirmation, { vm.confirmation = it }, "Confirmer le mot de passe", vm.errors["confirmation"])

            SectionTitle("La station")
            FormField(vm.stationNom, { vm.stationNom = it }, "Nom de la station", error = vm.errors["station.nom"])
            FormField(vm.adresse, { vm.adresse = it }, "Adresse", error = vm.errors["station.adresse"])
            FormField(vm.ville, { vm.ville = it }, "Ville", error = vm.errors["station.ville"])
            FormField(vm.stationTelephone, { vm.stationTelephone = it }, "Téléphone de la station (optionnel)",
                error = vm.errors["station.telephone"], keyboardType = KeyboardType.Phone)
            FormField(vm.description, { vm.description = it }, "Description (optionnel)", singleLine = false, minLines = 3)

            SectionTitle("Emplacement")
            LocationPicker(
                latitude = vm.latitude,
                longitude = vm.longitude,
                error = vm.errors["position"] ?: vm.errors["station.latitude"],
                locating = vm.locating,
                onUseMyLocation = requestPermission,
                onPick = vm::setPosition,
            )

            Spacer(Modifier.height(12.dp))
            PrimaryButton("Envoyer mon inscription", vm::submit, loading = vm.loading)
            Text(
                "L'administrateur doit valider votre station avant votre première connexion.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            )
        }
    }
}

/** Choix d'un emplacement : position GPS ou clic sur la carte. Réutilisé par le profil de la station. */
@Composable
fun LocationPicker(
    latitude: Double?,
    longitude: Double?,
    error: String?,
    locating: Boolean,
    onUseMyLocation: () -> Unit,
    onPick: (Double, Double) -> Unit,
) {
    val center = if (latitude != null && longitude != null) latitude to longitude
    else LocationProvider.TUNIS.latitude to LocationProvider.TUNIS.longitude
    Text("Touchez la carte pour placer la station.", style = MaterialTheme.typography.bodySmall)
    Spacer(Modifier.height(6.dp))
    OsmMap(
        centerLatitude = center.first,
        centerLongitude = center.second,
        zoom = if (latitude != null) 16.0 else 12.0,
        markers = if (latitude != null && longitude != null) listOf(MapMarker(0, latitude, longitude, "Ma station")) else emptyList(),
        onMapTap = onPick,
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
            .clip(RoundedCornerShape(12.dp)),
    )
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
        Text(
            if (latitude != null && longitude != null) "%.5f, %.5f".format(latitude, longitude) else "Aucun emplacement choisi",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onUseMyLocation, enabled = !locating) {
            Icon(Icons.Filled.LocationOn, contentDescription = null)
            Text(if (locating) "Localisation…" else "Ma position")
        }
    }
    error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackTopBar(title: String, onBack: () -> Unit, actions: @Composable RowScope.() -> Unit = {}) {
    TopAppBar(
        title = { Text(title) },
        navigationIcon = {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour") }
        },
        actions = actions,
    )
}
