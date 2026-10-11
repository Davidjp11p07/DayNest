-- Renta de inmuebles por dias - esquema logico de referencia, PostgreSQL 16+
-- Alcance: una reserva por inmueble, cobro en moneda del inmueble,
-- varios intentos de giro por reserva, maximo uno activo o ejecutado;
-- los bloqueos y reservas comparten calendario.
-- EJECUTAR en una base vacia con un usuario con permisos de CREATE EXTENSION.
-- La aplicacion debe usar transacciones e implementar la reconciliacion
-- de estados externos de pago/giro y la expiracion periodica de reservas.

BEGIN;
CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE TABLE usuario (
  id_usuario BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  nombre_completo VARCHAR(160) NOT NULL CHECK (length(btrim(nombre_completo)) > 0),
  correo VARCHAR(254) NOT NULL CHECK (correo = btrim(correo) AND position('@' in correo) > 1),
  contrasena_hash TEXT NOT NULL CHECK (length(contrasena_hash) > 0),
  telefono VARCHAR(30),
  activo BOOLEAN NOT NULL DEFAULT TRUE,
  creado_en TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX ux_usuario_correo_normalizado ON usuario (lower(correo));

-- Especializacion parcial, solapada: una misma persona puede estar en varios subtipos.
CREATE TABLE cliente (
  id_usuario BIGINT PRIMARY KEY REFERENCES usuario(id_usuario) ON DELETE RESTRICT
);
CREATE TABLE propietario (
  id_usuario BIGINT PRIMARY KEY REFERENCES usuario(id_usuario) ON DELETE RESTRICT
);
CREATE TABLE administrador (
  id_usuario BIGINT PRIMARY KEY REFERENCES usuario(id_usuario) ON DELETE RESTRICT
);

CREATE TABLE pais (
  id_pais BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  nombre VARCHAR(120) NOT NULL CHECK(length(btrim(nombre)) > 0),
  codigo_iso2 CHAR(2) NOT NULL UNIQUE CHECK (codigo_iso2 ~ '^[A-Z]{2}$')
);
CREATE TABLE division_administrativa (
  id_division BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  id_pais BIGINT NOT NULL REFERENCES pais(id_pais) ON DELETE RESTRICT,
  nombre VARCHAR(160) NOT NULL CHECK(length(btrim(nombre)) > 0),
  codigo_oficial VARCHAR(40) NOT NULL CHECK(length(btrim(codigo_oficial)) > 0),
  CONSTRAINT uq_division_codigo UNIQUE (id_pais, codigo_oficial)
);
CREATE TABLE ciudad (
  id_ciudad BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  id_division BIGINT NOT NULL REFERENCES division_administrativa(id_division) ON DELETE RESTRICT,
  nombre VARCHAR(160) NOT NULL CHECK(length(btrim(nombre)) > 0),
  codigo_oficial VARCHAR(40) NOT NULL CHECK(length(btrim(codigo_oficial)) > 0),
  CONSTRAINT uq_ciudad_codigo UNIQUE (id_division, codigo_oficial)
);
-- El nombre NO es UNIQUE: los homonimos pueden tener codigos distintos.

CREATE TABLE inmueble (
  id_inmueble BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  id_propietario BIGINT NOT NULL REFERENCES propietario(id_usuario) ON DELETE RESTRICT,
  id_ciudad BIGINT NOT NULL REFERENCES ciudad(id_ciudad) ON DELETE RESTRICT,
  titulo VARCHAR(150) NOT NULL CHECK(length(btrim(titulo)) > 0),
  descripcion TEXT,
  direccion_detallada VARCHAR(300) NOT NULL CHECK(length(btrim(direccion_detallada)) > 0),
  tipo VARCHAR(20) NOT NULL CHECK(tipo IN ('APARTAMENTO','CASA','HABITACION','OTRO')),
  capacidad SMALLINT NOT NULL CHECK(capacidad > 0),
  tarifa_noche NUMERIC(16,2) NOT NULL CHECK(tarifa_noche > 0),
  moneda VARCHAR(3) NOT NULL CHECK(moneda ~ '^[A-Z]{3}$'),
  estado VARCHAR(12) NOT NULL DEFAULT 'BORRADOR'
    CHECK(estado IN ('BORRADOR','PUBLICADO','INACTIVO')),
  creado_en TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Hecho comun: un periodo ocupa o bloquea un inmueble (no ambos).
CREATE TABLE ocupacion (
  id_ocupacion BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  id_inmueble BIGINT NOT NULL REFERENCES inmueble(id_inmueble) ON DELETE RESTRICT,
  fecha_inicio DATE NOT NULL,
  fecha_fin DATE NOT NULL,
  tipo VARCHAR(8) NOT NULL CHECK(tipo IN ('RESERVA','BLOQUEO')),
  activa BOOLEAN NOT NULL DEFAULT TRUE,
  creada_en TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT ck_ocupacion_fechas CHECK(fecha_fin > fecha_inicio),
  CONSTRAINT ex_ocupacion_no_solape EXCLUDE USING GIST (
    id_inmueble WITH =,
    daterange(fecha_inicio, fecha_fin, '[)') WITH &&
  ) WHERE (activa)
);

CREATE TABLE reserva (
  -- PK compartida con OCUPACION; existe exactamente una fila asociada.
  id_ocupacion BIGINT PRIMARY KEY REFERENCES ocupacion(id_ocupacion) ON DELETE RESTRICT,
  id_cliente BIGINT NOT NULL REFERENCES cliente(id_usuario) ON DELETE RESTRICT,
  id_propietario_pactado BIGINT NOT NULL REFERENCES propietario(id_usuario) ON DELETE RESTRICT,
  cantidad_huespedes SMALLINT NOT NULL CHECK(cantidad_huespedes > 0),
  precio_noche_pactado NUMERIC(16,2) NOT NULL CHECK(precio_noche_pactado > 0),
  moneda VARCHAR(3) NOT NULL CHECK(moneda ~ '^[A-Z]{3}$'),
  estado VARCHAR(16) NOT NULL DEFAULT 'PENDIENTE_PAGO'
    CHECK(estado IN ('PENDIENTE_PAGO','CONFIRMADA','FINALIZADA','CANCELADA','EXPIRADA')),
  expira_en TIMESTAMPTZ NOT NULL,
  creada_en TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE bloqueo (
  id_ocupacion BIGINT PRIMARY KEY REFERENCES ocupacion(id_ocupacion) ON DELETE RESTRICT,
  id_propietario_solicitante BIGINT NOT NULL REFERENCES propietario(id_usuario) ON DELETE RESTRICT,
  motivo VARCHAR(250) NOT NULL CHECK(length(btrim(motivo)) > 0)
);

CREATE TABLE movimiento_pago (
  id_movimiento BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  id_reserva BIGINT NOT NULL REFERENCES reserva(id_ocupacion) ON DELETE RESTRICT,
  clase VARCHAR(12) NOT NULL CHECK(clase IN ('COBRO','DEVOLUCION')),
  monto NUMERIC(16,2) NOT NULL CHECK(monto > 0),
  moneda VARCHAR(3) NOT NULL CHECK(moneda ~ '^[A-Z]{3}$'),
  estado VARCHAR(12) NOT NULL DEFAULT 'PENDIENTE'
    CHECK(estado IN ('PENDIENTE','APROBADO','RECHAZADO')),
  id_cobro_origen BIGINT,
  clave_idempotencia VARCHAR(120) NOT NULL UNIQUE,
  referencia_externa VARCHAR(140) UNIQUE,
  creado_en TIMESTAMPTZ NOT NULL DEFAULT now(),
  resuelto_en TIMESTAMPTZ,
  CONSTRAINT ck_devolucion_origen CHECK(
    (clase = 'COBRO' AND id_cobro_origen IS NULL) OR
    (clase = 'DEVOLUCION' AND id_cobro_origen IS NOT NULL)
  ),
  CONSTRAINT uq_movimiento_reserva UNIQUE(id_movimiento, id_reserva),
  CONSTRAINT fk_devolucion_misma_reserva FOREIGN KEY(id_cobro_origen, id_reserva)
    REFERENCES movimiento_pago(id_movimiento, id_reserva) ON DELETE RESTRICT
);

CREATE TABLE giro (
  id_giro BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  id_reserva BIGINT NOT NULL REFERENCES reserva(id_ocupacion) ON DELETE RESTRICT,
  id_admin_autorizador BIGINT REFERENCES administrador(id_usuario) ON DELETE RESTRICT,
  monto NUMERIC(16,2) NOT NULL CHECK(monto > 0),
  moneda VARCHAR(3) NOT NULL CHECK(moneda ~ '^[A-Z]{3}$'),
  estado VARCHAR(12) NOT NULL DEFAULT 'PENDIENTE'
    CHECK(estado IN ('PENDIENTE','PROCESANDO','EJECUTADO','FALLIDO')),
  clave_idempotencia VARCHAR(120) NOT NULL UNIQUE,
  referencia_externa VARCHAR(140) UNIQUE,
  creado_en TIMESTAMPTZ NOT NULL DEFAULT now(),
  ejecutado_en TIMESTAMPTZ,
  CONSTRAINT ck_giro_ejecutado CHECK (
    estado <> 'EJECUTADO' OR
    (id_admin_autorizador IS NOT NULL AND ejecutado_en IS NOT NULL)
  )
);

-- Se conservan giros fallidos para auditoria y se permite un NUEVO intento.
-- En cada instante, maximo un giro pendiente/procesando/ejecutado por reserva.
CREATE UNIQUE INDEX ux_giro_unico_activo_o_ejecutado ON giro(id_reserva)
WHERE estado IN ('PENDIENTE','PROCESANDO','EJECUTADO');

CREATE INDEX ix_division_pais ON division_administrativa(id_pais);
CREATE INDEX ix_ciudad_division ON ciudad(id_division);
CREATE INDEX ix_inmueble_ciudad_estado ON inmueble(id_ciudad, estado);
CREATE INDEX ix_inmueble_propietario ON inmueble(id_propietario);
CREATE INDEX ix_ocupacion_inmueble ON ocupacion(id_inmueble);
CREATE INDEX ix_reserva_cliente ON reserva(id_cliente);
CREATE INDEX ix_reserva_propietario_historico ON reserva(id_propietario_pactado);
CREATE INDEX ix_movimiento_reserva_estado ON movimiento_pago(id_reserva, estado);

-- Las reglas siguientes complementan las FK/CHECK/EXCLUDE. No sustituyen
-- las transacciones del servicio ni una integracion financiera real.

-- No se permite reescribir el inmueble ni las fechas historicas.
-- No reducir capacidad por debajo de ocupaciones futuras activas.
CREATE FUNCTION proteger_capacidad_inmueble() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
  IF NEW.capacidad < OLD.capacidad AND EXISTS (
    SELECT 1 FROM ocupacion o JOIN reserva r ON r.id_ocupacion=o.id_ocupacion
    WHERE o.id_inmueble=OLD.id_inmueble AND o.activa
      AND o.fecha_fin > CURRENT_DATE AND r.cantidad_huespedes > NEW.capacidad
  ) THEN
    RAISE EXCEPTION 'La nueva capacidad es menor que reservas futuras vigentes';
  END IF;
  RETURN NEW;
END;
$$;
CREATE TRIGGER tg_proteger_capacidad BEFORE UPDATE OF capacidad ON inmueble
FOR EACH ROW EXECUTE FUNCTION proteger_capacidad_inmueble();

CREATE FUNCTION impedir_reescritura_ocupacion() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
  IF (NEW.id_inmueble, NEW.fecha_inicio, NEW.fecha_fin, NEW.tipo)
      IS DISTINCT FROM (OLD.id_inmueble, OLD.fecha_inicio, OLD.fecha_fin, OLD.tipo) THEN
    RAISE EXCEPTION 'Fechas, inmueble y tipo de ocupacion son inmutables';
  END IF;
  RETURN NEW;
END;
$$;
CREATE TRIGGER tg_ocupacion_inmutable BEFORE UPDATE ON ocupacion
FOR EACH ROW EXECUTE FUNCTION impedir_reescritura_ocupacion();

CREATE FUNCTION validar_reserva() RETURNS TRIGGER LANGUAGE plpgsql AS $$
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
CREATE TRIGGER tg_validar_reserva BEFORE INSERT OR UPDATE ON reserva
FOR EACH ROW EXECUTE FUNCTION validar_reserva();

CREATE FUNCTION sincronizar_ocupacion_reserva() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
  UPDATE ocupacion SET activa = (NEW.estado NOT IN ('CANCELADA','EXPIRADA'))
  WHERE id_ocupacion = NEW.id_ocupacion;
  RETURN NEW;
END;
$$;
CREATE TRIGGER tg_sincronizar_ocupacion AFTER INSERT OR UPDATE OF estado ON reserva
FOR EACH ROW EXECUTE FUNCTION sincronizar_ocupacion_reserva();

CREATE FUNCTION validar_bloqueo() RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE p BIGINT;
BEGIN
  SELECT i.id_propietario INTO p
  FROM ocupacion o JOIN inmueble i USING(id_inmueble)
  WHERE o.id_ocupacion = NEW.id_ocupacion AND o.tipo = 'BLOQUEO'
  FOR SHARE OF i;
  IF NOT FOUND OR p <> NEW.id_propietario_solicitante THEN
    RAISE EXCEPTION 'El bloqueo exige el propietario actual y el tipo BLOQUEO';
  END IF;
  IF TG_OP = 'UPDATE' AND
     (NEW.id_ocupacion,NEW.id_propietario_solicitante) IS DISTINCT FROM
     (OLD.id_ocupacion,OLD.id_propietario_solicitante) THEN
    RAISE EXCEPTION 'No puede cambiarse la ocupacion ni el propietario historico del bloqueo';
  END IF;
  RETURN NEW;
END;
$$;
CREATE TRIGGER tg_validar_bloqueo BEFORE INSERT OR UPDATE ON bloqueo
FOR EACH ROW EXECUTE FUNCTION validar_bloqueo();

-- Invariante total/disjunto de subtipos: cada OCUPACION pertenece
-- exactamente a RESERVA o BLOQUEO, segun su tipo.
-- Es una comprobacion diferida porque primero se crea OCUPACION y luego su subtipo.
CREATE FUNCTION verificar_subtipo_ocupacion() RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE
  oid BIGINT;
  o ocupacion%ROWTYPE;
  nres INTEGER;
  nblo INTEGER;
  rst VARCHAR(16);
BEGIN
  oid := CASE WHEN TG_OP='DELETE' THEN OLD.id_ocupacion ELSE NEW.id_ocupacion END;
  SELECT * INTO o FROM ocupacion WHERE id_ocupacion = oid;
  IF NOT FOUND THEN RETURN NULL; END IF;
  SELECT count(*) INTO nres FROM reserva WHERE id_ocupacion=oid;
  SELECT count(*) INTO nblo FROM bloqueo WHERE id_ocupacion=oid;
  IF (o.tipo='RESERVA' AND (nres<>1 OR nblo<>0)) OR
     (o.tipo='BLOQUEO' AND (nblo<>1 OR nres<>0)) THEN
    RAISE EXCEPTION 'Ocupacion debe pertenecer exactamente a su subtipo %',o.tipo;
  END IF;
  IF o.tipo='RESERVA' THEN
    SELECT estado INTO rst FROM reserva WHERE id_ocupacion=oid;
    IF o.activa IS DISTINCT FROM (rst NOT IN ('CANCELADA','EXPIRADA')) THEN
      RAISE EXCEPTION 'Ocupacion y estado de reserva inconsistentes';
    END IF;
  END IF;
  RETURN NULL;
END;
$$;
CREATE CONSTRAINT TRIGGER ck_ocupacion_subtipo
AFTER INSERT OR UPDATE OR DELETE ON ocupacion
DEFERRABLE INITIALLY DEFERRED FOR EACH ROW
EXECUTE FUNCTION verificar_subtipo_ocupacion();
CREATE CONSTRAINT TRIGGER ck_reserva_subtipo
AFTER INSERT OR UPDATE OR DELETE ON reserva
DEFERRABLE INITIALLY DEFERRED FOR EACH ROW
EXECUTE FUNCTION verificar_subtipo_ocupacion();
CREATE CONSTRAINT TRIGGER ck_bloqueo_subtipo
AFTER INSERT OR UPDATE OR DELETE ON bloqueo
DEFERRABLE INITIALLY DEFERRED FOR EACH ROW
EXECUTE FUNCTION verificar_subtipo_ocupacion();

CREATE FUNCTION validar_movimiento_pago() RETURNS TRIGGER LANGUAGE plpgsql AS $$
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
CREATE TRIGGER tg_validar_movimiento BEFORE INSERT OR UPDATE ON movimiento_pago
FOR EACH ROW EXECUTE FUNCTION validar_movimiento_pago();

CREATE FUNCTION validar_giro() RETURNS TRIGGER LANGUAGE plpgsql AS $$
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
CREATE TRIGGER tg_validar_giro BEFORE INSERT OR UPDATE ON giro
FOR EACH ROW EXECUTE FUNCTION validar_giro();

CREATE FUNCTION prohibir_borrado_financiero() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
  RAISE EXCEPTION 'Historial inmutable: usar estados, no DELETE en %',TG_TABLE_NAME;
END;
$$;
CREATE TRIGGER no_delete_reserva BEFORE DELETE ON reserva
FOR EACH ROW EXECUTE FUNCTION prohibir_borrado_financiero();
CREATE TRIGGER no_delete_movimiento BEFORE DELETE ON movimiento_pago
FOR EACH ROW EXECUTE FUNCTION prohibir_borrado_financiero();
CREATE TRIGGER no_delete_giro BEFORE DELETE ON giro
FOR EACH ROW EXECUTE FUNCTION prohibir_borrado_financiero();

COMMIT;

-- Flujo minimo para crear una reserva (mismo BEGIN/COMMIT):
-- 1. INSERT ocupacion(id_inmueble,fecha_inicio,fecha_fin,tipo,activa)
--    VALUES(...,'RESERVA',TRUE) RETURNING id_ocupacion;
-- 2. INSERT reserva(id_ocupacion,id_cliente,id_propietario_pactado,
--    cantidad_huespedes,precio_noche_pactado,moneda,expira_en) VALUES(...);
-- 3. COMMIT. El constraint trigger valida la especializacion completa.
-- 4. Si vence el plazo, UPDATE reserva SET estado='EXPIRADA' ...
--    mediante tarea periodica. No usar now() dentro del predicado EXCLUDE.
-- 5. No confirmar pagos por peticiones del navegador: solo mediante
--    un webhook autenticado / conciliacion con el proveedor de pagos.
-- 6. Si el giro es FALLIDO, no sobrescribir: crear uno nuevo con nueva
--    clave idempotente, despues de conciliar con el proveedor.
-- 7. Realizar los pasos dependientes de pagos/giro en una transaccion.
-- NOTA: los pagos y los giros REALES requieren conciliacion, manejo de
-- fallos externos, estrategias de reintento y control de permisos de BD.
