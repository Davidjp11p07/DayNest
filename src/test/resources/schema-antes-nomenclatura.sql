-- DayNest: modelo del profesor. Diez tablas de negocio y siete tablas de soporte.
-- Negocio: cliente, propietario, banco, inmueble, ciudad, pais, asocia, vincula, relaciona, renta.
-- Soporte: usuario, administrador, division_administrativa, ocupacion, bloqueo, movimiento_pago, giro.
-- reserva e inmueble_operativo son vistas sin almacenamiento duplicado.
BEGIN;
CREATE EXTENSION IF NOT EXISTS btree_gist;
--
-- PostgreSQL database dump
--


-- Dumped from database version 18.6
-- Dumped by pg_dump version 18.6

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: renta_test_4513b510c955478abb72b5aa6e7f80da; Type: SCHEMA; Schema: -; Owner: -
--



--
-- Name: completar_entidad_persona(); Type: FUNCTION; Schema: esquema operativo; Owner: -
--

CREATE FUNCTION completar_entidad_persona() RETURNS trigger
    LANGUAGE plpgsql
    AS $$

DECLARE u usuario%ROWTYPE; uid BIGINT;

BEGIN

 uid:=NEW.id_usuario;

 IF TG_TABLE_NAME='cliente' THEN uid:=coalesce(uid,NEW.id_cliente); ELSE uid:=coalesce(uid,NEW.id_propietario); END IF;

 SELECT * INTO u FROM usuario WHERE id_usuario=uid;

 IF NOT FOUND THEN RAISE EXCEPTION 'Cuenta de acceso inexistente'; END IF;

 NEW.id_usuario:=uid;

 IF TG_TABLE_NAME='cliente' THEN NEW.id_cliente:=uid; ELSE NEW.id_propietario:=uid; END IF;

 NEW.nombre_completo:=u.nombre_completo;NEW.correo_normalizado:=u.correo_normalizado;NEW.telefono:=u.telefono;

 RETURN NEW;

END; $$;


--
-- Name: completar_pais_ciudad(); Type: FUNCTION; Schema: esquema operativo; Owner: -
--

CREATE FUNCTION completar_pais_ciudad() RETURNS trigger
    LANGUAGE plpgsql
    AS $$ DECLARE p BIGINT; BEGIN

 SELECT id_pais INTO p FROM division_administrativa WHERE id_division=NEW.id_division;

 IF NEW.id_pais IS NOT NULL AND NEW.id_pais<>p THEN RAISE EXCEPTION 'Pais incompatible con region'; END IF;

 NEW.id_pais:=p;RETURN NEW;

END; $$;


--
-- Name: completar_renta_calendario(); Type: FUNCTION; Schema: esquema operativo; Owner: -
--

CREATE FUNCTION completar_renta_calendario() RETURNS trigger
    LANGUAGE plpgsql
    AS $$ DECLARE o ocupacion%ROWTYPE; BEGIN

 SELECT * INTO o FROM ocupacion WHERE id_ocupacion=NEW.id_ocupacion;

 IF (NEW.id_inmueble IS NOT NULL AND NEW.id_inmueble<>o.id_inmueble) OR (NEW.fecha_entrada IS NOT NULL AND NEW.fecha_entrada<>o.fecha_inicio) OR (NEW.fecha_salida IS NOT NULL AND NEW.fecha_salida<>o.fecha_fin) THEN RAISE EXCEPTION 'Renta y calendario no coinciden'; END IF;

 NEW.id_inmueble:=o.id_inmueble;NEW.fecha_entrada:=o.fecha_inicio;NEW.fecha_salida:=o.fecha_fin;RETURN NEW;

END; $$;


--
-- Name: escribir_inmueble_operativo(); Type: FUNCTION; Schema: esquema operativo; Owner: -
--

CREATE FUNCTION escribir_inmueble_operativo() RETURNS trigger
    LANGUAGE plpgsql
    AS $$ BEGIN

 IF TG_OP='DELETE' THEN DELETE FROM inmueble WHERE id_inmueble=OLD.id_inmueble; RETURN OLD; END IF;

 IF TG_OP='INSERT' THEN

  IF NEW.id_inmueble IS NULL THEN

   INSERT INTO inmueble(id_ciudad,titulo,descripcion,direccion_detallada,tipo,es_rural,cerca_mar,capacidad,moneda,estado,area_m2,habitaciones)

   VALUES(NEW.id_ciudad,NEW.titulo,NEW.descripcion,NEW.direccion_detallada,NEW.tipo,NEW.es_rural,NEW.cerca_mar,NEW.capacidad,NEW.moneda,coalesce(NEW.estado,'BORRADOR'),NEW.area_m2,NEW.habitaciones) RETURNING id_inmueble,creado_en INTO NEW.id_inmueble,NEW.creado_en;

  ELSE

   INSERT INTO inmueble(id_inmueble,id_ciudad,titulo,descripcion,direccion_detallada,tipo,es_rural,cerca_mar,capacidad,moneda,estado,area_m2,habitaciones) OVERRIDING SYSTEM VALUE

   VALUES(NEW.id_inmueble,NEW.id_ciudad,NEW.titulo,NEW.descripcion,NEW.direccion_detallada,NEW.tipo,NEW.es_rural,NEW.cerca_mar,NEW.capacidad,NEW.moneda,coalesce(NEW.estado,'BORRADOR'),NEW.area_m2,NEW.habitaciones);

  END IF;

  INSERT INTO asocia(id_inmueble,id_propietario,precio_noche) VALUES(NEW.id_inmueble,NEW.id_propietario,NEW.tarifa_noche);

 ELSE

  UPDATE inmueble SET id_ciudad=NEW.id_ciudad,titulo=NEW.titulo,descripcion=NEW.descripcion,direccion_detallada=NEW.direccion_detallada,tipo=NEW.tipo,es_rural=NEW.es_rural,cerca_mar=NEW.cerca_mar,capacidad=NEW.capacidad,moneda=NEW.moneda,estado=NEW.estado,area_m2=NEW.area_m2,habitaciones=NEW.habitaciones WHERE id_inmueble=OLD.id_inmueble;

  UPDATE asocia SET id_propietario=NEW.id_propietario,precio_noche=NEW.tarifa_noche WHERE id_inmueble=OLD.id_inmueble;

 END IF;

 RETURN NEW;

END; $$;


--
-- Name: guardar_cuenta_financiera(); Type: FUNCTION; Schema: esquema operativo; Owner: -
--

CREATE FUNCTION guardar_cuenta_financiera() RETURNS trigger
    LANGUAGE plpgsql
    AS $$ DECLARE dato JSONB; BEGIN

 IF TG_OP='UPDATE' THEN

  IF NEW.cuenta_bancaria_pactada IS DISTINCT FROM OLD.cuenta_bancaria_pactada THEN RAISE EXCEPTION 'Cuenta bancaria historica inmutable'; END IF;

  RETURN NEW;

 END IF;

 IF TG_TABLE_NAME='giro' THEN

  SELECT jsonb_build_object('banco',b.nombre,'id_banco',b.id_banco,'numero_cuenta',x.numero_cuenta,'tipo_cuenta',x.tipo_cuenta) INTO dato FROM relaciona x JOIN banco b USING(id_banco) JOIN renta r ON r.id_propietario_pactado=x.id_propietario WHERE r.id_ocupacion=NEW.id_reserva;

 ELSE

  SELECT jsonb_build_object('banco',b.nombre,'id_banco',b.id_banco,'numero_cuenta',x.numero_cuenta,'tipo_cuenta',x.tipo_cuenta) INTO dato FROM vincula x JOIN banco b USING(id_banco) JOIN renta r ON r.id_cliente=x.id_cliente WHERE r.id_ocupacion=NEW.id_reserva;

 END IF;

 NEW.cuenta_bancaria_pactada:=dato;RETURN NEW;

