# Usage : python backend/scripts/smoke_test.py <chemin du log du backend>
# Prérequis : base réinitialisée avec database/schema.sql + data.sql, backend lancé sur :8081
"""Tests de bout en bout de l'API Wash My Car (backend lancé sur :8081 avec data.sql)."""
import datetime as dt
import json
import re
import sys
import urllib.error
import urllib.request

BASE = "http://localhost:8081"
LOG = sys.argv[1]
results = []


def call(method, path, body=None, token=None):
    req = urllib.request.Request(BASE + path, method=method)
    req.add_header("Content-Type", "application/json")
    if token:
        req.add_header("Authorization", "Bearer " + token)
    data = json.dumps(body).encode() if body is not None else None
    try:
        with urllib.request.urlopen(req, data) as r:
            raw = r.read().decode()
            return r.status, (json.loads(raw) if raw and raw.lstrip()[0] in "[{" else raw)
    except urllib.error.HTTPError as e:
        raw = e.read().decode()
        try:
            return e.code, json.loads(raw)
        except Exception:
            return e.code, raw


def check(name, cond, info=""):
    cond = bool(cond)
    results.append(cond)
    print(("OK   " if cond else "FAIL ") + name + ("" if cond else f"  -> {info}"))


def login(email, pwd):
    return call("POST", "/api/auth/login", {"email": email, "motDePasse": pwd})


# ---------------------------------------------------------------- connexion
s, admin = login("admin@washmycar.tn", "Admin@2026"); check("login admin", s == 200, admin)
s, sami = login("sami.benali@mail.tn", "Client@2026"); check("login client", s == 200, sami)
s, st1 = login("contact@lavage-centre.tn", "Station@2026"); check("login station validée", s == 200 and st1["stationId"] == 1, st1)
A, C, S = admin["token"], sami["token"], st1["token"]
s, b = login("sami.benali@mail.tn", "mauvais"); check("mauvais mot de passe -> 401", s == 401, b)
s, b = login("karim.jaziri@mail.tn", "Client@2026"); check("email non validé -> 403", s == 403, b)
s, b = login("contact@benarous-lavage.tn", "Station@2026"); check("station en attente -> 403", s == 403 and "attente" in b["message"], b)
s, b = login("contact@bizerte-wash.tn", "Station@2026"); check("station refusée -> 403", s == 403, b)

# ---------------------------------------------------------------- sécurité
s, b = call("GET", "/api/admin/dashboard"); check("sans jeton -> 401", s == 401, b)
s, b = call("GET", "/api/admin/dashboard", token=C); check("client sur /admin -> 403", s == 403, b)
s, b = call("GET", "/api/client/reservations", token=S); check("station sur /client -> 403", s == 403, b)
s, b = call("GET", "/v3/api-docs"); check("OpenAPI disponible", s == 200 and "paths" in b, s)

# ---------------------------------------------------------------- recherche
s, b = call("GET", "/api/stations/proches?lat=36.8065&lng=10.1815&rayonKm=15")
ids = [x["id"] for x in b] if s == 200 else []
dists = [x["distanceKm"] for x in b] if s == 200 else []
check("proches : triées par distance", s == 200 and dists == sorted(dists) and ids[0] == 1, b)
check("proches : station en attente exclue", 7 not in ids and 5 not in ids, ids)
s, b = call("GET", "/api/stations/proches?lat=35.8256&lng=10.636&rayonKm=5")
check("proches : Sousse seulement", s == 200 and [x["id"] for x in b] == [5], b)
s, b = call("GET", "/api/stations/1"); check("détail station", s == 200 and len(b["horaires"]) == 7 and b["tarifs"], b)
s, b = call("GET", "/api/stations/7"); check("détail station non validée -> 404", s == 404, b)
s, b = call("GET", "/api/stations/1/evaluations"); check("évaluations publiques", s == 200 and len(b) == 2, b)

