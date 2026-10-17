-- DayNest: diez tablas del profesor y nueve extensiones tecnicas.
-- Bancos y transferencias simulados; vistas sin almacenamiento duplicado.
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
-- Name: daynest; Type: SCHEMA; Schema: -; Owner: -
--



--
-- Name: asignar_intento_renta(); Type: FUNCTION; Schema: esquema operativo; Owner: -
--

CREATE FUNCTION asignar_intento_renta() RETURNS trigger
    LANGUAGE plpgsql
    AS $$ BEGIN

 IF TG_OP='UPDATE' THEN

  IF (NEW.id_cl,NEW.id_i,NEW.fch_e,NEW.num_intento) IS DISTINCT FROM (OLD.id_cl,OLD.id_i,OLD.fch_e,OLD.num_intento) THEN RAISE EXCEPTION 'Clave historica de renta inmutable'; END IF;

 ELSE

  PERFORM 1 FROM clientes WHERE id_cl=NEW.id_cl FOR UPDATE;

  IF NEW.num_intento IS NOT NULL THEN RAISE EXCEPTION 'El intento es asignado por el sistema'; END IF;

  SELECT coalesce(max(num_intento),0)+1 INTO NEW.num_intento FROM renta WHERE id_cl=NEW.id_cl AND id_i=NEW.id_i AND fch_e=NEW.fch_e;

 END IF; RETURN NEW; END; $$;


--
-- Name: completar_cliente_nomenclatura(); Type: FUNCTION; Schema: esquema operativo; Owner: -
--

CREATE FUNCTION completar_cliente_nomenclatura() RETURNS trigger
    LANGUAGE plpgsql
    AS $$



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



 IF NEW.id_ps IS NOT NULL AND NEW.id_ps<>p THEN RAISE EXCEPTION 'Pais incompatible con region'; END IF;



 NEW.id_ps:=p;RETURN NEW;



END; $$;


--
-- Name: completar_propietario_nomenclatura(); Type: FUNCTION; Schema: esquema operativo; Owner: -
--

CREATE FUNCTION completar_propietario_nomenclatura() RETURNS trigger
    LANGUAGE plpgsql
    AS $$



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


--
-- Name: completar_renta_calendario(); Type: FUNCTION; Schema: esquema operativo; Owner: -
--

CREATE FUNCTION completar_renta_calendario() RETURNS trigger
    LANGUAGE plpgsql
    AS $$ DECLARE o ocupacion%ROWTYPE; BEGIN



 SELECT * INTO o FROM ocupacion WHERE id_ocupacion=NEW.id_ocupacion;



 IF (NEW.id_i IS NOT NULL AND NEW.id_i<>o.id_inmueble) OR (NEW.fch_e IS NOT NULL AND NEW.fch_e<>o.fecha_inicio) OR (NEW.fch_s IS NOT NULL AND NEW.fch_s<>o.fecha_fin) THEN RAISE EXCEPTION 'Renta y calendario no coinciden'; END IF;



 NEW.id_i:=o.id_inmueble;NEW.fch_e:=o.fecha_inicio;NEW.fch_s:=o.fecha_fin;RETURN NEW;



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



  INSERT INTO asocia_operativa(id_inmueble,id_propietario,precio_noche) VALUES(NEW.id_inmueble,NEW.id_propietario,NEW.tarifa_noche);



 ELSE



  UPDATE inmueble SET id_ciudad=NEW.id_ciudad,titulo=NEW.titulo,descripcion=NEW.descripcion,direccion_detallada=NEW.direccion_detallada,tipo=NEW.tipo,es_rural=NEW.es_rural,cerca_mar=NEW.cerca_mar,capacidad=NEW.capacidad,moneda=NEW.moneda,estado=NEW.estado,area_m2=NEW.area_m2,habitaciones=NEW.habitaciones WHERE id_inmueble=OLD.id_inmueble;



  UPDATE asocia_operativa SET id_propietario=NEW.id_propietario,precio_noche=NEW.tarifa_noche WHERE id_inmueble=OLD.id_inmueble;



 END IF;



 RETURN NEW;



END; $$;


--
-- Name: fijar_comision_cobro(); Type: FUNCTION; Schema: esquema operativo; Owner: -
--

CREATE FUNCTION fijar_comision_cobro() RETURNS trigger
    LANGUAGE plpgsql
    AS $$ BEGIN

 IF NEW.clase='COBRO' AND NEW.estado='APROBADO' AND (TG_OP='INSERT' OR OLD.estado IS DISTINCT FROM NEW.estado) THEN

  IF NOT EXISTS(SELECT 1 FROM renta r JOIN vincula v USING(id_cl) WHERE r.id_ocupacion=NEW.id_reserva) THEN RAISE EXCEPTION 'El cliente debe vincular una cuenta bancaria antes de pagar'; END IF;

  UPDATE renta SET porcentaje_comision=(SELECT porcentaje FROM configuracion_comision ORDER BY id_configuracion DESC LIMIT 1) WHERE id_ocupacion=NEW.id_reserva AND porcentaje_comision IS NULL;

  IF EXISTS(SELECT 1 FROM renta WHERE id_ocupacion=NEW.id_reserva AND porcentaje_comision IS NULL) THEN RAISE EXCEPTION 'Falta configurar la comision'; END IF;

 END IF; RETURN NEW; END; $$;


--
-- Name: guardar_cuenta_financiera(); Type: FUNCTION; Schema: esquema operativo; Owner: -
--

CREATE FUNCTION guardar_cuenta_financiera() RETURNS trigger
    LANGUAGE plpgsql
    AS $$ DECLARE dato jsonb; BEGIN

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
-- Name: liquidar_reserva_automatica(bigint); Type: FUNCTION; Schema: esquema operativo; Owner: -
--

CREATE FUNCTION liquidar_reserva_automatica(k bigint) RETURNS bigint
    LANGUAGE plpgsql
    AS $$

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


--
-- Name: proteger_comision_renta(); Type: FUNCTION; Schema: esquema operativo; Owner: -
--

CREATE FUNCTION proteger_comision_renta() RETURNS trigger
    LANGUAGE plpgsql
    AS $$ BEGIN

 IF OLD.porcentaje_comision IS NOT NULL AND NEW.porcentaje_comision IS DISTINCT FROM OLD.porcentaje_comision THEN RAISE EXCEPTION 'La comision historica es inmutable'; END IF;

 RETURN NEW; END; $$;


--
-- Name: proteger_configuracion_comision(); Type: FUNCTION; Schema: esquema operativo; Owner: -
--

CREATE FUNCTION proteger_configuracion_comision() RETURNS trigger
    LANGUAGE plpgsql
    AS $$ BEGIN

 RAISE EXCEPTION 'La configuracion se versiona: inserte una nueva tasa'; END; $$;


--
-- Name: registrar_liquidacion(bigint, text, text, bigint); Type: FUNCTION; Schema: esquema operativo; Owner: -
--