END; $$;


--
-- Name: impedir_reescritura_ocupacion(); Type: FUNCTION; Schema: esquema operativo; Owner: -
--

CREATE FUNCTION impedir_reescritura_ocupacion() RETURNS trigger
    LANGUAGE plpgsql
    AS $$

BEGIN

  IF (NEW.id_inmueble, NEW.fecha_inicio, NEW.fecha_fin, NEW.tipo)

      IS DISTINCT FROM (OLD.id_inmueble, OLD.fecha_inicio, OLD.fecha_fin, OLD.tipo) THEN

    RAISE EXCEPTION 'Fechas, inmueble y tipo de ocupacion son inmutables';

  END IF;

  RETURN NEW;

END;

$$;


--
-- Name: prohibir_borrado_financiero(); Type: FUNCTION; Schema: esquema operativo; Owner: -
--

CREATE FUNCTION prohibir_borrado_financiero() RETURNS trigger
    LANGUAGE plpgsql
    AS $$

BEGIN

  RAISE EXCEPTION 'Historial inmutable: usar estados, no DELETE en %',TG_TABLE_NAME;

END;

$$;


--
-- Name: proteger_capacidad_inmueble(); Type: FUNCTION; Schema: esquema operativo; Owner: -
--

CREATE FUNCTION proteger_capacidad_inmueble() RETURNS trigger
    LANGUAGE plpgsql
    AS $$

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


--
-- Name: sincronizar_calendario_renta(); Type: FUNCTION; Schema: esquema operativo; Owner: -
--

CREATE FUNCTION sincronizar_calendario_renta() RETURNS trigger
    LANGUAGE plpgsql
    AS $$ BEGIN

 UPDATE renta SET id_inmueble=NEW.id_inmueble,fecha_entrada=NEW.fecha_inicio,fecha_salida=NEW.fecha_fin WHERE id_ocupacion=NEW.id_ocupacion;RETURN NEW;

END; $$;


--
-- Name: sincronizar_identidad_persona(); Type: FUNCTION; Schema: esquema operativo; Owner: -
--

CREATE FUNCTION sincronizar_identidad_persona() RETURNS trigger
    LANGUAGE plpgsql
    AS $$ BEGIN

 UPDATE cliente SET nombre_completo=NEW.nombre_completo,correo_normalizado=NEW.correo_normalizado,telefono=NEW.telefono WHERE id_usuario=NEW.id_usuario;

 UPDATE propietario SET nombre_completo=NEW.nombre_completo,correo_normalizado=NEW.correo_normalizado,telefono=NEW.telefono WHERE id_usuario=NEW.id_usuario;

 RETURN NEW;

END; $$;


--
-- Name: sincronizar_ocupacion_reserva(); Type: FUNCTION; Schema: esquema operativo; Owner: -
--

CREATE FUNCTION sincronizar_ocupacion_reserva() RETURNS trigger
    LANGUAGE plpgsql
    AS $$

BEGIN

  UPDATE ocupacion SET activa = (NEW.estado NOT IN ('CANCELADA','EXPIRADA'))

  WHERE id_ocupacion = NEW.id_ocupacion;

  RETURN NEW;

END;

$$;


--
-- Name: validar_bloqueo(); Type: FUNCTION; Schema: esquema operativo; Owner: -
--

CREATE FUNCTION validar_bloqueo() RETURNS trigger
    LANGUAGE plpgsql
    AS $$

DECLARE p BIGINT;

BEGIN

  SELECT i.id_propietario INTO p

  FROM ocupacion o JOIN inmueble_operativo i USING(id_inmueble)

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


--
-- Name: validar_giro(); Type: FUNCTION; Schema: esquema operativo; Owner: -
--

CREATE FUNCTION validar_giro() RETURNS trigger
    LANGUAGE plpgsql
    AS $$

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


--
-- Name: validar_movimiento_pago(); Type: FUNCTION; Schema: esquema operativo; Owner: -
--

CREATE FUNCTION validar_movimiento_pago() RETURNS trigger
    LANGUAGE plpgsql
    AS $$

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


--
-- Name: validar_reserva(); Type: FUNCTION; Schema: esquema operativo; Owner: -
--

CREATE FUNCTION validar_reserva() RETURNS trigger
    LANGUAGE plpgsql
    AS $$

DECLARE

  o ocupacion%ROWTYPE;

  i inmueble_operativo%ROWTYPE;

  dinero_aprobado NUMERIC(16,2);

BEGIN

  SELECT * INTO o FROM ocupacion WHERE id_ocupacion = NEW.id_ocupacion;

  IF NOT FOUND OR o.tipo <> 'RESERVA' THEN

    RAISE EXCEPTION 'La reserva requiere una ocupacion de tipo RESERVA';

  END IF;

  SELECT * INTO i FROM inmueble_operativo WHERE id_inmueble = o.id_inmueble FOR SHARE;

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


--
-- Name: verificar_asocia(); Type: FUNCTION; Schema: esquema operativo; Owner: -
--

CREATE FUNCTION verificar_asocia() RETURNS trigger
    LANGUAGE plpgsql
    AS $$ DECLARE k BIGINT; BEGIN

 k:=CASE WHEN TG_OP='DELETE' THEN OLD.id_inmueble ELSE NEW.id_inmueble END;

 IF EXISTS(SELECT 1 FROM inmueble WHERE id_inmueble=k) AND NOT EXISTS(SELECT 1 FROM asocia WHERE id_inmueble=k) THEN RAISE EXCEPTION 'El inmueble requiere propietario y precio en ASOCIA'; END IF;

 RETURN NULL;

END; $$;


--
-- Name: verificar_subtipo_ocupacion(); Type: FUNCTION; Schema: esquema operativo; Owner: -
--

CREATE FUNCTION verificar_subtipo_ocupacion() RETURNS trigger
    LANGUAGE plpgsql
    AS $$

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


SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: administrador; Type: TABLE; Schema: esquema operativo; Owner: -
--

CREATE TABLE administrador (
    id_usuario bigint NOT NULL
);


--
-- Name: asocia; Type: TABLE; Schema: esquema operativo; Owner: -
--

CREATE TABLE asocia (
    id_inmueble bigint NOT NULL,
    id_propietario bigint NOT NULL,
    precio_noche numeric(16,2) NOT NULL,
    CONSTRAINT asocia_precio_noche_check CHECK ((precio_noche > (0)::numeric))
);


--
-- Name: banco; Type: TABLE; Schema: esquema operativo; Owner: -
--

CREATE TABLE banco (
    id_banco bigint NOT NULL,
    nombre character varying(160) NOT NULL,
    id_pais bigint NOT NULL,
    CONSTRAINT banco_nombre_check CHECK ((length(btrim((nombre)::text)) > 0))
);


--
-- Name: banco_id_banco_seq; Type: SEQUENCE; Schema: esquema operativo; Owner: -
--

ALTER TABLE banco ALTER COLUMN id_banco ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME banco_id_banco_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: bloqueo; Type: TABLE; Schema: esquema operativo; Owner: -
--

