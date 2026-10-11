package co.edu.rentainmuebles.service;

import java.math.BigDecimal;

/** Estrategia intercambiable; el resultado conserva el porcentaje histórico. */
public interface PoliticaComision {
    Desglose calcular(BigDecimal total);
    record Desglose(BigDecimal porcentaje, BigDecimal comision, BigDecimal neto) { }
}