CREATE FUNCTION registrar_liquidacion(k bigint, e text, m text, g bigint DEFAULT NULL::bigint) RETURNS void
    LANGUAGE plpgsql
    AS $$

DECLARE evento jsonb:=jsonb_build_array(jsonb_build_object('estado',e,'motivo',m,'fecha',clock_timestamp()));

BEGIN INSERT INTO liquidacion_giro(id_ocupacion,estado,motivo,id_giro,historial) VALUES(k,e,m,g,evento)

 ON CONFLICT(id_ocupacion) DO UPDATE SET estado=e,motivo=m,id_giro=g,intentos=liquidacion_giro.intentos+1,revisada_en=clock_timestamp(),historial=liquidacion_giro.historial||CASE WHEN (liquidacion_giro.estado,liquidacion_giro.motivo) IS DISTINCT FROM (e,m) THEN evento ELSE '[]'::jsonb END;

END; $$;


--
-- Name: sincronizar_calendario_renta(); Type: FUNCTION; Schema: esquema operativo; Owner: -
--

CREATE FUNCTION sincronizar_calendario_renta() RETURNS trigger
    LANGUAGE plpgsql
    AS $$ BEGIN



 UPDATE renta_operativa SET id_inmueble=NEW.id_inmueble,fecha_entrada=NEW.fecha_inicio,fecha_salida=NEW.fecha_fin WHERE id_ocupacion=NEW.id_ocupacion;RETURN NEW;



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


--
-- Name: verificar_asocia(); Type: FUNCTION; Schema: esquema operativo; Owner: -
--

CREATE FUNCTION verificar_asocia() RETURNS trigger
    LANGUAGE plpgsql
    AS $$ DECLARE k BIGINT; BEGIN



 k:=CASE WHEN TG_OP='DELETE' THEN OLD.id_i ELSE NEW.id_i END;



 IF EXISTS(SELECT 1 FROM inmueble WHERE id_inmueble=k) AND NOT EXISTS(SELECT 1 FROM asocia_operativa WHERE id_inmueble=k) THEN RAISE EXCEPTION 'El inmueble requiere propietario y precio en ASOCIA'; END IF;



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
-- Name: TABLE administrador; Type: COMMENT; Schema: esquema operativo; Owner: -
--

COMMENT ON TABLE administrador IS 'Extension tecnica de DayNest; conserva identificadores e historial';


--
-- Name: asocia; Type: TABLE; Schema: esquema operativo; Owner: -
--

CREATE TABLE asocia (
    id_i bigint CONSTRAINT asocia_id_inmueble_not_null NOT NULL,
    id_p bigint CONSTRAINT asocia_id_propietario_not_null NOT NULL,
    prec_nch numeric(16,2) CONSTRAINT asocia_precio_noche_not_null NOT NULL,
    CONSTRAINT asocia_precio_noche_check CHECK ((prec_nch > (0)::numeric))
);


--
-- Name: TABLE asocia; Type: COMMENT; Schema: esquema operativo; Owner: -
--

COMMENT ON TABLE asocia IS 'Nucleo del modelo entidad relacion del profesor';


--
-- Name: asocia_operativa; Type: VIEW; Schema: esquema operativo; Owner: -
--

CREATE VIEW asocia_operativa AS
 SELECT id_i AS id_inmueble,
    id_p AS id_propietario,
    prec_nch AS precio_noche
   FROM asocia;


--
-- Name: banco; Type: TABLE; Schema: esquema operativo; Owner: -
--

CREATE TABLE banco (
    id_b bigint CONSTRAINT banco_id_banco_not_null NOT NULL,
    nom_b character varying(160) CONSTRAINT banco_nombre_not_null NOT NULL,
    id_ps bigint CONSTRAINT banco_id_pais_not_null NOT NULL,
    CONSTRAINT banco_nombre_check CHECK ((length(btrim((nom_b)::text)) > 0))
);


--
-- Name: TABLE banco; Type: COMMENT; Schema: esquema operativo; Owner: -
--

COMMENT ON TABLE banco IS 'Nucleo del modelo entidad relacion del profesor';


--
-- Name: banco_id_banco_seq; Type: SEQUENCE; Schema: esquema operativo; Owner: -
--

ALTER TABLE banco ALTER COLUMN id_b ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME banco_id_banco_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: banco_operativa; Type: VIEW; Schema: esquema operativo; Owner: -
--

CREATE VIEW banco_operativa AS
 SELECT id_b AS id_banco,
    nom_b AS nombre,
    id_ps AS id_pais
   FROM banco;


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
-- Name: TABLE bloqueo; Type: COMMENT; Schema: esquema operativo; Owner: -
--

COMMENT ON TABLE bloqueo IS 'Extension tecnica de DayNest; conserva identificadores e historial';


--
-- Name: ciudades; Type: TABLE; Schema: esquema operativo; Owner: -
--

CREATE TABLE ciudades (
    id_cd bigint CONSTRAINT ciudad_id_ciudad_not_null NOT NULL,
    id_division bigint CONSTRAINT ciudad_id_division_not_null NOT NULL,
    nom_cd character varying(160) CONSTRAINT ciudad_nombre_not_null NOT NULL,
    codigo_oficial character varying(40) CONSTRAINT ciudad_codigo_oficial_not_null NOT NULL,
    id_ps bigint CONSTRAINT ciudad_id_pais_not_null NOT NULL,
    CONSTRAINT ciudad_codigo_oficial_check CHECK ((length(btrim((codigo_oficial)::text)) > 0)),
    CONSTRAINT ciudad_nombre_check CHECK ((length(btrim((nom_cd)::text)) > 0))
);


--
-- Name: TABLE ciudades; Type: COMMENT; Schema: esquema operativo; Owner: -
--

COMMENT ON TABLE ciudades IS 'Nucleo del modelo entidad relacion del profesor';


--
-- Name: ciudad; Type: VIEW; Schema: esquema operativo; Owner: -
--

CREATE VIEW ciudad AS
 SELECT id_cd AS id_ciudad,
    id_division,
    nom_cd AS nombre,
    codigo_oficial,
    id_ps AS id_pais
   FROM ciudades;


--
-- Name: ciudad_id_ciudad_seq; Type: SEQUENCE; Schema: esquema operativo; Owner: -
--

ALTER TABLE ciudades ALTER COLUMN id_cd ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME ciudad_id_ciudad_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: clientes; Type: TABLE; Schema: esquema operativo; Owner: -
--

CREATE TABLE clientes (
    id_cl bigint CONSTRAINT cliente_id_usuario_not_null NOT NULL,
    id_usuario bigint CONSTRAINT cliente_id_usuario_not_null1 NOT NULL,
    nom_cl character varying(160) CONSTRAINT cliente_nombre_completo_not_null NOT NULL,
    emai_cl character varying(254) CONSTRAINT cliente_correo_normalizado_not_null NOT NULL,
    tel_cl character varying(30),
    id_cd bigint
);