CREATE TABLE bloqueo (
    id_ocupacion bigint NOT NULL,
    id_propietario_solicitante bigint NOT NULL,
    motivo character varying(250) NOT NULL,
    CONSTRAINT bloqueo_motivo_check CHECK ((length(btrim((motivo)::text)) > 0))
);


--
-- Name: ciudad; Type: TABLE; Schema: esquema operativo; Owner: -
--

CREATE TABLE ciudad (
    id_ciudad bigint NOT NULL,
    id_division bigint NOT NULL,
    nombre character varying(160) NOT NULL,
    codigo_oficial character varying(40) NOT NULL,
    id_pais bigint NOT NULL,
    CONSTRAINT ciudad_codigo_oficial_check CHECK ((length(btrim((codigo_oficial)::text)) > 0)),
    CONSTRAINT ciudad_nombre_check CHECK ((length(btrim((nombre)::text)) > 0))
);


--
-- Name: ciudad_id_ciudad_seq; Type: SEQUENCE; Schema: esquema operativo; Owner: -
--

ALTER TABLE ciudad ALTER COLUMN id_ciudad ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME ciudad_id_ciudad_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: cliente; Type: TABLE; Schema: esquema operativo; Owner: -
--

CREATE TABLE cliente (
    id_cliente bigint CONSTRAINT cliente_id_usuario_not_null NOT NULL,
    id_usuario bigint CONSTRAINT cliente_id_usuario_not_null1 NOT NULL,
    nombre_completo character varying(160) NOT NULL,
    correo_normalizado character varying(254) NOT NULL,
    telefono character varying(30),
    id_ciudad bigint
);


--
-- Name: division_administrativa; Type: TABLE; Schema: esquema operativo; Owner: -
--

CREATE TABLE division_administrativa (
    id_division bigint NOT NULL,
    id_pais bigint NOT NULL,
    nombre character varying(160) NOT NULL,
    codigo_oficial character varying(40) NOT NULL,
    CONSTRAINT division_administrativa_codigo_oficial_check CHECK ((length(btrim((codigo_oficial)::text)) > 0)),
    CONSTRAINT division_administrativa_nombre_check CHECK ((length(btrim((nombre)::text)) > 0))
);


--
-- Name: division_administrativa_id_division_seq; Type: SEQUENCE; Schema: esquema operativo; Owner: -
--

ALTER TABLE division_administrativa ALTER COLUMN id_division ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME division_administrativa_id_division_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: giro; Type: TABLE; Schema: esquema operativo; Owner: -
--

CREATE TABLE giro (
    porcentaje_comision numeric(5,4) DEFAULT 0 NOT NULL,
    monto_comision numeric(16,2) DEFAULT 0 NOT NULL,
    id_giro bigint NOT NULL,
    id_reserva bigint NOT NULL,
    id_admin_autorizador bigint,
    monto numeric(16,2) NOT NULL,
    estado character varying(12) DEFAULT 'PENDIENTE'::character varying NOT NULL,
    clave_idempotencia character varying(120) NOT NULL,
    referencia_externa character varying(140),
    creado_en timestamp with time zone DEFAULT now() NOT NULL,
    ejecutado_en timestamp with time zone,
    cuenta_bancaria_pactada jsonb,
    CONSTRAINT ck_giro_ejecutado CHECK ((((estado)::text <> 'EJECUTADO'::text) OR ((id_admin_autorizador IS NOT NULL) AND (ejecutado_en IS NOT NULL)))),
    CONSTRAINT giro_estado_check CHECK (((estado)::text = ANY ((ARRAY['PENDIENTE'::character varying, 'PROCESANDO'::character varying, 'EJECUTADO'::character varying, 'FALLIDO'::character varying])::text[]))),
    CONSTRAINT giro_monto_check CHECK ((monto > (0)::numeric)),
    CONSTRAINT giro_monto_comision_check CHECK ((monto_comision >= (0)::numeric)),
    CONSTRAINT giro_porcentaje_comision_check CHECK (((porcentaje_comision >= (0)::numeric) AND (porcentaje_comision < (1)::numeric)))
);


--
-- Name: giro_id_giro_seq; Type: SEQUENCE; Schema: esquema operativo; Owner: -
--

ALTER TABLE giro ALTER COLUMN id_giro ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME giro_id_giro_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: inmueble; Type: TABLE; Schema: esquema operativo; Owner: -
--

CREATE TABLE inmueble (
    id_inmueble bigint NOT NULL,
    id_ciudad bigint NOT NULL,
    titulo character varying(150) NOT NULL,
    descripcion text,
    direccion_detallada character varying(300) NOT NULL,
    tipo character varying(20),
    es_rural boolean,
    cerca_mar boolean,
    capacidad smallint NOT NULL,
    moneda character varying(3) NOT NULL,
    estado character varying(12) DEFAULT 'BORRADOR'::character varying NOT NULL,
    creado_en timestamp with time zone DEFAULT now() NOT NULL,
    area_m2 numeric(12,2),
    habitaciones smallint,
    CONSTRAINT inmueble_area_m2_check CHECK ((area_m2 > (0)::numeric)),
    CONSTRAINT inmueble_capacidad_check CHECK ((capacidad > 0)),
    CONSTRAINT inmueble_direccion_detallada_check CHECK ((length(btrim((direccion_detallada)::text)) > 0)),
    CONSTRAINT inmueble_estado_check CHECK (((estado)::text = ANY ((ARRAY['BORRADOR'::character varying, 'PUBLICADO'::character varying, 'INACTIVO'::character varying])::text[]))),
    CONSTRAINT inmueble_habitaciones_check CHECK ((habitaciones > 0)),
    CONSTRAINT inmueble_moneda_check CHECK (((moneda)::text ~ '^[A-Z]{3}$'::text)),
    CONSTRAINT inmueble_tipo_check CHECK (((tipo)::text = ANY ((ARRAY['APARTAMENTO'::character varying, 'CASA'::character varying, 'CABAÑA'::character varying, 'HABITACION'::character varying, 'OTRO'::character varying])::text[]))),
    CONSTRAINT inmueble_titulo_check CHECK ((length(btrim((titulo)::text)) > 0))
);


--
-- Name: inmueble_id_inmueble_seq; Type: SEQUENCE; Schema: esquema operativo; Owner: -
--

ALTER TABLE inmueble ALTER COLUMN id_inmueble ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME inmueble_id_inmueble_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: inmueble_operativo; Type: VIEW; Schema: esquema operativo; Owner: -
--

CREATE VIEW inmueble_operativo AS
 SELECT i.id_inmueble,
    a.id_propietario,
    i.id_ciudad,
    i.titulo,
    i.descripcion,
    i.direccion_detallada,
    i.tipo,
    i.es_rural,
    i.cerca_mar,
    i.capacidad,
    a.precio_noche AS tarifa_noche,
    i.moneda,
    i.estado,
    i.creado_en,
    i.area_m2,
    i.habitaciones
   FROM (inmueble i
     JOIN asocia a USING (id_inmueble));


--
-- Name: movimiento_pago; Type: TABLE; Schema: esquema operativo; Owner: -
--

