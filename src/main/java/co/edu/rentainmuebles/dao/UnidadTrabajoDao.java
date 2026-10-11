package co.edu.rentainmuebles.dao;

import java.util.function.Function;

/** Ejecuta operaciones DAO en una transacción; confirma al terminar o revierte ante un error. */
public interface UnidadTrabajoDao {
    <T> T ejecutar(Function<Daos, T> operacion);
    interface Daos {
        UsuarioDao usuarios();
        InmuebleDao inmuebles();
        ReservaDao reservas();
        PagoDao pagos();
        GiroDao giros();
    }
}
