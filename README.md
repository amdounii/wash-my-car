# Wash My Car

Application de recherche et de réservation de stations de lavage en Tunisie.

| Dossier           | Contenu                                                        |
|-------------------|----------------------------------------------------------------|
| `database/`       | `schema.sql` (structure) et `data.sql` (jeu de test) pour MySQL |
| `backend/`        | API REST Spring Boot 3 (Java 17), JWT, Swagger                 |
| `web-admin/`      | Back-office administrateur React (Vite, Tailwind)              |
| `mobile-android/` | Application Kotlin / Jetpack Compose (clients et stations)     |

## Prérequis

- Java 17+ (Maven n'est pas nécessaire : le wrapper `mvnw` est fourni)
- MySQL 8 et MySQL Workbench
- Node.js 18+
- Android Studio (pour l'application mobile)

## 1. Base de données

Dans MySQL Workbench : **File > Run SQL Script…**, exécuter `database/schema.sql`, puis `database/data.sql`.

En ligne de commande :

```bash
mysql -u root -p < database/schema.sql
mysql -u root -p < database/data.sql
```

> `schema.sql` supprime et recrée les tables : relancer les deux scripts remet la base à zéro.

### Comptes de test (`data.sql`)

| Rôle    | Email                        | Mot de passe   | Remarque                    |
|---------|------------------------------|----------------|-----------------------------|
| Admin   | admin@washmycar.tn           | Admin@2026     |                             |
| Client  | sami.benali@mail.tn          | Client@2026    | validé                      |
| Client  | ines.trabelsi@mail.tn        | Client@2026    | validé                      |
| Client  | karim.jaziri@mail.tn         | Client@2026    | email non validé            |
| Station | contact@lavage-centre.tn     | Station@2026   | validée (Tunis centre)      |
| Station | contact@benarous-lavage.tn   | Station@2026   | en attente de validation    |
| Station | contact@bizerte-wash.tn      | Station@2026   | refusée                     |

Toutes les stations de test utilisent le mot de passe `Station@2026`.

## 2. Backend (port 8081)

La configuration (`backend/src/main/resources/application.properties`) ne contient aucun secret :
tout est lu dans des variables d'environnement, listées dans `backend/.env.example`.

| Variable          | Rôle                                               | Défaut                                   |
|-------------------|----------------------------------------------------|------------------------------------------|
| `DB_URL`          | URL JDBC                                           | `jdbc:mysql://localhost:3306/washmycar…` |
| `DB_USERNAME`     | utilisateur MySQL                                  | `root`                                   |
| `DB_PASSWORD`     | mot de passe MySQL                                 | *(vide)*                                 |
| `JWT_SECRET`      | clé de signature JWT (≥ 32 caractères)             | clé aléatoire temporaire + avertissement |
| `APP_PUBLIC_URL`  | URL utilisée dans le lien de validation par email  | `http://localhost:8081`                  |
| `CORS_ORIGINS`    | origines autorisées pour le front                  | `http://localhost:5173`                  |
| `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD` | SMTP | vide = emails écrits dans les logs |
| `ADMIN_EMAIL`, `ADMIN_PASSWORD` | crée un admin au démarrage (optionnel) | —                             |

PowerShell :

```powershell
cd backend
$env:DB_PASSWORD = "votre_mot_de_passe"
$env:JWT_SECRET  = "une-longue-chaine-aleatoire-de-32-caracteres-minimum"
.\mvnw.cmd package -DskipTests
java -jar target\washmycar-api-1.0.0.jar
```

Bash :

```bash
cd backend
export DB_PASSWORD='votre_mot_de_passe' JWT_SECRET='une-longue-chaine-aleatoire-de-32-caracteres-minimum'
./mvnw package -DskipTests && java -jar target/washmycar-api-1.0.0.jar
```

> `mvnw spring-boot:run` fonctionne aussi, sauf si le chemin du projet contient des caractères
> accentués (ex. « 3 éme ») : sous Windows, le plugin ne trouve alors pas la classe principale.
> Dans ce cas, utiliser `java -jar` comme ci-dessus.

- **Swagger UI** : http://localhost:8081/swagger-ui.html. Se connecter via `POST /api/auth/login`,
  puis coller le jeton dans le bouton **Authorize**.
- **Emails** : sans SMTP configuré, le lien de validation du compte client apparaît dans la console du backend.
  Avec Gmail : `MAIL_HOST=smtp.gmail.com`, `MAIL_PORT=587` et un « mot de passe d'application ».
- **Tests de bout en bout** : après avoir réinitialisé la base, avec le backend lancé et sa sortie
  redirigée dans un fichier :
  `python backend/scripts/smoke_test.py chemin/vers/backend.log` (72 vérifications).

### Principales routes

| Préfixe          | Accès           | Contenu                                                          |
|------------------|-----------------|------------------------------------------------------------------|
| `/api/auth`      | public          | inscription client/station, validation email, connexion          |
| `/api/stations`  | public (GET)    | stations proches (Haversine), fiche, avis, créneaux occupés      |
| `/api/profil`    | connecté        | consulter / modifier son profil                                  |
| `/api/client`    | CLIENT          | réserver, mes réservations, annuler, évaluer                     |
| `/api/station`   | STATION         | profil, horaires, tarifs, réservations reçues, avis              |
| `/api/admin`     | ADMIN           | tableau de bord, CRUD stations / clients, réservations, avis     |

### Règles métier

- Un créneau doit tenir dans les horaires du jour. Il ne doit chevaucher aucune réservation
  *en attente* ou *acceptée* de la même station : un verrou sur la station empêche deux réservations simultanées.
- La fin du créneau est calculée à partir de la durée du tarif choisi.
- Une station non validée n'apparaît pas dans les recherches et ne peut pas se connecter.
- Un client ne se connecte qu'après avoir validé son email. Il ne peut évaluer qu'une station où il a une
  réservation acceptée (une évaluation par station, modifiable). La note moyenne est recalculée à chaque
  ajout ou suppression d'avis.

## 3. Web admin (port 5173)

```bash
cd web-admin
npm install
npm run dev        # http://localhost:5173
npm run build      # version de production dans dist/
```

L'URL de l'API se règle avec `VITE_API_URL` (voir `web-admin/.env.example`). Seuls les comptes ADMIN peuvent se connecter.

## 4. Application mobile Android

Kotlin, Jetpack Compose, MVVM, Hilt, Retrofit/OkHttp, Coroutines/Flow, Navigation Compose,
carte OpenStreetMap (osmdroid, sans clé API), FusedLocationProvider. Le jeton JWT est chiffré
avec une clé AES de l'Android Keystore, puis stocké dans DataStore.

### Lancer l'application

1. Démarrer le backend (section 2).
2. Android Studio : **File > Open**, puis choisir le dossier `mobile-android`. Attendre la fin de la synchronisation Gradle.
3. Choisir un appareil et cliquer sur **Run ▶**.

En ligne de commande, avec un émulateur ou un téléphone branché :

```powershell
cd mobile-android
.\gradlew.bat installDebug
```

### Adresse de l'API

| Appareil              | Réglage                                                                         |
|-----------------------|---------------------------------------------------------------------------------|
| Émulateur             | rien à faire : `http://10.0.2.2:8081/` (= le PC vu depuis l'émulateur)          |
| Téléphone réel (Wi-Fi) | dans `mobile-android/local.properties`, ajouter `washmycar.apiUrl=http://IP_DU_PC:8081/` |

Sur un téléphone réel : le PC et le téléphone doivent être sur le même réseau Wi-Fi, et le port 8081
doit être autorisé dans le pare-feu Windows. L'adresse IP du PC s'obtient avec `ipconfig`.
Le HTTP non chiffré n'est autorisé que dans la version debug.

### Position GPS sur l'émulateur

L'émulateur se place par défaut en Californie : l'application cherche alors autour de Tunis et
l'indique à l'écran. Pour simuler une position en Tunisie : **⋮ > Location** dans la barre de
l'émulateur, ou la commande `adb emu geo fix 10.1815 36.8065` (longitude, puis latitude).

### Parcours

- **Client** : inscription (avec email de validation) ; stations proches en liste ou sur carte, avec un
  rayon réglable ; fiche station (carte, horaires, tarifs, avis) ; réservation (type de véhicule,
  service, jour, créneau libre) ; mes réservations (annulation, évaluation) ; profil.
- **Station** : inscription (en attente de validation par l'admin) ; agenda des réservations reçues
  (accepter, refuser, annuler) ; horaires ; tarifs ; avis ; informations et emplacement de la station ; compte.

Les heures sont toujours calculées en heure de Tunisie, même si le téléphone est réglé sur un autre fuseau.