CREATE TABLE movimiento_pago (
    id_movimiento bigint NOT NULL,
    id_reserva bigint NOT NULL,
    clase character varying(12) NOT NULL,
    monto numeric(16,2) NOT NULL,
    estado character varying(12) DEFAULT 'PENDIENTE'::character varying NOT NULL,
    id_cobro_origen bigint,
    clave_idempotencia character varying(120) NOT NULL,
    referencia_externa character varying(140),
    creado_en timestamp with time zone DEFAULT now() NOT NULL,
    resuelto_en timestamp with time zone,
    cuenta_bancaria_pactada jsonb,
    CONSTRAINT ck_devolucion_origen CHECK (((((clase)::text = 'COBRO'::text) AND (id_cobro_origen IS NULL)) OR (((clase)::text = 'DEVOLUCION'::text) AND (id_cobro_origen IS NOT NULL)))),
    CONSTRAINT movimiento_pago_clase_check CHECK (((clase)::text = ANY ((ARRAY['COBRO'::character varying, 'DEVOLUCION'::character varying])::text[]))),
    CONSTRAINT movimiento_pago_estado_check CHECK (((estado)::text = ANY ((ARRAY['PENDIENTE'::character varying, 'APROBADO'::character varying, 'RECHAZADO'::character varying])::text[]))),
    CONSTRAINT movimiento_pago_monto_check CHECK ((monto > (0)::numeric))
);


--
-- Name: movimiento_pago_id_movimiento_seq; Type: SEQUENCE; Schema: esquema operativo; Owner: -
--

ALTER TABLE movimiento_pago ALTER COLUMN id_movimiento ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME movimiento_pago_id_movimiento_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: ocupacion; Type: TABLE; Schema: esquema operativo; Owner: -
--

CREATE TABLE ocupacion (
    id_ocupacion bigint NOT NULL,
    id_inmueble bigint NOT NULL,
    fecha_inicio date NOT NULL,
    fecha_fin date NOT NULL,
    tipo character varying(8) NOT NULL,
    activa boolean DEFAULT true NOT NULL,
    creada_en timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT ck_ocupacion_fechas CHECK ((fecha_fin > fecha_inicio)),
    CONSTRAINT ocupacion_tipo_check CHECK (((tipo)::text = ANY ((ARRAY['RESERVA'::character varying, 'BLOQUEO'::character varying])::text[])))
);


--
-- Name: ocupacion_id_ocupacion_seq; Type: SEQUENCE; Schema: esquema operativo; Owner: -
--

ALTER TABLE ocupacion ALTER COLUMN id_ocupacion ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME ocupacion_id_ocupacion_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: pais; Type: TABLE; Schema: esquema operativo; Owner: -
--

CREATE TABLE pais (
    id_pais bigint NOT NULL,
    nombre character varying(120) NOT NULL,
    codigo_iso2 character(2) NOT NULL,
    CONSTRAINT pais_codigo_iso2_check CHECK ((codigo_iso2 ~ '^[A-Z]{2}$'::text)),
    CONSTRAINT pais_nombre_check CHECK ((length(btrim((nombre)::text)) > 0))
);


--
-- Name: pais_id_pais_seq; Type: SEQUENCE; Schema: esquema operativo; Owner: -
--

ALTER TABLE pais ALTER COLUMN id_pais ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME pais_id_pais_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: propietario; Type: TABLE; Schema: esquema operativo; Owner: -
--

CREATE TABLE propietario (
    id_propietario bigint CONSTRAINT propietario_id_usuario_not_null NOT NULL,
    id_usuario bigint CONSTRAINT propietario_id_usuario_not_null1 NOT NULL,
    nombre_completo character varying(160) NOT NULL,
    correo_normalizado character varying(254) NOT NULL,
    telefono character varying(30)
);


--
-- Name: relaciona; Type: TABLE; Schema: esquema operativo; Owner: -
--

CREATE TABLE relaciona (
    id_propietario bigint NOT NULL,
    id_banco bigint NOT NULL,
    numero_cuenta character varying(34) NOT NULL,
    tipo_cuenta character varying(12) NOT NULL,
    actualizada_en timestamp with time zone DEFAULT clock_timestamp() NOT NULL,
    CONSTRAINT relaciona_numero_cuenta_check CHECK (((numero_cuenta)::text ~ '^[0-9]{4,34}$'::text)),
    CONSTRAINT relaciona_tipo_cuenta_check CHECK (((tipo_cuenta)::text = ANY ((ARRAY['AHORROS'::character varying, 'CORRIENTE'::character varying])::text[])))
);


--
-- Name: renta; Type: TABLE; Schema: esquema operativo; Owner: -
--

CREATE TABLE renta (
    id_ocupacion bigint CONSTRAINT reserva_id_ocupacion_not_null NOT NULL,
    id_cliente bigint CONSTRAINT reserva_id_cliente_not_null NOT NULL,
    id_propietario_pactado bigint CONSTRAINT reserva_id_propietario_pactado_not_null NOT NULL,
    cantidad_huespedes smallint CONSTRAINT reserva_cantidad_huespedes_not_null NOT NULL,
    precio_noche_pactado numeric(16,2) CONSTRAINT reserva_precio_noche_pactado_not_null NOT NULL,
    moneda character varying(3) CONSTRAINT reserva_moneda_not_null NOT NULL,
    estado character varying(16) DEFAULT 'PENDIENTE_PAGO'::character varying CONSTRAINT reserva_estado_not_null NOT NULL,
    expira_en timestamp with time zone CONSTRAINT reserva_expira_en_not_null NOT NULL,
    creada_en timestamp with time zone DEFAULT now() CONSTRAINT reserva_creada_en_not_null NOT NULL,
    id_inmueble bigint NOT NULL,
    fecha_entrada date NOT NULL,
    fecha_salida date NOT NULL,
    CONSTRAINT ck_renta_estancia CHECK ((fecha_salida > fecha_entrada)),
    CONSTRAINT reserva_cantidad_huespedes_check CHECK ((cantidad_huespedes > 0)),
    CONSTRAINT reserva_estado_check CHECK (((estado)::text = ANY ((ARRAY['PENDIENTE_PAGO'::character varying, 'CONFIRMADA'::character varying, 'FINALIZADA'::character varying, 'CANCELADA'::character varying, 'EXPIRADA'::character varying])::text[]))),
    CONSTRAINT reserva_moneda_check CHECK (((moneda)::text ~ '^[A-Z]{3}$'::text)),
    CONSTRAINT reserva_precio_noche_pactado_check CHECK ((precio_noche_pactado > (0)::numeric))
);


--
-- Name: TABLE renta; Type: COMMENT; Schema: esquema operativo; Owner: -
--

COMMENT ON TABLE renta IS 'Relacion RENTA del profesor; reserva es una vista de compatibilidad sin almacenamiento';


--
-- Name: reserva; Type: VIEW; Schema: esquema operativo; Owner: -
--

CREATE VIEW reserva AS
 SELECT id_ocupacion,
    id_cliente,
    id_propietario_pactado,
    cantidad_huespedes,
    precio_noche_pactado,
    moneda,
    estado,
    expira_en,
    creada_en,
    id_inmueble,
    fecha_entrada,
    fecha_salida
   FROM renta;


--
-- Name: usuario; Type: TABLE; Schema: esquema operativo; Owner: -
--

CREATE TABLE usuario (
    id_usuario bigint NOT NULL,
    nombre_completo character varying(160) NOT NULL,
    correo_normalizado character varying(254) NOT NULL,
    contrasena_hash text NOT NULL,
    telefono character varying(30),
    activo boolean DEFAULT true NOT NULL,
    creado_en timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT ck_usuario_correo_canonico CHECK (((correo_normalizado)::text = lower(btrim((correo_normalizado)::text)))),
    CONSTRAINT usuario_contrasena_hash_check CHECK ((length(contrasena_hash) > 0)),
    CONSTRAINT usuario_correo_check CHECK ((((correo_normalizado)::text = btrim((correo_normalizado)::text)) AND (POSITION(('@'::text) IN (correo_normalizado)) > 1))),
    CONSTRAINT usuario_nombre_completo_check CHECK ((length(btrim((nombre_completo)::text)) > 0))
);


