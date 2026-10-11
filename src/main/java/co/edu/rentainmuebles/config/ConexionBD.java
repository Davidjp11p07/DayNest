package co.edu.rentainmuebles.config;

import co.edu.rentainmuebles.dao.DaoException;
import java.io.InputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/** Cada llamada entrega una conexión nueva, que debe cerrarse con try-with-resources. */
public final class ConexionBD {
    private ConexionBD() { }
    public static Connection abrir() {
        Properties propiedades = new Properties();
        try (InputStream entrada = ConexionBD.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (entrada == null) { throw new DaoException("No se encontró db.properties en el classpath."); }
            propiedades.load(entrada);
            Connection conexion = DriverManager.getConnection(requerida(propiedades, "db.url"),
                    requerida(propiedades, "db.usuario"), requerida(propiedades, "db.contrasena"));
            // JUnit aísla sus tablas mediante una propiedad de JVM, sin cambiar credenciales.
            String esquema = System.getProperty("renta.db.schema",propiedades.getProperty("db.esquema","public"));
            try {
                if (esquema != null) {
                    if (!esquema.matches("[a-z][a-z0-9_]{0,62}")) { throw new DaoException("Nombre de esquema de pruebas inválido."); }
                    try(java.sql.Statement s=conexion.createStatement()){s.execute("SET search_path TO \""+esquema+"\",public");}
                }
                try(java.sql.Statement s=conexion.createStatement()){s.execute("SET TIME ZONE 'America/Bogota'");}
                return conexion;
            } catch (SQLException | RuntimeException e) {
                try { conexion.close(); } catch (SQLException cierre) { e.addSuppressed(cierre); }
                throw e;
            }
        } catch (IOException | SQLException e) {
            throw new DaoException("No se pudo abrir la conexión PostgreSQL; revise db.properties y el servidor.", e);
        }
    }
    private static String requerida(Properties p, String clave) {
        String valor = p.getProperty(clave);
        if (valor == null || valor.isBlank()) { throw new DaoException("Falta la propiedad " + clave); }
        return valor;
    }
}
