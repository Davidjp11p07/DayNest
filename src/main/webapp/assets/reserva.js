"use strict";
const form = document.querySelector("[data-reserva]");
if (form) {
  const update = () => {
    const entrada = form.elements.entrada.value, salida = form.elements.salida.value;
    if (entrada) {
      const fecha = new Date(entrada + "T00:00:00Z"); fecha.setUTCDate(fecha.getUTCDate() + 1);
      form.elements.salida.min = fecha.toISOString().slice(0,10);
    }
    form.elements.salida.setCustomValidity(entrada && salida && salida <= entrada ? "La salida debe ser posterior a la entrada." : "");
    const noches = (Date.parse(salida + "T00:00:00Z") - Date.parse(entrada + "T00:00:00Z")) / 86400000;
    const total = document.getElementById("total-reserva");
    if (noches > 0) {
      const importe = noches * Number(form.dataset.precio);
      total.textContent = `${noches} noche(s) · ${new Intl.NumberFormat("es-CO",{style:"currency",currency:form.dataset.moneda || "COP",maximumFractionDigits:2}).format(importe)} ${form.dataset.moneda || "COP"}`;
    } else { total.textContent = "Seleccione las fechas para calcular el total."; }
  };
  form.addEventListener("input",update); update();
}
