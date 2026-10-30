-- Reference migration for MySQL 8 for the AI Concierge, meeting coordination and support features.
-- By default the app creates these tables itself (JPA_DDL_AUTO=update). Use this file when you
-- switch to JPA_DDL_AUTO=validate. It has NOT been executed against a database in this delivery;
-- compare it with the schema Hibernate generates on a scratch database before relying on it.
-- Assumes tables `users` and `customer_orders`.

CREATE TABLE IF NOT EXISTS order_messages (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  order_id BIGINT NOT NULL,
  sender_id BIGINT NOT NULL,
  body VARCHAR(500) NOT NULL,
  created_at DATETIME(6) NOT NULL,
  KEY idx_order_messages_order (order_id, created_at),
  CONSTRAINT fk_order_messages_order FOREIGN KEY (order_id) REFERENCES customer_orders (id),
  CONSTRAINT fk_order_messages_sender FOREIGN KEY (sender_id) REFERENCES users (id)
);

CREATE TABLE IF NOT EXISTS meeting_updates (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  order_id BIGINT NOT NULL,
  sender_id BIGINT NOT NULL,
  type VARCHAR(20) NOT NULL,
  eta_minutes INT NULL,
  note VARCHAR(200) NULL,
  via_concierge BIT NOT NULL,
  created_at DATETIME(6) NOT NULL,
  KEY idx_meeting_updates_order (order_id, created_at),
  CONSTRAINT fk_meeting_updates_order FOREIGN KEY (order_id) REFERENCES customer_orders (id),
  CONSTRAINT fk_meeting_updates_sender FOREIGN KEY (sender_id) REFERENCES users (id)
);

CREATE TABLE IF NOT EXISTS location_shares (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  order_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  expires_at DATETIME(6) NOT NULL,
  latitude DOUBLE NOT NULL,
  longitude DOUBLE NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  started_at DATETIME(6) NOT NULL,
  UNIQUE KEY uk_location_share_order_user (order_id, user_id),
  KEY idx_location_shares_expires (expires_at),
  CONSTRAINT fk_location_shares_order FOREIGN KEY (order_id) REFERENCES customer_orders (id),
  CONSTRAINT fk_location_shares_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE IF NOT EXISTS support_cases (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  creator_id BIGINT NOT NULL,
  order_id BIGINT NULL,
  category VARCHAR(24) NOT NULL,
  status VARCHAR(16) NOT NULL,
  summary VARCHAR(1000) NOT NULL,
  source VARCHAR(12) NOT NULL,
  resolution_note VARCHAR(500) NULL,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  KEY idx_support_cases_creator (creator_id, created_at),
  KEY idx_support_cases_status (status, created_at),
  CONSTRAINT fk_support_cases_creator FOREIGN KEY (creator_id) REFERENCES users (id),
  CONSTRAINT fk_support_cases_order FOREIGN KEY (order_id) REFERENCES customer_orders (id)
);

CREATE TABLE IF NOT EXISTS concierge_messages (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  author VARCHAR(10) NOT NULL,
  body VARCHAR(2000) NOT NULL,
  created_at DATETIME(6) NOT NULL,
  KEY idx_concierge_messages_user (user_id, created_at),
  KEY idx_concierge_messages_created (created_at),
  CONSTRAINT fk_concierge_messages_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE IF NOT EXISTS audit_events (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  actor_id BIGINT NOT NULL,
  event_type VARCHAR(40) NOT NULL,
  order_id BIGINT NULL,
  detail VARCHAR(200) NULL,
  created_at DATETIME(6) NOT NULL,
  KEY idx_audit_events_actor (actor_id, created_at),
  KEY idx_audit_events_order (order_id, created_at)
);

-- EXISTING DATABASES ONLY: three values were added to the NotificationType enum (MEETING, MESSAGE,
-- SUPPORT). Hibernate 6 on MySQL may have created notifications.type as a native ENUM column, and
-- ddl-auto=update does not widen it. Check with `SHOW COLUMNS FROM notifications LIKE 'type'` and, if it is an
-- ENUM, convert it (this keeps the existing values):
--   ALTER TABLE notifications MODIFY type VARCHAR(20) NOT NULL;
-- (table name: notifications).
