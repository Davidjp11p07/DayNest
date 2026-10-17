-- Nomenclatura del profesor. Ejecutar solo tras respaldo y aprobacion.

-- Sin borrado de filas; IDs y claves foraneas existentes se conservan.

DO $$ BEGIN IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=current_schema() AND table_name='cliente' AND column_name='id_cliente') THEN RAISE EXCEPTION 'Se requiere el modelo del profesor previo a nomenclatura'; END IF; END $$;

ALTER TABLE pais RENAME TO paises;

ALTER TABLE paises RENAME COLUMN id_pais TO id_ps;

ALTER TABLE paises RENAME COLUMN nombre TO nom_ps;

CREATE VIEW pais AS SELECT id_ps AS id_pais, nom_ps AS nombre, codigo_iso2 FROM paises;

COMMENT ON TABLE paises IS 'Nucleo del modelo entidad relacion del profesor';

ALTER TABLE ciudad RENAME TO ciudades;

ALTER TABLE ciudades RENAME COLUMN id_ciudad TO id_cd;

ALTER TABLE ciudades RENAME COLUMN id_pais TO id_ps;

ALTER TABLE ciudades RENAME COLUMN nombre TO nom_cd;

CREATE VIEW ciudad AS SELECT id_cd AS id_ciudad, id_division, nom_cd AS nombre, codigo_oficial, id_ps AS id_pais FROM ciudades;

COMMENT ON TABLE ciudades IS 'Nucleo del modelo entidad relacion del profesor';

ALTER TABLE banco RENAME COLUMN id_banco TO id_b;

ALTER TABLE banco RENAME COLUMN id_pais TO id_ps;

ALTER TABLE banco RENAME COLUMN nombre TO nom_b;

CREATE VIEW banco_operativa AS SELECT id_b AS id_banco, nom_b AS nombre, id_ps AS id_pais FROM banco;

COMMENT ON TABLE banco IS 'Nucleo del modelo entidad relacion del profesor';

ALTER TABLE cliente RENAME TO clientes;

ALTER TABLE clientes RENAME COLUMN id_cliente TO id_cl;

ALTER TABLE clientes RENAME COLUMN nombre_completo TO nom_cl;

ALTER TABLE clientes RENAME COLUMN telefono TO tel_cl;

ALTER TABLE clientes RENAME COLUMN correo_normalizado TO emai_cl;

ALTER TABLE clientes RENAME COLUMN id_ciudad TO id_cd;

CREATE VIEW cliente AS SELECT id_cl AS id_cliente, id_usuario, nom_cl AS nombre_completo, emai_cl AS correo_normalizado, tel_cl AS telefono, id_cd AS id_ciudad FROM clientes;

COMMENT ON TABLE clientes IS 'Nucleo del modelo entidad relacion del profesor';

ALTER TABLE propietario RENAME TO propietarios;

ALTER TABLE propietarios RENAME COLUMN id_propietario TO id_p;

ALTER TABLE propietarios RENAME COLUMN nombre_completo TO nom_p;

ALTER TABLE propietarios RENAME COLUMN telefono TO tel_p;

ALTER TABLE propietarios RENAME COLUMN correo_normalizado TO emai_p;

CREATE VIEW propietario AS SELECT id_p AS id_propietario, id_usuario, nom_p AS nombre_completo, emai_p AS correo_normalizado, tel_p AS telefono FROM propietarios;

COMMENT ON TABLE propietarios IS 'Nucleo del modelo entidad relacion del profesor';

ALTER TABLE inmueble RENAME TO inmuebles;

ALTER TABLE inmuebles RENAME COLUMN id_inmueble TO id_i;

ALTER TABLE inmuebles RENAME COLUMN id_ciudad TO id_cd;

ALTER TABLE inmuebles RENAME COLUMN direccion_detallada TO dir_i;

ALTER TABLE inmuebles RENAME COLUMN area_m2 TO area;

ALTER TABLE inmuebles RENAME COLUMN habitaciones TO n_hab;

ALTER TABLE inmuebles RENAME COLUMN capacidad TO capacd;

CREATE VIEW inmueble AS SELECT id_i AS id_inmueble, id_cd AS id_ciudad, titulo, descripcion, dir_i AS direccion_detallada, tipo, es_rural, cerca_mar, capacd AS capacidad, moneda, estado, creado_en, area AS area_m2, n_hab AS habitaciones FROM inmuebles;

COMMENT ON TABLE inmuebles IS 'Nucleo del modelo entidad relacion del profesor';

ALTER TABLE asocia RENAME COLUMN id_inmueble TO id_i;

