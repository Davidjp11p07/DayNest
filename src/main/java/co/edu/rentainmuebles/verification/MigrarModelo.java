package co.edu.rentainmuebles.verification;

import co.edu.rentainmuebles.config.ConexionBD;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;

/** Respaldo pg_dump y migración transaccional del esquema anterior. */
public final class MigrarModelo {
    private MigrarModelo() { }
    public static void main(String[] args) throws Exception {
        if(args.length!=1 || !"--confirmar".equals(args[0]))
            throw new IllegalArgumentException("Use --confirmar para respaldar y migrar el modelo anterior.");
        try(Connection c=ConexionBD.abrir()) {
            if(!existe(c,"usuarios")) {
                if(existe(c,"usuario")) { System.out.println("El modelo ya está migrado; no se modificaron datos."); return; }
                throw new IllegalStateException("No existe el modelo anterior. Use InicializarBD para una base nueva.");
            }
            if(existe(c,"usuario")) throw new IllegalStateException("Hay modelos mezclados. Inspeccione antes de migrar.");
            Path respaldo=respaldar();
            c.setAutoCommit(false);
            try {
                ejecutar(c,"LOCK TABLE usuarios,inmuebles,reservas,pagos,giros IN ACCESS EXCLUSIVE MODE");
                ejecutar(c,Files.readString(Path.of("sql/instalacion/schema.sql")).replaceAll("(?im)^\\s*(BEGIN|COMMIT);\\s*$",""));
                ejecutar(c,Files.readString(Path.of("sql/migraciones/migrar-modelo.sql")));
                c.commit();
                System.out.println("Modelo migrado conservando datos y credenciales. Respaldo: "+respaldo);
            } catch(Exception e) { c.rollback(); throw e; }
        }
    }
    private static boolean existe(Connection c,String tabla) throws SQLException {
        try(PreparedStatement s=c.prepareStatement("SELECT EXISTS(SELECT 1 FROM information_schema.tables WHERE table_schema=current_schema() AND table_name=?)")) {
            s.setString(1,tabla); try(ResultSet r=s.executeQuery()) { r.next(); return r.getBoolean(1); }
        }
    }
    static Path respaldar() throws Exception {
        Properties p=new Properties();
        try(InputStream in=ConexionBD.class.getClassLoader().getResourceAsStream("db.properties")) { p.load(in); }
        URI uri=URI.create(p.getProperty("db.url").substring(5));
        String ejecutable=System.getProperty("renta.pg_dump",Path.of(System.getenv("ProgramFiles"),"PostgreSQL","18","bin","pg_dump.exe").toString());
        Path carpeta=Path.of(".tools","respaldos"); Files.createDirectories(carpeta);
        Path archivo=carpeta.resolve("antes-modelo-"+LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))+".dump");
        ProcessBuilder pb=new ProcessBuilder(ejecutable,"-h",uri.getHost(),"-p",Integer.toString(uri.getPort()<0?5432:uri.getPort()),
                "-U",p.getProperty("db.usuario"),"-d",uri.getPath().substring(1),"-Fc","--file",archivo.toAbsolutePath().toString(),"--no-password");
        pb.environment().put("PGPASSWORD",p.getProperty("db.contrasena"));
        pb.redirectError(ProcessBuilder.Redirect.INHERIT); pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
        if(pb.start().waitFor()!=0) throw new IllegalStateException("No se pudo respaldar; no se migró la base.");
        return archivo;
    }
    private static void ejecutar(Connection c,String sql) throws SQLException {
        try(PreparedStatement s=c.prepareStatement(sql)) { s.execute(); }
    }
}
