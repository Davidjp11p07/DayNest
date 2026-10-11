package co.edu.rentainmuebles.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/** Conserva la tarifa pactada aunque posteriormente cambie el precio del inmueble. */
public final class Reserva implements Identificable {
    private final Long id;
    private final Cliente cliente;
    private final Inmueble inmueble;
    private final PeriodoEstadia periodo;
    private final int huespedes;
    private final BigDecimal precioPorNoche;
    private final BigDecimal total;
    private final EstadoReserva estado;
    private final LocalDateTime fechaCreacion;
    private Propietario propietarioPactado;
    private String moneda;
    private java.time.OffsetDateTime expiraEn;

    public Reserva(Long id, Cliente cliente, Inmueble inmueble, PeriodoEstadia periodo, int huespedes,
                   BigDecimal precioPorNoche, EstadoReserva estado, LocalDateTime fechaCreacion) {
        this.propietarioPactado=inmueble.getPropietario(); this.moneda=inmueble.getMoneda();
        this.expiraEn=fechaCreacion.atZone(java.time.ZoneId.of("America/Bogota")).toOffsetDateTime().plusMinutes(30);
        this.id = ValidacionModelo.identificador(id);
        this.cliente = Objects.requireNonNull(cliente, "El cliente es obligatorio.");
        this.inmueble = Objects.requireNonNull(inmueble, "El inmueble es obligatorio.");
        this.periodo = Objects.requireNonNull(periodo, "El período es obligatorio.");
        if (huespedes <= 0) { throw new IllegalArgumentException("La cantidad de huéspedes debe ser positiva."); }
        this.huespedes = huespedes;
        this.precioPorNoche = ValidacionModelo.dineroPositivo(precioPorNoche, "La tarifa pactada");
        this.total = ValidacionModelo.dineroPositivo(this.precioPorNoche.multiply(BigDecimal.valueOf(periodo.getNoches())), "El total");
        this.estado = Objects.requireNonNull(estado, "El estado es obligatorio.");
        this.fechaCreacion = Objects.requireNonNull(fechaCreacion, "La fecha de creación es obligatoria.");
    }

    public Reserva(Long id, Cliente c, Inmueble i, PeriodoEstadia periodo, int huespedes, BigDecimal precio, EstadoReserva estado, LocalDateTime creada, Propietario pactado, String moneda, java.time.OffsetDateTime expira) {
        this(id,c,i,periodo,huespedes,precio,estado,creada);this.propietarioPactado=pactado;this.moneda=moneda;this.expiraEn=expira;
    }
    public Propietario getPropietarioPactado(){return propietarioPactado;}
    public String getMoneda(){return moneda;}
    public java.time.OffsetDateTime getExpiraEn(){return expiraEn;}
    @Override public Long getId() { return id; }
    public Cliente getCliente() { return cliente; }
    public Inmueble getInmueble() { return inmueble; }
    public PeriodoEstadia getPeriodo() { return periodo; }
    public int getHuespedes() { return huespedes; }
    public BigDecimal getPrecioPorNoche() { return precioPorNoche; }
    public BigDecimal getTotal() { return total; }
    public EstadoReserva getEstado() { return estado; }
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
}
