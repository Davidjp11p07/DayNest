package co.edu.rentainmuebles.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/** Un pago completo por reserva. La validación y confirmación se harán en el servicio. */
public final class Pago extends MovimientoFinanciero {
    private final MetodoPago metodo;
    private final EstadoPago estado;
    private CuentaBancaria cuentaOcasional;
    private String metodoRegistrado, bancoPago, numeroCuentaPago, tipoCuentaPago, usoCuenta;
    private boolean datosBancariosLeidos;
    public Pago conCuentaOcasional(CuentaBancaria cuenta){this.cuentaOcasional=Objects.requireNonNull(cuenta);return this;}
    public CuentaBancaria getCuentaOcasional(){return cuentaOcasional;}
    public Pago conDatosBancarios(String metodo,String banco,String numero,String tipo,String uso){
        datosBancariosLeidos=true;metodoRegistrado=metodo;bancoPago=banco;numeroCuentaPago=numero;tipoCuentaPago=tipo;usoCuenta=uso;return this;
    }
    public String getMetodoRegistrado(){return metodoRegistrado==null?"No registrado (historico)":metodoRegistrado;}
    public String getBancoPago(){return bancoPago;}
    public String getNumeroCuentaPago(){return numeroCuentaPago==null?"": "****"+numeroCuentaPago.substring(Math.max(0,numeroCuentaPago.length()-4));}
    public String getTipoCuentaPago(){return tipoCuentaPago;}
    public String getUsoCuenta(){return usoCuenta;}
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
    public MetodoPago getMetodo() { return datosBancariosLeidos ? ("TRANSFERENCIA".equals(metodoRegistrado)?MetodoPago.TRANSFERENCIA:null) : metodo; }
    public EstadoPago getEstado() { return estado; }
}
