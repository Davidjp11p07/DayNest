"use strict";
document.documentElement.classList.add("js");
const toggle = document.querySelector(".nav-toggle");
const navigation = document.getElementById("navegacion");
if (toggle && navigation) {
  const close = () => { navigation.classList.remove("is-open"); toggle.setAttribute("aria-expanded", "false"); toggle.setAttribute("aria-label", "Abrir menú"); };
  toggle.addEventListener("click", () => {
    const open = navigation.classList.toggle("is-open");
    toggle.setAttribute("aria-expanded", String(open));
    toggle.setAttribute("aria-label", open ? "Cerrar menú" : "Abrir menú");
  });
  document.addEventListener("keydown", e => { if (e.key === "Escape" && navigation.classList.contains("is-open")) { close(); toggle.focus(); } });
  document.addEventListener("click", e => { if (!e.target.closest(".site-header")) close(); });
}
for (const button of document.querySelectorAll("[data-password]")) {
  button.addEventListener("click", () => {
    const field = document.getElementById(button.dataset.password);
    const visible = field.type === "password";
    field.type = visible ? "text" : "password";
    button.textContent = visible ? "Ocultar" : "Mostrar";
    button.setAttribute("aria-label", visible ? "Ocultar contraseña" : "Mostrar contraseña");
    button.setAttribute("aria-pressed", String(visible));
  });
}
for (const badge of document.querySelectorAll(".badge")) {
  // Solo apariencia: nunca se deduce un pago a partir del estado de una reserva.
  badge.dataset.estado = badge.textContent.trim();
}
for (const form of document.forms) {
  for (const field of form.querySelectorAll("input:not([type=hidden]),select,textarea")) {
    const check = () => {
      const invalid = !field.validity.valid;
      field.setAttribute("aria-invalid", String(invalid));
      const wrapper = field.closest(".field");
      if (!wrapper) return;
      let error = wrapper.querySelector(".field-error");
      if (invalid && !error) {
        error = document.createElement("p"); error.className = "field-error";
        error.id = (field.id || "campo") + "-error";
        wrapper.append(error);
        const description = field.getAttribute("aria-describedby") || "";
        field.setAttribute("aria-describedby", (description + " " + error.id).trim());
      }
      if (error) { error.textContent = invalid ? field.validationMessage : ""; error.hidden = !invalid; }
    };
    field.addEventListener("blur", check);
    field.addEventListener("invalid", () => { const details = field.closest("details"); if (details) details.open = true; check(); });
    field.addEventListener("input", () => { if (field.getAttribute("aria-invalid") === "true") check(); });
  }
  form.addEventListener("submit", e => {
    if (e.defaultPrevented) return;
    const button = e.submitter;
    if (!button || !button.matches("button")) return;
    button.classList.add("is-loading"); button.setAttribute("aria-busy", "true");
    // No se deshabilitan campos ni CSRF; no se altera la petición enviada.
    requestAnimationFrame(() => { button.disabled = true; });
  });
}
window.addEventListener("pageshow", () => {
  for (const button of document.querySelectorAll("button.is-loading")) {
    button.disabled = false; button.classList.remove("is-loading"); button.removeAttribute("aria-busy");
  }
});
const estimate = document.querySelector("[data-estimacion]");
if (estimate) {
  const arrival = estimate.querySelector("[data-entrada]");
  const departure = estimate.querySelector("[data-salida]");
  const money = amount => new Intl.NumberFormat("es-CO", {style:"currency",currency:estimate.dataset.moneda || "COP",minimumFractionDigits:0,maximumFractionDigits:2}).format(amount) + " " + (estimate.dataset.moneda || "COP");
  const update = () => {
    const nights = (Date.parse(departure.value + "T00:00:00Z") - Date.parse(arrival.value + "T00:00:00Z")) / 86400000;
    const valid = Number.isInteger(nights) && nights > 0;
    departure.setAttribute("aria-invalid", String(Boolean(arrival.value && departure.value && !valid)));
    estimate.querySelector("[data-noches]").textContent = valid ? `${nights} noche${nights === 1 ? "" : "s"} × ${money(Number(estimate.dataset.precio))}` : arrival.value && departure.value ? "La salida debe ser posterior a la entrada" : "Elige las fechas";
    const total = valid ? money(nights * Number(estimate.dataset.precio)) : "—";
    estimate.querySelector("[data-subtotal]").textContent = total;
    estimate.querySelector("[data-total]").textContent = total;
  };
  estimate.addEventListener("input", update); update();
}
const modal = document.getElementById("galeria-modal");
if (modal && typeof modal.showModal === "function") {
  for (const button of document.querySelectorAll("[data-photo]")) {
    button.addEventListener("click", () => {
      const image = button.querySelector("img");
      const large = modal.querySelector("[data-modal-photo]");
      large.src = image.src; large.alt = image.alt; modal.showModal();
    });
  }
  modal.querySelector("[data-close-modal]").addEventListener("click", () => modal.close());
  modal.addEventListener("click", e => { if (e.target === modal) { const r = modal.getBoundingClientRect(); if (e.clientX < r.left || e.clientX > r.right || e.clientY < r.top || e.clientY > r.bottom) modal.close(); } });
}

