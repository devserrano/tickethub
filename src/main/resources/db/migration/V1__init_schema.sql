-- V1: esquema inicial de TicketHub (MySQL 8.4).
-- Ya incluye el modelo final: rol LIBERADOR y los 8 estatus (sin OPEN), historial, notificaciones y SLA con pausa.
-- Fechas: DATETIME(6) guardadas en UTC (el servidor y Hibernate usan UTC).
-- Regla de oro: una vez que el equipo aplicó esta migración NO se edita; los cambios van en V3, V4...

CREATE TABLE users (
    id            BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name          VARCHAR(100) NOT NULL,
    email         VARCHAR(150) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role          VARCHAR(20)  NOT NULL,
    active        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    DATETIME(6)  NOT NULL,
    updated_at    DATETIME(6)  NOT NULL,
    CONSTRAINT chk_users_role CHECK (role IN ('USER', 'LIBERADOR', 'AGENT', 'ADMIN'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE categories (
    id          BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(60)  NOT NULL UNIQUE,
    description VARCHAR(255),
    active      BOOLEAN      NOT NULL DEFAULT TRUE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE tickets (
    id                 BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    folio              VARCHAR(20)  NOT NULL UNIQUE,
    title              VARCHAR(120) NOT NULL,
    description        TEXT         NOT NULL,
    status             VARCHAR(20)  NOT NULL,
    priority           VARCHAR(20)  NOT NULL,
    solution           TEXT,
    created_by         BIGINT       NOT NULL,
    released_by        BIGINT,                 -- LIBERADOR/ADMIN que lo aprobó o rechazó
    assigned_to        BIGINT,                 -- AGENT responsable
    category_id        BIGINT       NOT NULL,
    -- SLA: respuesta = hasta pasar a IN_PROGRESS; resolución = hasta RESOLVED
    due_response_at    DATETIME(6),
    due_resolution_at  DATETIME(6),
    responded_at       DATETIME(6),
    resolved_at        DATETIME(6),
    closed_at          DATETIME(6),
    -- El reloj se pausa en REJECTED y ON_HOLD: guardamos desde cuándo y cuánto se acumuló
    sla_paused_at      DATETIME(6),
    sla_paused_seconds BIGINT       NOT NULL DEFAULT 0,
    sla_breached       BOOLEAN      NOT NULL DEFAULT FALSE,
    sla_warning_sent   BOOLEAN      NOT NULL DEFAULT FALSE,  -- evita mandar sla-warning más de una vez
    created_at         DATETIME(6)  NOT NULL,
    updated_at         DATETIME(6)  NOT NULL,
    CONSTRAINT chk_tickets_status CHECK (status IN
        ('PENDING_REVIEW', 'REJECTED', 'APPROVED', 'IN_PROGRESS', 'ON_HOLD', 'RESOLVED', 'CLOSED', 'CANCELLED')),
    CONSTRAINT chk_tickets_priority CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    CONSTRAINT fk_tickets_created_by FOREIGN KEY (created_by) REFERENCES users (id),
    CONSTRAINT fk_tickets_released_by FOREIGN KEY (released_by) REFERENCES users (id),
    CONSTRAINT fk_tickets_assigned_to FOREIGN KEY (assigned_to) REFERENCES users (id),
    CONSTRAINT fk_tickets_category FOREIGN KEY (category_id) REFERENCES categories (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE INDEX idx_tickets_status ON tickets (status);
CREATE INDEX idx_tickets_created_by ON tickets (created_by);
CREATE INDEX idx_tickets_assigned_to ON tickets (assigned_to);
CREATE INDEX idx_tickets_created_at ON tickets (created_at);
-- para el scheduler de SLA y el cierre automático
CREATE INDEX idx_tickets_status_due ON tickets (status, due_resolution_at);

CREATE TABLE comments (
    id         BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
    ticket_id  BIGINT      NOT NULL,
    author_id  BIGINT      NOT NULL,
    body       TEXT        NOT NULL,
    internal   BOOLEAN     NOT NULL DEFAULT FALSE,  -- true: el USER nunca lo ve
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_comments_ticket FOREIGN KEY (ticket_id) REFERENCES tickets (id),
    CONSTRAINT fk_comments_author FOREIGN KEY (author_id) REFERENCES users (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE INDEX idx_comments_ticket ON comments (ticket_id);

CREATE TABLE attachments (
    id            BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    ticket_id     BIGINT       NOT NULL,
    comment_id    BIGINT,
    uploaded_by   BIGINT       NOT NULL,
    original_name VARCHAR(255) NOT NULL,
    storage_key   VARCHAR(255) NOT NULL UNIQUE,
    content_type  VARCHAR(100) NOT NULL,
    size_bytes    BIGINT       NOT NULL,
    created_at    DATETIME(6)  NOT NULL,
    CONSTRAINT fk_attachments_ticket FOREIGN KEY (ticket_id) REFERENCES tickets (id),
    CONSTRAINT fk_attachments_comment FOREIGN KEY (comment_id) REFERENCES comments (id),
    CONSTRAINT fk_attachments_user FOREIGN KEY (uploaded_by) REFERENCES users (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE INDEX idx_attachments_ticket ON attachments (ticket_id);

-- Bitácora: una fila por cada cambio (STATUS, PRIORITY, ASSIGNEE...). `reason` guarda el motivo
-- cuando aplica (rechazo, cancelación administrativa, información solicitada).
CREATE TABLE ticket_history (
    id         BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    ticket_id  BIGINT       NOT NULL,
    changed_by BIGINT       NOT NULL,
    field      VARCHAR(30)  NOT NULL,
    old_value  VARCHAR(255),
    new_value  VARCHAR(255),
    reason     VARCHAR(500),
    changed_at DATETIME(6)  NOT NULL,
    CONSTRAINT fk_history_ticket FOREIGN KEY (ticket_id) REFERENCES tickets (id),
    CONSTRAINT fk_history_user FOREIGN KEY (changed_by) REFERENCES users (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE INDEX idx_history_ticket ON ticket_history (ticket_id);

CREATE TABLE password_reset_tokens (
    id         BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT       NOT NULL,
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    expires_at DATETIME(6)  NOT NULL,
    used       BOOLEAN      NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_reset_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- Notificaciones dentro de la app (pantalla /notifications). `is_read` porque READ es palabra reservada.
CREATE TABLE notifications (
    id         BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT       NOT NULL,
    ticket_id  BIGINT,
    type       VARCHAR(40)  NOT NULL,
    message    VARCHAR(255) NOT NULL,
    is_read    BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at DATETIME(6)  NOT NULL,
    CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_notifications_ticket FOREIGN KEY (ticket_id) REFERENCES tickets (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE INDEX idx_notifications_user_read ON notifications (user_id, is_read);