ALTER TABLE asocia RENAME COLUMN id_propietario TO id_p;

ALTER TABLE asocia RENAME COLUMN precio_noche TO prec_nch;

CREATE VIEW asocia_operativa AS SELECT id_i AS id_inmueble, id_p AS id_propietario, prec_nch AS precio_noche FROM asocia;

COMMENT ON TABLE asocia IS 'Nucleo del modelo entidad relacion del profesor';

ALTER TABLE vincula RENAME COLUMN id_cliente TO id_cl;

ALTER TABLE vincula RENAME COLUMN id_banco TO id_b;

ALTER TABLE vincula RENAME COLUMN numero_cuenta TO num_cta;

ALTER TABLE vincula RENAME COLUMN tipo_cuenta TO tipo_cta;

CREATE VIEW vincula_operativa AS SELECT id_cl AS id_cliente, id_b AS id_banco, num_cta AS numero_cuenta, tipo_cta AS tipo_cuenta, actualizada_en FROM vincula;

COMMENT ON TABLE vincula IS 'Nucleo del modelo entidad relacion del profesor';

ALTER TABLE relaciona RENAME COLUMN id_propietario TO id_p;

ALTER TABLE relaciona RENAME COLUMN id_banco TO id_b;

ALTER TABLE relaciona RENAME COLUMN numero_cuenta TO num_cta;

ALTER TABLE relaciona RENAME COLUMN tipo_cuenta TO tipo_cta;

CREATE VIEW relaciona_operativa AS SELECT id_p AS id_propietario, id_b AS id_banco, num_cta AS numero_cuenta, tipo_cta AS tipo_cuenta, actualizada_en FROM relaciona;

COMMENT ON TABLE relaciona IS 'Nucleo del modelo entidad relacion del profesor';

ALTER TABLE renta RENAME COLUMN id_cliente TO id_cl;

ALTER TABLE renta RENAME COLUMN id_inmueble TO id_i;

ALTER TABLE renta RENAME COLUMN id_propietario_pactado TO id_p;

ALTER TABLE renta RENAME COLUMN fecha_entrada TO fch_e;

ALTER TABLE renta RENAME COLUMN fecha_salida TO fch_s;

CREATE VIEW renta_operativa AS SELECT id_ocupacion, id_cl AS id_cliente, id_p AS id_propietario_pactado, cantidad_huespedes, precio_noche_pactado, moneda, estado, expira_en, creada_en, id_i AS id_inmueble, fch_e AS fecha_entrada, fch_s AS fecha_salida FROM renta;

COMMENT ON TABLE renta IS 'Nucleo del modelo entidad relacion del profesor';

CREATE OR REPLACE FUNCTION completar_cliente_nomenclatura() RETURNS trigger LANGUAGE plpgsql AS $$

DECLARE u usuario%ROWTYPE; uid BIGINT;

BEGIN

 uid:=NEW.id_usuario;

 uid:=coalesce(uid,NEW.id_cl);

 SELECT * INTO u FROM usuario WHERE id_usuario=uid;

 IF NOT FOUND THEN RAISE EXCEPTION 'Cuenta de acceso inexistente'; END IF;

 NEW.id_usuario:=uid;

 NEW.id_cl:=uid;

 NEW.nom_cl:=u.nombre_completo;NEW.emai_cl:=u.correo_normalizado;NEW.tel_cl:=u.telefono;

 RETURN NEW;

END; $$;

CREATE OR REPLACE FUNCTION completar_propietario_nomenclatura() RETURNS trigger LANGUAGE plpgsql AS $$

DECLARE u usuario%ROWTYPE; uid BIGINT;

BEGIN

 uid:=NEW.id_usuario;

 uid:=coalesce(uid,NEW.id_p);

 SELECT * INTO u FROM usuario WHERE id_usuario=uid;

 IF NOT FOUND THEN RAISE EXCEPTION 'Cuenta de acceso inexistente'; END IF;

 NEW.id_usuario:=uid;

 NEW.id_p:=uid;

 NEW.nom_p:=u.nombre_completo;NEW.emai_p:=u.correo_normalizado;NEW.tel_p:=u.telefono;

 RETURN NEW;

END; $$;

CREATE OR REPLACE FUNCTION completar_pais_ciudad() RETURNS trigger LANGUAGE plpgsql AS $$ DECLARE p BIGINT; BEGIN

 SELECT id_pais INTO p FROM division_administrativa WHERE id_division=NEW.id_division;

 IF NEW.id_ps IS NOT NULL AND NEW.id_ps<>p THEN RAISE EXCEPTION 'Pais incompatible con region'; END IF;

 NEW.id_ps:=p;RETURN NEW;

