-- ==========================================
-- CREATE REPLICATION USER
-- ==========================================

CREATE USER IF NOT EXISTS 'replicator'@'%' 
IDENTIFIED BY 'replicator_password';

-- ==========================================
-- GIVE REPLICATION PERMISSION
-- ==========================================

GRANT REPLICATION SLAVE, REPLICATION CLIENT
ON *.*
TO 'replicator'@'%';

-- ==========================================
-- APPLY PRIVILEGES
-- ==========================================

FLUSH PRIVILEGES;