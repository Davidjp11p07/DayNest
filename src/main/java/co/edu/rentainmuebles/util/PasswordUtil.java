package co.edu.rentainmuebles.util;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/** Formato: pbkdf2-sha256$600000$salBase64$hashBase64. Sin dependencias externas. */
public final class PasswordUtil {
    private static final int ITERACIONES = 600_000;
    private static final SecureRandom RANDOM = new SecureRandom();
    private PasswordUtil() { }
    public static String hash(char[] password) {
        if (password == null || password.length == 0) { throw new IllegalArgumentException("Contraseña obligatoria."); }
        byte[] sal = new byte[16]; RANDOM.nextBytes(sal);
        return "pbkdf2-sha256$" + ITERACIONES + "$" + Base64.getEncoder().encodeToString(sal)
                + "$" + Base64.getEncoder().encodeToString(derivar(password, sal, ITERACIONES));
    }
    public static boolean verificar(char[] password, String hash) {
        if (password == null || hash == null) { return false; }
        try {
            String[] partes = hash.split("\\$", -1);
            if (partes.length != 4 || !partes[0].equals("pbkdf2-sha256")) { return false; }
            int iteraciones = Integer.parseInt(partes[1]);
            if (iteraciones < ITERACIONES || iteraciones > 2_000_000) { return false; }
            byte[] sal = Base64.getDecoder().decode(partes[2]);
            byte[] esperado = Base64.getDecoder().decode(partes[3]);
            if (sal.length != 16 || esperado.length != 32) { return false; }
            byte[] actual = derivar(password, sal, iteraciones);
            try { return MessageDigest.isEqual(esperado, actual); }
            finally { Arrays.fill(actual, (byte) 0); }
        } catch (IllegalArgumentException e) { return false; }
    }
    private static byte[] derivar(char[] password, byte[] sal, int iteraciones) {
        PBEKeySpec spec = new PBEKeySpec(password, sal, iteraciones, 256);
        try { return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded(); }
        catch (GeneralSecurityException e) { throw new IllegalStateException("PBKDF2 no disponible.", e); }
        finally { spec.clearPassword(); }
    }
    /** Generación manual: java ... PasswordUtil; solicita la contraseña sin mostrarla. */
    public static void main(String[] args) {
        if (System.console() == null) { throw new IllegalStateException("Ejecute desde una consola interactiva."); }
        char[] password = System.console().readPassword("Contraseña: ");
        try { System.out.println(hash(password)); } finally { Arrays.fill(password, '\0'); }
    }
}
