# Probar DayNest — modelo definitivo

1. Inicie PostgreSQL y Jetty siguiendo README.md. Abra **http://localhost:8080/DayNest/inicio**. No reinicie los datos.
2. Escriba **B**: aparecen Barranquilla, Bogotá y Bucaramanga. Escriba **Bogota**, seleccione Bogotá y busque: salen los alojamientos aunque falte la tilde.
3. Escriba **Cartagena**: aparecen Bolívar/Colombia y Región de Murcia/España. Seleccione cada una por separado. La española tiene un alojamiento de 95 EUR/noche. Sin selección, el nombre encuentra ambas.
4. Abra el detalle español, indique tres noches: total informativo **285 EUR**. Compruebe fechas, capacidad y precio en el formulario real.
5. Entre como cliente1@renta.test / Cliente2026!. Reserve fechas futuras libres: estado **PENDIENTE_PAGO**, las noches quedan ocupadas y tiene **30 minutos** para pagar. Un segundo cliente no puede reservarlas.
6. Pague en la simulación: el saldo se liquida y pasa a **CONFIRMADA**. No se cobra dinero real. Un segundo pago íntegro se rechaza.
7. Cancele antes de la entrada: **CANCELADA**, fechas libres y movimiento **DEVOLUCION** ligado al cobro. El historial mantiene ambos movimientos y el neto pagado queda en cero. Probar una cancelación es una operación persistente; no borre su historial.
8. Entre como propietario1@renta.test / Propietario2026!. Desde **Mis inmuebles**, abra **Bloquear / liberar fechas**. Un bloqueo impide una reserva cruzada; al liberarlo, las fechas se ofrecen nuevamente. Otro propietario no puede bloquear sus inmuebles.
9. En Publicar/Editar, la ciudad se elige con país y región; la moneda corresponde al país del catálogo. Las tarifas y fotos son datos académicos e ilustraciones.
10. Entre como admin@renta.test / Admin2026!. Revise rentas, reservas, cobros/devoluciones y giros. Los resúmenes separan COP/EUR. Abra **Historial anterior**: conserva las operaciones del modelo previo, incluida su comisión histórica, en modo consulta.
11. Para un giro nuevo, la reserva debe estar cobrada y su salida haber llegado. La tarea la finaliza; **Generar giro** ejecuta una simulación por el neto cobrado **sin comisión** y conserva al propietario pactado. Antes de la salida, el botón no se habilita. No se falsean fechas para presentar un giro; las pruebas JUnit verifican el caso completo con un fixture temporal.
12. Para vencimiento, cree una pendiente, espere 30 minutos y hasta 60 segundos adicionales: pasa a **EXPIRADA**, liberando las fechas. Las pruebas automatizadas comprueban este caso con un vencimiento corto en esquema aislado.

## Corroborar ciudades en pgAdmin

Abra Query Tool de **renta_inmuebles**, no de postgres. La app usa el esquema **daynest**; public conserva el modelo anterior.

```sql
SELECT current_database(), current_schema();

SELECT c.id_ciudad, c.nombre AS ciudad, d.nombre AS region,
       p.nombre AS pais, c.codigo_oficial, count(i.id_inmueble) AS alojamientos
FROM daynest.ciudad c
JOIN daynest.division_administrativa d USING (id_division)
JOIN daynest.pais p USING (id_pais)
LEFT JOIN daynest.inmueble i USING (id_ciudad)
GROUP BY c.id_ciudad, c.nombre, d.nombre, p.nombre, c.codigo_oficial
ORDER BY c.nombre, p.nombre;
```

Debe haber **33 filas**: 32 capitales colombianas y Cartagena España. Las 32 colombianas tienen al menos dos alojamientos nuevos. Bogotá figura en Distrito Capital; Cundinamarca no se confunde con este.

```sql
SELECT c.id_ciudad, c.nombre, d.nombre, p.codigo_iso2
FROM daynest.ciudad c
JOIN daynest.division_administrativa d USING (id_division)
JOIN daynest.pais p USING (id_pais)
WHERE c.nombre = 'Cartagena';
```

Dos filas con identificadores y países diferentes. Para usar nombres sin prefijo en esta sesión:

```sql
SET search_path TO daynest, public;
```

## Automatización sin alterar la demostración

`mvnw.cmd -o test` usa esquemas temporales y confirma reservas concurrentes, pagos, devoluciones, expiración, bloqueos e inmutabilidad. `WebHttpIT` consulta la app sin escribir. La revisión con Chrome del flujo completo se realiza en servidor/esquema temporales; su historial se elimina junto con ese esquema, sin DELETE financiero sobre la base activa.
