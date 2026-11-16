-- =====================================================================
--  Wash My Car - Schéma MySQL 8
--  Import : MySQL Workbench > File > Run SQL Script (ou Server > Data Import)
--  ATTENTION : ce script recrée les tables (les données existantes sont perdues).
-- =====================================================================

CREATE DATABASE IF NOT EXISTS washmycar
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE washmycar;

SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS evaluations;
DROP TABLE IF EXISTS reservations;
DROP TABLE IF EXISTS tarifs;
DROP TABLE IF EXISTS horaires;
DROP TABLE IF EXISTS stations;
DROP TABLE IF EXISTS users;
SET FOREIGN_KEY_CHECKS = 1;

-- ---------------------------------------------------------------------
-- Utilisateurs (administrateurs, clients et comptes station)
-- ---------------------------------------------------------------------
CREATE TABLE users (
  id                 BIGINT        NOT NULL AUTO_INCREMENT,
  nom                VARCHAR(100)  NOT NULL,
  prenom             VARCHAR(100)  NOT NULL,
  email              VARCHAR(150)  NOT NULL,
  mot_de_passe       VARCHAR(100)  NOT NULL COMMENT 'Hash BCrypt',
  telephone          VARCHAR(20)   NULL,
  role               VARCHAR(20)   NOT NULL,
  email_verifie      BOOLEAN       NOT NULL DEFAULT FALSE,
  token_verification VARCHAR(100)  NULL,
  token_expiration   DATETIME      NULL,
  date_creation      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  CONSTRAINT uq_users_email UNIQUE (email),
  CONSTRAINT uq_users_token UNIQUE (token_verification),
  CONSTRAINT ck_users_role CHECK (role IN ('ADMIN', 'CLIENT', 'STATION'))
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- Stations de lavage (1 compte STATION <-> 1 station)
-- ---------------------------------------------------------------------
CREATE TABLE stations (
  id                 BIGINT        NOT NULL AUTO_INCREMENT,
  user_id            BIGINT        NOT NULL,
  nom                VARCHAR(150)  NOT NULL,
  adresse            VARCHAR(255)  NOT NULL,
  ville              VARCHAR(100)  NOT NULL,
  latitude           DOUBLE        NOT NULL,
  longitude          DOUBLE        NOT NULL,
  telephone          VARCHAR(20)   NULL,
  description        VARCHAR(1000) NULL,
  photo_url          VARCHAR(500)  NULL,
  statut_inscription VARCHAR(20)   NOT NULL DEFAULT 'EN_ATTENTE',
  note_moyenne       DECIMAL(2,1)  NOT NULL DEFAULT 0.0,
  PRIMARY KEY (id),
  CONSTRAINT uq_stations_user UNIQUE (user_id),
  CONSTRAINT fk_stations_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
  CONSTRAINT ck_stations_statut CHECK (statut_inscription IN ('EN_ATTENTE', 'VALIDEE', 'REFUSEE')),
  CONSTRAINT ck_stations_lat CHECK (latitude BETWEEN -90 AND 90),
  CONSTRAINT ck_stations_lng CHECK (longitude BETWEEN -180 AND 180),
  INDEX idx_stations_lat_lng (latitude, longitude),
  INDEX idx_stations_statut (statut_inscription)
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- Horaires d'ouverture (une ligne par jour de la semaine)
-- ---------------------------------------------------------------------
CREATE TABLE horaires (
  id               BIGINT      NOT NULL AUTO_INCREMENT,
  station_id       BIGINT      NOT NULL,
  jour_semaine     VARCHAR(10) NOT NULL,
  heure_ouverture  TIME        NULL,
  heure_fermeture  TIME        NULL,
  ferme            BOOLEAN     NOT NULL DEFAULT FALSE,
  PRIMARY KEY (id),
  CONSTRAINT uq_horaires_station_jour UNIQUE (station_id, jour_semaine),
  CONSTRAINT fk_horaires_station FOREIGN KEY (station_id) REFERENCES stations (id) ON DELETE CASCADE,
  CONSTRAINT ck_horaires_jour CHECK (jour_semaine IN
    ('LUNDI', 'MARDI', 'MERCREDI', 'JEUDI', 'VENDREDI', 'SAMEDI', 'DIMANCHE'))
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- Tarifs par type de véhicule et par service (prix en TND, 3 décimales)
-- ---------------------------------------------------------------------
CREATE TABLE tarifs (
  id             BIGINT       NOT NULL AUTO_INCREMENT,
  station_id     BIGINT       NOT NULL,
  type_vehicule  VARCHAR(20)  NOT NULL,
  service        VARCHAR(100) NOT NULL,
  prix           DECIMAL(8,3) NOT NULL,
  duree_minutes  INT          NOT NULL DEFAULT 30,
  PRIMARY KEY (id),
  CONSTRAINT uq_tarifs UNIQUE (station_id, type_vehicule, service),
  CONSTRAINT fk_tarifs_station FOREIGN KEY (station_id) REFERENCES stations (id) ON DELETE CASCADE,
  CONSTRAINT ck_tarifs_type CHECK (type_vehicule IN ('CITADINE', 'BERLINE', 'SUV', 'UTILITAIRE', 'MOTO')),
  CONSTRAINT ck_tarifs_prix CHECK (prix >= 0),
  CONSTRAINT ck_tarifs_duree CHECK (duree_minutes BETWEEN 5 AND 480)
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- Réservations (type, service et prix copiés du tarif au moment de la réservation)
-- ---------------------------------------------------------------------
CREATE TABLE reservations (
  id             BIGINT       NOT NULL AUTO_INCREMENT,
  client_id      BIGINT       NOT NULL,
  station_id     BIGINT       NOT NULL,
  type_vehicule  VARCHAR(20)  NOT NULL,
  service        VARCHAR(100) NOT NULL,
  date_debut     DATETIME     NOT NULL,
  date_fin       DATETIME     NOT NULL,
  statut         VARCHAR(20)  NOT NULL DEFAULT 'EN_ATTENTE',
  prix           DECIMAL(8,3) NOT NULL,
  date_creation  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  CONSTRAINT fk_reservations_client FOREIGN KEY (client_id) REFERENCES users (id) ON DELETE CASCADE,
  CONSTRAINT fk_reservations_station FOREIGN KEY (station_id) REFERENCES stations (id) ON DELETE CASCADE,
  CONSTRAINT ck_reservations_statut CHECK (statut IN ('EN_ATTENTE', 'ACCEPTEE', 'REFUSEE', 'ANNULEE')),
  CONSTRAINT ck_reservations_type CHECK (type_vehicule IN ('CITADINE', 'BERLINE', 'SUV', 'UTILITAIRE', 'MOTO')),
  CONSTRAINT ck_reservations_dates CHECK (date_fin > date_debut),
  INDEX idx_reservations_client (client_id),
  INDEX idx_reservations_station_debut (station_id, date_debut)
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- Évaluations (une par client et par station, modifiable)
-- ---------------------------------------------------------------------
CREATE TABLE evaluations (
  id             BIGINT        NOT NULL AUTO_INCREMENT,
  client_id      BIGINT        NOT NULL,
  station_id     BIGINT        NOT NULL,
  note           INT           NOT NULL,
  commentaire    VARCHAR(1000) NULL,
  date_creation  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  CONSTRAINT uq_evaluations_client_station UNIQUE (client_id, station_id),
  CONSTRAINT fk_evaluations_client FOREIGN KEY (client_id) REFERENCES users (id) ON DELETE CASCADE,
  CONSTRAINT fk_evaluations_station FOREIGN KEY (station_id) REFERENCES stations (id) ON DELETE CASCADE,
  CONSTRAINT ck_evaluations_note CHECK (note BETWEEN 1 AND 5),
  INDEX idx_evaluations_station (station_id)
) ENGINE = InnoDB;
