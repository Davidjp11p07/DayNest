-- Transformacion al modelo del profesor. Los datos bancarios desconocidos permanecen pendientes.
SET LOCAL lock_timeout='10s';
LOCK TABLE usuario,cliente,propietario,inmueble,reserva,ocupacion,movimiento_pago,giro IN ACCESS EXCLUSIVE MODE;
ALTER TABLE cliente RENAME COLUMN id_usuario TO id_cliente;
ALTER TABLE propietario RENAME COLUMN id_usuario TO id_propietario;
ALTER TABLE cliente ADD COLUMN id_usuario BIGINT UNIQUE REFERENCES usuario(id_usuario) ON DELETE RESTRICT,
 ADD COLUMN nombre_completo VARCHAR(160), ADD COLUMN correo_normalizado VARCHAR(254), ADD COLUMN telefono VARCHAR(30),
 ADD COLUMN id_ciudad BIGINT REFERENCES ciudad(id_ciudad) ON DELETE RESTRICT;
ALTER TABLE propietario ADD COLUMN id_usuario BIGINT UNIQUE REFERENCES usuario(id_usuario) ON DELETE RESTRICT,
 ADD COLUMN nombre_completo VARCHAR(160), ADD COLUMN correo_normalizado VARCHAR(254), ADD COLUMN telefono VARCHAR(30);
UPDATE cliente c SET id_usuario=c.id_cliente,nombre_completo=u.nombre_completo,correo_normalizado=u.correo_normalizado,telefono=u.telefono FROM usuario u WHERE u.id_usuario=c.id_cliente;
UPDATE propietario p SET id_usuario=p.id_propietario,nombre_completo=u.nombre_completo,correo_normalizado=u.correo_normalizado,telefono=u.telefono FROM usuario u WHERE u.id_usuario=p.id_propietario;
ALTER TABLE cliente ALTER COLUMN id_usuario SET NOT NULL,ALTER COLUMN nombre_completo SET NOT NULL,ALTER COLUMN correo_normalizado SET NOT NULL;
ALTER TABLE propietario ALTER COLUMN id_usuario SET NOT NULL,ALTER COLUMN nombre_completo SET NOT NULL,ALTER COLUMN correo_normalizado SET NOT NULL;
CREATE FUNCTION completar_entidad_persona() RETURNS TRIGGER LANGUAGE plpgsql AS $$
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
CREATE TRIGGER completar_cliente BEFORE INSERT OR UPDATE ON cliente FOR EACH ROW EXECUTE FUNCTION completar_entidad_persona();
CREATE TRIGGER completar_propietario BEFORE INSERT OR UPDATE ON propietario FOR EACH ROW EXECUTE FUNCTION completar_entidad_persona();
CREATE FUNCTION sincronizar_identidad_persona() RETURNS TRIGGER LANGUAGE plpgsql AS $$ BEGIN
 UPDATE cliente SET nombre_completo=NEW.nombre_completo,correo_normalizado=NEW.correo_normalizado,telefono=NEW.telefono WHERE id_usuario=NEW.id_usuario;
 UPDATE propietario SET nombre_completo=NEW.nombre_completo,correo_normalizado=NEW.correo_normalizado,telefono=NEW.telefono WHERE id_usuario=NEW.id_usuario;
 RETURN NEW;
END; $$;
CREATE TRIGGER sincronizar_identidad AFTER UPDATE OF nombre_completo,correo_normalizado,telefono ON usuario FOR EACH ROW EXECUTE FUNCTION sincronizar_identidad_persona();
ALTER TABLE ciudad ADD COLUMN id_pais BIGINT REFERENCES pais(id_pais) ON DELETE RESTRICT;
UPDATE ciudad c SET id_pais=d.id_pais FROM division_administrativa d WHERE d.id_division=c.id_division;
ALTER TABLE ciudad ALTER COLUMN id_pais SET NOT NULL;
CREATE FUNCTION completar_pais_ciudad() RETURNS TRIGGER LANGUAGE plpgsql AS $$ DECLARE p BIGINT; BEGIN
 SELECT id_pais INTO p FROM division_administrativa WHERE id_division=NEW.id_division;
 IF NEW.id_pais IS NOT NULL AND NEW.id_pais<>p THEN RAISE EXCEPTION 'Pais incompatible con region'; END IF;
 NEW.id_pais:=p;RETURN NEW;
