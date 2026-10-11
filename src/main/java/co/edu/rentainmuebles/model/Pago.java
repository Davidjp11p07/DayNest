package co.edu.rentainmuebles.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/** Un pago completo por reserva. La validación y confirmación se harán en el servicio. */
public final class Pago extends MovimientoFinanciero {
    private final MetodoPago metodo;
    private final EstadoPago estado;
    private String clase="COBRO";
    private Long cobroOrigen;
    private String claveIdempotencia=java.util.UUID.randomUUID().toString();

    public Pago(Long id, Reserva reserva, BigDecimal monto, String referencia, LocalDateTime fecha, MetodoPago metodo) {
        this(id,reserva,monto,referencia,fecha,metodo,EstadoPago.APROBADO);
    }
    public Pago(Long id, Reserva reserva, BigDecimal monto, String referencia, LocalDateTime fecha, MetodoPago metodo, EstadoPago estado) {
        super(id, reserva, monto, referencia, fecha);
        this.metodo = Objects.requireNonNull(metodo, "El método de pago es obligatorio.");
        this.estado = Objects.requireNonNull(estado);
    }

    public Pago(Long id, Reserva r, BigDecimal monto, String referencia, LocalDateTime fecha, EstadoPago estado, String clase, Long origen, String clave) {
        this(id,r,monto,referencia,fecha,MetodoPago.TRANSFERENCIA,estado);this.clase=clase;this.cobroOrigen=origen;this.claveIdempotencia=clave;
    }
    public String getClase(){return clase;}
    public Long getCobroOrigen(){return cobroOrigen;}
    public String getClaveIdempotencia(){return claveIdempotencia;}
    public String getMoneda(){return getReserva().getMoneda();}
    @Override public TipoMovimiento getTipo() { return TipoMovimiento.PAGO_CLIENTE; }
    public MetodoPago getMetodo() { return metodo; }
    public EstadoPago getEstado() { return estado; }
}
