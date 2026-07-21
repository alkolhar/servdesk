-- Unique constraints on soft-deletable columns are partial unique indexes
-- (WHERE deleted_at IS NULL) throughout: a soft-deleted row's email/username/name/
-- display number must not block recreating an active row with the same value.

CREATE TABLE team
(
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version     BIGINT       NOT NULL DEFAULT 0,
    created_by  VARCHAR(100),
    updated_by  VARCHAR(100),
    deleted_at  TIMESTAMPTZ
);

CREATE UNIQUE INDEX uk_team_name ON team (name) WHERE deleted_at IS NULL;

CREATE TABLE person
(
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    role       VARCHAR(20)  NOT NULL,
    name       VARCHAR(200) NOT NULL,
    email      VARCHAR(255) NOT NULL,
    phone      VARCHAR(50),
    username   VARCHAR(100),
    password   VARCHAR(255),
    enabled    BOOLEAN      NOT NULL DEFAULT TRUE,
    team_id    BIGINT,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version    BIGINT       NOT NULL DEFAULT 0,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    deleted_at TIMESTAMPTZ,
    CONSTRAINT fk_person_team FOREIGN KEY (team_id) REFERENCES team (id)
);

CREATE UNIQUE INDEX uk_person_email ON person (email) WHERE deleted_at IS NULL;
CREATE UNIQUE INDEX uk_person_username ON person (username) WHERE deleted_at IS NULL;

CREATE TABLE category
(
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name       VARCHAR(150) NOT NULL,
    parent_id  BIGINT,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version    BIGINT       NOT NULL DEFAULT 0,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    deleted_at TIMESTAMPTZ,
    CONSTRAINT fk_category_parent FOREIGN KEY (parent_id) REFERENCES category (id)
);

CREATE TABLE priority
(
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name       VARCHAR(50) NOT NULL,
    sort_order INT         NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version    BIGINT      NOT NULL DEFAULT 0,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    deleted_at TIMESTAMPTZ
);

CREATE UNIQUE INDEX uk_priority_name ON priority (name) WHERE deleted_at IS NULL;

CREATE TABLE ticket
(
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    status        VARCHAR(20)  NOT NULL,
    subject       VARCHAR(255) NOT NULL,
    description   TEXT,
    attributes    JSONB        NOT NULL DEFAULT '{}'::jsonb,
    category_id   BIGINT,
    priority_id   BIGINT,
    requester_id  BIGINT       NOT NULL,
    assignee_id   BIGINT,
    team_id       BIGINT,
    resolved_at   TIMESTAMPTZ,
    closed_at     TIMESTAMPTZ,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version       BIGINT       NOT NULL DEFAULT 0,
    created_by    VARCHAR(100),
    updated_by    VARCHAR(100),
    deleted_at    TIMESTAMPTZ,
    CONSTRAINT fk_ticket_category FOREIGN KEY (category_id) REFERENCES category (id),
    CONSTRAINT fk_ticket_priority FOREIGN KEY (priority_id) REFERENCES priority (id),
    CONSTRAINT fk_ticket_requester FOREIGN KEY (requester_id) REFERENCES person (id),
    CONSTRAINT fk_ticket_assignee FOREIGN KEY (assignee_id) REFERENCES person (id),
    CONSTRAINT fk_ticket_team FOREIGN KEY (team_id) REFERENCES team (id)
);

-- Customer-defined custom-field values (see attribute_definition below); GIN makes
-- jsonb containment (@>) and path lookups on arbitrary keys indexable without
-- per-field schema changes — the ADR-0002 design.
CREATE INDEX idx_ticket_attributes ON ticket USING GIN (attributes);