# ---------------------------------------------------------------- réservations
today = dt.date.today()
lundi = today + dt.timedelta(days=7 - today.weekday())
dimanche = lundi + dt.timedelta(days=6)
s, b = call("GET", f"/api/stations/1/creneaux-occupes?date={lundi}"); check("créneaux occupés", s == 200 and len(b) == 2, b)
tarifs = {(t["typeVehicule"], t["service"]): t for t in call("GET", "/api/stations/1")[1]["tarifs"]}
complet_citadine = tarifs[("CITADINE", "Complet")]["id"]  # 60 min

s, b = call("POST", "/api/client/reservations", {"stationId": 1, "tarifId": complet_citadine, "dateDebut": f"{lundi}T09:30:00"}, C)
check("chevauchement -> 409", s == 409, b)
ext_citadine = tarifs[("CITADINE", "Extérieur")]["id"]  # 30 min
s, b = call("POST", "/api/client/reservations", {"stationId": 1, "tarifId": ext_citadine, "dateDebut": f"{lundi}T10:15:00"}, C)
check("créneau entre deux réservations (bornes) -> 201", s == 201 and b["dateFin"].startswith(f"{lundi}T10:45"), b)
s, b = call("POST", "/api/client/reservations", {"stationId": 1, "tarifId": ext_citadine, "dateDebut": f"{lundi}T10:45:00"}, C)
check("créneau collé jusqu'à 11:15 chevauche 11:00 -> 409", s == 409, b)
s, b = call("POST", "/api/client/reservations", {"stationId": 1, "tarifId": complet_citadine, "dateDebut": f"{lundi}T14:00:00"}, C)
check("réservation -> 201 EN_ATTENTE", s == 201 and b["statut"] == "EN_ATTENTE" and b["prix"] == 20.0, b)
new_id = b.get("id")
s, b = call("POST", "/api/client/reservations", {"stationId": 1, "tarifId": complet_citadine, "dateDebut": f"{lundi}T17:30:00"}, C)
check("dépassement de fermeture -> 400", s == 400, b)
s, b = call("POST", "/api/client/reservations", {"stationId": 1, "tarifId": complet_citadine, "dateDebut": f"{dimanche}T10:00:00"}, C)
check("jour fermé -> 400", s == 400, b)
s, b = call("POST", "/api/client/reservations", {"stationId": 2, "tarifId": complet_citadine, "dateDebut": f"{lundi}T10:00:00"}, C)
check("tarif d'une autre station -> 400", s == 400, b)
s, b = call("POST", "/api/client/reservations", {"stationId": 1, "tarifId": complet_citadine, "dateDebut": "2020-01-01T10:00:00"}, C)
check("date passée -> 400 validation", s == 400 and "dateDebut" in b.get("erreurs", {}), b)

s, b = call("GET", "/api/station/reservations?statut=EN_ATTENTE", token=S); check("station : réservations en attente", s == 200 and any(r["id"] == new_id for r in b), b)
s, b = call("PATCH", f"/api/station/reservations/{new_id}/statut", {"statut": "ACCEPTEE"}, S); check("station accepte", s == 200 and b["statut"] == "ACCEPTEE", b)
s, b = call("PATCH", f"/api/station/reservations/{new_id}/statut", {"statut": "REFUSEE"}, S); check("transition ACCEPTEE->REFUSEE interdite", s == 400, b)
s, b = call("PATCH", f"/api/client/reservations/{new_id}/annuler", token=C); check("client annule", s == 200 and b["statut"] == "ANNULEE", b)
s, b = call("GET", "/api/client/reservations", token=C); check("mes réservations", s == 200 and len(b) >= 4, b)

# ---------------------------------------------------------------- évaluations
s, b = call("POST", "/api/client/evaluations", {"stationId": 3, "note": 5}, C); check("évaluer sans réservation acceptée -> 403", s == 403, b)
s, b = call("POST", "/api/client/evaluations", {"stationId": 1, "note": 3, "commentaire": "Moyen cette fois"}, C)
check("évaluation mise à jour", s == 200 and b["note"] == 3, b)
s, b = call("GET", "/api/stations/1"); check("note moyenne recalculée (3+4)/2 = 3.5", b["noteMoyenne"] == 3.5, b["noteMoyenne"])
s, b = call("POST", "/api/client/evaluations", {"stationId": 1, "note": 9}, C); check("note hors 1..5 -> 400", s == 400, b)

