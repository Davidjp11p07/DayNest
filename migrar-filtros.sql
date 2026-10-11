-- Extensión conceptual aprobada: dos atributos independientes; NULL = pendiente.
-- Ejecutar mediante MigrarFiltros después del ensayo sobre restauración completa.
BEGIN;
DO $$ BEGIN
 IF current_schema()<>'daynest' AND current_schema() !~ '^renta_test_[a-f0-9]{32}$' THEN RAISE EXCEPTION 'Esquema no autorizado'; END IF;
END; $$;
LOCK TABLE inmueble IN ACCESS EXCLUSIVE MODE;
ALTER TABLE inmueble ALTER COLUMN tipo DROP NOT NULL;
ALTER TABLE inmueble DROP CONSTRAINT inmueble_tipo_check;
ALTER TABLE inmueble ADD CONSTRAINT inmueble_tipo_check CHECK(tipo IN ('APARTAMENTO','CASA','CABAÑA','HABITACION','OTRO'));
ALTER TABLE inmueble ADD COLUMN es_rural BOOLEAN;
ALTER TABLE inmueble ADD COLUMN cerca_mar BOOLEAN;
-- Solo los 14 registros documentados que se importaron sin tipo desde public.
-- No alcanza nuevas propiedades con IDs iguales en una instalación independiente.
DO $$ BEGIN
 IF to_regclass('public.inmueble') IS NOT NULL THEN
  UPDATE inmueble i SET tipo=NULL FROM public.inmueble a
   WHERE i.id_inmueble=a.id_inmueble AND i.id_inmueble BETWEEN 1 AND 14
   AND i.titulo=a.titulo AND i.tipo='APARTAMENTO';
 END IF;
END; $$;
COMMIT;
