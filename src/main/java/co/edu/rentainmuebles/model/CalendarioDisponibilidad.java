package co.edu.rentainmuebles.model;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** Vista mensual de noches; el extremo de salida siempre es exclusivo. */
public final class CalendarioDisponibilidad {
    private final YearMonth mes;
    private final List<Ocupacion> rangos;
    private final List<Dia> dias = new ArrayList<>();

    public CalendarioDisponibilidad(YearMonth mes, LocalDate hoy, List<Ocupacion> ocupaciones) {
        this(mes,hoy,ocupaciones,false);
    }

    public CalendarioDisponibilidad(YearMonth mes, LocalDate hoy, List<Ocupacion> ocupaciones, boolean cliente) {
        this.mes = mes;
        this.rangos = List.copyOf(ocupaciones);
        LocalDate primeraEntrada=cliente?hoy.plusDays(1):hoy;
        for (int i = 1; i < mes.atDay(1).getDayOfWeek().getValue(); i++) dias.add(new Dia(null, "VACIO", false));
        for (int numero = 1; numero <= mes.lengthOfMonth(); numero++) {
            LocalDate fecha = mes.atDay(numero);
            String estado = fecha.isBefore(primeraEntrada) ? "PASADO" : "DISPONIBLE";
            if (!fecha.isBefore(primeraEntrada)) for (Ocupacion o : ocupaciones) {
                if (o.activa() && !fecha.isBefore(o.inicio()) && fecha.isBefore(o.fin())) {
                    estado = "BLOQUEO".equals(o.tipo()) ? "BLOQUEADO" : "RESERVADO"; break;
                }
            }
            dias.add(new Dia(fecha, estado, fecha.equals(hoy)));
        }
        while (dias.size() % 7 != 0) dias.add(new Dia(null, "VACIO", false));
    }

    public static YearMonth mes(String valor, LocalDate hoy) {
        if (valor == null || valor.isBlank()) return YearMonth.from(hoy);
        if (!valor.matches("[0-9]{4}-(0[1-9]|1[0-2])")) throw new IllegalArgumentException("Selecciona un mes válido.");
        YearMonth resultado = YearMonth.parse(valor);
        if (resultado.getYear() < 1901 || resultado.getYear() > 9998) throw new IllegalArgumentException("Selecciona un año entre 1901 y 9998.");
        return resultado;
    }

    public List<Dia> getDias() { return Collections.unmodifiableList(dias); }
    public List<Ocupacion> getRangos() { return rangos; }
    public String getTitulo() { return mes.format(DateTimeFormatter.ofPattern("MMMM 'de' yyyy", Locale.forLanguageTag("es-CO"))); }
    public String getTituloIngles() { return mes.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)); }
    public String getAnterior() { return mes.minusMonths(1).toString(); }
    public String getSiguiente() { return mes.plusMonths(1).toString(); }
    public String getMes() { return mes.toString(); }

    public record Dia(LocalDate fecha, String estado, boolean hoy) {
        public LocalDate getFecha() { return fecha; }
        public String getEstado() { return estado; }
        public boolean isHoy() { return hoy; }
        public String getNumero() { return fecha == null ? "" : Integer.toString(fecha.getDayOfMonth()); }
        public String getEtiqueta() {
            if (fecha == null) return "";
            return fecha.format(DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", Locale.forLanguageTag("es-CO")))
                    + ": " + estado.toLowerCase(Locale.ROOT);
        }
        public String getEtiquetaIngles() {
            if(fecha==null) return "";
            String label=switch(estado) {
                case "RESERVADO" -> "booked";
                case "BLOQUEADO" -> "blocked";
                case "PASADO" -> "unavailable";
                default -> "available";
            };
            return fecha.format(DateTimeFormatter.ofPattern("MMMM d, yyyy",Locale.ENGLISH))+": "+label;
        }
    }
}
