# Correcciones en la gestión de fotos de inmuebles

## Eliminación desde la ficha equivocada

Un propietario con dos inmuebles podía enviar el formulario de uno con el identificador de una foto del otro. El servidor verificaba quién era el dueño y eliminaba la foto aunque perteneciera a la otra ficha.

La eliminación ahora comprueba el propietario, el inmueble abierto y la foto en la misma operación de base de datos. Si los identificadores no coinciden, el propietario recibe un aviso y la foto se conserva. La prueba de integración en `ContenidoInmuebleTest` cubre dos inmuebles del mismo propietario, el intento de otro usuario y la eliminación desde la ficha correcta.

## Error al subir una imagen con la cabecera dañada

Un archivo podía empezar con la firma de un PNG y terminar antes de indicar sus dimensiones. El lector reconocía el formato, pero al consultar el tamaño lanzaba una excepción de entrada y salida que escapaba del formulario.

El formulario ahora muestra «No se pudo leer la imagen seleccionada» y permite elegir otro archivo. La validación ocurre antes de guardar la foto. `FotoInmuebleTest` cubre el PNG incompleto y una foto JPEG válida.
