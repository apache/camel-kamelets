CREATE TABLE IF NOT EXISTS test_data (id SERIAL PRIMARY KEY, name VARCHAR(100), value VARCHAR(100));
INSERT INTO test_data (name, value) VALUES ('test-entry', 'hello-postgres');
