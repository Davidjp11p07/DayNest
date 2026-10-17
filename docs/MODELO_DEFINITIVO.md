# Modelo del profesor y extensiones de DayNest

El nucleo corresponde a las seis entidades del PDF del profesor: propietarios, inmuebles, ciudades, clientes, banco y paises. Asocia almacena el precio por noche; vincula y relaciona almacenan banco, numero y tipo de cuenta por cada perfil; renta conserva cliente, inmueble y estancia.

La nomenclatura y la clave de renta (id_cl, id_i, fch_e, num_intento) ya estan aplicadas. Los identificadores anteriores permanecen como claves unicas para conservar contratos y referencias financieras. El esquema completo, con PK marcadas, esta en [esquema_relacional.md](esquema_relacional.md).

Cliente y propietario son entidades separadas con acceso tecnico compartido en usuario. Una persona puede activar ambos perfiles y copiar voluntariamente su banco, numero y tipo de cuenta a ambos. Despues puede modificarlos por separado. Los numeros son texto para conservar ceros iniciales. No se inventan cuentas para usuarios migrados.

El precio se almacena en asocia y las rentas conservan su precio y propietario pactados. Area y habitaciones son opcionales cuando se desconocen. Ciudad y banco tienen FK a pais; inmueble y cliente tienen FK a ciudad. Las regiones administrativas distinguen ciudades homonimas.

La version actual suma nueve extensiones tecnicas: usuario, administrador, division_administrativa, ocupacion, bloqueo, movimiento_pago, giro, configuracion_comision y liquidacion_giro. Son 19 tablas; las extensiones financieras estan activas en la base operativa. Las vistas de compatibilidad no duplican datos.

Ocupacion conserva el calendario y la exclusion de intervalos activos. Una ocupacion pertenece exactamente a renta o bloqueo, con validacion diferida. La salida no ocupa noches; las pendientes expiran a los 30 minutos.

La extension financiera fija la comision al primer cobro aprobado y liquida automaticamente al finalizar la estancia. El giro conserva beneficiario pactado, tasa y cuenta al ejecutar; el sistema registra incidencias y reintentos. El origen SISTEMA es distinto de la autorizacion humana historica. Todo es una simulacion academica, sin bancos reales. Consulta [el informe tecnico y plan de activacion](GIROS_AUTOMATICOS.md).
