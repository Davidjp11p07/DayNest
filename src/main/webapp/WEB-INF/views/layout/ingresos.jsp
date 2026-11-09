<%@ page pageEncoding="UTF-8" %>
<%@ include file="cabecera.jspf" %>
<h1>${ingresosAdmin ? textos['incomeTitleAdmin'] : textos['incomeTitleOwner']}</h1>
<p class="muted"><c:out value="${textos['ui204']}"/></p>
<c:if test="${ingresos.filtro gt 0}"><p><c:out value="${textos['ui205']}"/><c:out value="${ingresos.filtro}"/>. <a href="${fn:escapeXml(rutaIngresos)}"><c:out value="${textos['ui206']}"/></a></p></c:if>
<c:choose><c:when test="${empty ingresos.totales}"><p class="empty"><c:out value="${textos['ui207']}"/></p></c:when><c:otherwise>
<h2><c:out value="${textos['ui208']}"/> ${ingresos.filtro gt 0 ? textos['incomeSelected'] : textos['incomeAccumulated']}</h2>
<div class="grid">
<c:forEach var="t" items="${ingresos.totales}">
<section class="panel" data-resumen-moneda="${fn:escapeXml(t.moneda)}">
<h3><c:out value="${t.moneda}"/></h3>
<c:choose><c:when test="${ingresosAdmin}">
<p><c:out value="${textos['ui209']}"/></p><p class="price" data-importe="comision-liquidada"><fmt:formatNumber value="${t.comisionLiquidada}" pattern="#,##0.00"/> <c:out value="${t.moneda}"/></p>
<p><c:out value="${textos['ui210']}"/> <strong><fmt:formatNumber value="${t.comisionPendiente}" pattern="#,##0.00"/></strong></p>
<p><c:out value="${textos['ui211']}"/> <fmt:formatNumber value="${t.netoLiquidado}" pattern="#,##0.00"/></p>
<p><c:out value="${textos['ui212']}"/> <fmt:formatNumber value="${t.netoPendiente}" pattern="#,##0.00"/></p>
</c:when><c:otherwise>
<p><c:out value="${textos['ui213']}"/></p><p class="price" data-importe="neto-liquidado"><fmt:formatNumber value="${t.netoLiquidado}" pattern="#,##0.00"/> <c:out value="${t.moneda}"/></p>
<p><c:out value="${textos['ui214']}"/> <strong><fmt:formatNumber value="${t.netoPendiente}" pattern="#,##0.00"/></strong></p>
<p><c:out value="${textos['ui215']}"/> <fmt:formatNumber value="${t.comisionLiquidada}" pattern="#,##0.00"/></p>
<p><c:out value="${textos['ui210']}"/> <fmt:formatNumber value="${t.comisionPendiente}" pattern="#,##0.00"/></p>
</c:otherwise></c:choose>
<p class="muted"><c:out value="${t.reservas}"/> <c:out value="${textos['ui216']}"/> <fmt:formatNumber value="${t.netoCobrado}" pattern="#,##0.00"/> <c:out value="${t.moneda}"/></p>
</section></c:forEach></div>
<h2>${ingresosAdmin ? textos['incomeByProperty'] : textos['incomeProperties']}</h2>
<p class="muted"><c:out value="${textos['ui217']}"/></p>
<div class="table-wrap" style="max-height:32rem;overflow:auto"><table data-resumen-inmuebles><thead style="position:sticky;top:0;background:var(--surface);z-index:1"><tr><th><c:out value="${textos['ui026']}"/></th><th><c:out value="${textos['ui218']}"/></th><th><c:out value="${textos['ui046']}"/></th><th><c:out value="${textos['ui219']}"/></th><th><c:out value="${textos['ui220']}"/></th><th><c:out value="${textos['ui221']}"/></th><th><c:out value="${textos['ui222']}"/></th></tr></thead><tbody>
<c:forEach var="i" items="${ingresos.inmuebles}"><tr>
<td><a href="${fn:escapeXml(rutaIngresos)}?inmuebleId=${i.id}">#<c:out value="${i.id}"/> · <c:out value="${i.titulo}"/></a></td>
<td><c:out value="${i.importe.moneda}"/></td><td><c:out value="${i.importe.reservas}"/></td>
<td><fmt:formatNumber value="${i.importe.comisionLiquidada}" pattern="#,##0.00"/></td>
<td><fmt:formatNumber value="${i.importe.comisionPendiente}" pattern="#,##0.00"/></td>
<td><fmt:formatNumber value="${i.importe.netoLiquidado}" pattern="#,##0.00"/></td>
<td><fmt:formatNumber value="${i.importe.netoPendiente}" pattern="#,##0.00"/></td>
</tr></c:forEach></tbody></table></div>
</c:otherwise></c:choose>
<h2><c:out value="${textos['ui223']}"/></h2>
<c:choose><c:when test="${empty ingresos.detalle}"><p class="empty"><c:out value="${textos['ui224']}"/></p></c:when><c:otherwise>
<div class="table-wrap"><table data-historial-ingresos><thead><tr><th><c:out value="${textos['ui225']}"/></th><th><c:out value="${textos['ui226']}"/></th><th><c:out value="${textos['ui227']}"/></th><th><c:out value="${textos['ui228']}"/></th><th><c:out value="${textos['ui229']}"/></th><th><c:out value="${textos['ui230']}"/></th><th><c:out value="${textos['ui231']}"/></th><th><c:out value="${textos['ui232']}"/></th></tr></thead><tbody>
<c:forEach var="d" items="${ingresos.detalle}"><tr>
<td>#<c:out value="${d.reservaId}"/><br>#<c:out value="${d.inmuebleId}"/> · <c:out value="${d.titulo}"/></td>
<td><c:out value="${textos['ui115']}"/> <c:out value="${d.cliente}"/><c:if test="${ingresosAdmin}"><br><c:out value="${textos['ui117']}"/> <c:out value="${d.propietario}"/></c:if></td>
<td><c:out value="${d.entrada}"/> → <c:out value="${d.salida}"/><br><c:out value="${empty textos[d.estado.toString()] ? d.estado : textos[d.estado.toString()]}"/></td>
<td><fmt:formatNumber value="${d.total}" pattern="#,##0.00"/> / <fmt:formatNumber value="${d.netoCobrado}" pattern="#,##0.00"/> <c:out value="${d.moneda}"/></td>
<td><c:choose><c:when test="${empty d.porcentaje}"><c:out value="${textos['ui233']}"/></c:when><c:otherwise><fmt:formatNumber value="${d.porcentaje}" type="percent" maxFractionDigits="2"/></c:otherwise></c:choose></td>
<td><fmt:formatNumber value="${d.comisionLiquidada}" pattern="#,##0.00"/> / <fmt:formatNumber value="${d.comisionPendiente}" pattern="#,##0.00"/> <c:out value="${d.moneda}"/></td>
<td><fmt:formatNumber value="${d.netoLiquidado}" pattern="#,##0.00"/> / <fmt:formatNumber value="${d.netoPendiente}" pattern="#,##0.00"/> <c:out value="${d.moneda}"/></td>
<td><c:choose><c:when test="${not empty d.giroId}">#<c:out value="${d.giroId}"/> · <c:out value="${d.estadoGiro}"/><br><c:out value="${d.ejecutadoEn}"/></c:when><c:otherwise><c:out value="${textos['ui234']}"/></c:otherwise></c:choose><br><c:out value="${d.motivo}"/></td>
</tr></c:forEach></tbody></table></div>
<nav class="pagination" aria-label="${fn:escapeXml(textos['ui235'])}"><p class="muted"><c:out value="${ingresos.totalRentas}"/> <c:out value="${textos['ui236']}"/> <c:out value="${ingresos.pagina}"/> <c:out value="${textos['ui237']}"/> <c:out value="${ingresos.paginas}"/></p><div class="actions">
<c:if test="${ingresos.pagina gt 1}"><a class="button secondary" href="${fn:escapeXml(rutaIngresos)}?pagina=${ingresos.pagina-1}${ingresos.filtro gt 0 ? '&amp;inmuebleId=' : ''}${ingresos.filtro gt 0 ? ingresos.filtro : ''}"><c:out value="${textos['ui238']}"/></a></c:if>
<c:if test="${ingresos.pagina lt ingresos.paginas}"><a class="button secondary" href="${fn:escapeXml(rutaIngresos)}?pagina=${ingresos.pagina+1}${ingresos.filtro gt 0 ? '&amp;inmuebleId=' : ''}${ingresos.filtro gt 0 ? ingresos.filtro : ''}"><c:out value="${textos['ui239']}"/></a></c:if>
</div></nav>
</c:otherwise></c:choose>
<%@ include file="pie.jspf" %>
