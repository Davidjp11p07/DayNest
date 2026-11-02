# DayNest — Tu espacio, tus días

Aplicación académica web con Java 17, Maven, Jakarta Servlet/JSP/JSTL, JDBC y PostgreSQL. Usa 10 tablas del modelo del profesor y 11 tablas auxiliares. Consulta el [modelo implementado](docs/MODELO_DEFINITIVO.md) y el [esquema relacional](docs/esquema_relacional.md).

Los pagos, devoluciones y giros son simulados. No se mueve dinero real ni se convierten monedas.

## Ejecutar en este equipo

Inicia PostgreSQL y, desde la raíz en PowerShell:

```powershell
$env:JAVA_HOME = (Get-ChildItem .tools/jdk17 -Directory | Select-Object -First 1).FullName
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
$env:MAVEN_USER_HOME = Join-Path $pwd '.tools/maven-usuario'
.\mvnw.cmd -o jetty:run
```

Abre **http://localhost:8080/DayNest/inicio**. Ctrl+C detiene Jetty. Cada puerto usa su propio directorio temporal.

Conserva las herramientas, las dependencias y los respaldos de `.tools/`. En otro equipo se requiere JDK 17 y la descarga inicial de Maven y dependencias; en ese primer arranque no uses `-o`.

## Configuración e instalación nueva

La configuración local es `src/main/resources/db.properties`, excluida de Git. Copia `db.properties.example` y completa tus datos sin publicarlos:

```properties
db.url=jdbc:postgresql://localhost:5432/renta_inmuebles
db.usuario=postgres
db.contrasena=SU_CONTRASENA_LOCAL
db.esquema=daynest
```

Para una instalación nueva, crea una base vacía y después ejecuta:

```powershell
.\mvnw.cmd -o package
java -cp "target/classes;target/lib/*" co.edu.rentainmuebles.verification.InicializarBD
java -cp "target/classes;target/lib/*" co.edu.rentainmuebles.verification.InspeccionarBD
```

El inicializador instala las 19 tablas desde `sql/instalacion/schema.sql` y rechaza un esquema existente. No ejecutes inicialización, reinicio o reversión sobre una instalación con datos. La [guía SQL](sql/README.md) distingue instalación, demostración, migraciones y mantenimiento.

Las utilidades de instalación y migración permanecen en `target/classes` para usarlas por consola. Se excluyen del archivo de despliegue `target/DayNest.war`.

## Perfiles, cuentas e inmuebles

Una persona puede registrarse como cliente, propietario o ambos y activar posteriormente el perfil complementario desde **Mis perfiles**. Cada perfil tiene datos y cuenta bancaria propios.

**Mis datos y cuentas** permite cambiar banco, número y tipo de cuenta o copiar voluntariamente los datos a ambos perfiles. Los números se guardan como texto para conservar ceros iniciales. Las cuentas pueden ser de ahorros o corrientes.

El propietario publica y actualiza sus inmuebles y gestiona bloqueos del calendario. Las ciudades se identifican con país y región para distinguir nombres repetidos. Los precios se muestran en la moneda correspondiente y las imágenes son ilustraciones locales.

## Reserva con pago inmediato

**Reservar y pagar** recoge fechas, huéspedes y cuenta bancaria registrada u ocasional. La misma transacción crea la reserva, cobra el total simulado y confirma:

- **Realizado:** reserva confirmada y fechas ocupadas.
- **Fallado:** operación revertida, sin una reserva nueva ni fechas retenidas.

No existe un paso posterior de pago ni un plazo de 30 minutos para nuevas reservas. La cuenta ocasional queda asociada al movimiento y no reemplaza la predeterminada del perfil. El historial muestra la cuenta enmascarada.

La entrada debe ser posterior a hoy. El total es noches por tarifa pactada. La reserva conserva tarifa, moneda y propietario aunque cambie la oferta. La exclusión de intervalos activos `[entrada,salida)` protege frente a solapamientos concurrentes.

Las rutas antiguas de pago no permiten nuevos cobros. Los estados pendientes, el vencimiento y las funciones de registro de pagos anteriores permanecen para compatibilidad de datos y pruebas históricas. Una nueva reserva creada por el servicio siempre termina confirmada y pagada o se revierte.

La cancelación anterior a la entrada libera fechas y devuelve los cobros mediante una simulación transaccional.

## Giros, comisiones e ingresos

El primer cobro aprobado fija la comisión histórica. **Administración → Giros** permite cambiar el porcentaje de operaciones nuevas y revisar incidencias. La instalación nueva comienza en 30 %; editarlo no modifica operaciones ya pagadas.

Cada 60 segundos el ciclo finaliza las estancias cuya salida llegó y liquida el neto al propietario pactado si hay pago completo, cuenta válida y ninguna incidencia que lo impida. Los casos no liquidables conservan motivo y pueden reintentarse sin duplicar giros. El ciclo también atiende reservas pendientes históricas.

**Comisiones e ingresos** y **Mis ingresos** muestran importes por inmueble y moneda. Los pendientes de liquidación son estimaciones de rentas pagadas; no representan reservas pendientes de pago ni un saldo bancario.

Los registros anteriores en `public` se consultan desde **Administración → Historial anterior**. Las migraciones y sus fixtures se conservan para verificar compatibilidad y recuperación.

## Usuarios de demostración

En una instalación demo nueva:

| Perfil | Correo | Contraseña |
|---|---|---|
| Administrador | admin@renta.test | Admin2026! |
| Cliente y propietario | propietario1@renta.test | Propietario2026! |
| Cliente | cliente1@renta.test | Cliente2026! |

En una instalación existente utiliza sus credenciales actuales. Los cambios locales no alteran estos ejemplos.

Los scripts de `sql/demostracion` son opcionales: no forman parte del arranque. El de cuentas de destino completa únicamente propietarios con inmuebles sin cuenta y conserva las existentes.

## Construcción y verificación

```powershell
.\mvnw.cmd -o package
```

JUnit crea esquemas UUID temporales y los retira al terminar. No reinicia la base operativa. Las pruebas cubren permisos, perfiles, cuentas, fechas, concurrencia, pago inmediato, devoluciones, ingresos y migraciones.

Los recorridos de `src/test/browser` requieren un Jetty de pruebas y un esquema HTTP UUID; comprueban el aislamiento antes de operar. Consulta [sus instrucciones](src/test/browser/README.md).

## Documentación

- [Modelo implementado](docs/MODELO_DEFINITIVO.md), [esquema relacional](docs/esquema_relacional.md) y [adaptación del ER](docs/ADAPTACION_SVG.md).
- [Guion de demostración](docs/GUION_DEMO.md) y [giros automáticos](docs/GIROS_AUTOMATICOS.md).
- [Diagramas de la implementación](docs/diagramas/README.md).
- [Casos de uso corregidos para el profesor](docs/diagramas/profesor/DIAGRAMA_CASOS_USO.md). Son una especificación académica independiente del código.
- [Créditos de imágenes](docs/diseno/CREDITOS_IMAGENES.md).
- [Auditoría y limpieza](docs/LIMPIEZA.md).

La base reducida de entrega al profesor está fuera de este proyecto; no reemplaza la base de DayNest.
