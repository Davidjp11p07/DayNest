-- Finanzas academicas: comision historica y autorizacion automatica auditable.
CREATE TABLE configuracion_comision (
 id_configuracion bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 porcentaje numeric(5,4) NOT NULL CHECK(porcentaje>=0 AND porcentaje<1),
 id_usuario bigint REFERENCES administrador(id_usuario) ON DELETE RESTRICT,
 creada_en timestamptz NOT NULL DEFAULT clock_timestamp()
);
COMMENT ON TABLE configuracion_comision IS 'Extension: versiones inmutables de la comision; NULL identifica configuracion inicial del sistema';
INSERT INTO configuracion_comision(porcentaje) VALUES(0.3000);
CREATE FUNCTION proteger_configuracion_comision() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN
 RAISE EXCEPTION 'La configuracion se versiona: inserte una nueva tasa'; END; $$;
CREATE TRIGGER configuracion_inmutable BEFORE UPDATE OR DELETE ON configuracion_comision FOR EACH ROW EXECUTE FUNCTION proteger_configuracion_comision();

ALTER TABLE renta ADD COLUMN porcentaje_comision numeric(5,4) CHECK(porcentaje_comision>=0 AND porcentaje_comision<1);
UPDATE renta r SET porcentaje_comision=coalesce((SELECT g.porcentaje_comision FROM giro g WHERE g.id_reserva=r.id_ocupacion AND g.estado<>'FALLIDO' ORDER BY (g.estado='EJECUTADO') DESC,g.id_giro DESC LIMIT 1),0.3000)
 WHERE EXISTS(SELECT 1 FROM movimiento_pago m WHERE m.id_reserva=r.id_ocupacion AND m.clase='COBRO' AND m.estado='APROBADO') OR EXISTS(SELECT 1 FROM giro g WHERE g.id_reserva=r.id_ocupacion AND g.estado<>'FALLIDO');
CREATE FUNCTION proteger_comision_renta() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN
 IF OLD.porcentaje_comision IS NOT NULL AND NEW.porcentaje_comision IS DISTINCT FROM OLD.porcentaje_comision THEN RAISE EXCEPTION 'La comision historica es inmutable'; END IF;
 RETURN NEW; END; $$;
CREATE TRIGGER comision_historica BEFORE UPDATE OF porcentaje_comision ON renta FOR EACH ROW EXECUTE FUNCTION proteger_comision_renta();
CREATE FUNCTION fijar_comision_cobro() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN
 IF NEW.clase='COBRO' AND NEW.estado='APROBADO' AND (TG_OP='INSERT' OR OLD.estado IS DISTINCT FROM NEW.estado) THEN
  IF NOT EXISTS(SELECT 1 FROM renta r JOIN vincula v USING(id_cl) WHERE r.id_ocupacion=NEW.id_reserva) THEN RAISE EXCEPTION 'El cliente debe vincular una cuenta bancaria antes de pagar'; END IF;
  UPDATE renta SET porcentaje_comision=(SELECT porcentaje FROM configuracion_comision ORDER BY id_configuracion DESC LIMIT 1) WHERE id_ocupacion=NEW.id_reserva AND porcentaje_comision IS NULL;
  IF EXISTS(SELECT 1 FROM renta WHERE id_ocupacion=NEW.id_reserva AND porcentaje_comision IS NULL) THEN RAISE EXCEPTION 'Falta configurar la comision'; END IF;
 END IF; RETURN NEW; END; $$;
CREATE TRIGGER fijar_comision AFTER INSERT OR UPDATE OF estado ON movimiento_pago FOR EACH ROW EXECUTE FUNCTION fijar_comision_cobro();

