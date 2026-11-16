-- =====================================================================
--  Wash My Car - Jeu de données de test (à exécuter APRÈS schema.sql)
--
--  Comptes de test (mots de passe à ne JAMAIS réutiliser en production) :
--    Admin    : admin@washmycar.tn          / Admin@2026
--    Clients  : sami.benali@mail.tn         / Client@2026   (compte validé)
--               ines.trabelsi@mail.tn       / Client@2026   (compte validé)
--               karim.jaziri@mail.tn        / Client@2026   (email NON validé)
--    Stations : contact@lavage-centre.tn    / Station@2026  (validée)
--               (toutes les stations utilisent Station@2026 ;
--                ben-arous = en attente, bizerte = refusée)
-- =====================================================================
USE washmycar;

SET @admin   = '$2a$10$oQLHEy4wdLpeb6orNiOPX.h2qEtDcYVj/CLoFQZUH6xObcmIPlycy';
SET @client  = '$2a$10$/PQ.JlxSNNnhIf1VGGd4tO5JFGiZgdDoaA.Ciniv12NjqY11iVTd2';
SET @station = '$2a$10$UilLfzq9usAqhsEVV7.TquP9Bi1bnY3mEUcNqkC0fBMBOLuFJIrVu';

-- ---------------------------------------------------------------------
-- Utilisateurs
-- ---------------------------------------------------------------------
INSERT INTO users (id, nom, prenom, email, mot_de_passe, telephone, role, email_verifie, token_verification, token_expiration) VALUES
  (1,  'Admin',     'Wash My Car', 'admin@washmycar.tn',        @admin,   '71000000', 'ADMIN',   TRUE,  NULL, NULL),
  (2,  'Ben Ali',   'Sami',        'sami.benali@mail.tn',       @client,  '22123456', 'CLIENT',  TRUE,  NULL, NULL),
  (3,  'Trabelsi',  'Ines',        'ines.trabelsi@mail.tn',     @client,  '55234567', 'CLIENT',  TRUE,  NULL, NULL),
  (4,  'Jaziri',    'Karim',       'karim.jaziri@mail.tn',      @client,  '98345678', 'CLIENT',  FALSE,
       'demo-token-karim-jaziri', DATE_ADD(NOW(), INTERVAL 1 DAY)),
  (5,  'Hammami',   'Mohamed',     'contact@lavage-centre.tn',  @station, '71234567', 'STATION', TRUE,  NULL, NULL),
  (6,  'Gharbi',    'Leila',       'contact@lac-wash.tn',       @station, '71345678', 'STATION', TRUE,  NULL, NULL),
  (7,  'Bouzid',    'Nizar',       'contact@menzah-clean.tn',   @station, '71456789', 'STATION', TRUE,  NULL, NULL),
  (8,  'Chaabane',  'Amira',       'contact@marsa-shine.tn',    @station, '71567890', 'STATION', TRUE,  NULL, NULL),
  (9,  'Mejri',     'Hatem',       'contact@sousse-wash.tn',    @station, '73123456', 'STATION', TRUE,  NULL, NULL),
  (10, 'Kammoun',   'Fatma',       'contact@sfax-auto.tn',      @station, '74123456', 'STATION', TRUE,  NULL, NULL),
  (11, 'Riahi',     'Walid',       'contact@benarous-lavage.tn',@station, '79123456', 'STATION', TRUE,  NULL, NULL),
  (12, 'Mansouri',  'Sonia',       'contact@bizerte-wash.tn',   @station, '72123456', 'STATION', TRUE,  NULL, NULL);