--
-- Name: TABLE clientes; Type: COMMENT; Schema: esquema operativo; Owner: -
--

COMMENT ON TABLE clientes IS 'Nucleo del modelo entidad relacion del profesor';


--
-- Name: cliente; Type: VIEW; Schema: esquema operativo; Owner: -
--

CREATE VIEW cliente AS
 SELECT id_cl AS id_cliente,
    id_usuario,
    nom_cl AS nombre_completo,
    emai_cl AS correo_normalizado,
    tel_cl AS telefono,
    id_cd AS id_ciudad
   FROM clientes;


--
-- Name: configuracion_comision; Type: TABLE; Schema: esquema operativo; Owner: -
--

CREATE TABLE configuracion_comision (
    id_configuracion bigint NOT NULL,
    porcentaje numeric(5,4) NOT NULL,
    id_usuario bigint,
    creada_en timestamp with time zone DEFAULT clock_timestamp() NOT NULL,
    CONSTRAINT configuracion_comision_porcentaje_check CHECK (((porcentaje >= (0)::numeric) AND (porcentaje < (1)::numeric)))
);


--
-- Name: TABLE configuracion_comision; Type: COMMENT; Schema: esquema operativo; Owner: -
--

COMMENT ON TABLE configuracion_comision IS 'Extension: versiones inmutables de la comision; NULL identifica configuracion inicial del sistema';


--
-- Name: configuracion_comision_id_configuracion_seq; Type: SEQUENCE; Schema: esquema operativo; Owner: -
--

ALTER TABLE configuracion_comision ALTER COLUMN id_configuracion ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME configuracion_comision_id_configuracion_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
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
-- Name: TABLE division_administrativa; Type: COMMENT; Schema: esquema operativo; Owner: -
--

COMMENT ON TABLE division_administrativa IS 'Extension tecnica de DayNest; conserva identificadores e historial';


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
    id_giro bigint NOT NULL,
    id_reserva bigint NOT NULL,
    id_admin_autorizador bigint,
    monto numeric(16,2) NOT NULL,
    estado character varying(12) DEFAULT 'PENDIENTE'::character varying NOT NULL,
    clave_idempotencia character varying(120) NOT NULL,
    referencia_externa character varying(140),
    creado_en timestamp with time zone DEFAULT now() NOT NULL,
    ejecutado_en timestamp with time zone,
    porcentaje_comision numeric(5,4) DEFAULT 0 NOT NULL,
    monto_comision numeric(16,2) DEFAULT 0 NOT NULL,
    cuenta_bancaria_pactada jsonb,
    origen_autorizacion character varying(16) DEFAULT 'ADMINISTRADOR'::character varying NOT NULL,
    CONSTRAINT ck_giro_ejecutado CHECK ((((estado)::text <> 'EJECUTADO'::text) OR ((ejecutado_en IS NOT NULL) AND ((((origen_autorizacion)::text = 'ADMINISTRADOR'::text) AND (id_admin_autorizador IS NOT NULL)) OR (((origen_autorizacion)::text = 'SISTEMA'::text) AND (id_admin_autorizador IS NULL)))))),
    CONSTRAINT giro_estado_check CHECK (((estado)::text = ANY (ARRAY[('PENDIENTE'::character varying)::text, ('PROCESANDO'::character varying)::text, ('EJECUTADO'::character varying)::text, ('FALLIDO'::character varying)::text]))),
    CONSTRAINT giro_monto_check CHECK ((monto > (0)::numeric)),
    CONSTRAINT giro_monto_comision_check CHECK ((monto_comision >= (0)::numeric)),
    CONSTRAINT giro_origen_autorizacion_check CHECK (((origen_autorizacion)::text = ANY ((ARRAY['ADMINISTRADOR'::character varying, 'SISTEMA'::character varying])::text[]))),
    CONSTRAINT giro_porcentaje_comision_check CHECK (((porcentaje_comision >= (0)::numeric) AND (porcentaje_comision < (1)::numeric)))
);


--
-- Name: TABLE giro; Type: COMMENT; Schema: esquema operativo; Owner: -
--

COMMENT ON TABLE giro IS 'Extension tecnica de DayNest; conserva identificadores e historial';


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
-- Name: inmuebles; Type: TABLE; Schema: esquema operativo; Owner: -
--

CREATE TABLE inmuebles (
    id_i bigint CONSTRAINT inmueble_id_inmueble_not_null NOT NULL,
    id_cd bigint CONSTRAINT inmueble_id_ciudad_not_null NOT NULL,
    titulo character varying(150) CONSTRAINT inmueble_titulo_not_null NOT NULL,
    descripcion text,
    dir_i character varying(300) CONSTRAINT inmueble_direccion_detallada_not_null NOT NULL,
    tipo character varying(20),
    capacd smallint CONSTRAINT inmueble_capacidad_not_null NOT NULL,
    moneda character varying(3) CONSTRAINT inmueble_moneda_not_null NOT NULL,
    estado character varying(12) DEFAULT 'BORRADOR'::character varying CONSTRAINT inmueble_estado_not_null NOT NULL,
    creado_en timestamp with time zone DEFAULT now() CONSTRAINT inmueble_creado_en_not_null NOT NULL,
    es_rural boolean,
    cerca_mar boolean,
    area numeric(12,2),
    n_hab smallint,
    CONSTRAINT inmueble_area_m2_check CHECK ((area > (0)::numeric)),
    CONSTRAINT inmueble_capacidad_check CHECK ((capacd > 0)),
    CONSTRAINT inmueble_direccion_detallada_check CHECK ((length(btrim((dir_i)::text)) > 0)),
    CONSTRAINT inmueble_estado_check CHECK (((estado)::text = ANY (ARRAY[('BORRADOR'::character varying)::text, ('PUBLICADO'::character varying)::text, ('INACTIVO'::character varying)::text]))),
    CONSTRAINT inmueble_habitaciones_check CHECK ((n_hab > 0)),
    CONSTRAINT inmueble_moneda_check CHECK (((moneda)::text ~ '^[A-Z]{3}$'::text)),
    CONSTRAINT inmueble_tipo_check CHECK (((tipo)::text = ANY (ARRAY[('APARTAMENTO'::character varying)::text, ('CASA'::character varying)::text, ('CABAÑA'::character varying)::text, ('HABITACION'::character varying)::text, ('OTRO'::character varying)::text]))),
    CONSTRAINT inmueble_titulo_check CHECK ((length(btrim((titulo)::text)) > 0))
);


--
-- Name: TABLE inmuebles; Type: COMMENT; Schema: esquema operativo; Owner: -
--

COMMENT ON TABLE inmuebles IS 'Nucleo del modelo entidad relacion del profesor';


--
-- Name: inmueble; Type: VIEW; Schema: esquema operativo; Owner: -
--

