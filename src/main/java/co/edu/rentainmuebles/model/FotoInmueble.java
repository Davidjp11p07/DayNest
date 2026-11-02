package co.edu.rentainmuebles.model;

/** Propiedades JavaBean para su uso desde JSP/EL. */
public final class FotoInmueble {
    private final long id;
    private final String tipo;
    private final String mime;
    private final String descripcion;

    public FotoInmueble(long id,String tipo,String mime,String descripcion){this.id=id;this.tipo=tipo;this.mime=mime;this.descripcion=descripcion;}
    public long getId(){return id;}
    public String getTipo(){return tipo;}
    public String getMime(){return mime;}
    public String getDescripcion(){return descripcion;}
}
