ALTER TABLE inmueble ADD COLUMN area_m2 NUMERIC(12,2), ADD COLUMN habitaciones SMALLINT;
CREATE VIEW inmueble_operativo AS SELECT * FROM inmueble;