CREATE VIEW inmueble AS
 SELECT id_i AS id_inmueble,
    id_cd AS id_ciudad,
    titulo,
    descripcion,
    dir_i AS direccion_detallada,
    tipo,
    es_rural,
    cerca_mar,
    capacd AS capacidad,
    moneda,
    estado,
    creado_en,
    area AS area_m2,
    n_hab AS habitaciones
   FROM inmuebles;


--
-- Name: inmueble_id_inmueble_seq; Type: SEQUENCE; Schema: esquema operativo; Owner: -
--

ALTER TABLE inmuebles ALTER COLUMN id_i ADD GENERATED ALWAYS AS IDENTITY (
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
    a.id_p AS id_propietario,
    i.id_cd AS id_ciudad,
    i.titulo,
    i.descripcion,
    i.dir_i AS direccion_detallada,
    i.tipo,
    i.es_rural,
    i.cerca_mar,
    i.capacd AS capacidad,
    a.prec_nch AS tarifa_noche,
    i.moneda,
    i.estado,
    i.creado_en,
    i.area AS area_m2,
    i.n_hab AS habitaciones
   FROM (inmuebles i(id_inmueble, id_cd, titulo, descripcion, dir_i, tipo, capacd, moneda, estado, creado_en, es_rural, cerca_mar, area, n_hab)
     JOIN asocia a(id_inmueble, id_p, prec_nch) USING (id_inmueble));


--
-- Name: liquidacion_giro; Type: TABLE; Schema: esquema operativo; Owner: -
--

CREATE TABLE liquidacion_giro (
    id_ocupacion bigint NOT NULL,
    estado character varying(12) NOT NULL,
    motivo character varying(300) NOT NULL,
    intentos bigint DEFAULT 1 NOT NULL,
    revisada_en timestamp with time zone DEFAULT clock_timestamp() NOT NULL,
    id_giro bigint,
    historial jsonb DEFAULT '[]'::jsonb NOT NULL,
    CONSTRAINT liquidacion_giro_estado_check CHECK (((estado)::text = ANY ((ARRAY['PENDIENTE'::character varying, 'FALLIDA'::character varying, 'EJECUTADA'::character varying])::text[]))),
    CONSTRAINT liquidacion_giro_intentos_check CHECK ((intentos > 0))
);


--
-- Name: TABLE liquidacion_giro; Type: COMMENT; Schema: esquema operativo; Owner: -
--

COMMENT ON TABLE liquidacion_giro IS 'Extension: liquidaciones automaticas, motivos, intentos y cambios de estado; transferencias simuladas';


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
    CONSTRAINT movimiento_pago_clase_check CHECK (((clase)::text = ANY (ARRAY[('COBRO'::character varying)::text, ('DEVOLUCION'::character varying)::text]))),
    CONSTRAINT movimiento_pago_estado_check CHECK (((estado)::text = ANY (ARRAY[('PENDIENTE'::character varying)::text, ('APROBADO'::character varying)::text, ('RECHAZADO'::character varying)::text]))),
    CONSTRAINT movimiento_pago_monto_check CHECK ((monto > (0)::numeric))
);


--
-- Name: TABLE movimiento_pago; Type: COMMENT; Schema: esquema operativo; Owner: -
--

COMMENT ON TABLE movimiento_pago IS 'Extension tecnica de DayNest; conserva identificadores e historial';


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
    CONSTRAINT ocupacion_tipo_check CHECK (((tipo)::text = ANY (ARRAY[('RESERVA'::character varying)::text, ('BLOQUEO'::character varying)::text])))
);


--
-- Name: TABLE ocupacion; Type: COMMENT; Schema: esquema operativo; Owner: -
--

COMMENT ON TABLE ocupacion IS 'Extension tecnica de DayNest; conserva identificadores e historial';


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
-- Name: paises; Type: TABLE; Schema: esquema operativo; Owner: -
--

CREATE TABLE paises (
    id_ps bigint CONSTRAINT pais_id_pais_not_null NOT NULL,
    nom_ps character varying(120) CONSTRAINT pais_nombre_not_null NOT NULL,
    codigo_iso2 character(2) CONSTRAINT pais_codigo_iso2_not_null NOT NULL,
    CONSTRAINT pais_codigo_iso2_check CHECK ((codigo_iso2 ~ '^[A-Z]{2}$'::text)),
    CONSTRAINT pais_nombre_check CHECK ((length(btrim((nom_ps)::text)) > 0))
);


--
-- Name: TABLE paises; Type: COMMENT; Schema: esquema operativo; Owner: -
--

COMMENT ON TABLE paises IS 'Nucleo del modelo entidad relacion del profesor';


--
-- Name: pais; Type: VIEW; Schema: esquema operativo; Owner: -
--

CREATE VIEW pais AS
 SELECT id_ps AS id_pais,
    nom_ps AS nombre,
    codigo_iso2
   FROM paises;


--
-- Name: pais_id_pais_seq; Type: SEQUENCE; Schema: esquema operativo; Owner: -
--

ALTER TABLE paises ALTER COLUMN id_ps ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME pais_id_pais_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: propietarios; Type: TABLE; Schema: esquema operativo; Owner: -
--

CREATE TABLE propietarios (
    id_p bigint CONSTRAINT propietario_id_usuario_not_null NOT NULL,
    id_usuario bigint CONSTRAINT propietario_id_usuario_not_null1 NOT NULL,
    nom_p character varying(160) CONSTRAINT propietario_nombre_completo_not_null NOT NULL,
    emai_p character varying(254) CONSTRAINT propietario_correo_normalizado_not_null NOT NULL,
    tel_p character varying(30)
);


--
-- Name: TABLE propietarios; Type: COMMENT; Schema: esquema operativo; Owner: -
--

COMMENT ON TABLE propietarios IS 'Nucleo del modelo entidad relacion del profesor';


--
-- Name: propietario; Type: VIEW; Schema: esquema operativo; Owner: -
--

CREATE VIEW propietario AS
 SELECT id_p AS id_propietario,
    id_usuario,
    nom_p AS nombre_completo,
    emai_p AS correo_normalizado,
    tel_p AS telefono
   FROM propietarios;


--
-- Name: relaciona; Type: TABLE; Schema: esquema operativo; Owner: -
--

CREATE TABLE relaciona (
    id_p bigint CONSTRAINT relaciona_id_propietario_not_null NOT NULL,
    id_b bigint CONSTRAINT relaciona_id_banco_not_null NOT NULL,
    num_cta character varying(34) CONSTRAINT relaciona_numero_cuenta_not_null NOT NULL,
    tipo_cta character varying(12) CONSTRAINT relaciona_tipo_cuenta_not_null NOT NULL,
    actualizada_en timestamp with time zone DEFAULT clock_timestamp() NOT NULL,
    CONSTRAINT relaciona_numero_cuenta_check CHECK (((num_cta)::text ~ '^[0-9]{4,34}$'::text)),
    CONSTRAINT relaciona_tipo_cuenta_check CHECK (((tipo_cta)::text = ANY (ARRAY[('AHORROS'::character varying)::text, ('CORRIENTE'::character varying)::text])))
);