-- ---------------------------------------------------------------------
-- Stations
-- ---------------------------------------------------------------------
INSERT INTO stations (id, user_id, nom, adresse, ville, latitude, longitude, telephone, description, photo_url, statut_inscription) VALUES
  (1, 5,  'Lavage Centre Ville',  '45 Avenue Habib Bourguiba',      'Tunis',     36.800800, 10.180000, '71234567',
      'Lavage manuel et haute pression en plein centre-ville.', 'https://images.unsplash.com/photo-1520340356584-f9917d1eea6f', 'VALIDEE'),
  (2, 6,  'Lac Wash Premium',     'Rue du Lac Léman, Les Berges du Lac', 'Tunis',  36.838000, 10.240000, '71345678',
      'Lavage premium, nettoyage intérieur et lustrage.', 'https://images.unsplash.com/photo-1607860108855-64acf2078ed9', 'VALIDEE'),
  (3, 7,  'Menzah Clean',         '12 Rue Ibn Khaldoun, El Menzah 6', 'Ariana',    36.845000, 10.170000, '71456789',
      'Station de quartier, rapide et économique.', NULL, 'VALIDEE'),
  (4, 8,  'Marsa Shine',          'Avenue Taïeb Mhiri',             'La Marsa',  36.878000, 10.325000, '71567890',
      'Lavage écologique sans eau et detailing.', NULL, 'VALIDEE'),
  (5, 9,  'Sousse Auto Wash',     'Boulevard du 14 Janvier',        'Sousse',    35.825600, 10.636000, '73123456',
      'Portique automatique et lavage manuel.', NULL, 'VALIDEE'),
  (6, 10, 'Sfax Auto Clean',      'Route de Tunis km 2',            'Sfax',      34.740600, 10.760300, '74123456',
      'Spécialiste des véhicules utilitaires.', NULL, 'VALIDEE'),
  (7, 11, 'Ben Arous Lavage',     'Avenue de France',               'Ben Arous', 36.753000, 10.228000, '79123456',
      'Nouvelle station en attente de validation.', NULL, 'EN_ATTENTE'),
  (8, 12, 'Bizerte Wash',         'Corniche de Bizerte',            'Bizerte',   37.274400,  9.873900, '72123456',
      'Inscription refusée (dossier incomplet).', NULL, 'REFUSEE');

-- ---------------------------------------------------------------------
-- Horaires : lundi-samedi 08:00-18:00, dimanche fermé (Lac Wash : 09:00-20:00, ouvert le dimanche matin)
-- ---------------------------------------------------------------------
INSERT INTO horaires (station_id, jour_semaine, heure_ouverture, heure_fermeture, ferme)
SELECT s.id, j.jour,
       IF(j.jour = 'DIMANCHE', NULL, '08:00:00'),
       IF(j.jour = 'DIMANCHE', NULL, '18:00:00'),
       j.jour = 'DIMANCHE'
FROM stations s
CROSS JOIN (SELECT 'LUNDI' AS jour UNION ALL SELECT 'MARDI' UNION ALL SELECT 'MERCREDI' UNION ALL SELECT 'JEUDI'
            UNION ALL SELECT 'VENDREDI' UNION ALL SELECT 'SAMEDI' UNION ALL SELECT 'DIMANCHE') j
WHERE s.id <> 2;

INSERT INTO horaires (station_id, jour_semaine, heure_ouverture, heure_fermeture, ferme) VALUES
  (2, 'LUNDI',    '09:00:00', '20:00:00', FALSE),
  (2, 'MARDI',    '09:00:00', '20:00:00', FALSE),
  (2, 'MERCREDI', '09:00:00', '20:00:00', FALSE),
  (2, 'JEUDI',    '09:00:00', '20:00:00', FALSE),
  (2, 'VENDREDI', '09:00:00', '20:00:00', FALSE),
  (2, 'SAMEDI',   '09:00:00', '20:00:00', FALSE),
  (2, 'DIMANCHE', '09:00:00', '13:00:00', FALSE);

-- ---------------------------------------------------------------------
-- Tarifs (TND)
-- ---------------------------------------------------------------------
INSERT INTO tarifs (station_id, type_vehicule, service, prix, duree_minutes) VALUES
  (1, 'CITADINE',   'Extérieur',          12.000, 30),
  (1, 'CITADINE',   'Complet',            20.000, 60),
  (1, 'BERLINE',    'Extérieur',          15.000, 30),
  (1, 'BERLINE',    'Complet',            25.000, 60),
  (1, 'SUV',        'Extérieur',          18.000, 40),
  (1, 'SUV',        'Complet',            30.000, 75),
  (1, 'MOTO',       'Extérieur',           8.000, 20),
  (2, 'CITADINE',   'Complet',            35.000, 60),
  (2, 'BERLINE',    'Complet',            40.000, 75),
  (2, 'BERLINE',    'Lustrage',           90.000, 150),
  (2, 'SUV',        'Complet',            50.000, 90),
  (2, 'SUV',        'Lustrage',          120.000, 180),
  (3, 'CITADINE',   'Extérieur',          10.000, 20),
  (3, 'CITADINE',   'Intérieur',          12.000, 30),
  (3, 'BERLINE',    'Extérieur',          12.000, 25),
  (3, 'UTILITAIRE', 'Extérieur',          20.000, 40),
  (4, 'CITADINE',   'Sans eau',           25.000, 45),
  (4, 'BERLINE',    'Sans eau',           30.000, 50),
  (4, 'SUV',        'Detailing intérieur', 80.000, 120),
  (5, 'CITADINE',   'Portique',            7.000, 15),
  (5, 'BERLINE',    'Portique',            8.000, 15),
  (5, 'SUV',        'Complet',            28.000, 60),
  (6, 'UTILITAIRE', 'Extérieur',          25.000, 45),
  (6, 'UTILITAIRE', 'Complet',            40.000, 90),
  (6, 'BERLINE',    'Complet',            22.000, 60),
  (7, 'CITADINE',   'Extérieur',          10.000, 30),
  (8, 'CITADINE',   'Extérieur',          10.000, 30);

