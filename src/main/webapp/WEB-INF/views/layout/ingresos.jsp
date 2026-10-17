<%@ page pageEncoding="UTF-8" %>
<%@ include file="cabecera.jspf" %>
<h1>${ingresosAdmin ? 'Comisiones e ingresos de DayNest' : 'Mis ingresos por inmueble'}</h1>
<p class="muted">Importes registrados en la simulación, separados por moneda. Los liquidados corresponden a giros ejecutados; los pendientes son estimaciones de reservas pagadas y no son ingresos realizados ni un saldo bancario.</p>
<c:if test="${ingresos.filtro gt 0}"><p>Mostrando el inmueble #<c:out value="${ingresos.filtro}"/>. <a href="${fn:escapeXml(rutaIngresos)}">Ver todos los inmuebles</a></p></c:if>
<c:choose><c:when test="${empty ingresos.totales}"><p class="empty">No hay inmuebles ni ingresos para esta consulta.</p></c:when><c:otherwise>
<h2>Totales ${ingresos.filtro gt 0 ? 'del inmueble seleccionado' : 'acumulados'}</h2>
<div class="grid">
<c:forEach var="t" items="${ingresos.totales}">
<section class="panel" data-resumen-moneda="${fn:escapeXml(t.moneda)}">
<h3><c:out value="${t.moneda}"/></h3>
<c:choose><c:when test="${ingresosAdmin}">
<p>Comisiones liquidadas</p><p class="price" data-importe="comision-liquidada"><fmt:formatNumber value="${t.comisionLiquidada}" pattern="#,##0.00"/> <c:out value="${t.moneda}"/></p>
<p>Comisiones pendientes estimadas: <strong><fmt:formatNumber value="${t.comisionPendiente}" pattern="#,##0.00"/></strong></p>
<p>Girado a propietarios: <fmt:formatNumber value="${t.netoLiquidado}" pattern="#,##0.00"/></p>
<p>Neto pendiente a propietarios: <fmt:formatNumber value="${t.netoPendiente}" pattern="#,##0.00"/></p>
</c:when><c:otherwise>
<p>Neto liquidado a tu favor</p><p class="price" data-importe="neto-liquidado"><fmt:formatNumber value="${t.netoLiquidado}" pattern="#,##0.00"/> <c:out value="${t.moneda}"/></p>
<p>Neto pendiente estimado: <strong><fmt:formatNumber value="${t.netoPendiente}" pattern="#,##0.00"/></strong></p>
<p>Comisiones descontadas en giros ejecutados: <fmt:formatNumber value="${t.comisionLiquidada}" pattern="#,##0.00"/></p>
<p>Comisiones pendientes estimadas: <fmt:formatNumber value="${t.comisionPendiente}" pattern="#,##0.00"/></p>
</c:otherwise></c:choose>
<p class="muted"><c:out value="${t.reservas}"/> reservaciones · Pagos netos de clientes: <fmt:formatNumber value="${t.netoCobrado}" pattern="#,##0.00"/> <c:out value="${t.moneda}"/></p>
</section></c:forEach></div>
<h2>${ingresosAdmin ? 'Comisiones y liquidaciones por inmueble' : 'Resumen de mis inmuebles'}</h2>
<p class="muted">Una fila por inmueble y moneda, incluyendo inmuebles sin ingresos. Abre un inmueble para consultar solamente sus arriendos. El historial se atribuye al propietario pactado en cada reserva.</p>
<div class="table-wrap" style="max-height:32rem;overflow:auto"><table data-resumen-inmuebles><thead style="position:sticky;top:0;background:var(--surface);z-index:1"><tr><th>Inmueble</th><th>Moneda</th><th>Reservas</th><th>Comisión liquidada</th><th>Comisión pendiente estimada</th><th>Neto liquidado al propietario</th><th>Neto pendiente estimado</th></tr></thead><tbody>
<c:forEach var="i" items="${ingresos.inmuebles}"><tr>
<td><a href="${fn:escapeXml(rutaIngresos)}?inmuebleId=${i.id}">#<c:out value="${i.id}"/> · <c:out value="${i.titulo}"/></a></td>
<td><c:out value="${i.importe.moneda}"/></td><td><c:out value="${i.importe.reservas}"/></td>
<td><fmt:formatNumber value="${i.importe.comisionLiquidada}" pattern="#,##0.00"/></td>
<td><fmt:formatNumber value="${i.importe.comisionPendiente}" pattern="#,##0.00"/></td>
<td><fmt:formatNumber value="${i.importe.netoLiquidado}" pattern="#,##0.00"/></td>
<td><fmt:formatNumber value="${i.importe.netoPendiente}" pattern="#,##0.00"/></td>
</tr></c:forEach></tbody></table></div>
</c:otherwise></c:choose>
<h2>Historial por arriendo</h2>
<c:choose><c:when test="${empty ingresos.detalle}"><p class="empty">No hay arriendos para esta consulta.</p></c:when><c:otherwise>
<div class="table-wrap"><table data-historial-ingresos><thead><tr><th>Reserva / inmueble</th><th>Personas</th><th>Estancia / estado</th><th>Total / neto pagado</th><th>Tasa histórica</th><th>Comisión liquidada / pendiente estimada</th><th>Neto liquidado / pendiente estimado</th><th>Giro / revisión</th></tr></thead><tbody>
<c:forEach var="d" items="${ingresos.detalle}"><tr>
<td>#<c:out value="${d.reservaId}"/><br>#<c:out value="${d.inmuebleId}"/> · <c:out value="${d.titulo}"/></td>
<td>Cliente: <c:out value="${d.cliente}"/><c:if test="${ingresosAdmin}"><br>Propietario: <c:out value="${d.propietario}"/></c:if></td>
<td><c:out value="${d.entrada}"/> → <c:out value="${d.salida}"/><br><c:out value="${d.estado}"/></td>
<td><fmt:formatNumber value="${d.total}" pattern="#,##0.00"/> / <fmt:formatNumber value="${d.netoCobrado}" pattern="#,##0.00"/> <c:out value="${d.moneda}"/></td>
<td><c:choose><c:when test="${empty d.porcentaje}">Sin fijar</c:when><c:otherwise><fmt:formatNumber value="${d.porcentaje}" type="percent" maxFractionDigits="2"/></c:otherwise></c:choose></td>
<td><fmt:formatNumber value="${d.comisionLiquidada}" pattern="#,##0.00"/> / <fmt:formatNumber value="${d.comisionPendiente}" pattern="#,##0.00"/> <c:out value="${d.moneda}"/></td>
<td><fmt:formatNumber value="${d.netoLiquidado}" pattern="#,##0.00"/> / <fmt:formatNumber value="${d.netoPendiente}" pattern="#,##0.00"/> <c:out value="${d.moneda}"/></td>
<td><c:choose><c:when test="${not empty d.giroId}">#<c:out value="${d.giroId}"/> · <c:out value="${d.estadoGiro}"/><br><c:out value="${d.ejecutadoEn}"/></c:when><c:otherwise>Sin giro ejecutado</c:otherwise></c:choose><br><c:out value="${d.motivo}"/></td>
</tr></c:forEach></tbody></table></div>
<nav class="pagination" aria-label="Paginación de ingresos"><p class="muted"><c:out value="${ingresos.totalRentas}"/> arriendos · Página <c:out value="${ingresos.pagina}"/> de <c:out value="${ingresos.paginas}"/></p><div class="actions">
<c:if test="${ingresos.pagina gt 1}"><a class="button secondary" href="${fn:escapeXml(rutaIngresos)}?pagina=${ingresos.pagina-1}${ingresos.filtro gt 0 ? '&amp;inmuebleId=' : ''}${ingresos.filtro gt 0 ? ingresos.filtro : ''}">Anterior</a></c:if>
<c:if test="${ingresos.pagina lt ingresos.paginas}"><a class="button secondary" href="${fn:escapeXml(rutaIngresos)}?pagina=${ingresos.pagina+1}${ingresos.filtro gt 0 ? '&amp;inmuebleId=' : ''}${ingresos.filtro gt 0 ? ingresos.filtro : ''}">Siguiente</a></c:if>
</div></nav>
</c:otherwise></c:choose>
<%@ include file="pie.jspf" %>
