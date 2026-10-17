-- Conversión desde las cinco tablas de las etapas 1–5. Ejecutar con MigrarModelo.
-- Conserva IDs, hashes, tarifas pactadas, pagos, beneficiarios y netos.
INSERT INTO usuario(id_usuario,nombre_completo,correo,clave_hash,activo,es_administrador)
 SELECT id,nombre,correo,hash_contrasena,activo,rol='ADMINISTRADOR' FROM usuarios;
INSERT INTO cliente SELECT id FROM usuarios WHERE rol='CLIENTE';
INSERT INTO propietario SELECT id FROM usuarios WHERE rol='PROPIETARIO';
INSERT INTO inmueble(id_inmueble,id_propietario,titulo,descripcion,ciudad,direccion,capacidad_maxima,tarifa_noche,estado_publicacion)
 SELECT id,propietario_id,titulo,descripcion,ciudad,direccion,capacidad,precio_por_noche,estado FROM inmuebles;
INSERT INTO reserva(id_reserva,id_cliente,id_inmueble,fecha_entrada,fecha_salida,cantidad_huespedes,tarifa_noche_pactada,estado,creada_en)
 SELECT id,cliente_id,inmueble_id,entrada,salida,huespedes,precio_por_noche,estado,fecha_creacion FROM reservas;
INSERT INTO pago(id_pago,id_reserva,monto,medio,estado,fecha,referencia_externa)
 SELECT id,reserva_id,monto,metodo,'APROBADO',fecha,referencia FROM pagos;
INSERT INTO giro(id_giro,id_reserva,id_propietario,monto,estado,fecha,referencia_externa)
 SELECT id,reserva_id,propietario_id,monto_neto,'GENERADO',fecha,referencia FROM giros;
-- Perfil mixto de demostración; no elimina ninguna cuenta existente.
INSERT INTO cliente SELECT id_usuario FROM usuario WHERE correo='propietario1@renta.test' ON CONFLICT DO NOTHING;
SELECT setval(pg_get_serial_sequence('usuario','id_usuario'),COALESCE(max(id_usuario),1),max(id_usuario) IS NOT NULL) FROM usuario;
SELECT setval(pg_get_serial_sequence('inmueble','id_inmueble'),COALESCE(max(id_inmueble),1),max(id_inmueble) IS NOT NULL) FROM inmueble;
SELECT setval(pg_get_serial_sequence('reserva','id_reserva'),COALESCE(max(id_reserva),1),max(id_reserva) IS NOT NULL) FROM reserva;
SELECT setval(pg_get_serial_sequence('pago','id_pago'),COALESCE(max(id_pago),1),max(id_pago) IS NOT NULL) FROM pago;
SELECT setval(pg_get_serial_sequence('giro','id_giro'),COALESCE(max(id_giro),1),max(id_giro) IS NOT NULL) FROM giro;
DROP TABLE giros;
DROP TABLE pagos;
DROP TABLE reservas;
DROP TABLE inmuebles;
DROP TABLE usuarios;