--
-- Name: TABLE relaciona; Type: COMMENT; Schema: esquema operativo; Owner: -
--

COMMENT ON TABLE relaciona IS 'Nucleo del modelo entidad relacion del profesor';


--
-- Name: relaciona_operativa; Type: VIEW; Schema: esquema operativo; Owner: -
--

CREATE VIEW relaciona_operativa AS
 SELECT id_p AS id_propietario,
    id_b AS id_banco,
    num_cta AS numero_cuenta,
    tipo_cta AS tipo_cuenta,
    actualizada_en
   FROM relaciona;


--
-- Name: renta; Type: TABLE; Schema: esquema operativo; Owner: -
--

CREATE TABLE renta (
    id_ocupacion bigint CONSTRAINT reserva_id_ocupacion_not_null NOT NULL,
    id_cl bigint CONSTRAINT reserva_id_cliente_not_null NOT NULL,
    id_p bigint CONSTRAINT reserva_id_propietario_pactado_not_null NOT NULL,
    cantidad_huespedes smallint CONSTRAINT reserva_cantidad_huespedes_not_null NOT NULL,
    precio_noche_pactado numeric(16,2) CONSTRAINT reserva_precio_noche_pactado_not_null NOT NULL,
    moneda character varying(3) CONSTRAINT reserva_moneda_not_null NOT NULL,
    estado character varying(16) DEFAULT 'PENDIENTE_PAGO'::character varying CONSTRAINT reserva_estado_not_null NOT NULL,
    expira_en timestamp with time zone CONSTRAINT reserva_expira_en_not_null NOT NULL,
    creada_en timestamp with time zone DEFAULT now() CONSTRAINT reserva_creada_en_not_null NOT NULL,
    id_i bigint CONSTRAINT renta_id_inmueble_not_null NOT NULL,
    fch_e date CONSTRAINT renta_fecha_entrada_not_null NOT NULL,
    fch_s date CONSTRAINT renta_fecha_salida_not_null NOT NULL,
    num_intento bigint NOT NULL,
    porcentaje_comision numeric(5,4),
    CONSTRAINT ck_renta_estancia CHECK ((fch_s > fch_e)),
    CONSTRAINT ck_renta_intento CHECK ((num_intento > 0)),
    CONSTRAINT renta_porcentaje_comision_check CHECK (((porcentaje_comision >= (0)::numeric) AND (porcentaje_comision < (1)::numeric))),
    CONSTRAINT reserva_cantidad_huespedes_check CHECK ((cantidad_huespedes > 0)),
    CONSTRAINT reserva_estado_check CHECK (((estado)::text = ANY (ARRAY[('PENDIENTE_PAGO'::character varying)::text, ('CONFIRMADA'::character varying)::text, ('FINALIZADA'::character varying)::text, ('CANCELADA'::character varying)::text, ('EXPIRADA'::character varying)::text]))),
    CONSTRAINT reserva_moneda_check CHECK (((moneda)::text ~ '^[A-Z]{3}$'::text)),
    CONSTRAINT reserva_precio_noche_pactado_check CHECK ((precio_noche_pactado > (0)::numeric))
);


--
-- Name: TABLE renta; Type: COMMENT; Schema: esquema operativo; Owner: -
--

COMMENT ON TABLE renta IS 'Nucleo del modelo entidad relacion del profesor';


--
-- Name: renta_operativa; Type: VIEW; Schema: esquema operativo; Owner: -
--

CREATE VIEW renta_operativa AS
 SELECT id_ocupacion,
    id_cl AS id_cliente,
    id_p AS id_propietario_pactado,
    cantidad_huespedes,
    precio_noche_pactado,
    moneda,
    estado,
    expira_en,
    creada_en,
    id_i AS id_inmueble,
    fch_e AS fecha_entrada,
    fch_s AS fecha_salida
   FROM renta;


--
-- Name: reserva; Type: VIEW; Schema: esquema operativo; Owner: -
--

CREATE VIEW reserva AS
 SELECT id_ocupacion,
    id_cl AS id_cliente,
    id_p AS id_propietario_pactado,
    cantidad_huespedes,
    precio_noche_pactado,
    moneda,
    estado,
    expira_en,
    creada_en,
    id_i AS id_inmueble,
    fch_e AS fecha_entrada,
    fch_s AS fecha_salida
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

COMMENT ON TABLE usuario IS 'Extension tecnica de DayNest; conserva identificadores e historial';


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
    id_cl bigint CONSTRAINT vincula_id_cliente_not_null NOT NULL,
    id_b bigint CONSTRAINT vincula_id_banco_not_null NOT NULL,
    num_cta character varying(34) CONSTRAINT vincula_numero_cuenta_not_null NOT NULL,
    tipo_cta character varying(12) CONSTRAINT vincula_tipo_cuenta_not_null NOT NULL,
    actualizada_en timestamp with time zone DEFAULT clock_timestamp() NOT NULL,
    CONSTRAINT vincula_numero_cuenta_check CHECK (((num_cta)::text ~ '^[0-9]{4,34}$'::text)),
    CONSTRAINT vincula_tipo_cuenta_check CHECK (((tipo_cta)::text = ANY (ARRAY[('AHORROS'::character varying)::text, ('CORRIENTE'::character varying)::text])))
);


--
-- Name: TABLE vincula; Type: COMMENT; Schema: esquema operativo; Owner: -
--

COMMENT ON TABLE vincula IS 'Nucleo del modelo entidad relacion del profesor';


--
-- Name: vincula_operativa; Type: VIEW; Schema: esquema operativo; Owner: -
--

CREATE VIEW vincula_operativa AS
 SELECT id_cl AS id_cliente,
    id_b AS id_banco,
    num_cta AS numero_cuenta,
    tipo_cta AS tipo_cuenta,
    actualizada_en
   FROM vincula;


--
-- Name: administrador administrador_pkey; Type: CONSTRAINT; Schema: esquema operativo; Owner: -
--

ALTER TABLE ONLY administrador
    ADD CONSTRAINT administrador_pkey PRIMARY KEY (id_usuario);


--
-- Name: asocia asocia_pkey; Type: CONSTRAINT; Schema: esquema operativo; Owner: -
--

ALTER TABLE ONLY asocia
    ADD CONSTRAINT asocia_pkey PRIMARY KEY (id_i);


--
-- Name: banco banco_pkey; Type: CONSTRAINT; Schema: esquema operativo; Owner: -
--

ALTER TABLE ONLY banco
    ADD CONSTRAINT banco_pkey PRIMARY KEY (id_b);


--
-- Name: bloqueo bloqueo_pkey; Type: CONSTRAINT; Schema: esquema operativo; Owner: -
--

ALTER TABLE ONLY bloqueo
    ADD CONSTRAINT bloqueo_pkey PRIMARY KEY (id_ocupacion);


