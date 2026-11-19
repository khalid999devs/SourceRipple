CREATE TABLE roles (
    id   uuid        PRIMARY KEY,
    name varchar(20) NOT NULL,
    CONSTRAINT uq_roles_name UNIQUE (name),
    CONSTRAINT ck_roles_name CHECK (name IN ('ADMIN', 'MAINTAINER', 'REVIEWER'))
);

INSERT INTO roles (id, name) VALUES
    (gen_random_uuid(), 'ADMIN'),
    (gen_random_uuid(), 'MAINTAINER'),
    (gen_random_uuid(), 'REVIEWER');

CREATE TABLE users (
    id            uuid         PRIMARY KEY,
    email         varchar(320) NOT NULL,
    password_hash varchar(100) NOT NULL,
    display_name  varchar(100) NOT NULL,
    enabled       boolean      NOT NULL DEFAULT true,
    created_at    timestamptz  NOT NULL,
    last_login_at timestamptz,
    CONSTRAINT ck_users_email_lower CHECK (email = lower(email))
);

CREATE UNIQUE INDEX uq_users_email ON users (lower(email));

CREATE TABLE user_roles (
    user_id uuid NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    role_id uuid NOT NULL REFERENCES roles (id),
    PRIMARY KEY (user_id, role_id)
);
