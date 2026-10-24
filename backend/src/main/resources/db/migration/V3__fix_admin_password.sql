-- V3: Fix admin user password to Admin@1234
-- BCrypt strength 10 hash of "Admin@1234"
UPDATE users
SET password = '$2a$10$AiIsOsjH5VkAU6pLHxuqiO3NbJq1De2N.7EkmKRGpn965Rdfp.5Wi'
WHERE email = 'admin@flightreservation.com';
