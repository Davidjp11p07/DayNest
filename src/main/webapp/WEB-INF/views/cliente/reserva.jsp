<%@ page pageEncoding="UTF-8" %>
<%@ include file="../layout/cabecera.jspf" %>
<link rel="stylesheet" href="${fn:escapeXml(pageContext.request.contextPath)}/assets/reserva-detalle.css">
<a class="breadcrumb" href="${fn:escapeXml(pageContext.request.contextPath)}/cliente/reservas"><c:out value="${textos['ui071']}"/></a>
<header class="booking-heading">
<h1><c:out value="${textos['ui072']}"/><c:out value="${reserva.id}"/></h1>
<span class="badge" data-estado="${fn:escapeXml(reserva.estado)}"><c:out value="${empty textos[reserva.estado.toString()] ? reserva.estado : textos[reserva.estado.toString()]}"/></span>
</header>
<div class="booking-layout">
<article class="booking-property panel">
<c:choose><c:when test="${not empty fotosReserva}"><img class="booking-photo" src="${fn:escapeXml(pageContext.request.contextPath)}/foto?id=${fn:escapeXml(fotosReserva[0].id)}" alt="${fn:escapeXml(reserva.inmueble.titulo)}"></c:when><c:otherwise><div class="booking-photo booking-no-photo"><c:out value="${textos['ui073']}"/></div></c:otherwise></c:choose>
<h2><c:out value="${reserva.inmueble.titulo}"/></h2>
<p class="muted"><c:out value="${reserva.inmueble.ciudad}"/> &middot; <c:out value="${reserva.inmueble.direccion}"/></p>
<dl class="booking-facts">
<div><dt><c:out value="${textos['ui074']}"/></dt><dd><strong><c:out value="${entradaReserva}"/></strong><small><c:out value="${reserva.periodo.entrada}"/></small></dd></div>
<div><dt><c:out value="${textos['ui075']}"/></dt><dd><strong><c:out value="${salidaReserva}"/></strong><small><c:out value="${reserva.periodo.salida}"/></small></dd></div>
<div><dt><c:out value="${textos['ui076']}"/></dt><dd><c:out value="${reserva.huespedes}"/> <c:choose><c:when test="${reserva.huespedes eq 1}"><c:out value="${textos['ui077']}"/></c:when><c:otherwise><c:out value="${textos['ui078']}"/></c:otherwise></c:choose></dd></div>
<div><dt><c:out value="${textos['ui014']}"/></dt><dd><c:out value="${empty textos[reserva.estado.toString()] ? reserva.estado : textos[reserva.estado.toString()]}"/></dd></div>
<div><dt><c:out value="${textos['ui079']}"/></dt><dd>#<c:out value="${reserva.id}"/></dd></div>
<div><dt><c:out value="${textos['ui080']}"/></dt><dd><c:out value="${reserva.cliente.nombre}"/></dd></div>
</dl>
<c:if test="${(reserva.estado eq 'PENDIENTE_PAGO' or reserva.estado eq 'CONFIRMADA') and reserva.periodo.entrada.isAfter(hoy)}">
<form method="post" action="${fn:escapeXml(pageContext.request.contextPath)}/cliente/cancelar"><input type="hidden" name="csrf" value="${fn:escapeXml(sessionScope.csrf)}"><input type="hidden" name="id" value="${fn:escapeXml(reserva.id)}"><button type="submit" class="danger"><c:out value="${textos['ui081']}"/></button></form>
</c:if>
</article>
<aside class="booking-summary panel" aria-labelledby="booking-summary-title">
<h2 id="booking-summary-title"><c:out value="${textos['ui082']}"/></h2>
<dl>
<div><dt><c:out value="${textos['ui083']}"/></dt><dd><fmt:formatNumber value="${reserva.precioPorNoche}" pattern="#,##0.00"/> <c:out value="${reserva.moneda}"/></dd></div>
<div><dt><c:out value="${textos['ui084']}"/></dt><dd><c:out value="${resumen.noches}"/></dd></div>
<div class="booking-total"><dt><c:out value="${textos['ui085']}"/></dt><dd><fmt:formatNumber value="${reserva.total}" pattern="#,##0.00"/> <c:out value="${reserva.moneda}"/></dd></div>
<div><dt><c:out value="${textos['ui086']}"/></dt><dd><fmt:formatNumber value="${resumen.totalPagado}" pattern="#,##0.00"/> <c:out value="${reserva.moneda}"/></dd></div>
<div><dt><c:out value="${textos['ui087']}"/></dt><dd><fmt:formatNumber value="${resumen.saldo}" pattern="#,##0.00"/> <c:out value="${reserva.moneda}"/></dd></div>
</dl>
<p class="booking-notice"><c:out value="${textos['ui088']}"/></p>
<p class="muted"><c:out value="${textos['ui089']}"/></p>
</aside>
</div>
<%@ include file="../layout/pie.jspf" %>