ALTER TABLE giro ADD COLUMN origen_autorizacion varchar(16) NOT NULL DEFAULT 'ADMINISTRADOR' CHECK(origen_autorizacion IN ('ADMINISTRADOR','SISTEMA'));
ALTER TABLE giro DROP CONSTRAINT ck_giro_ejecutado;
ALTER TABLE giro ADD CONSTRAINT ck_giro_ejecutado CHECK(estado<>'EJECUTADO' OR (ejecutado_en IS NOT NULL AND ((origen_autorizacion='ADMINISTRADOR' AND id_admin_autorizador IS NOT NULL) OR (origen_autorizacion='SISTEMA' AND id_admin_autorizador IS NULL))));

CREATE TABLE liquidacion_giro (
 id_ocupacion bigint PRIMARY KEY REFERENCES renta(id_ocupacion) ON DELETE RESTRICT,
 estado varchar(12) NOT NULL CHECK(estado IN ('PENDIENTE','FALLIDA','EJECUTADA')),
 motivo varchar(300) NOT NULL,
 intentos bigint NOT NULL DEFAULT 1 CHECK(intentos>0),
 revisada_en timestamptz NOT NULL DEFAULT clock_timestamp(),
 id_giro bigint REFERENCES giro(id_giro) ON DELETE RESTRICT,
 historial jsonb NOT NULL DEFAULT '[]'::jsonb
);
COMMENT ON TABLE liquidacion_giro IS 'Extension: liquidaciones automaticas, motivos, intentos y cambios de estado; transferencias simuladas';
CREATE FUNCTION registrar_liquidacion(k bigint,e text,m text,g bigint DEFAULT NULL) RETURNS void LANGUAGE plpgsql AS $$
DECLARE evento jsonb:=jsonb_build_array(jsonb_build_object('estado',e,'motivo',m,'fecha',clock_timestamp()));
BEGIN INSERT INTO liquidacion_giro(id_ocupacion,estado,motivo,id_giro,historial) VALUES(k,e,m,g,evento)
 ON CONFLICT(id_ocupacion) DO UPDATE SET estado=e,motivo=m,id_giro=g,intentos=liquidacion_giro.intentos+1,revisada_en=clock_timestamp(),historial=liquidacion_giro.historial||CASE WHEN (liquidacion_giro.estado,liquidacion_giro.motivo) IS DISTINCT FROM (e,m) THEN evento ELSE '[]'::jsonb END;
END; $$;

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
 IF NEW.porcentaje_comision IS DISTINCT FROM (SELECT porcentaje_comision FROM renta WHERE id_ocupacion=NEW.id_reserva) THEN RAISE EXCEPTION 'El giro debe usar la comision historica'; END IF;
 IF EXISTS(SELECT 1 FROM movimiento_pago WHERE id_reserva=NEW.id_reserva AND (estado='PENDIENTE' OR (clase='DEVOLUCION' AND estado='APROBADO'))) THEN RAISE EXCEPTION 'Hay movimientos pendientes o devoluciones'; END IF;
 IF NOT EXISTS(SELECT 1 FROM renta x JOIN relaciona a ON a.id_p=x.id_p JOIN banco b USING(id_b) WHERE x.id_ocupacion=NEW.id_reserva) THEN RAISE EXCEPTION 'El propietario pactado no tiene cuenta bancaria valida'; END IF;




    IF r.estado<>'FINALIZADA' THEN



      RAISE EXCEPTION 'Solo se gira una reserva finalizada';



    END IF;



    SELECT COALESCE(SUM(CASE WHEN clase='COBRO' THEN monto ELSE -monto END),0)



    INTO neto FROM movimiento_pago



    WHERE id_reserva=NEW.id_reserva AND estado='APROBADO';



    IF neto<=0 OR NEW.monto_comision<>round(neto*NEW.porcentaje_comision,2) OR NEW.monto<>neto-NEW.monto_comision THEN



      RAISE EXCEPTION 'Giro y comision deben coincidir con el neto cobrado';



    END IF;



    IF NEW.ejecutado_en IS NULL OR (NEW.origen_autorizacion='ADMINISTRADOR' AND NEW.id_admin_autorizador IS NULL) OR (NEW.origen_autorizacion='SISTEMA' AND NEW.id_admin_autorizador IS NOT NULL) THEN



      RAISE EXCEPTION 'Giro ejecutado requiere administrador y fecha';



    END IF;



  END IF;



  RETURN NEW;



