-- Runs once, the first time the Postgres volume is created. identity_db is created by POSTGRES_DB; every other
-- service database (one per service) is listed here. For an existing volume run:
--   docker exec ecom-local-postgres-1 createdb -U ecom <name>
CREATE DATABASE listing_db;
CREATE DATABASE inventory_db;
CREATE DATABASE checkout_db;
CREATE DATABASE order_db;