END; $$;
CREATE TRIGGER completar_ciudad BEFORE INSERT OR UPDATE ON ciudad FOR EACH ROW EXECUTE FUNCTION completar_pais_ciudad();
CREATE TABLE banco (
 id_banco BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 nombre VARCHAR(160) NOT NULL CHECK(length(btrim(nombre))>0),
 id_pais BIGINT NOT NULL REFERENCES pais(id_pais) ON DELETE RESTRICT,
 CONSTRAINT uq_banco_pais_nombre UNIQUE(id_pais,nombre)
);
CREATE TABLE vincula (
 id_cliente BIGINT PRIMARY KEY REFERENCES cliente(id_cliente) ON DELETE CASCADE,
 id_banco BIGINT NOT NULL REFERENCES banco(id_banco) ON DELETE RESTRICT,
 numero_cuenta VARCHAR(34) NOT NULL CHECK(numero_cuenta ~ '^[0-9]{4,34}$'),
 tipo_cuenta VARCHAR(12) NOT NULL CHECK(tipo_cuenta IN ('AHORROS','CORRIENTE')),
 actualizada_en TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp()
);
CREATE TABLE relaciona (
 id_propietario BIGINT PRIMARY KEY REFERENCES propietario(id_propietario) ON DELETE CASCADE,
 id_banco BIGINT NOT NULL REFERENCES banco(id_banco) ON DELETE RESTRICT,
 numero_cuenta VARCHAR(34) NOT NULL CHECK(numero_cuenta ~ '^[0-9]{4,34}$'),
 tipo_cuenta VARCHAR(12) NOT NULL CHECK(tipo_cuenta IN ('AHORROS','CORRIENTE')),
 actualizada_en TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp()
);
CREATE TABLE asocia (
 id_inmueble BIGINT PRIMARY KEY REFERENCES inmueble(id_inmueble) ON DELETE CASCADE,
 id_propietario BIGINT NOT NULL REFERENCES propietario(id_propietario) ON DELETE RESTRICT,
 precio_noche NUMERIC(16,2) NOT NULL CHECK(precio_noche>0)
);
INSERT INTO asocia SELECT id_inmueble,id_propietario,tarifa_noche FROM inmueble;
CREATE INDEX ix_asocia_propietario ON asocia(id_propietario);
ALTER TABLE inmueble ADD COLUMN area_m2 NUMERIC(12,2) CHECK(area_m2>0), ADD COLUMN habitaciones SMALLINT CHECK(habitaciones>0);
CREATE VIEW inmueble_operativo AS SELECT id_inmueble,id_propietario,id_ciudad,titulo,descripcion,direccion_detallada,tipo,es_rural,cerca_mar,capacidad,tarifa_noche,moneda,estado,creado_en,area_m2,habitaciones FROM inmueble;

CREATE OR REPLACE FUNCTION validar_reserva() RETURNS TRIGGER LANGUAGE plpgsql AS $$
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

CREATE OR REPLACE FUNCTION validar_bloqueo() RETURNS TRIGGER LANGUAGE plpgsql AS $$
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

-- Las funciones ya usan la vista; se conserva su tipo mientras se transforma la tabla.
ALTER TABLE inmueble RENAME COLUMN id_propietario TO propietario_anterior;
ALTER TABLE inmueble RENAME COLUMN tarifa_noche TO tarifa_anterior;
-- La vista conserva nombres antiguos; se sustituye por la proyeccion del nuevo modelo.
CREATE OR REPLACE VIEW inmueble_operativo AS SELECT i.id_inmueble,a.id_propietario,i.id_ciudad,i.titulo,i.descripcion,i.direccion_detallada,i.tipo,i.es_rural,i.cerca_mar,i.capacidad,a.precio_noche AS tarifa_noche,i.moneda,i.estado,i.creado_en,i.area_m2,i.habitaciones FROM inmueble i JOIN asocia a USING(id_inmueble);
ALTER TABLE inmueble DROP COLUMN propietario_anterior, DROP COLUMN tarifa_anterior;
CREATE FUNCTION escribir_inmueble_operativo() RETURNS TRIGGER LANGUAGE plpgsql AS $$ BEGIN
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
CREATE TRIGGER escribir_inmueble INSTEAD OF INSERT OR UPDATE OR DELETE ON inmueble_operativo FOR EACH ROW EXECUTE FUNCTION escribir_inmueble_operativo();
CREATE FUNCTION verificar_asocia() RETURNS TRIGGER LANGUAGE plpgsql AS $$ DECLARE k BIGINT; BEGIN
 k:=CASE WHEN TG_OP='DELETE' THEN OLD.id_inmueble ELSE NEW.id_inmueble END;
 IF EXISTS(SELECT 1 FROM inmueble WHERE id_inmueble=k) AND NOT EXISTS(SELECT 1 FROM asocia WHERE id_inmueble=k) THEN RAISE EXCEPTION 'El inmueble requiere propietario y precio en ASOCIA'; END IF;
 RETURN NULL;
