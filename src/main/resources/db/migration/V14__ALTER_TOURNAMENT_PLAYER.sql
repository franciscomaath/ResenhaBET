ALTER TABLE tournament_player
DROP CONSTRAINT tournament_player_pkey,
ADD COLUMN id BIGSERIAL PRIMARY KEY,
ADD COLUMN team_id BIGINT,
ADD CONSTRAINT fk_tp_team FOREIGN KEY (team_id) REFERENCES team(id);


