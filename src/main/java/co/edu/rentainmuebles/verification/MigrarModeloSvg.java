package co.edu.rentainmuebles.verification;

import co.edu.rentainmuebles.config.ConexionBD;
import co.edu.rentainmuebles.dao.jdbc.*;
import java.io.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;
import java.util.*;

/** Adaptación incremental sin reiniciar datos; ensayo y reversión independientes. */
public final class MigrarModeloSvg {
    private static final Path EVIDENCIA = Path.of(".tools", "ensayo-svg-aprobado.properties");
    private MigrarModeloSvg() { }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("Use --ensayar o --aplicar");
        switch (args[0]) {
            case "--ensayar" -> ensayar();
            case "--aplicar" -> aplicar();
            case "--verificar-clon" -> verificarClon();
            default -> throw new IllegalArgumentException("Use --ensayar o --aplicar");
        }
    }

    public static void migrar(Connection c) throws Exception {
        if (!c.getAutoCommit()) throw new IllegalStateException("Se requiere conexión sin transacción abierta");
        String ns = c.getSchema();
        if (!"daynest".equals(ns) && !ns.matches("renta_test_[a-f0-9]{32}")) throw new IllegalStateException("Esquema no autorizado");
        if (columna(c,"usuario","correo_normalizado")) {
            if (columna(c,"usuario","correo") || columna(c,"movimiento_pago","moneda") || columna(c,"giro","moneda")) throw new IllegalStateException("Adaptación parcial: requiere revisión");
            return;
        }
        if (!columna(c,"usuario","correo") || !columna(c,"movimiento_pago","moneda") || !columna(c,"giro","moneda")) throw new IllegalStateException("No corresponde al esquema anterior");
        c.setAutoCommit(false);
        try {
            ejecutar(c,"SET LOCAL lock_timeout='10s'");
            ejecutar(c,"LOCK TABLE usuario,reserva,movimiento_pago,giro IN ACCESS EXCLUSIVE MODE");
            Map<String,String> antes = huellas(c);
            ejecutar(c,script("sql/migraciones/migrar-svg.sql"));
            if (!antes.equals(huellas(c))) throw new IllegalStateException("La migración alteraría filas históricas; ROLLBACK");
            c.commit();
        } catch (Exception e) { c.rollback(); throw e; }
        finally { c.setAutoCommit(true); }
    }

    /** Hash de todas las filas, incluidas las del archivo public, sin imprimir datos. */
    public static Map<String,String> huellas(Connection c) throws SQLException {
        Map<String,String> out = new TreeMap<>();
        String ns = c.getSchema();
        List<String[]> tablas = new ArrayList<>();
        try (PreparedStatement s=c.prepareStatement("SELECT schemaname,tablename FROM pg_tables WHERE schemaname IN (?, 'public') ORDER BY 1,2")) {
            s.setString(1,ns);
            try(ResultSet r=s.executeQuery()) { while(r.next()) tablas.add(new String[]{r.getString(1),r.getString(2)}); }
        }
        for(String[] tabla:tablas) {
            if(!tabla[0].matches("[a-z0-9_]+") || !tabla[1].matches("[a-z0-9_]+")) throw new SQLException("Identificador inesperado");
            String j="to_jsonb(t)";
            if(tabla[0].equals(ns) && tabla[1].equals("usuario") && columna(c,"usuario","correo")) j="to_jsonb(t)-'correo'||jsonb_build_object('correo_normalizado',t.correo)";
            if(tabla[0].equals(ns) && Set.of("movimiento_pago","giro").contains(tabla[1])) j="to_jsonb(t)-'moneda'";
            String sql="SELECT count(*),md5(COALESCE(string_agg(j::text,E'\\n' ORDER BY j::text),'')) FROM (SELECT "+j+" j FROM \""+tabla[0]+"\".\""+tabla[1]+"\" t) q";
            try(Statement s=c.createStatement();ResultSet r=s.executeQuery(sql)) { r.next();out.put(tabla[0]+"."+tabla[1],r.getLong(1)+":"+r.getString(2)); }
        }
        return out;
    }

    private static boolean columna(Connection c,String tabla,String columna) throws SQLException {
        try(PreparedStatement s=c.prepareStatement("SELECT EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=current_schema() AND table_name=? AND column_name=?)")) {
            s.setString(1,tabla);s.setString(2,columna);try(ResultSet r=s.executeQuery()){r.next();return r.getBoolean(1);}
        }
    }
    private static String script(String p) throws IOException {return Files.readString(Path.of(p)).replaceAll("(?im)^\\s*(BEGIN|COMMIT);\\s*$","");}
    private static void ejecutar(Connection c,String sql) throws SQLException {try(Statement s=c.createStatement()){s.execute(sql);}}
    private static String hash(Path p) throws Exception {return HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(p)));}
    private static Properties configuracion() throws Exception {
        Properties p=new Properties();try(InputStream in=ConexionBD.class.getClassLoader().getResourceAsStream("db.properties")){p.load(in);}return p;
    }
    private static Path respaldoVerificado() throws Exception {
        Path dump=MigrarModelo.respaldar();
        Process p=new ProcessBuilder(bin("pg_restore"),"--list",dump.toAbsolutePath().toString()).redirectError(ProcessBuilder.Redirect.INHERIT).start();
        String listado=new String(p.getInputStream().readAllBytes(),StandardCharsets.UTF_8);
        if(p.waitFor()!=0 || !listado.contains("TABLE DATA daynest usuario") || !listado.contains("TABLE DATA public reserva")) throw new IllegalStateException("Respaldo incompleto o ilegible");
        Files.writeString(Path.of(dump+".list"),listado);return dump;
    }
    private static String bin(String nombre){return Path.of(System.getenv("ProgramFiles"),"PostgreSQL","18","bin",nombre+".exe").toString();}

    private static void ensayar() throws Exception {
        Path respaldo=respaldoVerificado();
        Properties cfg=configuracion();URI uri=URI.create(cfg.getProperty("db.url").substring(5));
        String db="daynest_svg_test_"+UUID.randomUUID().toString().replace("-","");
        String clonUrl="jdbc:postgresql://"+uri.getHost()+":"+(uri.getPort()<0?5432:uri.getPort())+"/"+db;
        boolean creada=false;
        try {
            try(Connection c=ConexionBD.abrir()){ejecutar(c,"CREATE DATABASE \""+db+"\"");creada=true;}
            ProcessBuilder pb=new ProcessBuilder(bin("pg_restore"),"-h",uri.getHost(),"-p",Integer.toString(uri.getPort()<0?5432:uri.getPort()),"-U",cfg.getProperty("db.usuario"),"-d",db,"--exit-on-error","--no-owner","--no-acl",respaldo.toAbsolutePath().toString());
            pb.environment().put("PGPASSWORD",cfg.getProperty("db.contrasena"));pb.redirectErrorStream(true);pb.redirectOutput(Path.of(".tools","restauracion-svg.log").toFile());
            if(pb.start().waitFor()!=0)throw new IllegalStateException("Restauración falló; revise restauracion-svg.log");
            Map<String,String> inicial;
            try(Connection c=DriverManager.getConnection(clonUrl,cfg.getProperty("db.usuario"),cfg.getProperty("db.contrasena"))) {
                ejecutar(c,"SET search_path TO daynest,public");inicial=huellas(c);migrar(c);
                if(!inicial.equals(huellas(c)))throw new IllegalStateException("Filas del clon cambiaron");
                migrar(c); // reejecución inocua
                ejecutar(c,Files.readString(Path.of("sql/mantenimiento/revertir-svg.sql")));
                if(!inicial.equals(huellas(c)))throw new IllegalStateException("Reversión no conserva filas");
                migrar(c);
            }
            Path conf=Path.of(".tools","svg-clon-config");Files.createDirectories(conf);
            cfg.setProperty("db.url",clonUrl);
            try(Writer w=Files.newBufferedWriter(conf.resolve("db.properties"))){cfg.store(w,"Clon aislado; no publicar credenciales");}
            String cp=conf.toAbsolutePath()+File.pathSeparator+Path.of("target","classes").toAbsolutePath()+File.pathSeparator+Path.of("target","lib").toAbsolutePath()+File.separator+"*";
            Process p=new ProcessBuilder(Path.of(System.getProperty("java.home"),"bin","java.exe").toString(),"-Dfile.encoding=UTF-8","-cp",cp,MigrarModeloSvg.class.getName(),"--verificar-clon").redirectErrorStream(true).redirectOutput(Path.of(".tools","dao-clon-svg.log").toFile()).start();
            if(p.waitFor()!=0)throw new IllegalStateException("DAO incompatibles con clon; revise dao-clon-svg.log");
            // JUnit compara además el DDL nuevo instalado desde cero con el migrado.
            Properties evidencia=new Properties();evidencia.setProperty("migracion.sha256",hash(Path.of("sql/migraciones/migrar-svg.sql")));evidencia.setProperty("schema.sha256",hash(Path.of("sql/instalacion/schema.sql")));evidencia.setProperty("respaldo",respaldo.toAbsolutePath().toString());
            evidencia.setProperty("resultado","restauracion,migracion,idempotencia,reversion,DAO,public-preservado");
            try(Writer w=Files.newBufferedWriter(EVIDENCIA)){evidencia.store(w,"Ensayo completo antes de aplicar");}
            Files.writeString(Path.of(".tools","huellas-svg-clon.txt"),inicial.toString());
            System.out.println("Ensayo aprobado: restauración, migración, reejecución, reversión y DAO. Respaldo: "+respaldo);
        } finally {
            if(creada) {
                if(!db.matches("daynest_svg_test_[a-f0-9]{32}"))throw new IllegalStateException("Se rechazó limpieza de base no temporal");
                try(Connection c=ConexionBD.abrir()){ejecutar(c,"DROP DATABASE \""+db+"\" WITH (FORCE)");}
            }
            Path conf=Path.of(".tools","svg-clon-config","db.properties");Files.deleteIfExists(conf);
        }
    }
    private static void verificarClon() throws Exception {
        try(Connection c=ConexionBD.abrir()) {
            if(!c.getCatalog().matches("daynest_svg_test_[a-f0-9]{32}"))throw new IllegalStateException("Verificación requiere clon");
        }
        System.out.println("Usuarios="+new UsuarioDaoJdbc().listar().size());
        System.out.println("Ciudades="+new CiudadDaoJdbc().listar().size());
        System.out.println("Inmuebles="+new InmuebleDaoJdbc().listar().size());
        System.out.println("Reservas="+new ReservaDaoJdbc().listar().size());
        System.out.println("Pagos="+new PagoDaoJdbc().listar().size());
        System.out.println("Giros="+new GiroDaoJdbc().listar().size());
        System.out.println("Archivo="+new HistorialAnteriorDaoJdbc().consultar().size());
        var propietarios=new UsuarioDaoJdbc().buscarPorRol(co.edu.rentainmuebles.model.Rol.PROPIETARIO);
        for(var u:propietarios)new BloqueoDaoJdbc().listar(u.getId());
        if(new InmuebleDaoJdbc().buscar("Bogota",null,null,null,null).isEmpty())throw new IllegalStateException("Búsqueda incompatible");
    }
    private static void aplicar() throws Exception {
        if(!Files.exists(EVIDENCIA))throw new IllegalStateException("Ejecute --ensayar primero");
        Properties evidencia=new Properties();try(Reader r=Files.newBufferedReader(EVIDENCIA)){evidencia.load(r);}
        if(!hash(Path.of("sql/migraciones/migrar-svg.sql")).equals(evidencia.getProperty("migracion.sha256")) || !hash(Path.of("sql/instalacion/schema.sql")).equals(evidencia.getProperty("schema.sha256")))throw new IllegalStateException("Scripts cambiaron después del ensayo; vuelva a ensayar");
        // Evitar mezclar clientes Java viejos y nuevos durante el cambio de columnas.
        try(java.net.Socket s=new java.net.Socket()) {
            s.connect(new java.net.InetSocketAddress("127.0.0.1",8080),500);
            throw new IllegalStateException("Detenga Jetty en 8080 antes de adaptar");
        }catch(java.net.ConnectException e){/* servidor detenido */}
        Path respaldo=respaldoVerificado();
        try(Connection c=ConexionBD.abrir()) {
            if(!"daynest".equals(c.getSchema()) || !"renta_inmuebles".equals(c.getCatalog()))throw new IllegalStateException("Configuración operativa inesperada");
            Map<String,String> antes=huellas(c);migrar(c);
            Files.writeString(Path.of(".tools","huellas-svg-operativa.txt"),"ANTES="+antes+"\nDESPUES="+huellas(c)+"\nRESPALDO="+respaldo+"\n");
        }
        System.out.println("Adaptación operativa confirmada; filas e historial public conservados. Respaldo: "+respaldo);
    }
}
