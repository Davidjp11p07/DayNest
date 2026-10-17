BEGIN;
DO $$ BEGIN IF EXISTS(SELECT 1 FROM information_schema.tables WHERE table_schema=current_schema() AND table_name='asocia' AND table_type='BASE TABLE') THEN RAISE EXCEPTION 'Este script pertenece al modelo anterior. El modelo vigente usa MigrarProfesor'; END IF; END; $$;

DO $$ BEGIN
 IF current_schema()<>'daynest' AND current_schema() !~ '^renta_test_[a-f0-9]{32}$' THEN RAISE EXCEPTION 'Esquema no autorizado'; END IF;
 IF EXISTS(SELECT 1 FROM giro g JOIN reserva r ON r.id_ocupacion=g.id_reserva JOIN ocupacion o USING(id_ocupacion) WHERE g.monto_comision<>round((o.fecha_fin-o.fecha_inicio)*r.precio_noche_pactado*g.porcentaje_comision,2) OR g.monto<>((o.fecha_fin-o.fecha_inicio)*r.precio_noche_pactado)-g.monto_comision) THEN RAISE EXCEPTION 'Hay giros historicos inconsistentes: no se modificaron'; END IF;
END; $$;
SET LOCAL lock_timeout='10s';
LOCK TABLE reserva,movimiento_pago,giro IN ACCESS EXCLUSIVE MODE;
CREATE OR REPLACE FUNCTION validar_reserva() RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE
  o ocupacion%ROWTYPE;
  i inmueble%ROWTYPE;
  dinero_aprobado NUMERIC(16,2);
BEGIN
  SELECT * INTO o FROM ocupacion WHERE id_ocupacion = NEW.id_ocupacion;
  IF NOT FOUND OR o.tipo <> 'RESERVA' THEN
    RAISE EXCEPTION 'La reserva requiere una ocupacion de tipo RESERVA';
  END IF;
  SELECT * INTO i FROM inmueble WHERE id_inmueble = o.id_inmueble FOR SHARE;
  IF TG_OP = 'INSERT' THEN
    NEW.expira_en := NEW.creada_en + INTERVAL '30 minutes';
    IF i.estado <> 'PUBLICADO' THEN RAISE EXCEPTION 'Inmueble no publicado'; END IF;
    IF o.fecha_inicio < CURRENT_DATE OR NEW.expira_en <= clock_timestamp() THEN
      RAISE EXCEPTION 'Fechas de reserva o expiracion no futuras';
    END IF;
    IF NEW.precio_noche_pactado <> i.tarifa_noche THEN
      RAISE EXCEPTION 'La tarifa pactada debe igualar la publicada al reservar';
    END IF;
    IF NEW.id_propietario_pactado <> i.id_propietario THEN
      RAISE EXCEPTION 'Propietario pactado no coincide con propietario al reservar';
    END IF;
    IF NEW.moneda <> i.moneda THEN RAISE EXCEPTION 'Moneda de reserva diferente a inmueble'; END IF;
    IF NEW.cantidad_huespedes > i.capacidad THEN
      RAISE EXCEPTION 'Huespedes exceden capacidad del inmueble';
    END IF;
    IF NEW.estado <> 'PENDIENTE_PAGO' OR NEW.expira_en <= NEW.creada_en THEN
      RAISE EXCEPTION 'La reserva debe iniciar pendiente y con expiracion futura';
    END IF;
  ELSE
    IF (NEW.id_ocupacion, NEW.id_cliente, NEW.id_propietario_pactado,
        NEW.cantidad_huespedes, NEW.precio_noche_pactado, NEW.moneda, NEW.expira_en)
       IS DISTINCT FROM
       (OLD.id_ocupacion, OLD.id_cliente, OLD.id_propietario_pactado,
        OLD.cantidad_huespedes, OLD.precio_noche_pactado, OLD.moneda, OLD.expira_en) THEN
      RAISE EXCEPTION 'No se pueden alterar los terminos historicos de una reserva';
    END IF;
    IF NEW.estado <> OLD.estado THEN
      IF NOT (
        (OLD.estado = 'PENDIENTE_PAGO' AND NEW.estado IN ('CONFIRMADA','CANCELADA','EXPIRADA')) OR
        (OLD.estado = 'CONFIRMADA' AND NEW.estado IN ('FINALIZADA','CANCELADA'))
      ) THEN RAISE EXCEPTION 'Transicion de estado de reserva no permitida'; END IF;
      IF NEW.estado = 'CONFIRMADA' THEN
        IF NEW.expira_en <= clock_timestamp() THEN RAISE EXCEPTION 'La reserva vencio; no puede confirmarse'; END IF;
        SELECT COALESCE(SUM(CASE WHEN clase='COBRO' THEN monto ELSE -monto END),0)
          INTO dinero_aprobado FROM movimiento_pago
          WHERE id_reserva = NEW.id_ocupacion AND estado = 'APROBADO';
        IF dinero_aprobado <> (o.fecha_fin - o.fecha_inicio) * NEW.precio_noche_pactado THEN
          RAISE EXCEPTION 'Solo se confirma cuando el importe pactado esta pagado';
        END IF;
      END IF;
      IF NEW.estado = 'FINALIZADA' AND o.fecha_fin > CURRENT_DATE THEN
        RAISE EXCEPTION 'La estancia aun no termina';
      END IF;
      IF NEW.estado = 'CANCELADA' AND EXISTS (
        SELECT 1 FROM giro WHERE id_reserva=NEW.id_ocupacion AND estado='EJECUTADO'
      ) THEN RAISE EXCEPTION 'No puede cancelarse una reserva con giro ejecutado'; END IF;
    END IF;
  END IF;
  RETURN NEW;
END;
$$;

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
  IF NEW.clase='COBRO' AND NEW.estado='APROBADO' AND r.estado='PENDIENTE_PAGO' AND r.expira_en<=clock_timestamp() THEN
    RAISE EXCEPTION 'La reserva vencio; no puede aprobarse el cobro';
  END IF;
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
