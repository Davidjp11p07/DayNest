<%@ page pageEncoding="UTF-8" %>
<%@ include file="../layout/cabecera.jspf" %>
<section class="panel narrow">
<p class="alert">Transferencia bancaria simulada: no se mueve dinero real. La cuenta elegida se conserva en este pago; no cambia tu cuenta predeterminada.</p>
<h1>Pagar por transferencia</h1>
<p>Reserva #<c:out value="${reserva.id}"/> · <c:out value="${reserva.inmueble.titulo}"/></p>
<div class="total">Total: <fmt:formatNumber value="${reserva.total}" pattern="#,##0.00"/> <c:out value="${reserva.moneda}"/></div>
<p>Pagado: <fmt:formatNumber value="${resumen.totalPagado}" pattern="#,##0.00"/> · Saldo: <fmt:formatNumber value="${resumen.saldo}" pattern="#,##0.00"/> <c:out value="${reserva.moneda}"/></p>
<form method="post" action="${fn:escapeXml(pageContext.request.contextPath)}/cliente/pagar" class="form-stack" data-cuenta-pago>
<input type="hidden" name="csrf" value="${fn:escapeXml(sessionScope.csrf)}">
<input type="hidden" name="id" value="${fn:escapeXml(reserva.id)}">
<div class="field"><label for="cuentaPago">Cuenta para este pago</label>
<select id="cuentaPago" name="cuentaPago" required>
<c:if test="${not empty cuentaRegistrada}"><option value="REGISTRADA" ${form.cuentaPago ne 'OCASIONAL' ? 'selected' : ''}>Usar mi cuenta registrada</option></c:if>
<option value="OCASIONAL" ${form.cuentaPago eq 'OCASIONAL' or empty cuentaRegistrada ? 'selected' : ''}>Usar otra cuenta solo para este pago</option>
</select></div>
<c:if test="${not empty cuentaRegistrada}">
<p data-cuenta-registrada>
<c:forEach var="b" items="${bancos}"><c:if test="${b.id eq cuentaRegistrada.bancoId}"><c:out value="${b.etiqueta}"/></c:if></c:forEach>
· <c:out value="${cuentaRegistrada.tipoCuenta}"/> · termina en <c:out value="${fn:substring(cuentaRegistrada.numeroCuenta,fn:length(cuentaRegistrada.numeroCuenta)-4,fn:length(cuentaRegistrada.numeroCuenta))}"/>
</p></c:if>
<c:if test="${empty cuentaRegistrada}"><p class="muted">No tienes una cuenta registrada. Puedes indicar una para este pago sin guardarla en tu perfil.</p></c:if>
<fieldset data-cuenta-ocasional><legend>Otra cuenta bancaria</legend>
<p class="muted">Completa estos datos si elegiste otra cuenta. Se usarán únicamente en este pago y su eventual devolución.</p>
<div class="field"><label for="bancoId">Banco</label><select id="bancoId" name="bancoId"><option value="">Selecciona un banco y país</option><c:forEach var="b" items="${bancos}"><option value="${b.id}" ${form.bancoId eq b.id ? 'selected' : ''}><c:out value="${b.etiqueta}"/></option></c:forEach></select></div>
<div class="field"><label for="numeroCuenta">Número de cuenta</label><input id="numeroCuenta" name="numeroCuenta" type="text" inputmode="numeric" pattern="[0-9]{4,34}" minlength="4" maxlength="34" autocomplete="off" value="${fn:escapeXml(form.numeroCuenta)}"></div>
<div class="field"><label for="tipoCuenta">Tipo de cuenta</label><select id="tipoCuenta" name="tipoCuenta"><option value="">Selecciona un tipo</option><option value="AHORROS" ${form.tipoCuenta eq 'AHORROS' ? 'selected' : ''}>Ahorros</option><option value="CORRIENTE" ${form.tipoCuenta eq 'CORRIENTE' ? 'selected' : ''}>Corriente</option></select></div>
</fieldset>
<div class="field"><label for="referencia">Referencia (opcional)</label><input id="referencia" name="referencia" type="text" maxlength="120" value="${fn:escapeXml(form.referencia)}"></div>
<p class="muted">Al confirmar se registra el cobro simulado y se confirma la reserva. Para cambiar tu cuenta predeterminada utiliza Mis datos y cuentas.</p>
<button type="submit">Registrar transferencia y confirmar</button>
</form></section>
<%@ include file="../layout/pie.jspf" %>
