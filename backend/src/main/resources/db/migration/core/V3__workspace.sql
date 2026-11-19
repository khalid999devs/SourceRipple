CREATE TABLE organizations (
    id            uuid         PRIMARY KEY,
    owner_user_id uuid         NOT NULL REFERENCES users (id),
    name          varchar(100) NOT NULL,
    created_at    timestamptz  NOT NULL
);

CREATE INDEX ix_organizations_owner ON organizations (owner_user_id);

CREATE TABLE organization_members (
    organization_id uuid        NOT NULL REFERENCES organizations (id) ON DELETE CASCADE,
    user_id         uuid        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    added_at        timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (organization_id, user_id)
);

CREATE INDEX ix_organization_members_user ON organization_members (user_id);

CREATE TABLE projects (
    id              uuid          PRIMARY KEY,
    organization_id uuid          NOT NULL REFERENCES organizations (id),
    name            varchar(100)  NOT NULL,
    slug            varchar(120)  NOT NULL,
    description     varchar(1000),
    archived_at     timestamptz,
    created_at      timestamptz   NOT NULL,
    CONSTRAINT uq_projects_organization_slug UNIQUE (organization_id, slug)
);