--
-- Name: TABLE usuario; Type: COMMENT; Schema: esquema operativo; Owner: -
--

COMMENT ON TABLE usuario IS 'Soporte tecnico de acceso compartido; cliente y propietario son entidades completas independientes';


--
-- Name: usuario_id_usuario_seq; Type: SEQUENCE; Schema: esquema operativo; Owner: -
--

ALTER TABLE usuario ALTER COLUMN id_usuario ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME usuario_id_usuario_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: vincula; Type: TABLE; Schema: esquema operativo; Owner: -
--

CREATE TABLE vincula (
    id_cliente bigint NOT NULL,
    id_banco bigint NOT NULL,
    numero_cuenta character varying(34) NOT NULL,
    tipo_cuenta character varying(12) NOT NULL,
    actualizada_en timestamp with time zone DEFAULT clock_timestamp() NOT NULL,
    CONSTRAINT vincula_numero_cuenta_check CHECK (((numero_cuenta)::text ~ '^[0-9]{4,34}$'::text)),
    CONSTRAINT vincula_tipo_cuenta_check CHECK (((tipo_cuenta)::text = ANY ((ARRAY['AHORROS'::character varying, 'CORRIENTE'::character varying])::text[])))
);


--
-- Name: administrador administrador_pkey; Type: CONSTRAINT; Schema: esquema operativo; Owner: -
--

ALTER TABLE ONLY administrador
    ADD CONSTRAINT administrador_pkey PRIMARY KEY (id_usuario);


--
-- Name: asocia asocia_pkey; Type: CONSTRAINT; Schema: esquema operativo; Owner: -
--

ALTER TABLE ONLY asocia
    ADD CONSTRAINT asocia_pkey PRIMARY KEY (id_inmueble);


--
-- Name: banco banco_pkey; Type: CONSTRAINT; Schema: esquema operativo; Owner: -
--

ALTER TABLE ONLY banco
    ADD CONSTRAINT banco_pkey PRIMARY KEY (id_banco);


--
-- Name: bloqueo bloqueo_pkey; Type: CONSTRAINT; Schema: esquema operativo; Owner: -
--

ALTER TABLE ONLY bloqueo
    ADD CONSTRAINT bloqueo_pkey PRIMARY KEY (id_ocupacion);


--
-- Name: ciudad ciudad_pkey; Type: CONSTRAINT; Schema: esquema operativo; Owner: -
--

ALTER TABLE ONLY ciudad
    ADD CONSTRAINT ciudad_pkey PRIMARY KEY (id_ciudad);


--
-- Name: cliente cliente_id_usuario_key; Type: CONSTRAINT; Schema: esquema operativo; Owner: -
--

ALTER TABLE ONLY cliente
    ADD CONSTRAINT cliente_id_usuario_key UNIQUE (id_usuario);


--
-- Name: cliente cliente_pkey; Type: CONSTRAINT; Schema: esquema operativo; Owner: -
--

ALTER TABLE ONLY cliente
    ADD CONSTRAINT cliente_pkey PRIMARY KEY (id_cliente);


--
-- Name: division_administrativa division_administrativa_pkey; Type: CONSTRAINT; Schema: esquema operativo; Owner: -
--

ALTER TABLE ONLY division_administrativa
    ADD CONSTRAINT division_administrativa_pkey PRIMARY KEY (id_division);


--
-- Name: ocupacion ex_ocupacion_no_solape; Type: CONSTRAINT; Schema: esquema operativo; Owner: -
--

ALTER TABLE ONLY ocupacion
    ADD CONSTRAINT ex_ocupacion_no_solape EXCLUDE USING gist (id_inmueble WITH =, daterange(fecha_inicio, fecha_fin, '[)'::text) WITH &&) WHERE (activa);


--
-- Name: giro giro_clave_idempotencia_key; Type: CONSTRAINT; Schema: esquema operativo; Owner: -
--

ALTER TABLE ONLY giro
    ADD CONSTRAINT giro_clave_idempotencia_key UNIQUE (clave_idempotencia);


--
-- Name: giro giro_pkey; Type: CONSTRAINT; Schema: esquema operativo; Owner: -
--

ALTER TABLE ONLY giro
    ADD CONSTRAINT giro_pkey PRIMARY KEY (id_giro);


--
-- Name: giro giro_referencia_externa_key; Type: CONSTRAINT; Schema: esquema operativo; Owner: -
--

ALTER TABLE ONLY giro
    ADD CONSTRAINT giro_referencia_externa_key UNIQUE (referencia_externa);


--
-- Name: inmueble inmueble_pkey; Type: CONSTRAINT; Schema: esquema operativo; Owner: -
--

ALTER TABLE ONLY inmueble
    ADD CONSTRAINT inmueble_pkey PRIMARY KEY (id_inmueble);


--
-- Name: movimiento_pago movimiento_pago_clave_idempotencia_key; Type: CONSTRAINT; Schema: esquema operativo; Owner: -
--

ALTER TABLE ONLY movimiento_pago
    ADD CONSTRAINT movimiento_pago_clave_idempotencia_key UNIQUE (clave_idempotencia);


--
-- Name: movimiento_pago movimiento_pago_pkey; Type: CONSTRAINT; Schema: esquema operativo; Owner: -
--

ALTER TABLE ONLY movimiento_pago
    ADD CONSTRAINT movimiento_pago_pkey PRIMARY KEY (id_movimiento);


--
-- Name: movimiento_pago movimiento_pago_referencia_externa_key; Type: CONSTRAINT; Schema: esquema operativo; Owner: -
--

ALTER TABLE ONLY movimiento_pago
    ADD CONSTRAINT movimiento_pago_referencia_externa_key UNIQUE (referencia_externa);


--
-- Name: ocupacion ocupacion_pkey; Type: CONSTRAINT; Schema: esquema operativo; Owner: -
--

ALTER TABLE ONLY ocupacion
    ADD CONSTRAINT ocupacion_pkey PRIMARY KEY (id_ocupacion);


--
-- Name: pais pais_codigo_iso2_key; Type: CONSTRAINT; Schema: esquema operativo; Owner: -
--

ALTER TABLE ONLY pais
    ADD CONSTRAINT pais_codigo_iso2_key UNIQUE (codigo_iso2);


--
-- Name: pais pais_pkey; Type: CONSTRAINT; Schema: esquema operativo; Owner: -
--

ALTER TABLE ONLY pais
    ADD CONSTRAINT pais_pkey PRIMARY KEY (id_pais);


--
-- Name: propietario propietario_id_usuario_key; Type: CONSTRAINT; Schema: esquema operativo; Owner: -
--

ALTER TABLE ONLY propietario
    ADD CONSTRAINT propietario_id_usuario_key UNIQUE (id_usuario);


--
-- Name: propietario propietario_pkey; Type: CONSTRAINT; Schema: esquema operativo; Owner: -
--

ALTER TABLE ONLY propietario
    ADD CONSTRAINT propietario_pkey PRIMARY KEY (id_propietario);


--
-- Name: relaciona relaciona_pkey; Type: CONSTRAINT; Schema: esquema operativo; Owner: -
--

