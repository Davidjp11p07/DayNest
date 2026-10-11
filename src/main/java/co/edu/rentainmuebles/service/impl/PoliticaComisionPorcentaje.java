package co.edu.rentainmuebles.service.impl;

import co.edu.rentainmuebles.service.*;
import java.math.*;

public final class PoliticaComisionPorcentaje implements PoliticaComision {
    private final BigDecimal porcentaje;
    public PoliticaComisionPorcentaje(BigDecimal porcentaje) {
        if (porcentaje == null || porcentaje.signum() < 0 || porcentaje.compareTo(BigDecimal.ONE) >= 0) {
            throw new NegocioException("La comisión debe estar entre 0 (inclusive) y 1 (exclusive).");
        }
        try { this.porcentaje = porcentaje.setScale(4,RoundingMode.UNNECESSARY); }
        catch (ArithmeticException e) { throw new NegocioException("La comisión admite como máximo cuatro decimales.",e); }
    }
    @Override public Desglose calcular(BigDecimal total) {
        if (total == null || total.signum() <= 0) { throw new NegocioException("El total debe ser positivo."); }
        BigDecimal comision = total.multiply(porcentaje).setScale(2,RoundingMode.HALF_UP);
        BigDecimal neto = total.subtract(comision);
        if (neto.signum() <= 0) { throw new NegocioException("La comisión no puede consumir el importe del giro."); }
        return new Desglose(porcentaje,comision,neto);
    }
}
