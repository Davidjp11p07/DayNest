package co.edu.rentainmuebles.service;
import java.math.BigDecimal;
/** Consulta calculada, no entidad ni tabla de facturas. */
public record ResumenReserva(long idReserva,long noches,BigDecimal totalPactado,BigDecimal totalPagado,BigDecimal saldo) {
    public long getNoches() { return noches; }
    public BigDecimal getTotalPactado() { return totalPactado; }
    public BigDecimal getTotalPagado() { return totalPagado; }
    public BigDecimal getSaldo() { return saldo; }
}
