package co.edu.rentainmuebles.verification;

import co.edu.rentainmuebles.config.ConexionBD;
import java.nio.file.*;
import java.sql.*;
import java.util.UUID;

/** Fixture HTTP aislado en el mismo PostgreSQL de db.properties; nunca modifica public. */
public final class PrepararVerificacionHttp {
    private PrepararVerificacionHttp() { }
    public static void main(String[] args) throws Exception {
        if(args.length==1 && "--crear".equals(args[0])) {
            String esquema="renta_http_"+UUID.randomUUID().toString().replace("-","");
            try(Connection c=ConexionBD.abrir()) {
                c.setAutoCommit(false);
                try {
                    ejecutar(c,"CREATE SCHEMA \""+esquema+"\""); ejecutar(c,"SET search_path TO \""+esquema+"\",public");
                    for(String archivo:new String[]{"schema.sql","data.sql"})
                        ejecutar(c,Files.readString(Path.of(archivo)).replaceAll("(?im)^\\s*(BEGIN|COMMIT);\\s*$",""));
                    c.commit();
                } catch(Exception e) { c.rollback(); throw e; }
            }
            Files.createDirectories(Path.of(".tools"));
            Files.writeString(Path.of(".tools","http-modelo-schema.txt"),esquema);
            System.out.println("Esquema temporal HTTP creado: "+esquema);
        } else if(args.length==2 && "--eliminar".equals(args[0]) && args[1].matches("renta_http_[a-f0-9]{32}")) {
            try(Connection c=ConexionBD.abrir()) { ejecutar(c,"DROP SCHEMA IF EXISTS \""+args[1]+"\" CASCADE"); }
            System.out.println("Esquema temporal HTTP eliminado.");
        } else throw new IllegalArgumentException("Use --crear o --eliminar renta_http_UUID; nunca se acepta public.");
    }
    private static void ejecutar(Connection c,String sql) throws SQLException {
        try(PreparedStatement s=c.prepareStatement(sql)) { s.execute(); }
    }
}