--
-- Name: ciudades ciudad_pkey; Type: CONSTRAINT; Schema: esquema operativo; Owner: -
--

ALTER TABLE ONLY ciudades
    ADD CONSTRAINT ciudad_pkey PRIMARY KEY (id_cd);


--
-- Name: clientes cliente_id_usuario_key; Type: CONSTRAINT; Schema: esquema operativo; Owner: -
--

ALTER TABLE ONLY clientes
    ADD CONSTRAINT cliente_id_usuario_key UNIQUE (id_usuario);


--
-- Name: clientes cliente_pkey; Type: CONSTRAINT; Schema: esquema operativo; Owner: -
--

ALTER TABLE ONLY clientes
    ADD CONSTRAINT cliente_pkey PRIMARY KEY (id_cl);


--
-- Name: configuracion_comision configuracion_comision_pkey; Type: CONSTRAINT; Schema: esquema operativo; Owner: -
--

ALTER TABLE ONLY configuracion_comision
    ADD CONSTRAINT configuracion_comision_pkey PRIMARY KEY (id_configuracion);


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
-- Name: inmuebles inmueble_pkey; Type: CONSTRAINT; Schema: esquema operativo; Owner: -
--

ALTER TABLE ONLY inmuebles
    ADD CONSTRAINT inmueble_pkey PRIMARY KEY (id_i);


--
-- Name: liquidacion_giro liquidacion_giro_pkey; Type: CONSTRAINT; Schema: esquema operativo; Owner: -
--

ALTER TABLE ONLY liquidacion_giro
    ADD CONSTRAINT liquidacion_giro_pkey PRIMARY KEY (id_ocupacion);


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
-- Name: paises pais_codigo_iso2_key; Type: CONSTRAINT; Schema: esquema operativo; Owner: -
--

ALTER TABLE ONLY paises
    ADD CONSTRAINT pais_codigo_iso2_key UNIQUE (codigo_iso2);


--
-- Name: paises pais_pkey; Type: CONSTRAINT; Schema: esquema operativo; Owner: -
--

ALTER TABLE ONLY paises
    ADD CONSTRAINT pais_pkey PRIMARY KEY (id_ps);


--
-- Name: propietarios propietario_id_usuario_key; Type: CONSTRAINT; Schema: esquema operativo; Owner: -
--

ALTER TABLE ONLY propietarios
    ADD CONSTRAINT propietario_id_usuario_key UNIQUE (id_usuario);


--
-- Name: propietarios propietario_pkey; Type: CONSTRAINT; Schema: esquema operativo; Owner: -
--

ALTER TABLE ONLY propietarios
    ADD CONSTRAINT propietario_pkey PRIMARY KEY (id_p);


--
-- Name: relaciona relaciona_pkey; Type: CONSTRAINT; Schema: esquema operativo; Owner: -
--

ALTER TABLE ONLY relaciona
    ADD CONSTRAINT relaciona_pkey PRIMARY KEY (id_p);


--
-- Name: renta renta_pkey; Type: CONSTRAINT; Schema: esquema operativo; Owner: -
--

ALTER TABLE ONLY renta
    ADD CONSTRAINT renta_pkey PRIMARY KEY (id_cl, id_i, fch_e, num_intento);


--
-- Name: banco uq_banco_pais_nombre; Type: CONSTRAINT; Schema: esquema operativo; Owner: -
--

ALTER TABLE ONLY banco
    ADD CONSTRAINT uq_banco_pais_nombre UNIQUE (id_ps, nom_b);


--
-- Name: ciudades uq_ciudad_codigo; Type: CONSTRAINT; Schema: esquema operativo; Owner: -
--

ALTER TABLE ONLY ciudades
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
-- Name: renta uq_renta_identificador; Type: CONSTRAINT; Schema: esquema operativo; Owner: -
--

ALTER TABLE ONLY renta
    ADD CONSTRAINT uq_renta_identificador UNIQUE (id_ocupacion);


--
-- Name: usuario usuario_pkey; Type: CONSTRAINT; Schema: esquema operativo; Owner: -
--

ALTER TABLE ONLY usuario
    ADD CONSTRAINT usuario_pkey PRIMARY KEY (id_usuario);


--
-- Name: vincula vincula_pkey; Type: CONSTRAINT; Schema: esquema operativo; Owner: -
--

ALTER TABLE ONLY vincula
    ADD CONSTRAINT vincula_pkey PRIMARY KEY (id_cl);


--
-- Name: ix_asocia_propietario; Type: INDEX; Schema: esquema operativo; Owner: -
--

CREATE INDEX ix_asocia_propietario ON asocia USING btree (id_p);


--
-- Name: ix_ciudad_division; Type: INDEX; Schema: esquema operativo; Owner: -
--

CREATE INDEX ix_ciudad_division ON ciudades USING btree (id_division);


--
-- Name: ix_division_pais; Type: INDEX; Schema: esquema operativo; Owner: -
--

CREATE INDEX ix_division_pais ON division_administrativa USING btree (id_pais);


--
-- Name: ix_inmueble_ciudad_estado; Type: INDEX; Schema: esquema operativo; Owner: -
--

CREATE INDEX ix_inmueble_ciudad_estado ON inmuebles USING btree (id_cd, estado);


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

CREATE INDEX ix_reserva_cliente ON renta USING btree (id_cl);


--
-- Name: ix_reserva_propietario_historico; Type: INDEX; Schema: esquema operativo; Owner: -
--

CREATE INDEX ix_reserva_propietario_historico ON renta USING btree (id_p);


--
-- Name: ux_giro_unico_activo_o_ejecutado; Type: INDEX; Schema: esquema operativo; Owner: -
--

CREATE UNIQUE INDEX ux_giro_unico_activo_o_ejecutado ON giro USING btree (id_reserva) WHERE ((estado)::text = ANY (ARRAY[('PENDIENTE'::character varying)::text, ('PROCESANDO'::character varying)::text, ('EJECUTADO'::character varying)::text]));


--
-- Name: ux_usuario_correo_normalizado; Type: INDEX; Schema: esquema operativo; Owner: -
--

CREATE UNIQUE INDEX ux_usuario_correo_normalizado ON usuario USING btree (correo_normalizado);


--
-- Name: renta aa_completar_renta; Type: TRIGGER; Schema: esquema operativo; Owner: -
--

CREATE TRIGGER aa_completar_renta BEFORE INSERT OR UPDATE ON renta FOR EACH ROW EXECUTE FUNCTION completar_renta_calendario();


--
-- Name: renta ab_asignar_intento; Type: TRIGGER; Schema: esquema operativo; Owner: -
--

CREATE TRIGGER ab_asignar_intento BEFORE INSERT OR UPDATE ON renta FOR EACH ROW EXECUTE FUNCTION asignar_intento_renta();


--
-- Name: asocia ck_asocia_inmueble; Type: TRIGGER; Schema: esquema operativo; Owner: -
--

