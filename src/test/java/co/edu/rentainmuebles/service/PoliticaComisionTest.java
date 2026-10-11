package co.edu.rentainmuebles.service;
import co.edu.rentainmuebles.service.impl.PoliticaComisionPorcentaje;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;
class PoliticaComisionTest {
    @Test void redondeaHalfUpYConservaBalance() {
        PoliticaComision.Desglose d=new PoliticaComisionPorcentaje(new BigDecimal("0.10")).calcular(new BigDecimal("0.15"));
        assertEquals(new BigDecimal("0.02"),d.comision()); assertEquals(new BigDecimal("0.13"),d.neto());
    }
    @Test void rechazaPorcentajeFueraDeRangoOPrecision() {
        assertThrows(NegocioException.class,()->new PoliticaComisionPorcentaje(BigDecimal.ONE));
        assertThrows(NegocioException.class,()->new PoliticaComisionPorcentaje(new BigDecimal("-0.1")));
        assertThrows(NegocioException.class,()->new PoliticaComisionPorcentaje(new BigDecimal("0.12345")));
    }
}
