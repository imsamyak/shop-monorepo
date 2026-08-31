-- Runs once, the first time the Postgres volume is created. identity_db is created by POSTGRES_DB; every other
-- service database is listed here. For an existing volume run: docker exec ecom-local-postgres-1 createdb -U ecom <name>
CREATE DATABASE listing_db;