END;



$$;

CREATE OR REPLACE FUNCTION guardar_cuenta_financiera() RETURNS trigger LANGUAGE plpgsql AS $$ DECLARE dato jsonb; BEGIN
 IF TG_TABLE_NAME='giro' AND TG_OP='UPDATE' AND NEW.estado='EJECUTADO' AND OLD.estado<>'EJECUTADO' THEN
  SELECT jsonb_build_object('banco',b.nom_b,'id_banco',b.id_b,'numero_cuenta',a.num_cta,'tipo_cuenta',a.tipo_cta) INTO dato FROM renta r JOIN relaciona a ON a.id_p=r.id_p JOIN banco b USING(id_b) WHERE r.id_ocupacion=NEW.id_reserva FOR SHARE OF a,b;
  IF dato IS NULL THEN RAISE EXCEPTION 'Cuenta de destino inexistente'; END IF;
  NEW.cuenta_bancaria_pactada:=dato;RETURN NEW;
 END IF;
 IF TG_OP='UPDATE' THEN
  IF NEW.cuenta_bancaria_pactada IS DISTINCT FROM OLD.cuenta_bancaria_pactada THEN RAISE EXCEPTION 'Cuenta bancaria historica inmutable'; END IF; RETURN NEW;
 END IF;
 IF TG_TABLE_NAME='giro' THEN
  SELECT jsonb_build_object('banco',b.nom_b,'id_banco',b.id_b,'numero_cuenta',a.num_cta,'tipo_cuenta',a.tipo_cta) INTO dato FROM renta r JOIN relaciona a ON a.id_p=r.id_p JOIN banco b USING(id_b) WHERE r.id_ocupacion=NEW.id_reserva;
 ELSE
  SELECT jsonb_build_object('banco',b.nom_b,'id_banco',b.id_b,'numero_cuenta',a.num_cta,'tipo_cuenta',a.tipo_cta) INTO dato FROM renta r JOIN vincula a USING(id_cl) JOIN banco b USING(id_b) WHERE r.id_ocupacion=NEW.id_reserva;
 END IF;NEW.cuenta_bancaria_pactada:=dato;RETURN NEW;END; $$;