// Catálogo local renderizado por JSTL. Un id de ciudad distingue nombres iguales.
document.querySelectorAll('[data-city-search]').forEach(input=>{
 const root=input.closest('form'),hidden=root.querySelector('[data-city-id]'),list=root.querySelector('[role="listbox"]'),status=root.querySelector('[data-city-status]');
 const catalog=[...document.querySelectorAll('[data-city-catalog] span')].map(e=>({id:e.dataset.id,name:e.dataset.name,label:e.textContent}));
 const norm=s=>s.normalize('NFD').replace(/[\u0300-\u036f]/g,'').toLowerCase().trim();let active=-1,matches=[];
 const close=()=>{list.hidden=true;input.setAttribute('aria-expanded','false');input.removeAttribute('aria-activedescendant');};
 const choose=c=>{input.value=c.label;hidden.value=c.id;close();input.focus();};
 if(hidden.value){const c=catalog.find(c=>c.id===hidden.value);if(c)input.value=c.label;}
 input.addEventListener('input',()=>{hidden.value='';active=-1;const term=norm(input.value);matches=term?catalog.filter(c=>norm(c.name).startsWith(term)||norm(c.label).startsWith(term)):[];list.replaceChildren();matches.forEach((c,index)=>{const li=document.createElement('li');li.id='ciudad-opcion-'+index;li.setAttribute('role','option');li.textContent=c.label;li.addEventListener('mousedown',e=>{e.preventDefault();choose(c);});list.append(li);});list.hidden=!matches.length;input.setAttribute('aria-expanded',String(!!matches.length));status.textContent=matches.length+' ciudades disponibles';});
 input.addEventListener('keydown',e=>{if(e.key==='Escape'){close();return;}if(['ArrowDown','ArrowUp'].includes(e.key)&&matches.length){e.preventDefault();active=(active+(e.key==='ArrowDown'?1:-1)+matches.length)%matches.length;[...list.children].forEach((li,n)=>li.setAttribute('aria-selected',String(n===active)));input.setAttribute('aria-activedescendant',list.children[active].id);list.children[active].scrollIntoView({block:'nearest'});}if(e.key==='Enter'&&active>=0&&!list.hidden){e.preventDefault();choose(matches[active]);}});
 input.addEventListener('blur',()=>setTimeout(close,150));
 root.addEventListener('submit',()=>{if(!hidden.value){const a=catalog.filter(c=>norm(c.label)===norm(input.value)||norm(c.name)===norm(input.value));if(a.length===1)hidden.value=a[0].id;}});
});

// La cuenta ocasional solo forma parte del pago, nunca actualiza el perfil.
document.querySelectorAll('[data-cuenta-pago]').forEach(form => {
 const selector=form.querySelector('[name=cuentaPago]'), campos=form.querySelector('[data-cuenta-ocasional]'), registrada=form.querySelector('[data-cuenta-registrada]');
 const actualizar=()=>{const ocasional=selector.value==='OCASIONAL';campos.hidden=!ocasional;campos.disabled=!ocasional;campos.querySelectorAll('input,select').forEach(c=>c.required=ocasional);if(registrada)registrada.hidden=ocasional;};
 selector.addEventListener('change',actualizar);actualizar();
});

// Category anchors remain functional without JavaScript.
document.querySelectorAll('[data-category-key]').forEach(link => link.addEventListener('click', () => {
 const form=document.querySelector('form[aria-label="Buscar alojamiento"]'); if(!form)return;
 const url=new URL(link.href,location.href);const allowed=['ciudad','ciudadId','paisId','divisionId','entrada','salida','minimo','maximo','capacidad','tipo','rural','mar'];
 for(const key of allowed){const value=new FormData(form).get(key);if(value)url.searchParams.set(key,value);else url.searchParams.delete(key);}
 url.searchParams.set(link.dataset.categoryKey,link.dataset.categoryValue);link.href=url.href;
}));