END; $$;

CREATE OR REPLACE FUNCTION completar_renta_calendario() RETURNS trigger LANGUAGE plpgsql AS $$ DECLARE o ocupacion%ROWTYPE; BEGIN

 SELECT * INTO o FROM ocupacion WHERE id_ocupacion=NEW.id_ocupacion;

 IF (NEW.id_i IS NOT NULL AND NEW.id_i<>o.id_inmueble) OR (NEW.fch_e IS NOT NULL AND NEW.fch_e<>o.fecha_inicio) OR (NEW.fch_s IS NOT NULL AND NEW.fch_s<>o.fecha_fin) THEN RAISE EXCEPTION 'Renta y calendario no coinciden'; END IF;

 NEW.id_i:=o.id_inmueble;NEW.fch_e:=o.fecha_inicio;NEW.fch_s:=o.fecha_fin;RETURN NEW;

END; $$;

CREATE OR REPLACE FUNCTION escribir_inmueble_operativo() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN

 IF TG_OP='DELETE' THEN DELETE FROM inmueble WHERE id_inmueble=OLD.id_inmueble; RETURN OLD; END IF;

 IF TG_OP='INSERT' THEN

  IF NEW.id_inmueble IS NULL THEN

   INSERT INTO inmueble(id_ciudad,titulo,descripcion,direccion_detallada,tipo,es_rural,cerca_mar,capacidad,moneda,estado,area_m2,habitaciones)

   VALUES(NEW.id_ciudad,NEW.titulo,NEW.descripcion,NEW.direccion_detallada,NEW.tipo,NEW.es_rural,NEW.cerca_mar,NEW.capacidad,NEW.moneda,coalesce(NEW.estado,'BORRADOR'),NEW.area_m2,NEW.habitaciones) RETURNING id_inmueble,creado_en INTO NEW.id_inmueble,NEW.creado_en;

  ELSE

   INSERT INTO inmueble(id_inmueble,id_ciudad,titulo,descripcion,direccion_detallada,tipo,es_rural,cerca_mar,capacidad,moneda,estado,area_m2,habitaciones) OVERRIDING SYSTEM VALUE

   VALUES(NEW.id_inmueble,NEW.id_ciudad,NEW.titulo,NEW.descripcion,NEW.direccion_detallada,NEW.tipo,NEW.es_rural,NEW.cerca_mar,NEW.capacidad,NEW.moneda,coalesce(NEW.estado,'BORRADOR'),NEW.area_m2,NEW.habitaciones);

  END IF;

  INSERT INTO asocia_operativa(id_inmueble,id_propietario,precio_noche) VALUES(NEW.id_inmueble,NEW.id_propietario,NEW.tarifa_noche);

 ELSE

  UPDATE inmueble SET id_ciudad=NEW.id_ciudad,titulo=NEW.titulo,descripcion=NEW.descripcion,direccion_detallada=NEW.direccion_detallada,tipo=NEW.tipo,es_rural=NEW.es_rural,cerca_mar=NEW.cerca_mar,capacidad=NEW.capacidad,moneda=NEW.moneda,estado=NEW.estado,area_m2=NEW.area_m2,habitaciones=NEW.habitaciones WHERE id_inmueble=OLD.id_inmueble;

  UPDATE asocia_operativa SET id_propietario=NEW.id_propietario,precio_noche=NEW.tarifa_noche WHERE id_inmueble=OLD.id_inmueble;

 END IF;

 RETURN NEW;

END; $$;

CREATE OR REPLACE FUNCTION guardar_cuenta_financiera() RETURNS trigger LANGUAGE plpgsql AS $$ DECLARE dato JSONB; BEGIN

 IF TG_OP='UPDATE' THEN

  IF NEW.cuenta_bancaria_pactada IS DISTINCT FROM OLD.cuenta_bancaria_pactada THEN RAISE EXCEPTION 'Cuenta bancaria historica inmutable'; END IF;

  RETURN NEW;

 END IF;

 IF TG_TABLE_NAME='giro' THEN

  SELECT jsonb_build_object('banco_operativa',b.nombre,'id_banco',b.id_banco,'numero_cuenta',x.numero_cuenta,'tipo_cuenta',x.tipo_cuenta) INTO dato FROM relaciona_operativa x JOIN banco_operativa b USING(id_banco) JOIN renta_operativa r ON r.id_propietario_pactado=x.id_propietario WHERE r.id_ocupacion=NEW.id_reserva;

 ELSE

  SELECT jsonb_build_object('banco_operativa',b.nombre,'id_banco',b.id_banco,'numero_cuenta',x.numero_cuenta,'tipo_cuenta',x.tipo_cuenta) INTO dato FROM vincula_operativa x JOIN banco_operativa b USING(id_banco) JOIN renta_operativa r ON r.id_cliente=x.id_cliente WHERE r.id_ocupacion=NEW.id_reserva;

 END IF;

 NEW.cuenta_bancaria_pactada:=dato;RETURN NEW;

