"use strict";
for (const formulario of document.forms) {
  formulario.addEventListener("input", () => {
    for (const campo of formulario.querySelectorAll('input[type="text"][required], textarea[required]')) {
      campo.setCustomValidity(campo.value && !campo.value.trim() ? "Este campo no puede contener solo espacios." : "");
    }
  });
  if (formulario.method.toLowerCase() === "get" && formulario.elements.entrada && formulario.elements.salida) {
    const validar = () => {
      const entrada = formulario.elements.entrada, salida = formulario.elements.salida;
      entrada.setCustomValidity(!entrada.value && salida.value ? "Indique también la fecha de entrada." : "");
      salida.setCustomValidity(entrada.value && !salida.value ? "Indique también la fecha de salida." : entrada.value && salida.value <= entrada.value ? "La salida debe ser posterior a la entrada." : "");
      if (formulario.elements.minimo && formulario.elements.maximo) {
        const minimo = formulario.elements.minimo, maximo = formulario.elements.maximo;
        maximo.setCustomValidity(minimo.value && maximo.value && Number(maximo.value) < Number(minimo.value) ? "El precio máximo no puede ser menor que el mínimo." : "");
      }
    };
    formulario.addEventListener("input", validar); validar();
  }
}
for (const enlace of document.querySelectorAll('header nav a')) {
  if (new URL(enlace.href).pathname === window.location.pathname) enlace.setAttribute("aria-current", "page");
}
