package co.edu.rentainmuebles.dao;
import co.edu.rentainmuebles.model.Bloqueo;import java.time.LocalDate;import java.util.List;
public interface BloqueoDao {List<Bloqueo> listar(long propietario);void crear(long propietario,long inmueble,LocalDate inicio,LocalDate fin,String motivo);void liberar(long propietario,long bloqueo);}
