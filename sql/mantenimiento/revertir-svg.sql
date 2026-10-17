-- Aplicación detenida y artefactos Java anteriores restaurados al terminar.
-- Usar exclusivamente si la migración SVG fue aplicada.
BEGIN;
DO $$ BEGIN IF EXISTS(SELECT 1 FROM information_schema.tables WHERE table_schema=current_schema() AND table_name='asocia' AND table_type='BASE TABLE') THEN RAISE EXCEPTION 'Este script pertenece al modelo anterior. El modelo vigente usa MigrarProfesor'; END IF; END; $$;

DO $$ BEGIN
  IF current_schema() <> 'daynest' AND current_schema() !~ '^renta_test_[a-f0-9]{32}$' THEN
    RAISE EXCEPTION 'Esquema no autorizado';
  END IF;
END; $$;
LOCK TABLE usuario,reserva,movimiento_pago,giro IN ACCESS EXCLUSIVE MODE;
ALTER TABLE movimiento_pago ADD COLUMN moneda VARCHAR(3);
ALTER TABLE giro ADD COLUMN moneda VARCHAR(3);
-- Los triggers de historial resuelto no admiten UPDATE; se suspenden solo
-- estos dos triggers, bajo bloqueo exclusivo y dentro de esta transacción.
ALTER TABLE movimiento_pago DISABLE TRIGGER tg_validar_movimiento;
ALTER TABLE giro DISABLE TRIGGER tg_validar_giro;
UPDATE movimiento_pago m SET moneda=r.moneda FROM reserva r WHERE r.id_ocupacion=m.id_reserva;
UPDATE giro g SET moneda=r.moneda FROM reserva r WHERE r.id_ocupacion=g.id_reserva;
ALTER TABLE movimiento_pago ALTER COLUMN moneda SET NOT NULL;
ALTER TABLE giro ALTER COLUMN moneda SET NOT NULL;
ALTER TABLE movimiento_pago ADD CHECK(moneda ~ '^[A-Z]{3}$');
ALTER TABLE giro ADD CHECK(moneda ~ '^[A-Z]{3}$');
ALTER TABLE usuario DROP CONSTRAINT ck_usuario_correo_canonico;
ALTER TABLE usuario RENAME COLUMN correo_normalizado TO correo;
DO $$ BEGIN
  IF EXISTS(SELECT 1 FROM pg_constraint WHERE conrelid='usuario'::regclass AND conname='usuario_correo_normalizado_not_null') THEN
    ALTER TABLE usuario RENAME CONSTRAINT usuario_correo_normalizado_not_null TO usuario_correo_not_null;
  END IF;
END; $$;
DROP INDEX ux_usuario_correo_normalizado;
CREATE UNIQUE INDEX ux_usuario_correo_normalizado ON usuario(lower(correo));


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
       (NEW.id_reserva,NEW.clase,NEW.monto,NEW.moneda,NEW.id_cobro_origen,NEW.clave_idempotencia)
       IS DISTINCT FROM
       (OLD.id_reserva,OLD.clase,OLD.monto,OLD.moneda,OLD.id_cobro_origen,OLD.clave_idempotencia) THEN
      RAISE EXCEPTION 'Movimiento inmutable o ya resuelto';
    END IF;
  END IF;
  -- Bloqueo de la reserva serializa las validaciones financieras concurrentes.
  SELECT * INTO r FROM reserva WHERE id_ocupacion = NEW.id_reserva FOR UPDATE;
  IF NEW.moneda <> r.moneda THEN RAISE EXCEPTION 'Moneda distinta de la reserva'; END IF;
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
       (NEW.id_reserva,NEW.monto,NEW.moneda,NEW.clave_idempotencia)
       IS DISTINCT FROM (OLD.id_reserva,OLD.monto,OLD.moneda,OLD.clave_idempotencia) THEN
      RAISE EXCEPTION 'Giro ejecutado o condiciones historicas inmutables';
    END IF;
    IF NEW.estado <> OLD.estado AND NOT (
      (OLD.estado='PENDIENTE' AND NEW.estado IN ('PROCESANDO','FALLIDO')) OR
      (OLD.estado='PROCESANDO' AND NEW.estado IN ('EJECUTADO','FALLIDO'))
    ) THEN RAISE EXCEPTION 'Transicion de giro no permitida'; END IF;
  END IF;
  SELECT * INTO r FROM reserva WHERE id_ocupacion=NEW.id_reserva FOR UPDATE;
  IF NEW.moneda<>r.moneda THEN RAISE EXCEPTION 'Moneda del giro distinta de reserva'; END IF;
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

ALTER TABLE movimiento_pago ENABLE TRIGGER tg_validar_movimiento;
ALTER TABLE giro ENABLE TRIGGER tg_validar_giro;
COMMIT;
