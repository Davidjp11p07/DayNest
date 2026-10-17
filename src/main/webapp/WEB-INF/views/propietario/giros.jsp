<%@ page pageEncoding="UTF-8" %>
<%@ include file="../layout/cabecera.jspf" %>
<h1>Mis giros</h1><p class="muted">Giros del neto cobrado menos la comisión registrada. Operaciones simuladas.</p><c:if test="${finanzasDisponibles}"><h2>Estancias y liquidaciones</h2><%@ include file="../layout/liquidaciones.jspf" %></c:if><h2>Historial de giros</h2><%@ include file="../layout/giros.jspf" %>
<%@ include file="../layout/pie.jspf" %>
