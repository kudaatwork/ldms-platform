-- Extra databases for LDMS microservices (runs only on first MySQL init — empty data volume).
-- Base image already creates MYSQL_DATABASE (ldms_platform) and MYSQL_USER (developer).
CREATE DATABASE IF NOT EXISTS ldms_location_management
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

GRANT ALL PRIVILEGES ON ldms_location_management.* TO 'developer'@'%';
FLUSH PRIVILEGES;
