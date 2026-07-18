CREATE TABLE team
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    version     BIGINT       NOT NULL DEFAULT 0,
    created_by  VARCHAR(100),
    updated_by  VARCHAR(100),
    deleted_at  TIMESTAMP NULL,
    CONSTRAINT uk_team_name UNIQUE (name)
);

CREATE TABLE person
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    role       VARCHAR(20)  NOT NULL,
    name       VARCHAR(200) NOT NULL,
    email      VARCHAR(255) NOT NULL,
    phone      VARCHAR(50),
    username   VARCHAR(100),
    password   VARCHAR(255),
    enabled    BOOLEAN      NOT NULL DEFAULT TRUE,
    team_id    BIGINT,
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    version    BIGINT       NOT NULL DEFAULT 0,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    deleted_at TIMESTAMP NULL,
    CONSTRAINT uk_person_email UNIQUE (email),
    CONSTRAINT uk_person_username UNIQUE (username),
    CONSTRAINT fk_person_team FOREIGN KEY (team_id) REFERENCES team (id)
);

CREATE TABLE category
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    name       VARCHAR(150) NOT NULL,
    parent_id  BIGINT,
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    version    BIGINT       NOT NULL DEFAULT 0,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    deleted_at TIMESTAMP NULL,
    CONSTRAINT fk_category_parent FOREIGN KEY (parent_id) REFERENCES category (id)
);

CREATE TABLE priority
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    name       VARCHAR(50) NOT NULL,
    sort_order INT         NOT NULL,
    created_at TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    version    BIGINT      NOT NULL DEFAULT 0,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    deleted_at TIMESTAMP NULL,
    CONSTRAINT uk_priority_name UNIQUE (name)
);

CREATE TABLE ticket
(
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    ticket_number VARCHAR(20)  NOT NULL,
    type          VARCHAR(20)  NOT NULL,
    status        VARCHAR(20)  NOT NULL,
    subject       VARCHAR(255) NOT NULL,
    description   LONGTEXT,
    category_id   BIGINT,
    priority_id   BIGINT,
    requester_id  BIGINT       NOT NULL,
    assignee_id   BIGINT,
    team_id       BIGINT,
    resolved_at   TIMESTAMP NULL,
    closed_at     TIMESTAMP NULL,
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    version       BIGINT       NOT NULL DEFAULT 0,
    created_by    VARCHAR(100),
    updated_by    VARCHAR(100),
    deleted_at    TIMESTAMP NULL,
    CONSTRAINT uk_ticket_number UNIQUE (ticket_number),
    CONSTRAINT fk_ticket_category FOREIGN KEY (category_id) REFERENCES category (id),
    CONSTRAINT fk_ticket_priority FOREIGN KEY (priority_id) REFERENCES priority (id),
    CONSTRAINT fk_ticket_requester FOREIGN KEY (requester_id) REFERENCES person (id),
    CONSTRAINT fk_ticket_assignee FOREIGN KEY (assignee_id) REFERENCES person (id),
    CONSTRAINT fk_ticket_team FOREIGN KEY (team_id) REFERENCES team (id)
);

CREATE TABLE ticket_comment
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    ticket_id  BIGINT    NOT NULL,
    author_id  BIGINT    NOT NULL,
    body       LONGTEXT  NOT NULL,
    internal   BOOLEAN   NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    version    BIGINT    NOT NULL DEFAULT 0,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    deleted_at TIMESTAMP NULL,
    CONSTRAINT fk_comment_ticket FOREIGN KEY (ticket_id) REFERENCES ticket (id),
    CONSTRAINT fk_comment_author FOREIGN KEY (author_id) REFERENCES person (id)
);

CREATE SEQUENCE ticket_number_seq START WITH 1000 INCREMENT BY 1;