CREATE FUNCTION liquidar_reserva_automatica(k bigint) RETURNS bigint LANGUAGE plpgsql AS $$
DECLARE r renta%ROWTYPE; g giro%ROWTYPE; total numeric(16,2); pagado numeric(16,2); comision numeric(16,2);
BEGIN
 SELECT * INTO r FROM renta WHERE id_ocupacion=k FOR UPDATE;
 IF NOT FOUND OR r.estado<>'FINALIZADA' OR r.fch_s>CURRENT_DATE THEN RETURN NULL; END IF;
 SELECT * INTO g FROM giro WHERE id_reserva=k AND estado<>'FALLIDO' FOR UPDATE;
 IF FOUND AND g.estado='EJECUTADO' THEN RETURN g.id_giro; END IF;
 total:=(r.fch_s-r.fch_e)*r.precio_noche_pactado;
 SELECT coalesce(sum(CASE WHEN clase='COBRO' THEN monto ELSE -monto END),0) INTO pagado FROM movimiento_pago WHERE id_reserva=k AND estado='APROBADO';
 IF pagado<>total OR pagado<=0 THEN PERFORM registrar_liquidacion(k,'PENDIENTE','Pago aprobado incompleto');RETURN NULL; END IF;
 IF EXISTS(SELECT 1 FROM movimiento_pago WHERE id_reserva=k AND (estado='PENDIENTE' OR (clase='DEVOLUCION' AND estado='APROBADO'))) THEN PERFORM registrar_liquidacion(k,'PENDIENTE','Hay movimientos pendientes o devoluciones');RETURN NULL;END IF;
 IF r.porcentaje_comision IS NULL THEN PERFORM registrar_liquidacion(k,'PENDIENTE','Falta la comision historica');RETURN NULL;END IF;
 PERFORM 1 FROM relaciona a JOIN banco b USING(id_b) WHERE a.id_p=r.id_p FOR SHARE OF a,b;
 IF NOT FOUND THEN PERFORM registrar_liquidacion(k,'PENDIENTE','El propietario pactado no tiene cuenta bancaria valida');RETURN NULL;END IF;
 comision:=round(total*r.porcentaje_comision,2);
 BEGIN
  IF g.id_giro IS NULL THEN
   INSERT INTO giro(id_reserva,monto,clave_idempotencia,referencia_externa,porcentaje_comision,monto_comision,origen_autorizacion)
   VALUES(k,total-comision,'liquidacion-reserva-'||k||'-'||(SELECT count(*)+1 FROM giro WHERE id_reserva=k),'SIM-AUTO-'||k||'-'||(SELECT count(*)+1 FROM giro WHERE id_reserva=k),r.porcentaje_comision,comision,'SISTEMA') RETURNING * INTO g;
  END IF;
  IF g.estado='PENDIENTE' THEN UPDATE giro SET estado='PROCESANDO' WHERE id_giro=g.id_giro; END IF;
  UPDATE giro SET estado='EJECUTADO',origen_autorizacion='SISTEMA',id_admin_autorizador=NULL,ejecutado_en=clock_timestamp() WHERE id_giro=g.id_giro;
  PERFORM registrar_liquidacion(k,'EJECUTADA','Giro simulado ejecutado automaticamente',g.id_giro);
  RETURN g.id_giro;
 EXCEPTION WHEN OTHERS THEN
  PERFORM registrar_liquidacion(k,'FALLIDA','Fallo transaccional al simular el giro ('||SQLSTATE||'); se reintentara');RETURN NULL;
 END;
END; $$;

-- Completar la instalacion financiera con la seleccion de cuenta de pago.
-- Cuentas de uso unico: solo cambia funciones; no reescribe movimientos historicos.
CREATE OR REPLACE FUNCTION cuenta_pago_transferencia(reserva_id bigint, seleccion jsonb)
RETURNS jsonb LANGUAGE plpgsql AS $$
DECLARE dato jsonb; banco_id bigint; numero text; tipo text; nombre text;
BEGIN
 IF seleccion IS NULL THEN
  SELECT jsonb_build_object('id_banco',b.id_b,'banco',b.nom_b,'numero_cuenta',v.num_cta,'tipo_cuenta',v.tipo_cta,
   'metodo','TRANSFERENCIA','uso','REGISTRADA') INTO dato
   FROM renta r JOIN vincula v USING(id_cl) JOIN banco b USING(id_b)
   WHERE r.id_ocupacion=reserva_id FOR SHARE OF v,b;
  IF dato IS NULL THEN RAISE EXCEPTION 'Registre una cuenta o indique una solo para este pago'; END IF;
  RETURN dato;
 END IF;
 IF jsonb_typeof(seleccion)<>'object' OR seleccion->>'uso' IS DISTINCT FROM 'OCASIONAL'
  OR seleccion->>'metodo' IS DISTINCT FROM 'TRANSFERENCIA' THEN
  RAISE EXCEPTION 'Seleccion de cuenta de pago invalida';
 END IF;
 IF coalesce(seleccion->>'id_banco','') !~ '^[0-9]{1,18}$' THEN RAISE EXCEPTION 'Banco invalido'; END IF;
 banco_id:=(seleccion->>'id_banco')::bigint;
 numero:=seleccion->>'numero_cuenta';tipo:=seleccion->>'tipo_cuenta';
 IF numero IS NULL OR numero !~ '^[0-9]{4,34}$' OR tipo IS NULL OR tipo NOT IN ('AHORROS','CORRIENTE') THEN
  RAISE EXCEPTION 'Numero o tipo de cuenta invalido';
 END IF;
 SELECT nom_b INTO nombre FROM banco WHERE id_b=banco_id FOR SHARE;
 IF NOT FOUND THEN RAISE EXCEPTION 'El banco seleccionado no existe'; END IF;
 RETURN jsonb_build_object('id_banco',banco_id,'banco',nombre,'numero_cuenta',numero,'tipo_cuenta',tipo,
  'metodo','TRANSFERENCIA','uso','OCASIONAL');
