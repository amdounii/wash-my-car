package com.washmycar.ui.station

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.washmycar.data.model.Horaire
import com.washmycar.data.model.Reservation
import com.washmycar.data.model.Tarif
import com.washmycar.data.model.TarifRequest
import com.washmycar.ui.auth.BackTopBar
import com.washmycar.ui.auth.LocationPicker
import com.washmycar.ui.client.EvaluationItem
import com.washmycar.ui.client.Loadable
import com.washmycar.ui.components.*
import com.washmycar.util.Format
import java.time.LocalDateTime

/** Affiche les messages ponctuels d'un ViewModel dans une Snackbar. */
@Composable
private fun MessageEffect(message: String?, snackbar: SnackbarHostState, consume: () -> Unit) {
    LaunchedEffect(message) {
        message?.let {
            snackbar.showSnackbar(it)
            consume()
        }
    }
}

// ====================================================================== réservations reçues

private val FILTRES = listOf("EN_ATTENTE" to "En attente", "ACCEPTEE" to "Acceptées", null to "Toutes",
    "REFUSEE" to "Refusées", "ANNULEE" to "Annulées")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StationReservationsScreen(vm: StationReservationsViewModel = hiltViewModel()) {
    val snackbar = remember { SnackbarHostState() }
    var toCancel by remember { mutableStateOf<Reservation?>(null) }
    MessageEffect(vm.message, snackbar) { vm.message = null }

    toCancel?.let { r ->
        ConfirmDialog("Annuler la réservation", "Annuler la réservation de ${r.clientNom} du ${Format.dateTime(r.dateDebut)} ?",
            "Annuler la réservation", onConfirm = { vm.changeStatut(r, "ANNULEE"); toCancel = null }, onDismiss = { toCancel = null })
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Réservations reçues") },
                actions = { IconButton(onClick = vm::load) { Icon(Icons.Filled.Refresh, contentDescription = "Actualiser") } },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(Modifier.padding(padding)) {
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(FILTRES) { (code, label) ->
                    FilterChip(selected = vm.filtre == code, onClick = { vm.selectFiltre(code) }, label = { Text(label) })
                }
            }
            when (val s = vm.state) {
                Loadable.Loading -> LoadingBox()
                is Loadable.Error -> ErrorBox(s.message, vm::load)
                is Loadable.Ready -> if (s.data.isEmpty()) {
                    EmptyBox("Aucune réservation.")
                } else {
                    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(s.data, key = { it.id }) { r ->
                            ReceivedReservationCard(
                                r,
                                busy = vm.busyId == r.id,
                                onAccept = { vm.changeStatut(r, "ACCEPTEE") },
                                onRefuse = { vm.changeStatut(r, "REFUSEE") },
                                onCancel = { toCancel = r },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReceivedReservationCard(r: Reservation, busy: Boolean, onAccept: () -> Unit, onRefuse: () -> Unit, onCancel: () -> Unit) {
    val future = Format.parse(r.dateDebut).isAfter(Format.now())
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(Format.dateTime(r.dateDebut), fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                StatusChip(r.statut)
            }
            Text("Fin prévue à ${Format.time(r.dateFin)}", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(6.dp))
            Text("${r.service} · ${Format.vehicule(r.typeVehicule)} · ${Format.tnd(r.prix)}")
            Text("${r.clientNom} · ${r.clientTelephone ?: r.clientEmail}",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            if (busy) {
                LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 8.dp))
            } else if (r.statut == "EN_ATTENTE") {
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onAccept, modifier = Modifier.weight(1f)) { Text("Accepter") }
                    OutlinedButton(onClick = onRefuse, modifier = Modifier.weight(1f)) { Text("Refuser") }
                }
            } else if (r.statut == "ACCEPTEE" && future) {
                TextButton(onClick = onCancel, modifier = Modifier.align(Alignment.End)) {
                    Text("Annuler", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

// ====================================================================== horaires

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HorairesScreen(vm: HorairesViewModel = hiltViewModel()) {
    val snackbar = remember { SnackbarHostState() }
    var picking by remember { mutableStateOf<Pair<String, Boolean>?>(null) } // (jour, ouverture?)
    MessageEffect(vm.message, snackbar) { vm.message = null }

    picking?.let { (jour, ouverture) ->
        val h = vm.horaires.first { it.jourSemaine == jour }
        val initial = Format.hour(if (ouverture) h.heureOuverture else h.heureFermeture).split(":")
        TimePickerDialog(
            title = "${Format.JOURS[jour]} – ${if (ouverture) "ouverture" else "fermeture"}",
            hour = initial[0].toIntOrNull() ?: 8,
            minute = initial.getOrNull(1)?.toIntOrNull() ?: 0,
            onConfirm = { hh, mm ->
                val value = "%02d:%02d".format(hh, mm)
                vm.update(jour) { if (ouverture) it.copy(heureOuverture = value) else it.copy(heureFermeture = value) }
                picking = null
            },
            onDismiss = { picking = null },
        )
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Horaires d'ouverture") }) },
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            if (vm.state is Loadable.Ready) {
                Surface(shadowElevation = 8.dp) {
                    PrimaryButton("Enregistrer", vm::save, loading = vm.saving, modifier = Modifier.padding(16.dp))
                }
            }
        },
    ) { padding ->
        when (val s = vm.state) {
            Loadable.Loading -> LoadingBox(Modifier.padding(padding))
            is Loadable.Error -> ErrorBox(s.message, vm::load, Modifier.padding(padding))
            is Loadable.Ready -> LazyColumn(Modifier.padding(padding), contentPadding = PaddingValues(16.dp)) {
                items(vm.horaires, key = { it.jourSemaine }) { h ->
                    HoraireRow(
                        h,
                        onToggle = { vm.toggleOuvert(h.jourSemaine, it) },
                        onPickOpen = { picking = h.jourSemaine to true },
                        onPickClose = { picking = h.jourSemaine to false },
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun HoraireRow(h: Horaire, onToggle: (Boolean) -> Unit, onPickOpen: () -> Unit, onPickClose: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(Format.JOURS[h.jourSemaine] ?: h.jourSemaine, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Text(if (h.ferme) "Fermé" else "Ouvert", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.width(8.dp))
            Switch(checked = !h.ferme, onCheckedChange = onToggle)
        }
        if (!h.ferme) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = onPickOpen, label = { Text("De ${Format.hour(h.heureOuverture)}") })
                AssistChip(onClick = onPickClose, label = { Text("À ${Format.hour(h.heureFermeture)}") })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(title: String, hour: Int, minute: Int, onConfirm: (Int, Int) -> Unit, onDismiss: () -> Unit) {
    val state = rememberTimePickerState(initialHour = hour, initialMinute = minute, is24Hour = true)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { TimePicker(state = state) },
        confirmButton = { TextButton(onClick = { onConfirm(state.hour, state.minute) }) { Text("OK") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } },
    )
}

// ====================================================================== tarifs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TarifsScreen(vm: TarifsViewModel = hiltViewModel()) {
    val snackbar = remember { SnackbarHostState() }
    var editing by remember { mutableStateOf<Tarif?>(null) }
    var creating by remember { mutableStateOf(false) }
    var toDelete by remember { mutableStateOf<Tarif?>(null) }
    MessageEffect(vm.message, snackbar) { vm.message = null }

    if (creating || editing != null) {
        TarifDialog(
            initial = editing,
            saving = vm.saving,
            error = vm.formError,
            onSave = { req -> vm.save(editing?.id, req) { creating = false; editing = null } },
            onDismiss = { creating = false; editing = null; vm.clearFormError() },
        )
    }
    toDelete?.let { t ->
        ConfirmDialog("Supprimer le tarif", "Supprimer « ${t.service} » (${Format.vehicule(t.typeVehicule)}) ?", "Supprimer",
            onConfirm = { vm.delete(t); toDelete = null }, onDismiss = { toDelete = null })
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Mes tarifs") }) },
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { creating = true },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Ajouter") },
            )
        },
    ) { padding ->
        when (val s = vm.state) {
            Loadable.Loading -> LoadingBox(Modifier.padding(padding))
            is Loadable.Error -> ErrorBox(s.message, vm::load, Modifier.padding(padding))
            is Loadable.Ready -> if (s.data.isEmpty()) {
                EmptyBox("Aucun tarif. Ajoutez vos services pour recevoir des réservations.", Modifier.padding(padding))
            } else {
                LazyColumn(Modifier.padding(padding), contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 96.dp)) {
                    s.data.groupBy { it.typeVehicule }
                        .toSortedMap(compareBy { Format.TYPES_VEHICULE.keys.indexOf(it) })
                        .forEach { (type, tarifs) ->
                        item(key = type) { SectionTitle(Format.vehicule(type)) }
                        items(tarifs, key = { it.id }) { t ->
                            ListItem(
                                headlineContent = { Text(t.service) },
                                supportingContent = { Text("${t.dureeMinutes} min") },
                                trailingContent = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(Format.tnd(t.prix), fontWeight = FontWeight.SemiBold)
                                        IconButton(onClick = { toDelete = t }) {
                                            Icon(Icons.Filled.Delete, contentDescription = "Supprimer", tint = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                },
                                modifier = Modifier.clickable { editing = t },
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TarifDialog(initial: Tarif?, saving: Boolean, error: String?, onSave: (TarifRequest) -> Unit, onDismiss: () -> Unit) {
    var type by remember { mutableStateOf(initial?.typeVehicule ?: "CITADINE") }
    var service by remember { mutableStateOf(initial?.service.orEmpty()) }
    var prix by remember { mutableStateOf(initial?.prix?.toString().orEmpty()) }
    var duree by remember { mutableStateOf(initial?.dureeMinutes?.toString() ?: "30") }
    var localError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Nouveau tarif" else "Modifier le tarif") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                ErrorBanner(localError ?: error)
                Text("Type de véhicule", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 4.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Format.TYPES_VEHICULE.forEach { (code, label) ->
                        FilterChip(selected = type == code, onClick = { type = code }, label = { Text(label) })
                    }
                }
                FormField(service, { service = it }, "Service (ex. Extérieur, Complet)")
                FormField(prix, { prix = it.replace(',', '.') }, "Prix (TND)", keyboardType = KeyboardType.Decimal)
                FormField(duree, { duree = it.filter(Char::isDigit) }, "Durée (minutes)", keyboardType = KeyboardType.Number)
            }
        },
        confirmButton = {
            TextButton(enabled = !saving, onClick = {
                val p = prix.toDoubleOrNull()
                val d = duree.toIntOrNull()
                localError = when {
                    service.isBlank() -> "Indiquez le nom du service"
                    p == null || p < 0 -> "Prix invalide"
                    d == null || d !in 5..480 -> "Durée entre 5 et 480 minutes"
                    else -> null
                }
                if (localError == null) onSave(TarifRequest(type, service.trim(), p!!, d!!))
            }) { Text(if (saving) "Enregistrement…" else "Enregistrer") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } },
    )
}

// ====================================================================== avis

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StationEvaluationsScreen(vm: StationEvaluationsViewModel = hiltViewModel()) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Avis des clients") },
                actions = { IconButton(onClick = vm::load) { Icon(Icons.Filled.Refresh, contentDescription = "Actualiser") } },
            )
        },
    ) { padding ->
        when (val s = vm.state) {
            Loadable.Loading -> LoadingBox(Modifier.padding(padding))
            is Loadable.Error -> ErrorBox(s.message, vm::load, Modifier.padding(padding))
            is Loadable.Ready -> if (s.data.isEmpty()) {
                EmptyBox("Aucun avis pour le moment.", Modifier.padding(padding))
            } else {
                val moyenne = s.data.map { it.note }.average()
                LazyColumn(Modifier.padding(padding), contentPadding = PaddingValues(16.dp)) {
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
                            Text(Format.note(moyenne), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.width(12.dp))
                            Column {
                                RatingStars(moyenne, 20)
                                Text("${s.data.size} avis", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                    items(s.data, key = { it.id }) { EvaluationItem(it) }
                }
            }
        }
    }
}

// ====================================================================== menu et fiche station

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StationMenuScreen(onEditStation: () -> Unit, onAccount: () -> Unit, vm: StationInfoViewModel = hiltViewModel()) {
    // Au retour de l'écran d'édition, on recharge pour afficher les nouvelles informations.
    LaunchedEffect(Unit) { if (vm.state is Loadable.Ready) vm.load() }
    Scaffold(topBar = { TopAppBar(title = { Text("Ma station") }) }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            when (val s = vm.state) {
                Loadable.Loading -> LinearProgressIndicator(Modifier.fillMaxWidth())
                is Loadable.Error -> ErrorBanner(s.message)
                is Loadable.Ready -> ElevatedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text(s.data.nom, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("${s.data.adresse}, ${s.data.ville}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StatusChip(s.data.statutInscription, Format.STATUT_INSCRIPTION)
                            RatingStars(s.data.noteMoyenne)
                            Text(Format.note(s.data.noteMoyenne))
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            MenuItem(Icons.Filled.Place, "Informations et emplacement", onEditStation)
            MenuItem(Icons.Filled.Person, "Mon compte et déconnexion", onAccount)
        }
    }
}

@Composable
private fun MenuItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(label) },
        leadingContent = { Icon(icon, contentDescription = null) },
        trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) },
        modifier = Modifier.clickable(onClick = onClick),
    )
    HorizontalDivider()
}

@Composable
fun StationInfoScreen(onBack: () -> Unit, vm: StationInfoViewModel = hiltViewModel()) {
    val snackbar = remember { SnackbarHostState() }
    val requestPermission = rememberLocationPermissionRequest { granted -> if (granted) vm.useMyLocation() }
    MessageEffect(vm.message, snackbar) { vm.message = null }

    Scaffold(topBar = { BackTopBar("Informations de la station", onBack) }, snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        when (val s = vm.state) {
            Loadable.Loading -> LoadingBox(Modifier.padding(padding))
            is Loadable.Error -> ErrorBox(s.message, vm::load, Modifier.padding(padding))
            is Loadable.Ready -> Column(
                Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                ErrorBanner(vm.error)
                FormField(vm.nom, { vm.nom = it }, "Nom de la station", error = vm.errors["nom"])
                FormField(vm.adresse, { vm.adresse = it }, "Adresse", error = vm.errors["adresse"])
                FormField(vm.ville, { vm.ville = it }, "Ville", error = vm.errors["ville"])
                FormField(vm.telephone, { vm.telephone = it }, "Téléphone", error = vm.errors["telephone"], keyboardType = KeyboardType.Phone)
                FormField(vm.photoUrl, { vm.photoUrl = it }, "URL d'une photo (optionnel)", error = vm.errors["photoUrl"],
                    keyboardType = KeyboardType.Uri)
                FormField(vm.description, { vm.description = it }, "Description", error = vm.errors["description"],
                    singleLine = false, minLines = 3)
                SectionTitle("Emplacement")
                LocationPicker(
                    latitude = vm.latitude,
                    longitude = vm.longitude,
                    error = vm.errors["position"] ?: vm.errors["latitude"],
                    locating = vm.locating,
                    onUseMyLocation = requestPermission,
                    onPick = vm::setPosition,
                )
                Spacer(Modifier.height(12.dp))
                PrimaryButton("Enregistrer", vm::save, loading = vm.saving)
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}
