<%@ page pageEncoding="UTF-8" %>
<%@ include file="../layout/cabecera.jspf" %>
<span class="eyebrow"><c:out value="${textos['ui430']}"/></span><h1><c:out value="${textos['ui431']}"/></h1><p class="muted"><c:out value="${textos['ui432']}"/></p>
<c:set var="filtrosExtendidos" value="${true}"/><div class="results-search"><%@ include file="../layout/buscador.jspf" %></div>
<%@ include file="../layout/filtros-activos.jspf" %>
<div class="section-head"><div><h2><c:out value="${textos['ui433']}"/></h2><p class="muted"><c:out value="${textos['ui434']}"/></p></div></div><%@ include file="../layout/inmuebles.jspf" %>
<%@ include file="../layout/pie.jspf" %>