CREATE CONSTRAINT TRIGGER ck_asocia_inmueble AFTER INSERT OR DELETE OR UPDATE ON asocia DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION verificar_asocia();


--
-- Name: bloqueo ck_bloqueo_subtipo; Type: TRIGGER; Schema: esquema operativo; Owner: -
--

CREATE CONSTRAINT TRIGGER ck_bloqueo_subtipo AFTER INSERT OR DELETE OR UPDATE ON bloqueo DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION verificar_subtipo_ocupacion();


--
-- Name: inmuebles ck_inmueble_asocia; Type: TRIGGER; Schema: esquema operativo; Owner: -
--

CREATE CONSTRAINT TRIGGER ck_inmueble_asocia AFTER INSERT OR UPDATE ON inmuebles DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION verificar_asocia();


--
-- Name: ocupacion ck_ocupacion_subtipo; Type: TRIGGER; Schema: esquema operativo; Owner: -
--

CREATE CONSTRAINT TRIGGER ck_ocupacion_subtipo AFTER INSERT OR DELETE OR UPDATE ON ocupacion DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION verificar_subtipo_ocupacion();


--
-- Name: renta ck_reserva_subtipo; Type: TRIGGER; Schema: esquema operativo; Owner: -
--

CREATE CONSTRAINT TRIGGER ck_reserva_subtipo AFTER INSERT OR DELETE OR UPDATE ON renta DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION verificar_subtipo_ocupacion();


--
-- Name: renta comision_historica; Type: TRIGGER; Schema: esquema operativo; Owner: -
--

CREATE TRIGGER comision_historica BEFORE UPDATE OF porcentaje_comision ON renta FOR EACH ROW EXECUTE FUNCTION proteger_comision_renta();


--
-- Name: ciudades completar_ciudad; Type: TRIGGER; Schema: esquema operativo; Owner: -
--

CREATE TRIGGER completar_ciudad BEFORE INSERT OR UPDATE ON ciudades FOR EACH ROW EXECUTE FUNCTION completar_pais_ciudad();


--
-- Name: clientes completar_cliente; Type: TRIGGER; Schema: esquema operativo; Owner: -
--

CREATE TRIGGER completar_cliente BEFORE INSERT OR UPDATE ON clientes FOR EACH ROW EXECUTE FUNCTION completar_cliente_nomenclatura();


--
-- Name: propietarios completar_propietario; Type: TRIGGER; Schema: esquema operativo; Owner: -
--

CREATE TRIGGER completar_propietario BEFORE INSERT OR UPDATE ON propietarios FOR EACH ROW EXECUTE FUNCTION completar_propietario_nomenclatura();


--
-- Name: configuracion_comision configuracion_inmutable; Type: TRIGGER; Schema: esquema operativo; Owner: -
--

CREATE TRIGGER configuracion_inmutable BEFORE DELETE OR UPDATE ON configuracion_comision FOR EACH ROW EXECUTE FUNCTION proteger_configuracion_comision();


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
-- Name: movimiento_pago fijar_comision; Type: TRIGGER; Schema: esquema operativo; Owner: -
--

CREATE TRIGGER fijar_comision AFTER INSERT OR UPDATE OF estado ON movimiento_pago FOR EACH ROW EXECUTE FUNCTION fijar_comision_cobro();


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
-- Name: inmuebles tg_proteger_capacidad; Type: TRIGGER; Schema: esquema operativo; Owner: -
--

CREATE TRIGGER tg_proteger_capacidad BEFORE UPDATE OF capacd ON inmuebles FOR EACH ROW EXECUTE FUNCTION proteger_capacidad_inmueble();


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
    ADD CONSTRAINT asocia_id_inmueble_fkey FOREIGN KEY (id_i) REFERENCES inmuebles(id_i) ON DELETE CASCADE;


--
-- Name: asocia asocia_id_propietario_fkey; Type: FK CONSTRAINT; Schema: esquema operativo; Owner: -
--

ALTER TABLE ONLY asocia
    ADD CONSTRAINT asocia_id_propietario_fkey FOREIGN KEY (id_p) REFERENCES propietarios(id_p) ON DELETE RESTRICT;


--
-- Name: banco banco_id_pais_fkey; Type: FK CONSTRAINT; Schema: esquema operativo; Owner: -
--

ALTER TABLE ONLY banco
    ADD CONSTRAINT banco_id_pais_fkey FOREIGN KEY (id_ps) REFERENCES paises(id_ps) ON DELETE RESTRICT;


--
-- Name: bloqueo bloqueo_id_ocupacion_fkey; Type: FK CONSTRAINT; Schema: esquema operativo; Owner: -
--

ALTER TABLE ONLY bloqueo
    ADD CONSTRAINT bloqueo_id_ocupacion_fkey FOREIGN KEY (id_ocupacion) REFERENCES ocupacion(id_ocupacion) ON DELETE RESTRICT;


--
-- Name: bloqueo bloqueo_id_propietario_solicitante_fkey; Type: FK CONSTRAINT; Schema: esquema operativo; Owner: -
--

ALTER TABLE ONLY bloqueo
    ADD CONSTRAINT bloqueo_id_propietario_solicitante_fkey FOREIGN KEY (id_propietario_solicitante) REFERENCES propietarios(id_p) ON DELETE RESTRICT;


--
-- Name: ciudades ciudad_id_division_fkey; Type: FK CONSTRAINT; Schema: esquema operativo; Owner: -
--

ALTER TABLE ONLY ciudades
    ADD CONSTRAINT ciudad_id_division_fkey FOREIGN KEY (id_division) REFERENCES division_administrativa(id_division) ON DELETE RESTRICT;


--
-- Name: ciudades ciudad_id_pais_fkey; Type: FK CONSTRAINT; Schema: esquema operativo; Owner: -
--

ALTER TABLE ONLY ciudades
    ADD CONSTRAINT ciudad_id_pais_fkey FOREIGN KEY (id_ps) REFERENCES paises(id_ps) ON DELETE RESTRICT;


--
-- Name: clientes cliente_id_ciudad_fkey; Type: FK CONSTRAINT; Schema: esquema operativo; Owner: -
--

ALTER TABLE ONLY clientes
    ADD CONSTRAINT cliente_id_ciudad_fkey FOREIGN KEY (id_cd) REFERENCES ciudades(id_cd) ON DELETE RESTRICT;


--
-- Name: clientes cliente_id_usuario_fkey; Type: FK CONSTRAINT; Schema: esquema operativo; Owner: -
--

ALTER TABLE ONLY clientes
    ADD CONSTRAINT cliente_id_usuario_fkey FOREIGN KEY (id_cl) REFERENCES usuario(id_usuario) ON DELETE RESTRICT;


--
-- Name: clientes cliente_id_usuario_fkey1; Type: FK CONSTRAINT; Schema: esquema operativo; Owner: -
--

