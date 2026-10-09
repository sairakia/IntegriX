ALTER TABLE report_feedback
    ADD COLUMN IF NOT EXISTS admin_reply TEXT NULL,
    ADD COLUMN IF NOT EXISTS processed_at DATETIME NULL,
    ADD COLUMN IF NOT EXISTS updated_at DATETIME NULL;

UPDATE report_feedback
SET updated_at = created_at
WHERE updated_at IS NULL;
