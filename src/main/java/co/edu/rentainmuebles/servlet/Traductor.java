package co.edu.rentainmuebles.servlet;

import java.util.*;

/** Localiza mensajes de interfaz sin modificar nombres ni contenido de los usuarios. */
public final class Traductor {
    private final boolean ingles;
    private final Map<String,String> mensajes=new HashMap<>();

    public Traductor(String idioma) {
        ingles="en".equals(idioma);
        var origen=ResourceBundle.getBundle("i18n.mensajes",Locale.ROOT);
        var destino=ResourceBundle.getBundle("i18n.mensajes",Locale.ENGLISH);
        for(String clave:origen.keySet()) mensajes.put(origen.getString(clave),destino.getString(clave));
    }

    public String traducir(String texto) {
        if(texto==null) return "";
        if(!ingles) return texto;
        String traduccion=mensajes.get(texto);
        if(traduccion!=null) return traduccion;
        if(texto.startsWith("Bienvenido, ")) return "Welcome, "+texto.substring("Bienvenido, ".length());
        if(texto.startsWith("Pago/reserva fallado: ")) return "Payment/booking failed: "+traducir(texto.substring("Pago/reserva fallado: ".length()));
        if(texto.matches("Pago realizado por [0-9.]+ [A-Z]{3}\\. Reserva confirmada \\(simulación\\)\\.")) {
            return texto.replace("Pago realizado por ","Payment completed for ").replace(". Reserva confirmada (simulación).",". Booking confirmed (simulation).");
        }
        return texto;
    }
}
