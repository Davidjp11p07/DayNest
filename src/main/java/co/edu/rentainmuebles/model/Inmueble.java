package co.edu.rentainmuebles.model;

import java.math.BigDecimal;
import java.util.Objects;

/** Datos del inmueble; la autorización para editarlo corresponde al servicio. */
public final class Inmueble implements Identificable {
    private final Long id;
    private final Propietario propietario;
    private final String titulo;
    private final String ciudad;
    private final String direccion;
    private final String descripcion;
    private final int capacidad;
    private final BigDecimal precioPorNoche;
    private final EstadoInmueble estado;
    private Ciudad ubicacion;
    private String tipo;
    private String moneda="COP";
    private Boolean esRural,cercaMar;

    public Inmueble(Long id, Propietario propietario, String titulo, String ciudad, String direccion,
                    String descripcion, int capacidad, BigDecimal precioPorNoche, EstadoInmueble estado) {
        this.id = ValidacionModelo.identificador(id);
        this.propietario = Objects.requireNonNull(propietario, "El propietario es obligatorio.");
        this.titulo = ValidacionModelo.texto(titulo, "El título", 150);
        this.ciudad = ValidacionModelo.texto(ciudad, "La ciudad", 400);
        this.direccion = ValidacionModelo.texto(direccion, "La dirección", 300);
        this.descripcion = descripcion == null ? "" : descripcion;
        if (capacidad <= 0 || capacidad > 32767) { throw new IllegalArgumentException("La capacidad debe estar entre 1 y 32767 huéspedes."); }
        this.capacidad = capacidad;
        this.precioPorNoche = ValidacionModelo.dineroPositivo(precioPorNoche, "El precio por noche");
        this.estado = Objects.requireNonNull(estado, "El estado es obligatorio.");
    }

    public Inmueble(Long id, Propietario p, String titulo, Ciudad ciudad, String direccion, String descripcion, int capacidad, BigDecimal precio, EstadoInmueble estado, String tipo, String moneda) {
        this(id,p,titulo,ciudad.getEtiqueta(),direccion,descripcion==null?"":descripcion,capacidad,precio,estado);
        if(!moneda.matches("[A-Z]{3}"))throw new IllegalArgumentException("Moneda inválida.");
        this.ubicacion=ciudad; this.tipo=FiltrosInmueble.validarTipo(tipo); this.moneda=moneda;
    }
    public Inmueble(Long id,Propietario p,String titulo,Ciudad ciudad,String direccion,String descripcion,int capacidad,BigDecimal precio,EstadoInmueble estado,String tipo,String moneda,Boolean rural,Boolean mar){this(id,p,titulo,ciudad,direccion,descripcion,capacidad,precio,estado,tipo,moneda);this.esRural=rural;this.cercaMar=mar;}
    public Boolean getEsRural(){return esRural;}
    public Boolean getCercaMar(){return cercaMar;}
    public Ciudad getUbicacion(){return ubicacion;}
    public String getTipo(){return tipo;}
    public String getMoneda(){return moneda;}
    @Override public Long getId() { return id; }
    public Propietario getPropietario() { return propietario; }
    public String getTitulo() { return titulo; }
    public String getCiudad() { return ciudad; }
    public String getDireccion() { return direccion; }
    public String getDescripcion() { return descripcion; }
    public int getCapacidad() { return capacidad; }
    public BigDecimal getPrecioPorNoche() { return precioPorNoche; }
    public EstadoInmueble getEstado() { return estado; }
}
