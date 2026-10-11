-- Solo reinicio explícito de datos académicos; ejecutarlo en el esquema correcto.
BEGIN;
TRUNCATE giro,movimiento_pago,bloqueo,reserva,ocupacion,inmueble,administrador,cliente,propietario,usuario,pais RESTART IDENTITY CASCADE;
COMMIT;
