-- File sharing
CREATE TABLE IF NOT EXISTS file_shares (
    id               BIGSERIAL PRIMARY KEY,
    file_id          BIGINT        NOT NULL REFERENCES files(id) ON DELETE CASCADE,
    owner_email      VARCHAR(255)  NOT NULL,
    shared_with_email VARCHAR(255),
    token            VARCHAR(36)   NOT NULL UNIQUE,
    permission       VARCHAR(16)   NOT NULL DEFAULT 'VIEW',
    created_at       TIMESTAMP(6),
    expires_at       TIMESTAMP(6)
);

CREATE INDEX IF NOT EXISTS idx_file_shares_token         ON file_shares(token);
CREATE INDEX IF NOT EXISTS idx_file_shares_shared_with   ON file_shares(shared_with_email);
CREATE INDEX IF NOT EXISTS idx_file_shares_file_id       ON file_shares(file_id);

-- Teams
CREATE TABLE IF NOT EXISTS teams (
    id           BIGSERIAL PRIMARY KEY,
    name         VARCHAR(100)  NOT NULL,
    owner_email  VARCHAR(255)  NOT NULL,
    created_at   TIMESTAMP(6)
);

CREATE TABLE IF NOT EXISTS team_members (
    id            BIGSERIAL PRIMARY KEY,
    team_id       BIGINT        NOT NULL REFERENCES teams(id) ON DELETE CASCADE,
    user_email    VARCHAR(255)  NOT NULL,
    role          VARCHAR(16)   NOT NULL DEFAULT 'MEMBER',
    invite_token  VARCHAR(36),
    status        VARCHAR(16)   NOT NULL DEFAULT 'PENDING',
    invited_at    TIMESTAMP(6),
    joined_at     TIMESTAMP(6),
    UNIQUE (team_id, user_email)
);

CREATE INDEX IF NOT EXISTS idx_team_members_team_id      ON team_members(team_id);
CREATE INDEX IF NOT EXISTS idx_team_members_user_email   ON team_members(user_email);
CREATE INDEX IF NOT EXISTS idx_team_members_invite_token ON team_members(invite_token);

-- Subscriptions
CREATE TABLE IF NOT EXISTS subscriptions (
    id                      BIGSERIAL PRIMARY KEY,
    user_email              VARCHAR(255)  NOT NULL UNIQUE,
    plan                    VARCHAR(16)   NOT NULL DEFAULT 'FREE',
    status                  VARCHAR(16)   NOT NULL DEFAULT 'ACTIVE',
    start_date              TIMESTAMP(6),
    end_date                TIMESTAMP(6),
    stripe_customer_id      VARCHAR(255),
    stripe_subscription_id  VARCHAR(255)
);

CREATE INDEX IF NOT EXISTS idx_subscriptions_user_email ON subscriptions(user_email);