-- Solo reinicio explícito de datos académicos; ejecutarlo en el esquema correcto.
BEGIN;
TRUNCATE giro,movimiento_pago,bloqueo,renta,ocupacion,inmuebles,administrador,clientes,propietarios,usuario,paises RESTART IDENTITY CASCADE;
COMMIT;
