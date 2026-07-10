CREATE TABLE wallet (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    balance DECIMAL(19, 2) NOT NULL DEFAULT 0.0,
    CONSTRAINT fk_wallet_user FOREIGN KEY (user_id) REFERENCES users(id));