END; $$;

CREATE OR REPLACE FUNCTION impedir_reescritura_ocupacion() RETURNS trigger LANGUAGE plpgsql AS $$

BEGIN

  IF (NEW.id_inmueble, NEW.fecha_inicio, NEW.fecha_fin, NEW.tipo)

      IS DISTINCT FROM (OLD.id_inmueble, OLD.fecha_inicio, OLD.fecha_fin, OLD.tipo) THEN

    RAISE EXCEPTION 'Fechas, inmueble y tipo de ocupacion son inmutables';

  END IF;

  RETURN NEW;

END;

$$;

CREATE OR REPLACE FUNCTION prohibir_borrado_financiero() RETURNS trigger LANGUAGE plpgsql AS $$

BEGIN

  RAISE EXCEPTION 'Historial inmutable: usar estados, no DELETE en %',TG_TABLE_NAME;

END;

$$;

CREATE OR REPLACE FUNCTION proteger_capacidad_inmueble() RETURNS trigger LANGUAGE plpgsql AS $$

BEGIN

  IF NEW.capacd < OLD.capacd AND EXISTS (

    SELECT 1 FROM ocupacion o JOIN reserva r ON r.id_ocupacion=o.id_ocupacion

    WHERE o.id_inmueble=OLD.id_i AND o.activa

      AND o.fecha_fin > CURRENT_DATE AND r.cantidad_huespedes > NEW.capacd

  ) THEN

    RAISE EXCEPTION 'La nueva capacidad es menor que reservas futuras vigentes';

  END IF;

  RETURN NEW;

END;

$$;

CREATE OR REPLACE FUNCTION sincronizar_calendario_renta() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN

 UPDATE renta_operativa SET id_inmueble=NEW.id_inmueble,fecha_entrada=NEW.fecha_inicio,fecha_salida=NEW.fecha_fin WHERE id_ocupacion=NEW.id_ocupacion;RETURN NEW;

END; $$;

CREATE OR REPLACE FUNCTION sincronizar_identidad_persona() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN

 UPDATE cliente SET nombre_completo=NEW.nombre_completo,correo_normalizado=NEW.correo_normalizado,telefono=NEW.telefono WHERE id_usuario=NEW.id_usuario;

 UPDATE propietario SET nombre_completo=NEW.nombre_completo,correo_normalizado=NEW.correo_normalizado,telefono=NEW.telefono WHERE id_usuario=NEW.id_usuario;

 RETURN NEW;

END; $$;

CREATE OR REPLACE FUNCTION sincronizar_ocupacion_reserva() RETURNS trigger LANGUAGE plpgsql AS $$

BEGIN

  UPDATE ocupacion SET activa = (NEW.estado NOT IN ('CANCELADA','EXPIRADA'))

  WHERE id_ocupacion = NEW.id_ocupacion;

  RETURN NEW;

END;

$$;

CREATE OR REPLACE FUNCTION validar_bloqueo() RETURNS trigger LANGUAGE plpgsql AS $$

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

CREATE OR REPLACE FUNCTION validar_giro() RETURNS trigger LANGUAGE plpgsql AS $$

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

CREATE OR REPLACE FUNCTION validar_movimiento_pago() RETURNS trigger LANGUAGE plpgsql AS $$

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

