-- Solo inicializacion explicita: nunca ejecutar como recarga de datos.
DO $$ BEGIN
 IF EXISTS(SELECT 1 FROM administrador) THEN RAISE EXCEPTION 'Ya existe un administrador; no se crea cuenta demo'; END IF;
INSERT INTO usuario(nombre_completo,correo_normalizado,contrasena_hash) VALUES ('Ana Administración','admin@renta.test','pbkdf2-sha256$600000$mrYgkjw9i6tYTNfrgNvPuw==$q01eUpoj7Gyyfq6gGUbNysnNPMRQVvq4W7lfpASHZfE=') ON CONFLICT DO NOTHING;
INSERT INTO administrador SELECT id_usuario FROM usuario WHERE correo_normalizado='admin@renta.test' ON CONFLICT DO NOTHING;
END; $$;