# ---------------------------------------------------------------- espace station
s, b = call("PUT", "/api/station/horaires", {"horaires": [{"jourSemaine": "DIMANCHE", "heureOuverture": "09:00", "heureFermeture": "12:00", "ferme": False}]}, S)
check("station ouvre le dimanche", s == 200 and any(h["jourSemaine"] == "DIMANCHE" and not h["ferme"] for h in b), b)
s, b = call("PUT", "/api/station/horaires", {"horaires": [{"jourSemaine": "LUNDI", "heureOuverture": "18:00", "heureFermeture": "08:00", "ferme": False}]}, S)
check("horaires incohérents -> 400", s == 400, b)
s, b = call("POST", "/api/station/tarifs", {"typeVehicule": "UTILITAIRE", "service": "Complet", "prix": 35.5, "dureeMinutes": 90}, S)
check("ajout tarif", s == 201, b); tid = b.get("id")
s, b = call("POST", "/api/station/tarifs", {"typeVehicule": "UTILITAIRE", "service": "complet", "prix": 30, "dureeMinutes": 90}, S)
check("tarif en double -> 409", s == 409, b)
s, b = call("PUT", f"/api/station/tarifs/{tid}", {"typeVehicule": "UTILITAIRE", "service": "Complet", "prix": 38.250, "dureeMinutes": 80}, S)
check("modification tarif", s == 200 and b["prix"] == 38.25, b)
s, b = call("DELETE", f"/api/station/tarifs/{tid}", token=S); check("suppression tarif", s == 204, b)
s, b = call("GET", "/api/station/evaluations", token=S); check("station : évaluations reçues", s == 200 and len(b) == 2, b)
s, b = call("GET", "/api/station/profil", token=S); check("station : profil", s == 200 and b["nom"] == "Lavage Centre Ville", b)

# ---------------------------------------------------------------- inscription + validation email
s, b = call("POST", "/api/auth/register/client", {"nom": "Test", "prenom": "Nour", "email": "nour.test@mail.tn", "motDePasse": "Secret123", "telephone": "23456789"})
check("inscription client", s == 201, b)
s, b = call("POST", "/api/auth/register/client", {"nom": "Test", "prenom": "Nour", "email": "NOUR.test@mail.tn", "motDePasse": "Secret123"})
check("email déjà utilisé -> 409", s == 409, b)
s, b = call("POST", "/api/auth/register/client", {"nom": "", "prenom": "Nour", "email": "pas-un-email", "motDePasse": "123"})
check("validation -> 400 avec détails", s == 400 and {"nom", "email", "motDePasse"} <= set(b.get("erreurs", {})), b)
s, b = login("nour.test@mail.tn", "Secret123"); check("connexion avant validation -> 403", s == 403, b)
with open(LOG, encoding="utf-8", errors="replace") as f:
    tokens = re.findall(r"/api/auth/verify\?token=([0-9a-f-]{36})", f.read())
check("lien de validation écrit dans les logs", bool(tokens), "aucun lien")
if tokens:
    s, b = call("GET", "/api/auth/verify?token=" + tokens[-1]); check("clic sur le lien", s == 200 and "Votre compte est validé" in b, b)
s, b = login("nour.test@mail.tn", "Secret123"); check("connexion après validation", s == 200, b)
s, b = call("PUT", "/api/profil", {"nom": "Testeur", "prenom": "Nour", "telephone": "", "ancienMotDePasse": "faux", "nouveauMotDePasse": "Nouveau123"}, b.get("token"))
check("profil : mauvais ancien mot de passe -> 400", s == 400, b)

s, b = call("POST", "/api/auth/register/station", {"nom": "Saidi", "prenom": "Omar", "email": "omar@nabeul-wash.tn", "motDePasse": "Station123",
            "station": {"nom": "Nabeul Wash", "adresse": "Av. Habib Thameur", "ville": "Nabeul", "latitude": 36.4561, "longitude": 10.7376}})
