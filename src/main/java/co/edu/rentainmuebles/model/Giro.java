package co.edu.rentainmuebles.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Objects;

/** Guarda el neto y el beneficiario efectivo; la comisión monetaria se deriva. */
public final class Giro extends MovimientoFinanciero {
    private String origenAutorizacion="ADMINISTRADOR";
    private String cuentaDestino="";
    public Giro conAuditoria(String origen,String destino){origenAutorizacion=origen==null?"ADMINISTRADOR":origen;cuentaDestino=destino;return this;}
    public String getOrigenAutorizacion(){return origenAutorizacion;}
    public String getCuentaDestino(){return cuentaDestino;}
    private BigDecimal porcentajeComision = BigDecimal.ZERO;
    private BigDecimal montoComision = BigDecimal.ZERO;
    private final Propietario propietario;
    private final EstadoGiro estado;
    private Long administradorId;
    private String claveIdempotencia=java.util.UUID.randomUUID().toString();
    public Giro(Long id, Reserva reserva, Propietario propietario, BigDecimal monto, String referencia,
                LocalDateTime fecha, EstadoGiro estado) {
        super(id,reserva,monto,referencia,fecha);
        this.propietario=Objects.requireNonNull(propietario,"El propietario es obligatorio.");
        this.estado=Objects.requireNonNull(estado);
    }
    public Giro(Long id, Reserva reserva, Propietario propietario, BigDecimal monto, String referencia,
                LocalDateTime fecha, BigDecimal porcentaje, BigDecimal comision) {
        this(id,reserva,propietario,monto,referencia,fecha,EstadoGiro.EJECUTADO);
        if (porcentaje==null || porcentaje.signum()<0 || porcentaje.compareTo(BigDecimal.ONE)>=0
                || comision==null || reserva.getTotal().subtract(monto).compareTo(comision)!=0
                || reserva.getTotal().multiply(porcentaje).setScale(2,RoundingMode.HALF_UP).compareTo(comision)!=0)
            throw new IllegalArgumentException("El desglose de comisión no coincide con el giro.");
        this.porcentajeComision=porcentaje;this.montoComision=comision;
    }
    public Giro(Long id, Reserva r, Propietario p, BigDecimal monto, String referencia, LocalDateTime fecha, EstadoGiro estado, Long admin, String clave) {
        this(id,r,p,monto,referencia,fecha,estado);this.administradorId=admin;this.claveIdempotencia=clave;
    }
    public Giro(Long id, Reserva r, Propietario p, BigDecimal monto, String referencia, LocalDateTime fecha, EstadoGiro estado, Long admin, String clave, BigDecimal porcentaje, BigDecimal comision) {
        this(id,r,p,monto,referencia,fecha,estado,admin,clave);
        if(porcentaje==null||comision==null||porcentaje.signum()<0||porcentaje.compareTo(BigDecimal.ONE)>=0||comision.signum()<0||r.getTotal().multiply(porcentaje).setScale(2,RoundingMode.HALF_UP).compareTo(comision)!=0||r.getTotal().subtract(comision).compareTo(monto)!=0) throw new IllegalArgumentException("Comision invalida");
        this.porcentajeComision=porcentaje;this.montoComision=comision;
        this.administradorId=admin;this.claveIdempotencia=clave;

    }
    public Long getAdministradorId(){return administradorId;}
    public String getClaveIdempotencia(){return claveIdempotencia;}
    public String getMoneda(){return getReserva().getMoneda();}
    @Override public TipoMovimiento getTipo() { return TipoMovimiento.GIRO_PROPIETARIO; }
    public Propietario getPropietario() { return propietario; }
    public EstadoGiro getEstado() { return estado; }
    public BigDecimal getMontoComision() { return montoComision; }
    /** Porcentaje historico persistido; cambios de configuracion no alteran giros. */
    public BigDecimal getPorcentajeComision() { return porcentajeComision; }
}