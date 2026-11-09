package co.edu.rentainmuebles.verification;

import co.edu.rentainmuebles.config.ConexionBD;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.List;

/** Completa referencias interiores sin sustituir fotos existentes ni tocar reservas. */
public final class CompletarInterioresDemo {
    private CompletarInterioresDemo() { }
    private static final List<String> ARCHIVOS = List.of("interior-01.jpg", "apto-01.jpg", "apto-02.jpg");

    public static int completar(Connection c) throws Exception {
        if (!c.getAutoCommit()) throw new IllegalArgumentException("Se requiere transacción propia.");
        String esquema = c.getSchema();
        if (!"daynest".equals(esquema) && !esquema.matches("renta_(test|http)_[a-f0-9]{32}"))
            throw new IllegalArgumentException("Esquema no autorizado.");
        byte[][] imagenes = new byte[ARCHIVOS.size()][];
        for (int i = 0; i < imagenes.length; i++)
            imagenes[i] = Files.readAllBytes(Path.of("src/main/webapp/assets/img", ARCHIVOS.get(i)));
        c.setAutoCommit(false);
        int agregadas = 0;
        try (Statement s = c.createStatement()) {
            // Mismo bloqueo que la carga del propietario: serializa el límite de diez fotos.
            try (ResultSet ids = s.executeQuery("SELECT id_i FROM inmuebles ORDER BY id_i FOR UPDATE")) {
                while (ids.next()) {
                    long inmueble = ids.getLong(1);
                    int cantidad;
                    try (PreparedStatement q = c.prepareStatement("SELECT count(*),count(*) FILTER (WHERE tipo='INTERIOR') FROM inmueble_foto WHERE id_inmueble=?")) {
                        q.setLong(1, inmueble);
                        try (ResultSet r = q.executeQuery()) {
                            r.next(); cantidad = r.getInt(1);
                            if (r.getInt(2) > 0) continue;
                        }
                    }
                    if (cantidad >= 10) throw new IllegalStateException("El inmueble " + inmueble + " ya tiene diez fotos; no se reemplazó ninguna.");
                    for (int i = 0; i < Math.min(3, 10 - cantidad); i++) {
                        int indice = (int) Math.floorMod(inmueble + i, imagenes.length);
                        try (PreparedStatement q = c.prepareStatement("INSERT INTO inmueble_foto(id_inmueble,tipo,mime,descripcion,datos) VALUES(?,'INTERIOR','image/jpeg',?,?)")) {
                            q.setLong(1, inmueble);
                            q.setString(2, "Referencia interior ilustrativa · " + ARCHIVOS.get(indice));
                            q.setBytes(3, imagenes[indice]); q.executeUpdate(); agregadas++;
                        }
                    }
                }
            }
            c.commit(); return agregadas;
        } catch (Exception | Error e) { c.rollback(); throw e; }
        finally { c.setAutoCommit(true); }
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1 || !"--aplicar".equals(args[0])) throw new IllegalArgumentException("Use --aplicar.");
        try (Connection c = ConexionBD.abrir()) {
            System.out.println("Referencias interiores agregadas: " + completar(c));
            try (Statement s = c.createStatement(); ResultSet r = s.executeQuery("SELECT count(*) FROM inmuebles i WHERE NOT EXISTS(SELECT 1 FROM inmueble_foto f WHERE f.id_inmueble=i.id_i AND f.tipo='INTERIOR')")) {
                r.next(); System.out.println("Inmuebles sin interior: " + r.getInt(1));
            }
        } finally { ConexionBD.cerrar(); }
    }
}
