package co.edu.rentainmuebles.verification;

import co.edu.rentainmuebles.config.ConexionBD;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;

/** Reinicio explícito; reset y carga se confirman juntos, o se revierten juntos. */
public final class ReiniciarDatos {
    private ReiniciarDatos() { }
    public static void main(String[] args) throws Exception {
        if (args.length != 1 || !"--confirmar".equals(args[0])) {
            throw new IllegalArgumentException("Este comando borra todos los datos. Use --confirmar para reiniciar la demostración.");
        }
        try (Connection c = ConexionBD.abrir()) {
            c.setAutoCommit(false);
            try {
                ejecutar(c,"reset.sql"); ejecutar(c,"data.sql"); ejecutar(c,"admin-demo.sql"); c.commit();
                System.out.println("Demostración reiniciada: reset.sql y data.sql confirmados.");
            } catch (Exception e) { c.rollback(); throw e; }
        }
    }
    private static void ejecutar(Connection c, String nombre) throws Exception {
        String sql = Files.readString(Path.of(nombre),StandardCharsets.UTF_8).replaceAll("(?im)^\\s*(BEGIN|COMMIT);\\s*$","");
        try (PreparedStatement s = c.prepareStatement(sql)) { s.execute(); }
    }
}
