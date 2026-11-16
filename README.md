# Wash My Car

- `backend/` : API Spring Boot + MySQL (port 8081). Lancer : `mvn spring-boot:run`
- `web-admin/` : interface admin React/Vite. Lancer : `npm install` puis `npm run dev`
- `mobile-android/` : application Android (Kotlin + Compose), à ouvrir avec Android Studio

## Remarques
- Créer la base MySQL locale (user/mot de passe dans `backend/src/main/resources/application.properties`).
- Dans `mobile-android/.../data/Api.kt`, remplacer l'IP `192.168.81.177` par l'IP de ton PC pour tester sur téléphone réel.
- Compte admin initial : voir `app.admin.*` dans `application.properties`.