END; $$;
CREATE CONSTRAINT TRIGGER ck_inmueble_asocia AFTER INSERT OR UPDATE ON inmueble DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION verificar_asocia();
CREATE CONSTRAINT TRIGGER ck_asocia_inmueble AFTER INSERT OR UPDATE OR DELETE ON asocia DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION verificar_asocia();
ALTER TABLE reserva RENAME TO renta;
CREATE VIEW reserva AS SELECT * FROM renta;

ALTER TABLE renta ADD COLUMN id_inmueble BIGINT REFERENCES inmueble(id_inmueble) ON DELETE RESTRICT,
 ADD COLUMN fecha_entrada DATE, ADD COLUMN fecha_salida DATE;
UPDATE renta r SET id_inmueble=o.id_inmueble,fecha_entrada=o.fecha_inicio,fecha_salida=o.fecha_fin FROM ocupacion o WHERE o.id_ocupacion=r.id_ocupacion;
SET CONSTRAINTS ALL IMMEDIATE;
ALTER TABLE renta ALTER COLUMN id_inmueble SET NOT NULL, ALTER COLUMN fecha_entrada SET NOT NULL, ALTER COLUMN fecha_salida SET NOT NULL;
ALTER TABLE renta ADD CONSTRAINT ck_renta_estancia CHECK(fecha_salida>fecha_entrada);
CREATE OR REPLACE VIEW reserva AS SELECT * FROM renta;
CREATE FUNCTION completar_renta_calendario() RETURNS TRIGGER LANGUAGE plpgsql AS $$ DECLARE o ocupacion%ROWTYPE; BEGIN
 SELECT * INTO o FROM ocupacion WHERE id_ocupacion=NEW.id_ocupacion;
 IF (NEW.id_inmueble IS NOT NULL AND NEW.id_inmueble<>o.id_inmueble) OR (NEW.fecha_entrada IS NOT NULL AND NEW.fecha_entrada<>o.fecha_inicio) OR (NEW.fecha_salida IS NOT NULL AND NEW.fecha_salida<>o.fecha_fin) THEN RAISE EXCEPTION 'Renta y calendario no coinciden'; END IF;
 NEW.id_inmueble:=o.id_inmueble;NEW.fecha_entrada:=o.fecha_inicio;NEW.fecha_salida:=o.fecha_fin;RETURN NEW;
END; $$;
CREATE TRIGGER aa_completar_renta BEFORE INSERT OR UPDATE ON renta FOR EACH ROW EXECUTE FUNCTION completar_renta_calendario();
-- Mantener la proyeccion del calendario en los fixtures historicos y cambios internos autorizados.
CREATE FUNCTION sincronizar_calendario_renta() RETURNS TRIGGER LANGUAGE plpgsql AS $$ BEGIN
 UPDATE renta SET id_inmueble=NEW.id_inmueble,fecha_entrada=NEW.fecha_inicio,fecha_salida=NEW.fecha_fin WHERE id_ocupacion=NEW.id_ocupacion;RETURN NEW;
END; $$;
CREATE TRIGGER sincronizar_calendario AFTER UPDATE OF fecha_inicio,fecha_fin,id_inmueble ON ocupacion FOR EACH ROW EXECUTE FUNCTION sincronizar_calendario_renta();
ALTER TABLE movimiento_pago ADD COLUMN cuenta_bancaria_pactada JSONB;
ALTER TABLE giro ADD COLUMN cuenta_bancaria_pactada JSONB;
CREATE FUNCTION guardar_cuenta_financiera() RETURNS TRIGGER LANGUAGE plpgsql AS $$ DECLARE dato JSONB; BEGIN
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
CREATE TRIGGER cuenta_historica_pago BEFORE INSERT OR UPDATE ON movimiento_pago FOR EACH ROW EXECUTE FUNCTION guardar_cuenta_financiera();
CREATE TRIGGER cuenta_historica_giro BEFORE INSERT OR UPDATE ON giro FOR EACH ROW EXECUTE FUNCTION guardar_cuenta_financiera();
COMMENT ON TABLE usuario IS 'Soporte tecnico de acceso compartido; cliente y propietario son entidades completas independientes';
COMMENT ON TABLE renta IS 'Relacion RENTA del profesor; reserva es una vista de compatibilidad sin almacenamiento';

INSERT INTO banco(nombre,id_pais) SELECT 'Banco demo '||nombre,id_pais FROM pais ON CONFLICT DO NOTHING;
