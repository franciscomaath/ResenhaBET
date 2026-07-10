ALTER TABLE groups ADD COLUMN group_code VARCHAR(6);
UPDATE groups SET group_code = lpad(floor(random() * 900000 + 100000)::int::text, 6, '0') WHERE group_code IS NULL;
ALTER TABLE groups ALTER COLUMN group_code SET NOT NULL;
ALTER TABLE groups ADD CONSTRAINT uk_groups_group_code UNIQUE (group_code);
ALTER TABLE groups ADD CONSTRAINT chk_groups_group_code CHECK (group_code ~ '^\d{6}$');