ALTER TABLE ONLY clientes
    ADD CONSTRAINT cliente_id_usuario_fkey1 FOREIGN KEY (id_usuario) REFERENCES usuario(id_usuario) ON DELETE RESTRICT;


--
-- Name: configuracion_comision configuracion_comision_id_usuario_fkey; Type: FK CONSTRAINT; Schema: esquema operativo; Owner: -
--

ALTER TABLE ONLY configuracion_comision
    ADD CONSTRAINT configuracion_comision_id_usuario_fkey FOREIGN KEY (id_usuario) REFERENCES administrador(id_usuario) ON DELETE RESTRICT;


--
-- Name: division_administrativa division_administrativa_id_pais_fkey; Type: FK CONSTRAINT; Schema: esquema operativo; Owner: -
--

ALTER TABLE ONLY division_administrativa
    ADD CONSTRAINT division_administrativa_id_pais_fkey FOREIGN KEY (id_pais) REFERENCES paises(id_ps) ON DELETE RESTRICT;


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
-- Name: inmuebles inmueble_id_ciudad_fkey; Type: FK CONSTRAINT; Schema: esquema operativo; Owner: -
--

ALTER TABLE ONLY inmuebles
    ADD CONSTRAINT inmueble_id_ciudad_fkey FOREIGN KEY (id_cd) REFERENCES ciudades(id_cd) ON DELETE RESTRICT;


--
-- Name: liquidacion_giro liquidacion_giro_id_giro_fkey; Type: FK CONSTRAINT; Schema: esquema operativo; Owner: -
--

ALTER TABLE ONLY liquidacion_giro
    ADD CONSTRAINT liquidacion_giro_id_giro_fkey FOREIGN KEY (id_giro) REFERENCES giro(id_giro) ON DELETE RESTRICT;


--
-- Name: liquidacion_giro liquidacion_giro_id_ocupacion_fkey; Type: FK CONSTRAINT; Schema: esquema operativo; Owner: -
--

ALTER TABLE ONLY liquidacion_giro
    ADD CONSTRAINT liquidacion_giro_id_ocupacion_fkey FOREIGN KEY (id_ocupacion) REFERENCES renta(id_ocupacion) ON DELETE RESTRICT;


--
-- Name: movimiento_pago movimiento_pago_id_reserva_fkey; Type: FK CONSTRAINT; Schema: esquema operativo; Owner: -
--

ALTER TABLE ONLY movimiento_pago
    ADD CONSTRAINT movimiento_pago_id_reserva_fkey FOREIGN KEY (id_reserva) REFERENCES renta(id_ocupacion) ON DELETE RESTRICT;


--
-- Name: ocupacion ocupacion_id_inmueble_fkey; Type: FK CONSTRAINT; Schema: esquema operativo; Owner: -
--

ALTER TABLE ONLY ocupacion
    ADD CONSTRAINT ocupacion_id_inmueble_fkey FOREIGN KEY (id_inmueble) REFERENCES inmuebles(id_i) ON DELETE RESTRICT;


--
-- Name: propietarios propietario_id_usuario_fkey; Type: FK CONSTRAINT; Schema: esquema operativo; Owner: -
--

ALTER TABLE ONLY propietarios
    ADD CONSTRAINT propietario_id_usuario_fkey FOREIGN KEY (id_p) REFERENCES usuario(id_usuario) ON DELETE RESTRICT;


--
-- Name: propietarios propietario_id_usuario_fkey1; Type: FK CONSTRAINT; Schema: esquema operativo; Owner: -
--

ALTER TABLE ONLY propietarios
    ADD CONSTRAINT propietario_id_usuario_fkey1 FOREIGN KEY (id_usuario) REFERENCES usuario(id_usuario) ON DELETE RESTRICT;


--
-- Name: relaciona relaciona_id_banco_fkey; Type: FK CONSTRAINT; Schema: esquema operativo; Owner: -
--

ALTER TABLE ONLY relaciona
    ADD CONSTRAINT relaciona_id_banco_fkey FOREIGN KEY (id_b) REFERENCES banco(id_b) ON DELETE RESTRICT;


--
-- Name: relaciona relaciona_id_propietario_fkey; Type: FK CONSTRAINT; Schema: esquema operativo; Owner: -
--

ALTER TABLE ONLY relaciona
    ADD CONSTRAINT relaciona_id_propietario_fkey FOREIGN KEY (id_p) REFERENCES propietarios(id_p) ON DELETE CASCADE;


--
-- Name: renta renta_id_inmueble_fkey; Type: FK CONSTRAINT; Schema: esquema operativo; Owner: -
--

ALTER TABLE ONLY renta
    ADD CONSTRAINT renta_id_inmueble_fkey FOREIGN KEY (id_i) REFERENCES inmuebles(id_i) ON DELETE RESTRICT;


--
-- Name: renta reserva_id_cliente_fkey; Type: FK CONSTRAINT; Schema: esquema operativo; Owner: -
--

ALTER TABLE ONLY renta
    ADD CONSTRAINT reserva_id_cliente_fkey FOREIGN KEY (id_cl) REFERENCES clientes(id_cl) ON DELETE RESTRICT;


--
-- Name: renta reserva_id_ocupacion_fkey; Type: FK CONSTRAINT; Schema: esquema operativo; Owner: -
--

ALTER TABLE ONLY renta
    ADD CONSTRAINT reserva_id_ocupacion_fkey FOREIGN KEY (id_ocupacion) REFERENCES ocupacion(id_ocupacion) ON DELETE RESTRICT;


--
-- Name: renta reserva_id_propietario_pactado_fkey; Type: FK CONSTRAINT; Schema: esquema operativo; Owner: -
--

ALTER TABLE ONLY renta
    ADD CONSTRAINT reserva_id_propietario_pactado_fkey FOREIGN KEY (id_p) REFERENCES propietarios(id_p) ON DELETE RESTRICT;


--
-- Name: vincula vincula_id_banco_fkey; Type: FK CONSTRAINT; Schema: esquema operativo; Owner: -
--

ALTER TABLE ONLY vincula
    ADD CONSTRAINT vincula_id_banco_fkey FOREIGN KEY (id_b) REFERENCES banco(id_b) ON DELETE RESTRICT;


--
-- Name: vincula vincula_id_cliente_fkey; Type: FK CONSTRAINT; Schema: esquema operativo; Owner: -
--

ALTER TABLE ONLY vincula
    ADD CONSTRAINT vincula_id_cliente_fkey FOREIGN KEY (id_cl) REFERENCES clientes(id_cl) ON DELETE CASCADE;


--
-- PostgreSQL database dump complete
--


SET check_function_bodies = true;

-- Una instalacion nueva comienza con la tasa acordada; no afecta bases existentes.
INSERT INTO configuracion_comision(porcentaje) VALUES(0.3000);

-- Transferencias con cuenta ocasional, sin tablas ni columnas nuevas.
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

COMMIT;
