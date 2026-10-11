package co.edu.rentainmuebles.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/** Abstracción común para consultar pagos y giros de forma polimórfica. */
public abstract class MovimientoFinanciero implements Identificable {
    private final Long id;
    private final Reserva reserva;
    private final BigDecimal monto;
    private final String referencia;
    private final LocalDateTime fecha;

    protected MovimientoFinanciero(Long id, Reserva reserva, BigDecimal monto, String referencia, LocalDateTime fecha) {
        this.id = ValidacionModelo.identificador(id);
        this.reserva = Objects.requireNonNull(reserva, "La reserva es obligatoria.");
        this.monto = ValidacionModelo.dineroPositivo(monto, "El monto");
        this.referencia = referencia == null ? null : ValidacionModelo.texto(referencia, "La referencia", 140);
        this.fecha = Objects.requireNonNull(fecha, "La fecha es obligatoria.");
    }

    public abstract TipoMovimiento getTipo();
    @Override public Long getId() { return id; }
    public Reserva getReserva() { return reserva; }
    public BigDecimal getMonto() { return monto; }
    public String getReferencia() { return referencia; }
    public LocalDateTime getFecha() { return fecha; }
}