CREATE OR REPLACE FUNCTION validar_reserva() RETURNS trigger LANGUAGE plpgsql AS $$

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

    IF NEW.id_p <> i.id_propietario THEN

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

    IF (NEW.id_ocupacion, NEW.id_cl, NEW.id_p,

        NEW.cantidad_huespedes, NEW.precio_noche_pactado, NEW.moneda, NEW.expira_en)

       IS DISTINCT FROM

       (OLD.id_ocupacion, OLD.id_cl, OLD.id_p,

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

CREATE OR REPLACE FUNCTION verificar_asocia() RETURNS trigger LANGUAGE plpgsql AS $$ DECLARE k BIGINT; BEGIN

 k:=CASE WHEN TG_OP='DELETE' THEN OLD.id_i ELSE NEW.id_i END;

 IF EXISTS(SELECT 1 FROM inmueble WHERE id_inmueble=k) AND NOT EXISTS(SELECT 1 FROM asocia_operativa WHERE id_inmueble=k) THEN RAISE EXCEPTION 'El inmueble requiere propietario y precio en ASOCIA'; END IF;

 RETURN NULL;

END; $$;

CREATE OR REPLACE FUNCTION verificar_subtipo_ocupacion() RETURNS trigger LANGUAGE plpgsql AS $$

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

DROP TRIGGER completar_cliente ON clientes;

CREATE TRIGGER completar_cliente BEFORE INSERT OR UPDATE ON clientes FOR EACH ROW EXECUTE FUNCTION completar_cliente_nomenclatura();

DROP TRIGGER completar_propietario ON propietarios;

CREATE TRIGGER completar_propietario BEFORE INSERT OR UPDATE ON propietarios FOR EACH ROW EXECUTE FUNCTION completar_propietario_nomenclatura();

ALTER TABLE renta ADD COLUMN num_intento bigint;

WITH orden AS (SELECT id_ocupacion,row_number() OVER(PARTITION BY id_cl,id_i,fch_e ORDER BY creada_en,id_ocupacion) n FROM renta) UPDATE renta r SET num_intento=o.n FROM orden o WHERE o.id_ocupacion=r.id_ocupacion;

SET CONSTRAINTS ALL IMMEDIATE;

ALTER TABLE renta ALTER COLUMN num_intento SET NOT NULL;

ALTER TABLE renta ADD CONSTRAINT uq_renta_identificador UNIQUE(id_ocupacion);

DO $$ DECLARE x record; defs jsonb:='[]'; BEGIN FOR x IN SELECT conrelid::regclass::text tabla,conname,pg_get_constraintdef(oid) definicion FROM pg_constraint WHERE contype='f' AND confrelid='renta'::regclass LOOP defs:=defs||jsonb_build_array(jsonb_build_object('tabla',x.tabla,'nombre',x.conname,'def',x.definicion)); EXECUTE format('ALTER TABLE %s DROP CONSTRAINT %I',x.tabla,x.conname); END LOOP; ALTER TABLE renta DROP CONSTRAINT reserva_pkey; ALTER TABLE renta ADD CONSTRAINT renta_pkey PRIMARY KEY(id_cl,id_i,fch_e,num_intento); FOR x IN SELECT value FROM jsonb_array_elements(defs) LOOP EXECUTE format('ALTER TABLE %s ADD CONSTRAINT %I %s',x.value->>'tabla',x.value->>'nombre',x.value->>'def'); END LOOP; END $$;

CREATE FUNCTION asignar_intento_renta() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN
 IF TG_OP='UPDATE' THEN
  IF (NEW.id_cl,NEW.id_i,NEW.fch_e,NEW.num_intento) IS DISTINCT FROM (OLD.id_cl,OLD.id_i,OLD.fch_e,OLD.num_intento) THEN RAISE EXCEPTION 'Clave historica de renta inmutable'; END IF;
 ELSE
  PERFORM 1 FROM clientes WHERE id_cl=NEW.id_cl FOR UPDATE;
  IF NEW.num_intento IS NOT NULL THEN RAISE EXCEPTION 'El intento es asignado por el sistema'; END IF;
  SELECT coalesce(max(num_intento),0)+1 INTO NEW.num_intento FROM renta WHERE id_cl=NEW.id_cl AND id_i=NEW.id_i AND fch_e=NEW.fch_e;
 END IF; RETURN NEW; END; $$;

CREATE TRIGGER ab_asignar_intento BEFORE INSERT OR UPDATE ON renta FOR EACH ROW EXECUTE FUNCTION asignar_intento_renta();

ALTER TABLE renta ADD CONSTRAINT ck_renta_intento CHECK(num_intento>0);

COMMENT ON TABLE usuario IS 'Extension tecnica de DayNest; conserva identificadores e historial';

COMMENT ON TABLE administrador IS 'Extension tecnica de DayNest; conserva identificadores e historial';

COMMENT ON TABLE division_administrativa IS 'Extension tecnica de DayNest; conserva identificadores e historial';

COMMENT ON TABLE ocupacion IS 'Extension tecnica de DayNest; conserva identificadores e historial';

COMMENT ON TABLE bloqueo IS 'Extension tecnica de DayNest; conserva identificadores e historial';

COMMENT ON TABLE movimiento_pago IS 'Extension tecnica de DayNest; conserva identificadores e historial';

COMMENT ON TABLE giro IS 'Extension tecnica de DayNest; conserva identificadores e historial';
