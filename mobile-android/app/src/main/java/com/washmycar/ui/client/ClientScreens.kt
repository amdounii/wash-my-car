package com.washmycar.ui.client

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.washmycar.data.model.Evaluation
import com.washmycar.data.model.Reservation
import com.washmycar.data.model.StationDetail
import com.washmycar.data.model.StationSummary
import com.washmycar.ui.auth.BackTopBar
import com.washmycar.ui.components.*
import com.washmycar.util.Format
import java.time.LocalDateTime

// ====================================================================== stations proches

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NearbyScreen(onStationClick: (Long) -> Unit, vm: NearbyViewModel = hiltViewModel()) {
    val requestPermission = rememberLocationPermissionRequest { vm.locateAndLoad() }

    // Premier affichage : demande la permission si besoin, puis recherche.
    LaunchedEffect(Unit) {
        if (vm.position == null) {
            if (vm.hasLocationPermission) vm.locateAndLoad() else requestPermission()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Stations proches") },
                actions = {
                    TextButton(onClick = { vm.showMap = !vm.showMap }) {
                        Icon(
                            if (vm.showMap) Icons.AutoMirrored.Filled.List else Icons.Filled.Place,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(if (vm.showMap) " Liste" else " Carte")
                    }
                    IconButton(onClick = { if (vm.hasLocationPermission) vm.locateAndLoad() else requestPermission() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Actualiser ma position")
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(vm.rayons) { km ->
                    FilterChip(
                        selected = vm.rayonKm == km,
                        onClick = { vm.changeRayon(km) },
                        label = { Text("${km.toInt()} km") },
                    )
                }
            }
            vm.positionInfo?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }

            when (val s = vm.state) {
                Loadable.Loading -> LoadingBox()
                is Loadable.Error -> ErrorBox(s.message, onRetry = vm::load)
                is Loadable.Ready -> {
                    val position = vm.position
                    if (vm.showMap && position != null) {
                        OsmMap(
                            centerLatitude = position.latitude,
                            centerLongitude = position.longitude,
                            zoom = if (vm.rayonKm <= 10) 13.0 else 11.0,
                            markers = s.data.map {
                                MapMarker(it.id, it.latitude, it.longitude, it.nom, Format.km(it.distanceKm))
                            } + MapMarker(-1, position.latitude, position.longitude, "Vous êtes ici", isUser = true),
                            onMarkerClick = onStationClick,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else if (s.data.isEmpty()) {
                        EmptyBox("Aucune station validée dans un rayon de ${vm.rayonKm.toInt()} km.\nEssayez un rayon plus large.")
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            items(s.data, key = { it.id }) { StationCard(it) { onStationClick(it.id) } }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StationCard(station: StationSummary, onClick: () -> Unit) {
    ElevatedCard(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    station.nom,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(Format.km(station.distanceKm), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
            }
            Text("${station.adresse}, ${station.ville}", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RatingStars(station.noteMoyenne)
                Text(Format.note(station.noteMoyenne), style = MaterialTheme.typography.bodySmall)
                OpenChip(station.ouverteMaintenant)
                Spacer(Modifier.weight(1f))
                station.prixMin?.let {
                    Text("dès ${Format.tnd(it)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

// ====================================================================== fiche station

@Composable
fun StationDetailScreen(onBack: () -> Unit, onBook: (Long) -> Unit, vm: StationDetailViewModel = hiltViewModel()) {
    Scaffold(
        topBar = { BackTopBar("Station", onBack) },
        bottomBar = {
            (vm.state as? Loadable.Ready)?.let { ready ->
                Surface(shadowElevation = 8.dp) {
                    PrimaryButton(
                        "Réserver un créneau",
                        onClick = { onBook(ready.data.detail.id) },
                        enabled = ready.data.detail.tarifs.isNotEmpty(),
                        modifier = Modifier.navigationBarsPadding().padding(16.dp),
                    )
                }
            }
        },
    ) { padding ->
        when (val s = vm.state) {
            Loadable.Loading -> LoadingBox(Modifier.padding(padding))
            is Loadable.Error -> ErrorBox(s.message, vm::load, Modifier.padding(padding))
            is Loadable.Ready -> StationDetailContent(s.data.detail, s.data.evaluations, Modifier.padding(padding))
        }
    }
}

@Composable
private fun StationDetailContent(d: StationDetail, evaluations: List<Evaluation>, modifier: Modifier) {
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text(d.nom, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("${d.adresse}, ${d.ville}", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RatingStars(d.noteMoyenne, 18)
            Text("${Format.note(d.noteMoyenne)} (${d.nbEvaluations} avis)")
            OpenChip(d.ouverteMaintenant)
        }
        d.telephone?.let {
            Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Phone, contentDescription = null, Modifier.size(18.dp))
                Text(" $it")
            }
        }
        d.description?.let { Text(it, Modifier.padding(top = 8.dp)) }

        SectionTitle("Localisation")
        OsmMap(
            centerLatitude = d.latitude,
            centerLongitude = d.longitude,
            zoom = 15.0,
            interactive = false,
            markers = listOf(MapMarker(d.id, d.latitude, d.longitude, d.nom)),
            modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(12.dp)),
        )

        SectionTitle("Horaires")
        d.horaires.forEach { h ->
            Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                Text(Format.JOURS[h.jourSemaine] ?: h.jourSemaine, Modifier.weight(1f))
                Text(
                    if (h.ferme) "Fermé" else "${Format.hour(h.heureOuverture)} – ${Format.hour(h.heureFermeture)}",
                    color = if (h.ferme) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        SectionTitle("Tarifs")
        if (d.tarifs.isEmpty()) Text("Aucun tarif renseigné pour le moment.")
        d.tarifs.groupBy { it.typeVehicule }.forEach { (type, tarifs) ->
            Text(Format.vehicule(type), fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 6.dp))
            tarifs.forEach { t ->
                Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                    Text("${t.service} · ${t.dureeMinutes} min", Modifier.weight(1f))
                    Text(Format.tnd(t.prix), fontWeight = FontWeight.Medium)
                }
            }
        }

        SectionTitle("Avis des clients")
        if (evaluations.isEmpty()) Text("Aucun avis pour le moment.")
        evaluations.forEach { EvaluationItem(it) }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
fun EvaluationItem(e: Evaluation) {
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(e.clientNom, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            RatingStars(e.note.toDouble(), 14)
        }
        Text(Format.date(e.dateCreation), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        e.commentaire?.let { Text(it, Modifier.padding(top = 4.dp)) }
        HorizontalDivider(Modifier.padding(top = 8.dp))
    }
}

// ====================================================================== réservation

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BookingScreen(onBack: () -> Unit, onBooked: () -> Unit, vm: BookingViewModel = hiltViewModel()) {
    vm.confirmation?.let { r ->
        InfoDialog(
            "Demande envoyée",
            "${r.service} (${Format.vehicule(r.typeVehicule)})\n${Format.dateTime(r.dateDebut)}\n${Format.tnd(r.prix)}\n\n" +
                "Votre réservation est en attente de confirmation par la station.",
            onBooked,
        )
    }
    Scaffold(
        topBar = { BackTopBar("Réserver", onBack) },
        bottomBar = {
            Surface(shadowElevation = 8.dp) {
                Column(Modifier.navigationBarsPadding().padding(16.dp)) {
                    vm.tarif?.let { t ->
                        Text(
                            "${t.service} · ${Format.tnd(t.prix)}" + (vm.heure?.let { " · ${Format.dayShort(vm.date)} à $it" } ?: ""),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                    }
                    PrimaryButton("Confirmer la réservation", vm::reserve, loading = vm.submitting,
                        enabled = vm.tarif != null && vm.heure != null)
                }
            }
        },
    ) { padding ->
        when (val s = vm.station) {
            Loadable.Loading -> LoadingBox(Modifier.padding(padding))
            is Loadable.Error -> ErrorBox(s.message, vm::loadStation, Modifier.padding(padding))
            is Loadable.Ready -> {
                val d = s.data
                Column(Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
                    Text(d.nom, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    ErrorBanner(vm.error)

                    SectionTitle("1. Type de véhicule")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        vm.typesDisponibles(d).forEach { type ->
                            FilterChip(selected = vm.typeVehicule == type, onClick = { vm.selectType(type) },
                                label = { Text(Format.vehicule(type)) })
                        }
                    }

                    SectionTitle("2. Service")
                    d.tarifs.filter { it.typeVehicule == vm.typeVehicule }.forEach { t ->
                        val selected = vm.tarif?.id == t.id
                        OutlinedCard(
                            onClick = { vm.selectTarif(t) },
                            border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                            else CardDefaults.outlinedCardBorder(),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        ) {
                            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = selected, onClick = { vm.selectTarif(t) })
                                Column(Modifier.weight(1f)) {
                                    Text(t.service, fontWeight = FontWeight.SemiBold)
                                    Text("${t.dureeMinutes} min", style = MaterialTheme.typography.bodySmall)
                                }
                                Text(Format.tnd(t.prix), fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    if (vm.tarif != null) {
                        SectionTitle("3. Jour")
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(vm.jours) { jour ->
                                val ferme = vm.horaireDu(d, jour)?.ferme != false
                                FilterChip(
                                    selected = vm.date == jour,
                                    onClick = { vm.selectDate(jour) },
                                    enabled = !ferme,
                                    label = { Text(Format.dayShort(jour)) },
                                )
                            }
                        }

                        SectionTitle("4. Heure")
                        when (val slots = vm.slots) {
                            Loadable.Loading -> CircularProgressIndicator(Modifier.padding(8.dp))
                            is Loadable.Error -> Text(slots.message, color = MaterialTheme.colorScheme.error)
                            is Loadable.Ready -> if (slots.data.isEmpty()) {
                                Text("Aucun créneau disponible ce jour-là.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            } else {
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    slots.data.forEach { h ->
                                        FilterChip(selected = vm.heure == h, onClick = { vm.heure = h }, label = { Text(h.toString()) })
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

// ====================================================================== mes réservations

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyReservationsScreen(vm: MyReservationsViewModel = hiltViewModel()) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var toCancel by remember { mutableStateOf<Reservation?>(null) }
    var toEvaluate by remember { mutableStateOf<Reservation?>(null) }
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(vm.message) {
        vm.message?.let {
            snackbar.showSnackbar(it)
            vm.message = null
        }
    }
    toCancel?.let { r ->
        ConfirmDialog(
            "Annuler la réservation",
            "Annuler « ${r.service} » chez ${r.stationNom} le ${Format.dateTime(r.dateDebut)} ?",
            confirmLabel = "Annuler la réservation",
            onConfirm = { vm.cancel(r); toCancel = null },
            onDismiss = { toCancel = null },
        )
    }
    toEvaluate?.let { r ->
        EvaluationDialog(r.stationNom, onSubmit = { note, commentaire ->
            vm.evaluate(r.stationId, note, commentaire)
            toEvaluate = null
        }, onDismiss = { toEvaluate = null })
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mes réservations") },
                actions = { IconButton(onClick = vm::load) { Icon(Icons.Filled.Refresh, contentDescription = "Actualiser") } },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(Modifier.padding(padding)) {
            PrimaryTabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("À venir") })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Historique") })
            }
            when (val s = vm.state) {
                Loadable.Loading -> LoadingBox()
                is Loadable.Error -> ErrorBox(s.message, vm::load)
                is Loadable.Ready -> {
                    val now = Format.now()
                    val (upcoming, past) = s.data.partition { Format.parse(it.dateFin).isAfter(now) }
                    val list = if (tab == 0) upcoming.sortedBy { it.dateDebut } else past
                    if (list.isEmpty()) {
                        EmptyBox(if (tab == 0) "Aucune réservation à venir." else "Aucune réservation passée.")
                    } else {
                        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(list, key = { it.id }) { r ->
                                ReservationCard(
                                    r,
                                    busy = vm.busy,
                                    onCancel = { toCancel = r },
                                    onEvaluate = { toEvaluate = r },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReservationCard(r: Reservation, busy: Boolean, onCancel: () -> Unit, onEvaluate: () -> Unit) {
    val future = Format.parse(r.dateDebut).isAfter(Format.now())
    val cancellable = future && (r.statut == "EN_ATTENTE" || r.statut == "ACCEPTEE")
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(r.stationNom, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f))
                StatusChip(r.statut)
            }
            Text(Format.dateTime(r.dateDebut) + " – " + Format.time(r.dateFin), Modifier.padding(top = 4.dp))
            Text("${r.service} · ${Format.vehicule(r.typeVehicule)} · ${Format.tnd(r.prix)}",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (cancellable || r.evaluable) {
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.End) {
                    if (r.evaluable) {
                        TextButton(onClick = onEvaluate, enabled = !busy) {
                            Icon(Icons.Filled.Star, contentDescription = null, Modifier.size(18.dp))
                            Text(" Évaluer")
                        }
                    }
                    if (cancellable) {
                        TextButton(onClick = onCancel, enabled = !busy) {
                            Text("Annuler", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EvaluationDialog(stationNom: String, onSubmit: (Int, String) -> Unit, onDismiss: () -> Unit) {
    var note by remember { mutableIntStateOf(5) }
    var commentaire by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Évaluer $stationNom") },
        text = {
            Column {
                RatingInput(note) { note = it }
                OutlinedTextField(
                    value = commentaire,
                    onValueChange = { if (it.length <= 1000) commentaire = it },
                    label = { Text("Commentaire (optionnel)") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("Si vous avez déjà évalué cette station, votre avis sera remplacé.",
                    style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
            }
        },
        confirmButton = { TextButton(onClick = { onSubmit(note, commentaire) }) { Text("Envoyer") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } },
    )
}
