-- Adds optimistic-lock versioning to the BOM row. The entity mapping was
-- introduced with @Version; the column was missing from V5 because V5 had
-- already been applied, so it is added here instead of editing applied history.

ALTER TABLE bom ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
