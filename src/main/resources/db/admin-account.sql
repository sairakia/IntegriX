INSERT INTO user_info (user_id, email, password, name, role, status, profile_image_url)
VALUES (
    'admin',
    'admin@integrix.local',
    '$2a$10$UU7rFh4F47oTGeqJpScAwu6EgL04t3asPwCv5pEMLUpntMNFTH3mK',
    'Administrator',
    'ADMIN',
    'ACTIVE',
    NULL
)
ON DUPLICATE KEY UPDATE
    password = VALUES(password),
    name = VALUES(name),
    role = VALUES(role),
    status = VALUES(status);
