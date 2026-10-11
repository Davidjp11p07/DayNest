package co.edu.rentainmuebles.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Validaciones estructurales. Las reglas de negocio se implementarán en los servicios. */
final class ValidacionModelo {
    private ValidacionModelo() { }

    static Long identificador(Long id) {
        if (id != null && id <= 0) {
            throw new IllegalArgumentException("El identificador debe ser positivo.");
        }
        return id;
    }

    static String texto(String valor, String campo, int longitudMaxima) {
        if (valor == null || valor.isBlank() || valor.trim().length() > longitudMaxima) {
            throw new IllegalArgumentException(campo + " es obligatorio y admite hasta " + longitudMaxima + " caracteres.");
        }
        return valor.trim();
    }

    static BigDecimal dineroPositivo(BigDecimal valor, String campo) {
        if (valor == null || valor.signum() <= 0 || valor.compareTo(new BigDecimal("99999999999999.99")) > 0) {
            throw new IllegalArgumentException(campo + " debe ser positivo y caber en NUMERIC(16,2).");
        }
        try {
            return valor.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException excepcion) {
            throw new IllegalArgumentException(campo + " admite como máximo dos decimales.", excepcion);
        }
    }
}
