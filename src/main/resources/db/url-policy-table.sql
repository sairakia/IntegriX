CREATE TABLE IF NOT EXISTS url_policy (
    policy_id BIGINT NOT NULL AUTO_INCREMENT,
    list_type VARCHAR(20) NOT NULL,
    match_type VARCHAR(20) NOT NULL DEFAULT 'PATTERN',
    pattern VARCHAR(500) NOT NULL,
    reason VARCHAR(500) NULL,
    active BIT NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (policy_id),
    INDEX idx_url_policy_active (active),
    INDEX idx_url_policy_type_match (list_type, match_type)
);
