package co.edu.rentainmuebles.model;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/** Intervalo [entrada, salida): la noche de salida no se cobra ni bloquea disponibilidad. */
public final class PeriodoEstadia {
    private final LocalDate entrada;
    private final LocalDate salida;

    public PeriodoEstadia(LocalDate entrada, LocalDate salida) {
        this.entrada = Objects.requireNonNull(entrada, "La entrada es obligatoria.");
        this.salida = Objects.requireNonNull(salida, "La salida es obligatoria.");
        if (!salida.isAfter(entrada)) {
            throw new IllegalArgumentException("La salida debe ser posterior a la entrada.");
        }
        // Se aceptan fechas pasadas para reconstruir el historial desde JDBC.
    }

    public LocalDate getEntrada() { return entrada; }
    public LocalDate getSalida() { return salida; }
    public long getNoches() { return ChronoUnit.DAYS.between(entrada, salida); }

    @Override public boolean equals(Object objeto) {
        if (this == objeto) { return true; }
        if (!(objeto instanceof PeriodoEstadia otro)) { return false; }
        return entrada.equals(otro.entrada) && salida.equals(otro.salida);
    }

    @Override public int hashCode() { return Objects.hash(entrada, salida); }
}
