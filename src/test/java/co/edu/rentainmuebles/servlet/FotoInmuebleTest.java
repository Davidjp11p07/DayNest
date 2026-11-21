package co.edu.rentainmuebles.servlet;

import co.edu.rentainmuebles.service.NegocioException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FotoInmuebleTest {
    @Test void pngConCabeceraIncompletaDevuelveErrorDeFormulario() {
        byte[] firmaPng = {(byte) 137, 80, 78, 71, 13, 10, 26, 10};
        assertThrows(NegocioException.class, () -> PropietarioServlet.tipoImagen(firmaPng));
    }

    @Test void identificaFotoJpeg() throws Exception {
        byte[] foto = Files.readAllBytes(Path.of("src/main/webapp/assets/img/interior-01.jpg"));
        assertEquals("image/jpeg", PropietarioServlet.tipoImagen(foto));
    }
}