-- ---------------------------------------------------------------------
-- Réservations : passées (pour les évaluations) et à venir (lundi prochain)
-- ---------------------------------------------------------------------
SET @lundi_prochain = DATE_ADD(CURDATE(), INTERVAL (7 - WEEKDAY(CURDATE())) DAY);
SET @lundi_passe    = DATE_SUB(CURDATE(), INTERVAL (WEEKDAY(CURDATE()) + 7) DAY);

INSERT INTO reservations (client_id, station_id, type_vehicule, service, date_debut, date_fin, statut, prix) VALUES
  -- historique (semaine dernière)
  (2, 1, 'BERLINE',  'Complet',   TIMESTAMP(@lundi_passe, '10:00:00'), TIMESTAMP(@lundi_passe, '11:00:00'), 'ACCEPTEE', 25.000),
  (2, 2, 'BERLINE',  'Complet',   TIMESTAMP(@lundi_passe, '14:00:00'), TIMESTAMP(@lundi_passe, '15:15:00'), 'ACCEPTEE', 40.000),
  (3, 1, 'CITADINE', 'Extérieur', TIMESTAMP(DATE_ADD(@lundi_passe, INTERVAL 1 DAY), '09:00:00'),
                                  TIMESTAMP(DATE_ADD(@lundi_passe, INTERVAL 1 DAY), '09:30:00'), 'ACCEPTEE', 12.000),
  (3, 3, 'CITADINE', 'Intérieur', TIMESTAMP(DATE_ADD(@lundi_passe, INTERVAL 2 DAY), '16:00:00'),
                                  TIMESTAMP(DATE_ADD(@lundi_passe, INTERVAL 2 DAY), '16:30:00'), 'REFUSEE',  12.000),
  -- à venir
  (2, 1, 'SUV',      'Complet',   TIMESTAMP(@lundi_prochain, '09:00:00'), TIMESTAMP(@lundi_prochain, '10:15:00'), 'EN_ATTENTE', 30.000),
  (3, 1, 'CITADINE', 'Complet',   TIMESTAMP(@lundi_prochain, '11:00:00'), TIMESTAMP(@lundi_prochain, '12:00:00'), 'ACCEPTEE',   20.000),
  (3, 4, 'BERLINE',  'Sans eau',  TIMESTAMP(DATE_ADD(@lundi_prochain, INTERVAL 1 DAY), '10:00:00'),
                                  TIMESTAMP(DATE_ADD(@lundi_prochain, INTERVAL 1 DAY), '10:50:00'), 'EN_ATTENTE', 30.000),
  (2, 5, 'CITADINE', 'Portique',  TIMESTAMP(DATE_ADD(@lundi_prochain, INTERVAL 2 DAY), '15:00:00'),
                                  TIMESTAMP(DATE_ADD(@lundi_prochain, INTERVAL 2 DAY), '15:15:00'), 'ANNULEE',     7.000);

-- ---------------------------------------------------------------------
-- Évaluations (uniquement sur des stations avec réservation acceptée)
-- ---------------------------------------------------------------------
INSERT INTO evaluations (client_id, station_id, note, commentaire, date_creation) VALUES
  (2, 1, 5, 'Travail impeccable et personnel très accueillant.', DATE_SUB(NOW(), INTERVAL 5 DAY)),
  (3, 1, 4, 'Rapide et efficace, un peu d''attente.',           DATE_SUB(NOW(), INTERVAL 4 DAY)),
  (2, 2, 4, 'Très bon résultat, prix un peu élevé.',             DATE_SUB(NOW(), INTERVAL 3 DAY));

-- Note moyenne calculée à partir des évaluations
UPDATE stations s
SET s.note_moyenne = COALESCE((SELECT ROUND(AVG(e.note), 1) FROM evaluations e WHERE e.station_id = s.id), 0.0)
WHERE s.id > 0; -- clause sur la clé : compatible avec le « safe update mode » de Workbench
