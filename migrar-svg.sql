-- Adaptación incremental al SVG; NO inicializa, trunca ni elimina tablas.
-- Ejecutar con MigrarModeloSvg, respaldo verificado y aplicación detenida.
BEGIN;
DO $$
BEGIN
  IF current_schema() <> 'daynest' AND current_schema() !~ '^renta_test_[a-f0-9]{32}$' THEN
    RAISE EXCEPTION 'Esquema no autorizado para esta migración: %',current_schema();
  END IF;
END;
$$;
LOCK TABLE usuario,reserva,movimiento_pago,giro IN ACCESS EXCLUSIVE MODE;
DO $$
BEGIN
  IF EXISTS(SELECT 1 FROM movimiento_pago m JOIN reserva r ON r.id_ocupacion=m.id_reserva WHERE m.moneda IS DISTINCT FROM r.moneda)
     OR EXISTS(SELECT 1 FROM giro g JOIN reserva r ON r.id_ocupacion=g.id_reserva WHERE g.moneda IS DISTINCT FROM r.moneda) THEN
    RAISE EXCEPTION 'Monedas inconsistentes: se rechaza la adaptación sin perder datos';
  END IF;
  IF EXISTS(SELECT 1 FROM usuario WHERE correo<>lower(btrim(correo))) THEN
    RAISE EXCEPTION 'Hay correos no canónicos: requiere revisión previa, sin sobrescribirlos';
  END IF;
END;
$$;
ALTER TABLE usuario RENAME COLUMN correo TO correo_normalizado;
-- PostgreSQL 18 nombra NOT NULL como constraint; versiones previas no.
DO $$ BEGIN
  IF EXISTS(SELECT 1 FROM pg_constraint WHERE conrelid='usuario'::regclass AND conname='usuario_correo_not_null') THEN
    ALTER TABLE usuario RENAME CONSTRAINT usuario_correo_not_null TO usuario_correo_normalizado_not_null;
  END IF;
END; $$;
ALTER TABLE usuario ADD CONSTRAINT ck_usuario_correo_canonico
  CHECK(correo_normalizado=lower(btrim(correo_normalizado)));
-- El índice anterior se recrea sobre el valor canónico, sin expresión redundante.
DROP INDEX ux_usuario_correo_normalizado;
CREATE UNIQUE INDEX ux_usuario_correo_normalizado ON usuario(correo_normalizado);
CREATE OR REPLACE FUNCTION validar_movimiento_pago() RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE
  r reserva%ROWTYPE;
  total_pactado NUMERIC(16,2);
  cobros NUMERIC(16,2);
  devoluciones NUMERIC(16,2);
  total_origen NUMERIC(16,2);
  pago_origen movimiento_pago%ROWTYPE;
