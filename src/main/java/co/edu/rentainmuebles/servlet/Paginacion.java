package co.edu.rentainmuebles.servlet;

/** Información de navegación de un listado; la vista muestra diez filas por página. */
public final class Paginacion {
    private final int numero,paginas,total,desde,hasta;
    private final String anterior,siguiente;
    public Paginacion(int numero,int paginas,int total,int desde,int hasta,String anterior,String siguiente) {
        this.numero=numero; this.paginas=paginas; this.total=total; this.desde=desde; this.hasta=hasta;
        this.anterior=anterior; this.siguiente=siguiente;
    }
    public int getNumero() { return numero; }
    public int getPaginas() { return paginas; }
    public int getTotal() { return total; }
    public int getDesde() { return desde; }
    public int getHasta() { return hasta; }
    public String getAnterior() { return anterior; }
    public String getSiguiente() { return siguiente; }
}
