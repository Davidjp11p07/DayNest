"use strict";
// Only interface messages are translated. Names and property content remain as entered.
window.DayNestI18n = (() => {
  const english = document.documentElement.lang === "en";
  const messages = {
    "Abrir menú":"Open menu", "Cerrar menú":"Close menu",
    "Ocultar":"Hide", "Mostrar":"Show",
    "Ocultar contraseña":"Hide password", "Mostrar contraseña":"Show password",
    "Este campo no puede contener solo espacios.":"This field cannot contain only spaces.",
    "Indique también la fecha de entrada.":"Please also enter a check-in date.",
    "Indique también la fecha de salida.":"Please also enter a check-out date.",
    "La salida debe ser posterior a la entrada.":"Check-out must be after check-in.",
    "La salida debe ser posterior a la entrada":"Check-out must be after check-in",
    "El precio máximo no puede ser menor que el mínimo.":"The maximum price cannot be lower than the minimum.",
    "Elige las fechas":"Choose dates",
    "Indica también la fecha de entrada.":"Please also enter a check-in date.",
    "La entrada debe ser un día disponible.":"Check-in must be on an available day.",
    "El rango incluye noches reservadas o bloqueadas. Elige otras fechas.":"This range includes booked or blocked nights. Choose other dates.",
    "Rango seleccionado. La disponibilidad se confirma al enviar el formulario.":"Date range selected. Availability is confirmed when you submit the form.",
    "Indica también la fecha de salida.":"Please also enter a check-out date.",
    "Ahora elige la salida. Puede coincidir con el primer día de un rango ocupado.":"Now choose check-out. It can be the first day of an occupied range.",
    "Elige primero la entrada y luego la salida. El día de salida no ocupa una noche.":"Choose check-in first, then check-out. Check-out day does not count as a night.",
    "Elige un día disponible para la entrada.":"Choose an available check-in day.",
    "Ese rango incluye noches reservadas o bloqueadas. Elige otra salida.":"That range includes booked or blocked nights. Choose another check-out date.",
    "ciudades disponibles":"cities available",
    "noche":"night", "noches":"nights"
  };
  return {locale:english?"en-US":"es-CO", t:message=>english?(messages[message]||message):message};
})();
document.querySelector("#daynest-language")?.addEventListener("change", event => {
  // Preserve edits to search filters when changing language, without submitting payment forms.
  const url = new URL(window.location.href);
  const search = document.querySelector("form[data-search-form]");
  if (search) for (const [name,value] of new FormData(search)) {
    if (typeof value === "string" && value) url.searchParams.set(name,value);
    else url.searchParams.delete(name);
  }
  url.searchParams.set("lang",event.target.value);
  window.location.assign(url.href);
});