BEGIN
  IF TG_OP='INSERT' AND NEW.estado <> 'PENDIENTE' THEN
    RAISE EXCEPTION 'El movimiento de pago debe iniciar PENDIENTE';
  END IF;
  IF TG_OP='UPDATE' THEN
    IF OLD.estado <> 'PENDIENTE' OR
       (NEW.id_reserva,NEW.clase,NEW.monto,NEW.id_cobro_origen,NEW.clave_idempotencia)
       IS DISTINCT FROM
       (OLD.id_reserva,OLD.clase,OLD.monto,OLD.id_cobro_origen,OLD.clave_idempotencia) THEN
      RAISE EXCEPTION 'Movimiento inmutable o ya resuelto';
    END IF;
  END IF;
  -- Bloqueo de la reserva serializa las validaciones financieras concurrentes.
  SELECT * INTO r FROM reserva WHERE id_ocupacion = NEW.id_reserva FOR UPDATE;
  IF NEW.clase='DEVOLUCION' THEN
    SELECT * INTO pago_origen FROM movimiento_pago WHERE id_movimiento=NEW.id_cobro_origen;
    IF NOT FOUND OR pago_origen.clase<>'COBRO' OR pago_origen.estado<>'APROBADO'
      OR pago_origen.id_reserva<>NEW.id_reserva THEN
      RAISE EXCEPTION 'Devolucion debe asociarse a un cobro aprobado de esa reserva';
    END IF;
  ELSIF r.estado IN ('CANCELADA','EXPIRADA') THEN
    RAISE EXCEPTION 'No se admiten cobros para reserva cancelada/expirada';
  END IF;
  IF NEW.estado='APROBADO' AND NEW.clase='DEVOLUCION'
     AND r.estado NOT IN ('CANCELADA','EXPIRADA') THEN
    RAISE EXCEPTION 'Devolucion solo despues de cancelar o expirar reserva';
  END IF;
  IF NEW.estado='APROBADO' THEN
    IF EXISTS (SELECT 1 FROM giro WHERE id_reserva=NEW.id_reserva AND estado='EJECUTADO') THEN
      RAISE EXCEPTION 'Operaciones posteriores a giro ejecutado requieren conciliacion externa';
    END IF;
    SELECT (o.fecha_fin-o.fecha_inicio)*r.precio_noche_pactado
      INTO total_pactado FROM ocupacion o WHERE o.id_ocupacion=r.id_ocupacion;
    SELECT COALESCE(SUM(monto) FILTER(WHERE clase='COBRO'),0),
           COALESCE(SUM(monto) FILTER(WHERE clase='DEVOLUCION'),0)
      INTO cobros,devoluciones
      FROM movimiento_pago WHERE id_reserva=NEW.id_reserva AND estado='APROBADO'
      AND (TG_OP='INSERT' OR id_movimiento<>NEW.id_movimiento);
    IF NEW.clase='COBRO' THEN cobros := cobros + NEW.monto;
    ELSE devoluciones := devoluciones + NEW.monto; END IF;
    IF cobros > total_pactado OR devoluciones > cobros THEN
      RAISE EXCEPTION 'Sobrecobro o devolucion superior a lo cobrado';
    END IF;
    IF NEW.clase='DEVOLUCION' THEN
      SELECT COALESCE(SUM(monto),0) INTO total_origen
      FROM movimiento_pago WHERE id_cobro_origen=NEW.id_cobro_origen
      AND estado='APROBADO' AND (TG_OP='INSERT' OR id_movimiento<>NEW.id_movimiento);
      IF total_origen + NEW.monto > pago_origen.monto THEN
        RAISE EXCEPTION 'Devoluciones exceden el cobro origen';
      END IF;
    END IF;
  END IF;
  RETURN NEW;
END;
$$;

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
       (NEW.id_reserva,NEW.monto,NEW.clave_idempotencia)
       IS DISTINCT FROM (OLD.id_reserva,OLD.monto,OLD.clave_idempotencia) THEN
      RAISE EXCEPTION 'Giro ejecutado o condiciones historicas inmutables';
    END IF;
    IF NEW.estado <> OLD.estado AND NOT (
      (OLD.estado='PENDIENTE' AND NEW.estado IN ('PROCESANDO','FALLIDO')) OR
      (OLD.estado='PROCESANDO' AND NEW.estado IN ('EJECUTADO','FALLIDO'))
    ) THEN RAISE EXCEPTION 'Transicion de giro no permitida'; END IF;
  END IF;
  SELECT * INTO r FROM reserva WHERE id_ocupacion=NEW.id_reserva FOR UPDATE;
  IF NEW.estado='EJECUTADO' THEN
    IF r.estado<>'FINALIZADA' THEN
      RAISE EXCEPTION 'Solo se gira una reserva finalizada';
    END IF;
    SELECT COALESCE(SUM(CASE WHEN clase='COBRO' THEN monto ELSE -monto END),0)
    INTO neto FROM movimiento_pago
    WHERE id_reserva=NEW.id_reserva AND estado='APROBADO';
    IF neto<=0 OR NEW.monto<>neto THEN
      RAISE EXCEPTION 'Giro debe igualar el neto cobrado (sin comision, por alcance)';
    END IF;
    IF NEW.id_admin_autorizador IS NULL OR NEW.ejecutado_en IS NULL THEN
      RAISE EXCEPTION 'Giro ejecutado requiere administrador y fecha';
    END IF;
  END IF;
  RETURN NEW;
END;
$$;
-- Pérdida cero: cada moneda retirada es reconstruible por la FK NOT NULL
-- hacia reserva.moneda, que validar_reserva impide modificar.
ALTER TABLE movimiento_pago DROP COLUMN moneda;
ALTER TABLE giro DROP COLUMN moneda;
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
