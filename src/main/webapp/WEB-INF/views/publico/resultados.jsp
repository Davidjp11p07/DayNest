<%@ page pageEncoding="UTF-8" %>
<%@ include file="../layout/cabecera.jspf" %>
<span class="eyebrow">Un lugar para tus planes</span><h1>Encuentra tu próximo espacio</h1><p class="muted">Elige ciudad, fechas, presupuesto y capacidad. Nosotros te mostramos los alojamientos publicados.</p>
<c:set var="filtrosExtendidos" value="${true}"/><div class="results-search"><%@ include file="../layout/buscador.jspf" %></div>
<%@ include file="../layout/filtros-activos.jspf" %>
<div class="section-head"><div><h2>Resultados de búsqueda</h2><p class="muted">Precios por noche en la moneda indicada · fotografías ilustrativas.</p></div></div><%@ include file="../layout/inmuebles.jspf" %>
<%@ include file="../layout/pie.jspf" %>
