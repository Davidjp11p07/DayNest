# Esquema relacional del profesor

Estado al 17 de octubre de 2026: nomenclatura y extension financiera aplicadas en la base operativa: 19 tablas y 120 columnas. Claves primarias en **negrita**.

## Nucleo: diez tablas

paises = (**id_ps**, nom_ps, codigo_iso2)

ciudades = (**id_cd**, id_division, nom_cd, codigo_oficial, id_ps)

banco = (**id_b**, nom_b, id_ps)

propietarios = (**id_p**, id_usuario, nom_p, emai_p, tel_p)

inmuebles = (**id_i**, id_cd, titulo, descripcion, dir_i, tipo, capacd, moneda, estado, creado_en, es_rural, cerca_mar, area, n_hab)

clientes = (**id_cl**, id_usuario, nom_cl, emai_cl, tel_cl, id_cd)

asocia = (**id_i**, id_p, prec_nch)

vincula = (**id_cl**, id_b, num_cta, tipo_cta, actualizada_en)

relaciona = (**id_p**, id_b, num_cta, tipo_cta, actualizada_en)

renta = (**id_cl**, **id_i**, **fch_e**, **num_intento**, id_ocupacion, id_p, cantidad_huespedes, precio_noche_pactado, moneda, estado, expira_en, creada_en, fch_s, porcentaje_comision)

## Extensiones: nueve tablas

Cada extension se identifica con COMMENT ON TABLE. Conservan nombres descriptivos, incluidos papeles financieros distintos como cobro de origen, solicitante y autorizador.

administrador = (**id_usuario**)

bloqueo = (**id_ocupacion**, id_propietario_solicitante, motivo)

configuracion_comision = (**id_configuracion**, porcentaje, id_usuario, creada_en)

division_administrativa = (**id_division**, id_pais, nombre, codigo_oficial)

giro = (**id_giro**, id_reserva, id_admin_autorizador, monto, estado, clave_idempotencia, referencia_externa, creado_en, ejecutado_en, porcentaje_comision, monto_comision, cuenta_bancaria_pactada, origen_autorizacion)

liquidacion_giro = (**id_ocupacion**, estado, motivo, intentos, revisada_en, id_giro, historial)

movimiento_pago = (**id_movimiento**, id_reserva, clase, monto, estado, id_cobro_origen, clave_idempotencia, referencia_externa, creado_en, resuelto_en, cuenta_bancaria_pactada)

ocupacion = (**id_ocupacion**, id_inmueble, fecha_inicio, fecha_fin, tipo, activa, creada_en)

usuario = (**id_usuario**, nombre_completo, correo_normalizado, contrasena_hash, telefono, activo, creado_en)

## Auditoria

| Elemento | Nombre real | Estado |
|---|---|---|
| Entidades | paises, ciudades, banco, propietarios, inmuebles, clientes | Corregido y aplicado |
| Identificadores y atributos del profesor | id_ps, nom_ps, id_cd, nom_cd, id_b, nom_b, id_p, nom_p, tel_p, emai_p, id_i, dir_i, area, n_hab, capacd, id_cl, nom_cl, tel_cl, emai_cl | Corregido y aplicado |
| Relaciones | asocia, vincula, relaciona, renta | OK |
| Precio | asocia.prec_nch | Corregido y aplicado |
| Cuentas | num_cta y tipo_cta en ambas relaciones bancarias | Corregido y aplicado |
| Clave de renta | id_cl, id_i, fch_e, num_intento | Corregido y aplicado |
| Identificador historico | renta.id_ocupacion UNIQUE y FK financieras conservadas | OK |
| Nuevas extensiones financieras | configuracion_comision, liquidacion_giro | Aplicadas |

Asocia, vincula y relaciona son 1:N: un propietario por inmueble y un banco por perfil. Por eso sus claves reflejan el lado de asociacion unica. Banco-pais se materializa con banco.id_ps. Banco conserva el singular indicado expresamente en el requerimiento.

## Compatibilidad e historial

Las vistas cliente, propietario, inmueble, ciudad y pais conservan consultas anteriores. Banco y las relaciones ofrecen banco_operativa, asocia_operativa, vincula_operativa, relaciona_operativa y renta_operativa. Reserva e inmueble_operativo siguen como adaptadores. No duplican almacenamiento.

La migracion de nombres conserva IDs, filas, credenciales, calendario, reservas y finanzas. num_intento se asigna en orden historico por cliente, inmueble y entrada; los intentos nuevos se serializan y no pueden reescribirse. Permite cancelar y volver a reservar las mismas fechas conservando ambos contratos.

Nomenclatura aplicada con respaldo .tools/respaldos/antes-modelo-20261017-001157.dump. Se comprobaron las huellas originales de las 17 tablas y los 80 inmuebles; Jetty se reinicio y respondio HTTP 200.

La extension financiera se ensayó sobre copia restaurada del respaldo .tools/respaldos/antes-modelo-20261017-003441.dump. Conserva las columnas originales y los giros existentes; inicializa a 30 % los pagos antiguos sin giro vigente, segun lo confirmado por el usuario. El porcentaje de giros anteriores se mantiene.

El plan y las verificaciones de la extension estan en [GIROS_AUTOMATICOS.md](GIROS_AUTOMATICOS.md). Aplicacion operativa con respaldo `.tools/respaldos/antes-modelo-20261017-004616.dump`, conservando las columnas originales y los 80 inmuebles.