ALTER TABLE ONLY relaciona
    ADD CONSTRAINT relaciona_pkey PRIMARY KEY (id_propietario);


--
-- Name: renta reserva_pkey; Type: CONSTRAINT; Schema: esquema operativo; Owner: -
--

ALTER TABLE ONLY renta
    ADD CONSTRAINT reserva_pkey PRIMARY KEY (id_ocupacion);


--
-- Name: banco uq_banco_pais_nombre; Type: CONSTRAINT; Schema: esquema operativo; Owner: -
--

ALTER TABLE ONLY banco
    ADD CONSTRAINT uq_banco_pais_nombre UNIQUE (id_pais, nombre);


--
-- Name: ciudad uq_ciudad_codigo; Type: CONSTRAINT; Schema: esquema operativo; Owner: -
--

ALTER TABLE ONLY ciudad
    ADD CONSTRAINT uq_ciudad_codigo UNIQUE (id_division, codigo_oficial);


--
-- Name: division_administrativa uq_division_codigo; Type: CONSTRAINT; Schema: esquema operativo; Owner: -
--

ALTER TABLE ONLY division_administrativa
    ADD CONSTRAINT uq_division_codigo UNIQUE (id_pais, codigo_oficial);


--
-- Name: movimiento_pago uq_movimiento_reserva; Type: CONSTRAINT; Schema: esquema operativo; Owner: -
--

ALTER TABLE ONLY movimiento_pago
    ADD CONSTRAINT uq_movimiento_reserva UNIQUE (id_movimiento, id_reserva);


--
-- Name: usuario usuario_pkey; Type: CONSTRAINT; Schema: esquema operativo; Owner: -
--

ALTER TABLE ONLY usuario
    ADD CONSTRAINT usuario_pkey PRIMARY KEY (id_usuario);


--
-- Name: vincula vincula_pkey; Type: CONSTRAINT; Schema: esquema operativo; Owner: -
--

ALTER TABLE ONLY vincula
    ADD CONSTRAINT vincula_pkey PRIMARY KEY (id_cliente);


--
-- Name: ix_asocia_propietario; Type: INDEX; Schema: esquema operativo; Owner: -
--

CREATE INDEX ix_asocia_propietario ON asocia USING btree (id_propietario);


--
-- Name: ix_ciudad_division; Type: INDEX; Schema: esquema operativo; Owner: -
--

CREATE INDEX ix_ciudad_division ON ciudad USING btree (id_division);


--
-- Name: ix_division_pais; Type: INDEX; Schema: esquema operativo; Owner: -
--

CREATE INDEX ix_division_pais ON division_administrativa USING btree (id_pais);


--
-- Name: ix_inmueble_ciudad_estado; Type: INDEX; Schema: esquema operativo; Owner: -
--

CREATE INDEX ix_inmueble_ciudad_estado ON inmueble USING btree (id_ciudad, estado);


--
-- Name: ix_movimiento_reserva_estado; Type: INDEX; Schema: esquema operativo; Owner: -
--

CREATE INDEX ix_movimiento_reserva_estado ON movimiento_pago USING btree (id_reserva, estado);


--
-- Name: ix_ocupacion_inmueble; Type: INDEX; Schema: esquema operativo; Owner: -
--

CREATE INDEX ix_ocupacion_inmueble ON ocupacion USING btree (id_inmueble);


--
-- Name: ix_reserva_cliente; Type: INDEX; Schema: esquema operativo; Owner: -
--

CREATE INDEX ix_reserva_cliente ON renta USING btree (id_cliente);


--
-- Name: ix_reserva_propietario_historico; Type: INDEX; Schema: esquema operativo; Owner: -
--

CREATE INDEX ix_reserva_propietario_historico ON renta USING btree (id_propietario_pactado);


--
-- Name: ux_giro_unico_activo_o_ejecutado; Type: INDEX; Schema: esquema operativo; Owner: -
--

CREATE UNIQUE INDEX ux_giro_unico_activo_o_ejecutado ON giro USING btree (id_reserva) WHERE ((estado)::text = ANY ((ARRAY['PENDIENTE'::character varying, 'PROCESANDO'::character varying, 'EJECUTADO'::character varying])::text[]));


--
-- Name: ux_usuario_correo_normalizado; Type: INDEX; Schema: esquema operativo; Owner: -
--

CREATE UNIQUE INDEX ux_usuario_correo_normalizado ON usuario USING btree (correo_normalizado);


--
-- Name: renta aa_completar_renta; Type: TRIGGER; Schema: esquema operativo; Owner: -
--

CREATE TRIGGER aa_completar_renta BEFORE INSERT OR UPDATE ON renta FOR EACH ROW EXECUTE FUNCTION completar_renta_calendario();


--
-- Name: asocia ck_asocia_inmueble; Type: TRIGGER; Schema: esquema operativo; Owner: -
--

CREATE CONSTRAINT TRIGGER ck_asocia_inmueble AFTER INSERT OR DELETE OR UPDATE ON asocia DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION verificar_asocia();


--
-- Name: bloqueo ck_bloqueo_subtipo; Type: TRIGGER; Schema: esquema operativo; Owner: -
--

CREATE CONSTRAINT TRIGGER ck_bloqueo_subtipo AFTER INSERT OR DELETE OR UPDATE ON bloqueo DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION verificar_subtipo_ocupacion();


--
-- Name: inmueble ck_inmueble_asocia; Type: TRIGGER; Schema: esquema operativo; Owner: -
--

CREATE CONSTRAINT TRIGGER ck_inmueble_asocia AFTER INSERT OR UPDATE ON inmueble DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION verificar_asocia();


--
-- Name: ocupacion ck_ocupacion_subtipo; Type: TRIGGER; Schema: esquema operativo; Owner: -
--

CREATE CONSTRAINT TRIGGER ck_ocupacion_subtipo AFTER INSERT OR DELETE OR UPDATE ON ocupacion DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION verificar_subtipo_ocupacion();


--
-- Name: renta ck_reserva_subtipo; Type: TRIGGER; Schema: esquema operativo; Owner: -
--

CREATE CONSTRAINT TRIGGER ck_reserva_subtipo AFTER INSERT OR DELETE OR UPDATE ON renta DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION verificar_subtipo_ocupacion();


--
-- Name: ciudad completar_ciudad; Type: TRIGGER; Schema: esquema operativo; Owner: -
--

CREATE TRIGGER completar_ciudad BEFORE INSERT OR UPDATE ON ciudad FOR EACH ROW EXECUTE FUNCTION completar_pais_ciudad();


--
-- Name: cliente completar_cliente; Type: TRIGGER; Schema: esquema operativo; Owner: -
--

CREATE TRIGGER completar_cliente BEFORE INSERT OR UPDATE ON cliente FOR EACH ROW EXECUTE FUNCTION completar_entidad_persona();


--
-- Name: propietario completar_propietario; Type: TRIGGER; Schema: esquema operativo; Owner: -
--

CREATE TRIGGER completar_propietario BEFORE INSERT OR UPDATE ON propietario FOR EACH ROW EXECUTE FUNCTION completar_entidad_persona();


--
-- Name: giro cuenta_historica_giro; Type: TRIGGER; Schema: esquema operativo; Owner: -
--

CREATE TRIGGER cuenta_historica_giro BEFORE INSERT OR UPDATE ON giro FOR EACH ROW EXECUTE FUNCTION guardar_cuenta_financiera();