-- Admin-editable custom-field definitions (issue #29): what keys are allowed on a
-- target aggregate (only TICKET today; CMDB configuration items later), their type,
-- and validation facts. Values live in the target's own `attributes` jsonb column.
CREATE TABLE attribute_definition
(
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    target_type VARCHAR(20)  NOT NULL,
    attr_key    VARCHAR(100) NOT NULL,
    label       VARCHAR(150) NOT NULL,
    attr_type   VARCHAR(20)  NOT NULL,
    required    BOOLEAN      NOT NULL DEFAULT FALSE,
    enum_values JSONB,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version     BIGINT       NOT NULL DEFAULT 0,
    created_by  VARCHAR(100),
    updated_by  VARCHAR(100),
    deleted_at  TIMESTAMPTZ
);

CREATE UNIQUE INDEX uk_attribute_definition_target_key ON attribute_definition (target_type, attr_key)
    WHERE deleted_at IS NULL;

CREATE TABLE ticket_comment
(
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    ticket_id  BIGINT      NOT NULL,
    author_id  BIGINT      NOT NULL,
    body       TEXT        NOT NULL,
    internal   BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version    BIGINT      NOT NULL DEFAULT 0,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    deleted_at TIMESTAMPTZ,
    CONSTRAINT fk_comment_ticket FOREIGN KEY (ticket_id) REFERENCES ticket (id),
    CONSTRAINT fk_comment_author FOREIGN KEY (author_id) REFERENCES person (id)
);

-- Ticket subtypes (ADR-0001): each shares its primary key with a `ticket` row via
-- `@OneToOne @MapsId` rather than extending it, so `id` here is both this table's PK and
-- an FK back to `ticket.id` — no identity column of its own. Each subtype gets its own
-- display-number sequence/prefix, since human-facing ticket numbers are type-specific.

CREATE SEQUENCE problem_number_seq START WITH 1000 INCREMENT BY 1;

CREATE TABLE problem
(
    id              BIGINT PRIMARY KEY,
    display_number  VARCHAR(20) NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version         BIGINT      NOT NULL DEFAULT 0,
    created_by      VARCHAR(100),
    updated_by      VARCHAR(100),
    deleted_at      TIMESTAMPTZ,
    CONSTRAINT fk_problem_ticket FOREIGN KEY (id) REFERENCES ticket (id)
);

CREATE UNIQUE INDEX uk_problem_display_number ON problem (display_number) WHERE deleted_at IS NULL;

CREATE SEQUENCE incident_number_seq START WITH 1000 INCREMENT BY 1;

CREATE TABLE incident
(
    id                 BIGINT PRIMARY KEY,
    display_number     VARCHAR(20) NOT NULL,
    related_problem_id BIGINT,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version            BIGINT      NOT NULL DEFAULT 0,
    created_by         VARCHAR(100),
    updated_by         VARCHAR(100),
    deleted_at         TIMESTAMPTZ,
    CONSTRAINT fk_incident_ticket FOREIGN KEY (id) REFERENCES ticket (id),
    CONSTRAINT fk_incident_related_problem FOREIGN KEY (related_problem_id) REFERENCES problem (id)
);

CREATE UNIQUE INDEX uk_incident_display_number ON incident (display_number) WHERE deleted_at IS NULL;

-- Table named `change_request`, not `change`: kept from the original MariaDB-era schema
-- (CHANGE is reserved in MariaDB's grammar), and `change_request` is the clearer name anyway.
-- The Java entity is still named `Change`.
CREATE SEQUENCE change_number_seq START WITH 1000 INCREMENT BY 1;

CREATE TABLE change_request
(
    id              BIGINT PRIMARY KEY,
    display_number  VARCHAR(20) NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version         BIGINT      NOT NULL DEFAULT 0,
    created_by      VARCHAR(100),
    updated_by      VARCHAR(100),
    deleted_at      TIMESTAMPTZ,
    CONSTRAINT fk_change_ticket FOREIGN KEY (id) REFERENCES ticket (id)
);

CREATE UNIQUE INDEX uk_change_display_number ON change_request (display_number) WHERE deleted_at IS NULL;

CREATE SEQUENCE service_request_number_seq START WITH 1000 INCREMENT BY 1;

CREATE TABLE service_request
(
    id              BIGINT PRIMARY KEY,
    display_number  VARCHAR(20) NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version         BIGINT      NOT NULL DEFAULT 0,
    created_by      VARCHAR(100),
    updated_by      VARCHAR(100),
    deleted_at      TIMESTAMPTZ,
    CONSTRAINT fk_service_request_ticket FOREIGN KEY (id) REFERENCES ticket (id)
);

CREATE UNIQUE INDEX uk_service_request_display_number ON service_request (display_number) WHERE deleted_at IS NULL;