END; $$;

CREATE OR REPLACE FUNCTION guardar_cuenta_financiera() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE dato jsonb;
BEGIN
 IF TG_TABLE_NAME='giro' AND TG_OP='UPDATE' AND NEW.estado='EJECUTADO' AND OLD.estado<>'EJECUTADO' THEN
  SELECT jsonb_build_object('banco',b.nom_b,'id_banco',b.id_b,'numero_cuenta',a.num_cta,'tipo_cuenta',a.tipo_cta)
   INTO dato FROM renta r JOIN relaciona a ON a.id_p=r.id_p JOIN banco b USING(id_b)
   WHERE r.id_ocupacion=NEW.id_reserva FOR SHARE OF a,b;
  IF dato IS NULL THEN RAISE EXCEPTION 'Cuenta de destino inexistente'; END IF;
  NEW.cuenta_bancaria_pactada:=dato;RETURN NEW;
 END IF;
 IF TG_OP='UPDATE' THEN
  IF NEW.cuenta_bancaria_pactada IS DISTINCT FROM OLD.cuenta_bancaria_pactada THEN
   RAISE EXCEPTION 'Cuenta bancaria historica inmutable';
  END IF;RETURN NEW;
 END IF;
 IF TG_TABLE_NAME='giro' THEN
  SELECT jsonb_build_object('banco',b.nom_b,'id_banco',b.id_b,'numero_cuenta',a.num_cta,'tipo_cuenta',a.tipo_cta)
   INTO dato FROM renta r JOIN relaciona a ON a.id_p=r.id_p JOIN banco b USING(id_b)
   WHERE r.id_ocupacion=NEW.id_reserva;
 ELSIF NEW.clase='DEVOLUCION' THEN
  SELECT cuenta_bancaria_pactada INTO dato FROM movimiento_pago WHERE id_movimiento=NEW.id_cobro_origen;
 ELSE
  dato:=cuenta_pago_transferencia(NEW.id_reserva,NEW.cuenta_bancaria_pactada);
 END IF;
 NEW.cuenta_bancaria_pactada:=dato;RETURN NEW;
END; $$;

CREATE OR REPLACE FUNCTION fijar_comision_cobro() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 IF NEW.clase='COBRO' AND NEW.estado='APROBADO' AND (TG_OP='INSERT' OR OLD.estado IS DISTINCT FROM NEW.estado) THEN
  IF NEW.cuenta_bancaria_pactada->>'numero_cuenta' IS NULL THEN
   RAISE EXCEPTION 'El cobro requiere una cuenta bancaria';
  END IF;
  UPDATE renta SET porcentaje_comision=(SELECT porcentaje FROM configuracion_comision ORDER BY id_configuracion DESC LIMIT 1)
   WHERE id_ocupacion=NEW.id_reserva AND porcentaje_comision IS NULL;
  IF EXISTS(SELECT 1 FROM renta WHERE id_ocupacion=NEW.id_reserva AND porcentaje_comision IS NULL) THEN
   RAISE EXCEPTION 'Falta configurar la comision';
  END IF;
 END IF;RETURN NEW;
END; $$;
