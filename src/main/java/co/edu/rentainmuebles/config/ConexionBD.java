package co.edu.rentainmuebles.config;

import co.edu.rentainmuebles.dao.DaoException;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.io.*;
import java.sql.*;
import java.util.Properties;

/** Pool compartido; close() devuelve la conexion. El esquema se fija en cada prestamo. */
public final class ConexionBD {
    private static HikariDataSource pool;
    private static String esquemaBase;
    private static String zonaHoraria;
    private ConexionBD() { }

    private static synchronized HikariDataSource fuente() {
        if (pool != null && !pool.isClosed()) return pool;
        Properties p = new Properties();
        try (InputStream entrada = ConexionBD.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (entrada == null) throw new DaoException("No se encontro db.properties en el classpath.");
            p.load(entrada);
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(requerida(p,"db.url"));
            config.setUsername(requerida(p,"db.usuario"));
            config.setPassword(requerida(p,"db.contrasena"));
            config.setMaximumPoolSize(Integer.parseInt(p.getProperty("db.pool.maximo","10")));
            config.setMinimumIdle(Integer.parseInt(p.getProperty("db.pool.minimo","1")));
            config.setConnectionTimeout(Long.parseLong(p.getProperty("db.pool.esperaMs","10000")));
            config.setPoolName("DayNest");
            config.addDataSourceProperty("tcpKeepAlive","true");
            esquemaBase = p.getProperty("db.esquema","public");
            Properties negocio=new Properties();
            try(InputStream n=ConexionBD.class.getClassLoader().getResourceAsStream("negocio.properties")) {if(n==null)throw new IOException("Falta negocio.properties");negocio.load(n);}
            zonaHoraria=java.time.ZoneId.of(negocio.getProperty("negocio.zonaHoraria")).getId();
            pool = new HikariDataSource(config);
            return pool;
        } catch (IOException | IllegalArgumentException e) {
            throw new DaoException("Revise db.properties y la configuracion del pool.",e);
        }
    }

    public static Connection abrir() {
        HikariDataSource fuente = fuente();
        String esquema = System.getProperty("renta.db.schema",esquemaBase);
        if (!esquema.matches("[a-z][a-z0-9_]{0,62}")) throw new DaoException("Nombre de esquema invalido.");
        try {
            Connection c = fuente.getConnection();
            try (Statement s = c.createStatement()) {
                s.execute("SET search_path TO \""+esquema+"\",public");
                try(PreparedStatement z=c.prepareStatement("SELECT set_config('TimeZone',?,false)")){z.setString(1,zonaHoraria);z.executeQuery().close();}
                return c;
            } catch (SQLException | RuntimeException e) {
                try { c.close(); } catch (SQLException cierre) { e.addSuppressed(cierre); }
                throw e;
            }
        } catch (SQLException e) {
            throw new DaoException("No se pudo obtener una conexion PostgreSQL.",e);
        }
    }

    public static synchronized void cerrar() {
        if (pool != null) { pool.close(); pool = null; }
    }

    private static String requerida(Properties p, String clave) {
        String valor = p.getProperty(clave);
        if (valor == null || valor.isBlank()) throw new DaoException("Falta la propiedad " + clave);
        return valor;
    }
}
