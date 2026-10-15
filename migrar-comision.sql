-- Incremental: conserva giros anteriores con comision cero.
BEGIN;
DO $$ BEGIN
 IF current_schema()<>'daynest' AND current_schema() !~ '^renta_test_[a-f0-9]{32}$' THEN RAISE EXCEPTION 'Esquema no autorizado'; END IF;
END; $$;
SET LOCAL lock_timeout='10s';
LOCK TABLE giro IN ACCESS EXCLUSIVE MODE;
ALTER TABLE giro ADD COLUMN IF NOT EXISTS porcentaje_comision NUMERIC(5,4) NOT NULL DEFAULT 0 CHECK(porcentaje_comision >= 0 AND porcentaje_comision < 1);
ALTER TABLE giro ADD COLUMN IF NOT EXISTS monto_comision NUMERIC(16,2) NOT NULL DEFAULT 0 CHECK(monto_comision >= 0);
CREATE OR REPLACE FUNCTION validar_giro() RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE
  r reserva%ROWTYPE;
  neto NUMERIC(16,2);
BEGIN
  IF TG_OP='INSERT' AND NEW.estado <> 'PENDIENTE' THEN
    RAISE EXCEPTION 'El giro debe iniciarse PENDIENTE';
  END IF;
  IF TG_OP='UPDATE' THEN
    IF OLD.estado='EJECUTADO' OR
       (NEW.id_reserva,NEW.monto,NEW.clave_idempotencia,NEW.porcentaje_comision,NEW.monto_comision)
       IS DISTINCT FROM (OLD.id_reserva,OLD.monto,OLD.clave_idempotencia,OLD.porcentaje_comision,OLD.monto_comision) THEN
      RAISE EXCEPTION 'Giro ejecutado o condiciones historicas inmutables';
    END IF;
    IF NEW.estado <> OLD.estado AND NOT (
      (OLD.estado='PENDIENTE' AND NEW.estado IN ('PROCESANDO','FALLIDO')) OR
      (OLD.estado='PROCESANDO' AND NEW.estado IN ('EJECUTADO','FALLIDO'))
    ) THEN RAISE EXCEPTION 'Transicion de giro no permitida'; END IF;
  END IF;
  SELECT * INTO r FROM reserva WHERE id_ocupacion=NEW.id_reserva FOR UPDATE;
  SELECT (o.fecha_fin-o.fecha_inicio)*r.precio_noche_pactado INTO neto FROM ocupacion o WHERE o.id_ocupacion=r.id_ocupacion;
  IF NEW.monto_comision<>round(neto*NEW.porcentaje_comision,2) OR NEW.monto<>neto-NEW.monto_comision THEN
    RAISE EXCEPTION 'Desglose de giro inconsistente con reserva';
  END IF;
  IF NEW.estado='EJECUTADO' THEN
    IF r.estado<>'FINALIZADA' THEN
      RAISE EXCEPTION 'Solo se gira una reserva finalizada';
    END IF;
    SELECT COALESCE(SUM(CASE WHEN clase='COBRO' THEN monto ELSE -monto END),0)
    INTO neto FROM movimiento_pago
    WHERE id_reserva=NEW.id_reserva AND estado='APROBADO';
    IF neto<=0 OR NEW.monto_comision<>round(neto*NEW.porcentaje_comision,2) OR NEW.monto<>neto-NEW.monto_comision THEN
      RAISE EXCEPTION 'Giro y comision deben coincidir con el neto cobrado';
    END IF;
    IF NEW.id_admin_autorizador IS NULL OR NEW.ejecutado_en IS NULL THEN
      RAISE EXCEPTION 'Giro ejecutado requiere administrador y fecha';
    END IF;
  END IF;
  RETURN NEW;
END;
$$;
COMMIT;