--
-- Name: movimiento_pago cuenta_historica_pago; Type: TRIGGER; Schema: esquema operativo; Owner: -
--

CREATE TRIGGER cuenta_historica_pago BEFORE INSERT OR UPDATE ON movimiento_pago FOR EACH ROW EXECUTE FUNCTION guardar_cuenta_financiera();


--
-- Name: inmueble_operativo escribir_inmueble; Type: TRIGGER; Schema: esquema operativo; Owner: -
--

CREATE TRIGGER escribir_inmueble INSTEAD OF INSERT OR DELETE OR UPDATE ON inmueble_operativo FOR EACH ROW EXECUTE FUNCTION escribir_inmueble_operativo();


--
-- Name: giro no_delete_giro; Type: TRIGGER; Schema: esquema operativo; Owner: -
--

CREATE TRIGGER no_delete_giro BEFORE DELETE ON giro FOR EACH ROW EXECUTE FUNCTION prohibir_borrado_financiero();


--
-- Name: movimiento_pago no_delete_movimiento; Type: TRIGGER; Schema: esquema operativo; Owner: -
--

CREATE TRIGGER no_delete_movimiento BEFORE DELETE ON movimiento_pago FOR EACH ROW EXECUTE FUNCTION prohibir_borrado_financiero();


--
-- Name: renta no_delete_reserva; Type: TRIGGER; Schema: esquema operativo; Owner: -
--

CREATE TRIGGER no_delete_reserva BEFORE DELETE ON renta FOR EACH ROW EXECUTE FUNCTION prohibir_borrado_financiero();


--
-- Name: ocupacion sincronizar_calendario; Type: TRIGGER; Schema: esquema operativo; Owner: -
--

CREATE TRIGGER sincronizar_calendario AFTER UPDATE OF fecha_inicio, fecha_fin, id_inmueble ON ocupacion FOR EACH ROW EXECUTE FUNCTION sincronizar_calendario_renta();


--
-- Name: usuario sincronizar_identidad; Type: TRIGGER; Schema: esquema operativo; Owner: -
--

CREATE TRIGGER sincronizar_identidad AFTER UPDATE OF nombre_completo, correo_normalizado, telefono ON usuario FOR EACH ROW EXECUTE FUNCTION sincronizar_identidad_persona();


--
-- Name: ocupacion tg_ocupacion_inmutable; Type: TRIGGER; Schema: esquema operativo; Owner: -
--

CREATE TRIGGER tg_ocupacion_inmutable BEFORE UPDATE ON ocupacion FOR EACH ROW EXECUTE FUNCTION impedir_reescritura_ocupacion();


--
-- Name: inmueble tg_proteger_capacidad; Type: TRIGGER; Schema: esquema operativo; Owner: -
--

CREATE TRIGGER tg_proteger_capacidad BEFORE UPDATE OF capacidad ON inmueble FOR EACH ROW EXECUTE FUNCTION proteger_capacidad_inmueble();


--
-- Name: renta tg_sincronizar_ocupacion; Type: TRIGGER; Schema: esquema operativo; Owner: -
--

CREATE TRIGGER tg_sincronizar_ocupacion AFTER INSERT OR UPDATE OF estado ON renta FOR EACH ROW EXECUTE FUNCTION sincronizar_ocupacion_reserva();


--
-- Name: bloqueo tg_validar_bloqueo; Type: TRIGGER; Schema: esquema operativo; Owner: -
--

CREATE TRIGGER tg_validar_bloqueo BEFORE INSERT OR UPDATE ON bloqueo FOR EACH ROW EXECUTE FUNCTION validar_bloqueo();


--
-- Name: giro tg_validar_giro; Type: TRIGGER; Schema: esquema operativo; Owner: -
--

CREATE TRIGGER tg_validar_giro BEFORE INSERT OR UPDATE ON giro FOR EACH ROW EXECUTE FUNCTION validar_giro();


--
-- Name: movimiento_pago tg_validar_movimiento; Type: TRIGGER; Schema: esquema operativo; Owner: -
--

CREATE TRIGGER tg_validar_movimiento BEFORE INSERT OR UPDATE ON movimiento_pago FOR EACH ROW EXECUTE FUNCTION validar_movimiento_pago();


--
-- Name: renta tg_validar_reserva; Type: TRIGGER; Schema: esquema operativo; Owner: -
--

CREATE TRIGGER tg_validar_reserva BEFORE INSERT OR UPDATE ON renta FOR EACH ROW EXECUTE FUNCTION validar_reserva();


--
-- Name: administrador administrador_id_usuario_fkey; Type: FK CONSTRAINT; Schema: esquema operativo; Owner: -
--

ALTER TABLE ONLY administrador
    ADD CONSTRAINT administrador_id_usuario_fkey FOREIGN KEY (id_usuario) REFERENCES usuario(id_usuario) ON DELETE RESTRICT;


--
-- Name: asocia asocia_id_inmueble_fkey; Type: FK CONSTRAINT; Schema: esquema operativo; Owner: -
--

ALTER TABLE ONLY asocia
    ADD CONSTRAINT asocia_id_inmueble_fkey FOREIGN KEY (id_inmueble) REFERENCES inmueble(id_inmueble) ON DELETE CASCADE;


--
-- Name: asocia asocia_id_propietario_fkey; Type: FK CONSTRAINT; Schema: esquema operativo; Owner: -
--

ALTER TABLE ONLY asocia
    ADD CONSTRAINT asocia_id_propietario_fkey FOREIGN KEY (id_propietario) REFERENCES propietario(id_propietario) ON DELETE RESTRICT;


--
-- Name: banco banco_id_pais_fkey; Type: FK CONSTRAINT; Schema: esquema operativo; Owner: -
--

ALTER TABLE ONLY banco
    ADD CONSTRAINT banco_id_pais_fkey FOREIGN KEY (id_pais) REFERENCES pais(id_pais) ON DELETE RESTRICT;


--
-- Name: bloqueo bloqueo_id_ocupacion_fkey; Type: FK CONSTRAINT; Schema: esquema operativo; Owner: -
--

ALTER TABLE ONLY bloqueo
    ADD CONSTRAINT bloqueo_id_ocupacion_fkey FOREIGN KEY (id_ocupacion) REFERENCES ocupacion(id_ocupacion) ON DELETE RESTRICT;


--
-- Name: bloqueo bloqueo_id_propietario_solicitante_fkey; Type: FK CONSTRAINT; Schema: esquema operativo; Owner: -
--

ALTER TABLE ONLY bloqueo
    ADD CONSTRAINT bloqueo_id_propietario_solicitante_fkey FOREIGN KEY (id_propietario_solicitante) REFERENCES propietario(id_propietario) ON DELETE RESTRICT;


--
-- Name: ciudad ciudad_id_division_fkey; Type: FK CONSTRAINT; Schema: esquema operativo; Owner: -
--

ALTER TABLE ONLY ciudad
    ADD CONSTRAINT ciudad_id_division_fkey FOREIGN KEY (id_division) REFERENCES division_administrativa(id_division) ON DELETE RESTRICT;


--
-- Name: ciudad ciudad_id_pais_fkey; Type: FK CONSTRAINT; Schema: esquema operativo; Owner: -
--

ALTER TABLE ONLY ciudad
    ADD CONSTRAINT ciudad_id_pais_fkey FOREIGN KEY (id_pais) REFERENCES pais(id_pais) ON DELETE RESTRICT;


