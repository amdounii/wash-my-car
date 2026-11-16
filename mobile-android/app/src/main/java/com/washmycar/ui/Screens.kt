package com.washmycar.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.washmycar.data.*
import kotlinx.coroutines.launch

enum class Screen { LOGIN, CLIENT, STATION, HOME }

@Composable
fun WashMyCarApp() {
    var screen by remember { mutableStateOf(Screen.LOGIN) }
    var session by remember { mutableStateOf<LoginResponse?>(null) }

    when (screen) {
        Screen.LOGIN -> LoginScreen(
            onLogged = { session = it; screen = Screen.HOME },
            goClient = { screen = Screen.CLIENT },
            goStation = { screen = Screen.STATION }
        )
        Screen.CLIENT -> RegisterClientScreen { screen = Screen.LOGIN }
        Screen.STATION -> RegisterStationScreen { screen = Screen.LOGIN }
        Screen.HOME -> HomeScreen(session) { session = null; screen = Screen.LOGIN }
    }
}

// ---------- composants communs ----------
@Composable
private fun Field(
    label: String, value: String, onChange: (String) -> Unit,
    password: Boolean = false, type: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value, onValueChange = onChange, label = { Text(label) },
        singleLine = true, modifier = Modifier.fillMaxWidth(),
        visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = if (password) KeyboardType.Password else type)
    )
}

@Composable
private fun Page(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(title, style = MaterialTheme.typography.headlineSmall)
        content()
    }
}

@Composable
private fun Feedback(message: String?, success: Boolean) {
    message?.let {
        Text(it, color = if (success) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
    }
}

// ---------- US02 / US19 : connexion ----------
@Composable
fun LoginScreen(onLogged: (LoginResponse) -> Unit, goClient: () -> Unit, goStation: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var mdp by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Page("Connexion") {
        Field("E-mail", email, { email = it }, type = KeyboardType.Email)
        Field("Mot de passe", mdp, { mdp = it }, password = true)
        Feedback(error, false)
        Button(
            enabled = !loading, modifier = Modifier.fillMaxWidth(),
            onClick = {
                scope.launch {
                    loading = true; error = null
                    try {
                        val r = Api.service.login(LoginRequest(email.trim(), mdp))
                        if (r.isSuccessful) onLogged(r.body()!!) else error = r.errorMessage()
                    } catch (e: Exception) {
                        error = e.localizedMessage ?: "Serveur injoignable"
                    }
                    loading = false
                }
            }
        ) { Text("Se connecter") }
        TextButton(onClick = goClient) { Text("Créer un compte Client") }
        TextButton(onClick = goStation) { Text("Inscrire ma station de lavage") }
    }
}

// ---------- US01 : inscription Client ----------
@Composable
fun RegisterClientScreen(onBack: () -> Unit) {
    var nom by remember { mutableStateOf("") }
    var prenom by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var tel by remember { mutableStateOf("") }
    var mdp by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var success by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Page("Inscription Client") {
        Field("Nom", nom, { nom = it })
        Field("Prénom", prenom, { prenom = it })
        Field("E-mail", email, { email = it }, type = KeyboardType.Email)
        Field("Téléphone", tel, { tel = it }, type = KeyboardType.Phone)
        Field("Mot de passe", mdp, { mdp = it }, password = true)
        Field("Confirmer le mot de passe", confirm, { confirm = it }, password = true)
        Feedback(message, success)
        Button(
            enabled = !loading, modifier = Modifier.fillMaxWidth(),
            onClick = {
                if (mdp != confirm) { message = "Les mots de passe ne correspondent pas"; success = false; return@Button }
                scope.launch {
                    loading = true; message = null
                    try {
                        val r = Api.service.registerClient(
                            ClientRegister(nom, prenom, email.trim(), tel, mdp)
                        )
                        success = r.isSuccessful
                        message = if (r.isSuccessful) r.body()?.message else r.errorMessage()
                    } catch (e: Exception) {
                        success = false; message = e.localizedMessage ?: "Serveur injoignable"
                    }
                    loading = false
                }
            }
        ) { Text("S'inscrire") }
        TextButton(onClick = onBack) { Text("Retour à la connexion") }
    }
}

// ---------- US09 : inscription Station ----------
@Composable
fun RegisterStationScreen(onBack: () -> Unit) {
    var nomStation by remember { mutableStateOf("") }
    var adresse by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var nom by remember { mutableStateOf("") }
    var prenom by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var tel by remember { mutableStateOf("") }
    var mdp by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var success by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Page("Inscription Station") {
        Field("Nom de la station", nomStation, { nomStation = it })
        Field("Adresse", adresse, { adresse = it })
        Field("Description", description, { description = it })
        Field("Nom du responsable", nom, { nom = it })
        Field("Prénom du responsable", prenom, { prenom = it })
        Field("E-mail", email, { email = it }, type = KeyboardType.Email)
        Field("Téléphone", tel, { tel = it }, type = KeyboardType.Phone)
        Field("Mot de passe", mdp, { mdp = it }, password = true)
        Field("Confirmer le mot de passe", confirm, { confirm = it }, password = true)
        Feedback(message, success)
        Button(
            enabled = !loading, modifier = Modifier.fillMaxWidth(),
            onClick = {
                if (mdp != confirm) { message = "Les mots de passe ne correspondent pas"; success = false; return@Button }
                scope.launch {
                    loading = true; message = null
                    try {
                        val r = Api.service.registerStation(
                            StationRegister(nomStation, adresse, description, nom, prenom, email.trim(), tel, mdp)
                        )
                        success = r.isSuccessful
                        message = if (r.isSuccessful) r.body()?.message else r.errorMessage()
                    } catch (e: Exception) {
                        success = false; message = e.localizedMessage ?: "Serveur injoignable"
                    }
                    loading = false
                }
            }
        ) { Text("Envoyer la demande") }
        TextButton(onClick = onBack) { Text("Retour à la connexion") }
    }
}

// ---------- Accueil (espace selon le rôle) ----------
@Composable
fun HomeScreen(session: LoginResponse?, onLogout: () -> Unit) {
    Page(if (session?.role == "STATION") "Espace Station" else "Espace Client") {
        Text("Connexion réussie (rôle : ${session?.role}).")
        Text("Les fonctionnalités de ce rôle seront ajoutées dans les prochains Sprints.")
        Button(onClick = onLogout) { Text("Se déconnecter") }
    }
}
