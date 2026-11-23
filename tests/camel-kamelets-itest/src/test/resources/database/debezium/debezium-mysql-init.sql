CREATE TABLE products (id INTEGER PRIMARY KEY, name VARCHAR(100), price DECIMAL(10,2), added DATE, note VARCHAR(100));
INSERT INTO products VALUES (1, 'Tea', 4.50, '2026-01-01', 'not sent');
CREATE TABLE notes (id INTEGER PRIMARY KEY, text VARCHAR(100));
-- the privileges the description of the Kamelet names, and no more
GRANT SELECT, RELOAD, SHOW DATABASES, REPLICATION SLAVE, REPLICATION CLIENT ON *.* TO 'test'@'%';