--
-- Name: cliente cliente_id_ciudad_fkey; Type: FK CONSTRAINT; Schema: esquema operativo; Owner: -
--

ALTER TABLE ONLY cliente
    ADD CONSTRAINT cliente_id_ciudad_fkey FOREIGN KEY (id_ciudad) REFERENCES ciudad(id_ciudad) ON DELETE RESTRICT;


--
-- Name: cliente cliente_id_usuario_fkey; Type: FK CONSTRAINT; Schema: esquema operativo; Owner: -
--

ALTER TABLE ONLY cliente
    ADD CONSTRAINT cliente_id_usuario_fkey FOREIGN KEY (id_cliente) REFERENCES usuario(id_usuario) ON DELETE RESTRICT;


--
-- Name: cliente cliente_id_usuario_fkey1; Type: FK CONSTRAINT; Schema: esquema operativo; Owner: -
--

ALTER TABLE ONLY cliente
    ADD CONSTRAINT cliente_id_usuario_fkey1 FOREIGN KEY (id_usuario) REFERENCES usuario(id_usuario) ON DELETE RESTRICT;


--
-- Name: division_administrativa division_administrativa_id_pais_fkey; Type: FK CONSTRAINT; Schema: esquema operativo; Owner: -
--

ALTER TABLE ONLY division_administrativa
    ADD CONSTRAINT division_administrativa_id_pais_fkey FOREIGN KEY (id_pais) REFERENCES pais(id_pais) ON DELETE RESTRICT;


--
-- Name: movimiento_pago fk_devolucion_misma_reserva; Type: FK CONSTRAINT; Schema: esquema operativo; Owner: -
--

ALTER TABLE ONLY movimiento_pago
    ADD CONSTRAINT fk_devolucion_misma_reserva FOREIGN KEY (id_cobro_origen, id_reserva) REFERENCES movimiento_pago(id_movimiento, id_reserva) ON DELETE RESTRICT;


--
-- Name: giro giro_id_admin_autorizador_fkey; Type: FK CONSTRAINT; Schema: esquema operativo; Owner: -
--

ALTER TABLE ONLY giro
    ADD CONSTRAINT giro_id_admin_autorizador_fkey FOREIGN KEY (id_admin_autorizador) REFERENCES administrador(id_usuario) ON DELETE RESTRICT;


--
-- Name: giro giro_id_reserva_fkey; Type: FK CONSTRAINT; Schema: esquema operativo; Owner: -
--

ALTER TABLE ONLY giro
    ADD CONSTRAINT giro_id_reserva_fkey FOREIGN KEY (id_reserva) REFERENCES renta(id_ocupacion) ON DELETE RESTRICT;


--
-- Name: inmueble inmueble_id_ciudad_fkey; Type: FK CONSTRAINT; Schema: esquema operativo; Owner: -
--

ALTER TABLE ONLY inmueble
    ADD CONSTRAINT inmueble_id_ciudad_fkey FOREIGN KEY (id_ciudad) REFERENCES ciudad(id_ciudad) ON DELETE RESTRICT;


--
-- Name: movimiento_pago movimiento_pago_id_reserva_fkey; Type: FK CONSTRAINT; Schema: esquema operativo; Owner: -
--

ALTER TABLE ONLY movimiento_pago
    ADD CONSTRAINT movimiento_pago_id_reserva_fkey FOREIGN KEY (id_reserva) REFERENCES renta(id_ocupacion) ON DELETE RESTRICT;


--
-- Name: ocupacion ocupacion_id_inmueble_fkey; Type: FK CONSTRAINT; Schema: esquema operativo; Owner: -
--

ALTER TABLE ONLY ocupacion
    ADD CONSTRAINT ocupacion_id_inmueble_fkey FOREIGN KEY (id_inmueble) REFERENCES inmueble(id_inmueble) ON DELETE RESTRICT;


--
-- Name: propietario propietario_id_usuario_fkey; Type: FK CONSTRAINT; Schema: esquema operativo; Owner: -
--

ALTER TABLE ONLY propietario
    ADD CONSTRAINT propietario_id_usuario_fkey FOREIGN KEY (id_propietario) REFERENCES usuario(id_usuario) ON DELETE RESTRICT;


--
-- Name: propietario propietario_id_usuario_fkey1; Type: FK CONSTRAINT; Schema: esquema operativo; Owner: -
--

ALTER TABLE ONLY propietario
    ADD CONSTRAINT propietario_id_usuario_fkey1 FOREIGN KEY (id_usuario) REFERENCES usuario(id_usuario) ON DELETE RESTRICT;


--
-- Name: relaciona relaciona_id_banco_fkey; Type: FK CONSTRAINT; Schema: esquema operativo; Owner: -
--

ALTER TABLE ONLY relaciona
    ADD CONSTRAINT relaciona_id_banco_fkey FOREIGN KEY (id_banco) REFERENCES banco(id_banco) ON DELETE RESTRICT;


--
-- Name: relaciona relaciona_id_propietario_fkey; Type: FK CONSTRAINT; Schema: esquema operativo; Owner: -
--

ALTER TABLE ONLY relaciona
    ADD CONSTRAINT relaciona_id_propietario_fkey FOREIGN KEY (id_propietario) REFERENCES propietario(id_propietario) ON DELETE CASCADE;


--
-- Name: renta renta_id_inmueble_fkey; Type: FK CONSTRAINT; Schema: esquema operativo; Owner: -
--

ALTER TABLE ONLY renta
    ADD CONSTRAINT renta_id_inmueble_fkey FOREIGN KEY (id_inmueble) REFERENCES inmueble(id_inmueble) ON DELETE RESTRICT;


--
-- Name: renta reserva_id_cliente_fkey; Type: FK CONSTRAINT; Schema: esquema operativo; Owner: -
--

ALTER TABLE ONLY renta
    ADD CONSTRAINT reserva_id_cliente_fkey FOREIGN KEY (id_cliente) REFERENCES cliente(id_cliente) ON DELETE RESTRICT;


--
-- Name: renta reserva_id_ocupacion_fkey; Type: FK CONSTRAINT; Schema: esquema operativo; Owner: -
--

ALTER TABLE ONLY renta
    ADD CONSTRAINT reserva_id_ocupacion_fkey FOREIGN KEY (id_ocupacion) REFERENCES ocupacion(id_ocupacion) ON DELETE RESTRICT;


--
-- Name: renta reserva_id_propietario_pactado_fkey; Type: FK CONSTRAINT; Schema: esquema operativo; Owner: -
--

ALTER TABLE ONLY renta
    ADD CONSTRAINT reserva_id_propietario_pactado_fkey FOREIGN KEY (id_propietario_pactado) REFERENCES propietario(id_propietario) ON DELETE RESTRICT;


--
-- Name: vincula vincula_id_banco_fkey; Type: FK CONSTRAINT; Schema: esquema operativo; Owner: -
--

ALTER TABLE ONLY vincula
    ADD CONSTRAINT vincula_id_banco_fkey FOREIGN KEY (id_banco) REFERENCES banco(id_banco) ON DELETE RESTRICT;


--
-- Name: vincula vincula_id_cliente_fkey; Type: FK CONSTRAINT; Schema: esquema operativo; Owner: -
--

ALTER TABLE ONLY vincula
    ADD CONSTRAINT vincula_id_cliente_fkey FOREIGN KEY (id_cliente) REFERENCES cliente(id_cliente) ON DELETE CASCADE;


--
-- PostgreSQL database dump complete
--


SET check_function_bodies = true;
COMMIT;
