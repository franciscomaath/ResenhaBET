CREATE TABLE team (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    abbreviation VARCHAR(4) NOT NULL UNIQUE,
    external_api_id BIGINT UNIQUE,
    badge_url VARCHAR(255)
);

