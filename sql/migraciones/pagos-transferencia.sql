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
