"use strict";
document.querySelectorAll("[data-calendar]").forEach(calendar => {
  const scope = calendar.closest("[data-calendar-scope]");
  if (!scope) return;
  const start = scope.querySelector("[data-calendar-start], [data-entrada]");
  const end = scope.querySelector("[data-calendar-end], [data-salida]");
  if (!start || !end) return;
  const status = calendar.querySelector("[data-calendar-status]");
  const days = [...calendar.querySelectorAll("[data-calendar-date]")];
  const ranges = [...calendar.querySelectorAll("[data-calendar-range]")].map(r => ({start:r.dataset.inicio,end:r.dataset.fin}));
  const occupied = date => ranges.some(r => date >= r.start && date < r.end);
  const overlaps = (a,b) => ranges.some(r => a < r.end && b > r.start);
  const tell = (message, error=false) => { status.textContent=message;status.classList.toggle("is-error",error); };
  const paint = () => {
    days.forEach(day => {
      const date=day.dataset.calendarDate;
      const selected=date===start.value || date===end.value;
      day.classList.toggle("is-selected",selected);
      day.classList.toggle("in-range",!!start.value && !!end.value && date>start.value && date<end.value);
      day.setAttribute("aria-pressed",String(selected));
    });
  };
  const validate = () => {
    start.setCustomValidity("");end.setCustomValidity("");
    if (!start.value && end.value) {
      start.setCustomValidity(window.DayNestI18n.t("Indica también la fecha de entrada."));tell(start.validationMessage,true);
    } else if (start.value && occupied(start.value)) {
      start.setCustomValidity(window.DayNestI18n.t("La entrada debe ser un día disponible."));tell(start.validationMessage,true);
    } else if (start.value && end.value && end.value<=start.value) {
      end.setCustomValidity(window.DayNestI18n.t("La salida debe ser posterior a la entrada."));tell(end.validationMessage,true);
    } else if (start.value && end.value && overlaps(start.value,end.value)) {
      end.setCustomValidity(window.DayNestI18n.t("El rango incluye noches reservadas o bloqueadas. Elige otras fechas."));tell(end.validationMessage,true);
    } else if (start.value && end.value) tell(window.DayNestI18n.t("Rango seleccionado. La disponibilidad se confirma al enviar el formulario."));
    else if (start.value) {
      end.setCustomValidity(window.DayNestI18n.t("Indica también la fecha de salida."));
      tell(window.DayNestI18n.t("Ahora elige la salida. Puede coincidir con el primer día de un rango ocupado."));
    }
    else tell(window.DayNestI18n.t("Elige primero la entrada y luego la salida. El día de salida no ocupa una noche."));
    paint();
  };
  days.forEach(day => day.addEventListener("click", () => {
    const date=day.dataset.calendarDate;
    if (!start.value || end.value || date<=start.value) {
      if (occupied(date) || (start.min && date<start.min)) { tell(window.DayNestI18n.t("Elige un día disponible para la entrada."),true);return; }
      start.value=date;end.value="";
    } else {
      if (overlaps(start.value,date)) { tell(window.DayNestI18n.t("Ese rango incluye noches reservadas o bloqueadas. Elige otra salida."),true);return; }
      end.value=date;
    }
    start.dispatchEvent(new Event("input",{bubbles:true}));end.dispatchEvent(new Event("input",{bubbles:true}));
    validate();
  }));
  calendar.addEventListener("keydown", e => {
    const current=days.indexOf(document.activeElement);
    const offset={ArrowLeft:-1,ArrowRight:1,ArrowUp:-7,ArrowDown:7}[e.key];
    if (current<0 || offset===undefined) return;
    const next=days[current+offset];
    if(next && !next.disabled){e.preventDefault();next.focus();}
  });
  // Se ejecuta después de las validaciones generales del formulario para no perder el aviso de ocupación.
  if(start.form)start.form.addEventListener("input",validate);
  else [start,end].forEach(field => field.addEventListener("input",validate));
  calendar.querySelectorAll("[data-calendar-nav]").forEach(link => link.addEventListener("click", () => {
    const url=new URL(link.href,location.href);
    for(const [name,field] of [["entrada",start],["salida",end],["huespedes",scope.querySelector("[name=huespedes]")]]) {
      if(field && field.value)url.searchParams.set(name,field.value);else url.searchParams.delete(name);
    }
    link.href=url.toString();
  }));
  validate();
});
