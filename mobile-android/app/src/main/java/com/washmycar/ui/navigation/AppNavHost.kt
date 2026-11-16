package com.washmycar.ui.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.washmycar.data.local.Session
import com.washmycar.data.repository.AuthRepository
import com.washmycar.ui.auth.LoginScreen
import com.washmycar.ui.auth.RegisterClientScreen
import com.washmycar.ui.auth.RegisterStationScreen
import com.washmycar.ui.client.*
import com.washmycar.ui.components.LoadingBox
import com.washmycar.ui.profile.ProfileScreen
import com.washmycar.ui.station.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** État de la session, observé à la racine pour choisir le parcours à afficher. */
sealed interface SessionState {
    data object Loading : SessionState
    data object LoggedOut : SessionState
    data class LoggedIn(val session: Session) : SessionState
}

@HiltViewModel
class SessionViewModel @Inject constructor(private val authRepository: AuthRepository) : ViewModel() {
    val state = authRepository.session
        .map { if (it == null) SessionState.LoggedOut else SessionState.LoggedIn(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, SessionState.Loading)

    fun logout() {
        viewModelScope.launch { authRepository.logout() }
    }
}

/**
 * Racine de la navigation : connexion / inscription tant qu'aucune session n'existe,
 * puis parcours client ou parcours station selon le rôle.
 * La déconnexion (ou un jeton expiré) ramène automatiquement à l'écran de connexion.
 */
@Composable
fun AppNavHost(vm: SessionViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    when (val s = state) {
        SessionState.Loading -> LoadingBox()
        SessionState.LoggedOut -> AuthNavHost()
        is SessionState.LoggedIn -> when {
            s.session.isClient -> ClientNavHost()
            s.session.isStation -> StationNavHost()
            else -> LaunchedEffect(Unit) { vm.logout() }
        }
    }
}

// ====================================================================== authentification

@Composable
private fun AuthNavHost() {
    val nav = rememberNavController()
    NavHost(nav, startDestination = "login") {
        composable("login") {
            LoginScreen(
                onRegisterClient = { nav.navigate("register/client") },
                onRegisterStation = { nav.navigate("register/station") },
            )
        }
        composable("register/client") { RegisterClientScreen(onBack = { nav.popBackStack() }) }
        composable("register/station") { RegisterStationScreen(onBack = { nav.popBackStack() }) }
    }
}

// ====================================================================== onglets

private data class Tab(val route: String, val label: String, val icon: ImageVector)

/** Barre d'onglets : conserve l'état de chaque onglet et évite les doublons dans la pile. */
@Composable
private fun BottomBar(nav: NavHostController, tabs: List<Tab>) {
    val backStack by nav.currentBackStackEntryAsState()
    val current = backStack?.destination?.route
    // La barre n'est affichée que sur les écrans racines des onglets.
    if (tabs.none { it.route == current }) return
    NavigationBar {
        tabs.forEach { tab ->
            NavigationBarItem(
                selected = current == tab.route,
                onClick = {
                    nav.navigate(tab.route) {
                        popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(tab.icon, contentDescription = null) },
                label = { Text(tab.label) },
            )
        }
    }
}

// ====================================================================== parcours client

private val CLIENT_TABS = listOf(
    Tab("nearby", "Stations", Icons.Filled.Place),
    Tab("reservations", "Réservations", Icons.Filled.DateRange),
    Tab("profile", "Profil", Icons.Filled.Person),
)

@Composable
private fun ClientNavHost() {
    val nav = rememberNavController()
    Scaffold(bottomBar = { BottomBar(nav, CLIENT_TABS) }) { padding ->
        NavHost(nav, startDestination = "nearby", modifier = Modifier.padding(bottom = padding.calculateBottomPadding()).consumeWindowInsets(PaddingValues(bottom = padding.calculateBottomPadding()))) {
            composable("nearby") { NearbyScreen(onStationClick = { nav.navigate("station/$it") }) }
            composable(
                "station/{stationId}",
                arguments = listOf(navArgument("stationId") { type = NavType.LongType }),
            ) {
                StationDetailScreen(onBack = { nav.popBackStack() }, onBook = { nav.navigate("booking/$it") })
            }
            composable(
                "booking/{stationId}",
                arguments = listOf(navArgument("stationId") { type = NavType.LongType }),
            ) {
                BookingScreen(
                    onBack = { nav.popBackStack() },
                    onBooked = {
                        nav.navigate("reservations") {
                            popUpTo(nav.graph.findStartDestination().id)
                            launchSingleTop = true
                        }
                    },
                )
            }
            composable("reservations") { MyReservationsScreen() }
            composable("profile") { ProfileScreen() }
        }
    }
}

// ====================================================================== parcours station

private val STATION_TABS = listOf(
    Tab("s/reservations", "Agenda", Icons.AutoMirrored.Filled.List),
    Tab("s/horaires", "Horaires", Icons.Filled.DateRange),
    Tab("s/tarifs", "Tarifs", Icons.Filled.ShoppingCart),
    Tab("s/avis", "Avis", Icons.Filled.Star),
    Tab("s/menu", "Station", Icons.Filled.Home),
)

@Composable
private fun StationNavHost() {
    val nav = rememberNavController()
    Scaffold(bottomBar = { BottomBar(nav, STATION_TABS) }) { padding ->
        NavHost(nav, startDestination = "s/reservations", modifier = Modifier.padding(bottom = padding.calculateBottomPadding()).consumeWindowInsets(PaddingValues(bottom = padding.calculateBottomPadding()))) {
            composable("s/reservations") { StationReservationsScreen() }
            composable("s/horaires") { HorairesScreen() }
            composable("s/tarifs") { TarifsScreen() }
            composable("s/avis") { StationEvaluationsScreen() }
            composable("s/menu") {
                StationMenuScreen(onEditStation = { nav.navigate("s/info") }, onAccount = { nav.navigate("s/account") })
            }
            composable("s/info") { StationInfoScreen(onBack = { nav.popBackStack() }) }
            composable("s/account") { ProfileScreen(onBack = { nav.popBackStack() }) }
        }
    }
}