check("inscription station", s == 201, b)
s, b = login("omar@nabeul-wash.tn", "Station123"); check("station non validée ne peut pas se connecter", s == 403, b)

# ---------------------------------------------------------------- administration
s, b = call("GET", "/api/admin/dashboard", token=A); check("dashboard", s == 200 and b["stationsEnAttente"] == 2, b)
s, b = call("GET", "/api/admin/stations?statut=EN_ATTENTE&q=nabeul", token=A); check("recherche stations", s == 200 and b["totalElements"] == 1, b)
nabeul = b["content"][0]["id"] if s == 200 and b["content"] else None
s, b = call("PATCH", f"/api/admin/stations/{nabeul}/statut", {"statut": "VALIDEE"}, A); check("validation station", s == 200 and b["statutInscription"] == "VALIDEE", b)
s, b = login("omar@nabeul-wash.tn", "Station123"); check("station validée peut se connecter", s == 200, b)
s, b = call("GET", "/api/station/horaires", token=b.get("token")); check("horaires par défaut créés", s == 200 and len(b) == 7, b)
s, b = call("PUT", f"/api/admin/stations/{nabeul}", {"nom": "Nabeul Wash Pro", "adresse": "Av. Habib Thameur", "ville": "Nabeul", "latitude": 36.4561, "longitude": 10.7376}, A)
check("admin modifie station", s == 200 and b["nom"] == "Nabeul Wash Pro", b)
s, b = call("POST", "/api/admin/stations", {"nom": "Ayari", "prenom": "Sana", "email": "sana@monastir-wash.tn", "motDePasse": "Station123",
            "station": {"nom": "Monastir Wash", "adresse": "Route de la plage", "ville": "Monastir", "latitude": 35.7643, "longitude": 10.8113}}, A)
check("admin crée station (validée)", s == 201 and b["statutInscription"] == "VALIDEE", b); mon = b.get("id")
s, b = call("DELETE", f"/api/admin/stations/{mon}", token=A); check("admin supprime station", s == 204, b)
s, b = call("GET", f"/api/admin/stations/{mon}", token=A); check("station supprimée -> 404", s == 404, b)

s, b = call("POST", "/api/admin/clients", {"nom": "Admin", "prenom": "Cree", "email": "cree@mail.tn", "motDePasse": "Client123"}, A)
check("admin crée client", s == 201 and b["emailVerifie"], b); cid = b.get("id")
s, b = call("PUT", f"/api/admin/clients/{cid}", {"nom": "Modifie", "prenom": "Cree", "email": "cree2@mail.tn", "telephone": "29999999"}, A)
check("admin modifie client", s == 200 and b["email"] == "cree2@mail.tn", b)
s, b = call("GET", "/api/admin/clients?q=modifie", token=A); check("recherche clients", s == 200 and b["totalElements"] == 1, b)
s, b = call("DELETE", f"/api/admin/clients/{cid}", token=A); check("admin supprime client", s == 204, b)
s, b = call("DELETE", "/api/admin/clients/1", token=A); check("supprimer un admin via /clients -> 404", s == 404, b)

s, b = call("GET", "/api/admin/reservations?statut=EN_ATTENTE", token=A); check("admin liste réservations", s == 200 and b["totalElements"] >= 2, b)
rid = b["content"][0]["id"]
s, b = call("PATCH", f"/api/admin/reservations/{rid}/statut", {"statut": "REFUSEE"}, A); check("admin refuse réservation", s == 200 and b["statut"] == "REFUSEE", b)
s, b = call("GET", "/api/admin/evaluations?stationId=1", token=A); check("admin liste évaluations", s == 200 and b["totalElements"] == 2, b)
eid = [e for e in b["content"] if e["note"] == 3][0]["id"]
s, b = call("DELETE", f"/api/admin/evaluations/{eid}", token=A); check("admin supprime évaluation", s == 204, b)
s, b = call("GET", "/api/stations/1"); check("note moyenne recalculée après suppression = 4.0", b["noteMoyenne"] == 4.0, b["noteMoyenne"])

print(f"\n{sum(results)}/{len(results)} tests OK")
sys.exit(0 if all(results) else 1)
