package co.edu.rentainmuebles.verification;

import co.edu.rentainmuebles.util.PasswordUtil;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.regex.*;

/** Regenera exclusivamente los hashes de las ocho cuentas demo de data.sql. */
public final class GenerarHashesDatos {
    private GenerarHashesDatos() { }
    public static void main(String[] args) throws Exception {
        Path archivo = Path.of("data.sql");
        String sql = Files.readString(archivo, StandardCharsets.UTF_8);
        Pattern patron = Pattern.compile("'((?:admin|propietario[1-3]|cliente[1-4])@renta\\.test)','pbkdf2-sha256\\$[^']+'");
        Matcher matcher = patron.matcher(sql); StringBuffer salida = new StringBuffer(); int cantidad = 0;
        while (matcher.find()) {
            String correo=matcher.group(1);
            char[] password = (correo.startsWith("admin")?"Admin2026!":correo.startsWith("propietario")?"Propietario2026!":"Cliente2026!").toCharArray();
            try { matcher.appendReplacement(salida, Matcher.quoteReplacement("'"+correo+"','" + PasswordUtil.hash(password) + "'")); }
            finally { Arrays.fill(password, '\0'); }
            cantidad++;
        }
        matcher.appendTail(salida);
        if (cantidad != 8) { throw new IllegalStateException("Se esperaban ocho cuentas demo."); }
        Files.writeString(archivo, salida, StandardCharsets.UTF_8);
        System.out.println("Hashes generados con PasswordUtil: " + cantidad);
    }
}
