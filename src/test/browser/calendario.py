"""Calendarios de cliente/propietario: fixture PrepararDisponibilidadHttp y Jetty 8081."""
from pathlib import Path
from datetime import date, timedelta
import json, re, subprocess, sys
ROOT = Path(__file__).resolve().parents[3]
sys.path.insert(0, str(ROOT / '.tools/daynest-python'))
from playwright.sync_api import sync_playwright
datos = json.loads((ROOT / '.tools/disponibilidad-http.json').read_text())
assert re.fullmatch(r'renta_http_[a-f0-9]{32}', datos['schema'])
cmd = subprocess.check_output(['powershell', '-NoProfile', '-Command',
 '$p=Get-NetTCPConnection -LocalPort 8081 -State Listen; '
 '(Get-CimInstance Win32_Process -Filter "ProcessId=$($p[0].OwningProcess)").CommandLine'], text=True)
assert '-Drenta.db.schema=' + datos['schema'] in cmd
BASE = 'http://localhost:8081/DayNest'
inicio = date.fromisoformat(datos['inicio'])
mes = inicio.strftime('%Y-%m')
detalle = '/inmueble?id=' + str(datos['inmueble']) + '&mes=' + mes
propio = '/propietario/calendario?id=' + str(datos['inmueble'])
checks, errores = [], []
with sync_playwright() as pw:
    browser = pw.chromium.launch(executable_path='C:/Program Files/Google/Chrome/Application/chrome.exe', headless=True)
    ctx = browser.new_context(locale='es-CO', timezone_id='America/Bogota')
    page = ctx.new_page(); page.on('pageerror', lambda e: errores.append(str(e)))
    def go(path):
        r = page.goto(BASE + path, wait_until='networkidle'); assert r.status == 200, (path, r.status)
    def day(fecha): return page.locator('[data-calendar-date="' + str(fecha) + '"]')
    def estado(fecha): return day(fecha).get_attribute('data-estado')
    def login(correo, password):
        go('/login'); page.locator('[name=correo]').fill(correo); page.locator('[name=password]').fill(password)
        page.locator('form[action$="/login"] button[type=submit]').click(); page.wait_for_load_state('networkidle')
        assert '/login' not in page.url
    def salir():
        page.locator('form[action$="/salir"]').evaluate('(f)=>f.requestSubmit()'); page.wait_for_url('**/login')

    go(detalle)
    assert estado(inicio) == 'RESERVADO'
    assert estado(date.fromisoformat(datos['salida'])) == 'DISPONIBLE'
    assert 'Motivo privado no publicable' not in page.locator('main').inner_text()
    assert page.locator('.unavailable-dates').count() == 0
    day(inicio - timedelta(days=2)).click(); day(inicio).click()
    assert page.locator('[data-entrada]').input_value() == str(inicio - timedelta(days=2))
    assert page.locator('[data-salida]').input_value() == str(inicio)
    checks.append('Cliente ve calendario y elige rango libre con salida al empezar una reserva')
    day(inicio - timedelta(days=1)).click(); day(inicio + timedelta(days=1)).click()
    assert page.locator('[data-salida]').input_value() == ''
    assert 'incluye noches' in page.locator('[data-calendar-status]').inner_text()
    page.locator('[data-entrada]').fill(str(inicio)); page.locator('[data-salida]').fill(datos['salida'])
    assert not page.locator('[data-entrada]').evaluate('(e)=>e.checkValidity()')
    checks.append('Calendario y fechas escritas rechazan noches reservadas o bloqueadas')
    page.locator('[data-entrada]').fill(str(inicio - timedelta(days=2)));page.locator('[data-salida]').fill(str(inicio))
    page.get_by_role('link', name='Mes siguiente', exact=True).click(); page.wait_for_load_state('networkidle')
    assert page.locator('[data-entrada]').input_value() == str(inicio - timedelta(days=2))
    assert page.locator('[data-salida]').input_value() == str(inicio)
    checks.append('Cambiar de mes conserva las fechas elegidas')
    for width in [390, 768, 1440]:
        page.set_viewport_size({'width': width, 'height': 950});go(detalle)
        assert page.evaluate('document.documentElement.scrollWidth <= innerWidth')
        if width == 390: page.locator('.availability').screenshot(path=str(ROOT / '.tools/calendario-cliente-390.png'))
    checks.append('Calendario de cliente sin desbordamiento en móvil, tableta y escritorio')

    login('propietario1@renta.test', 'Propietario2026!'); go('/propietario/inmuebles')
    assert page.get_by_role('link', name='Calendario', exact=True).count() > 0
    go(propio + '&mes=' + mes); assert estado(inicio) == 'RESERVADO'
    page.screenshot(path=str(ROOT / '.tools/calendario-propietario-1440.png'), full_page=True)
    csrf = page.locator('[name=csrf]').first.input_value()
    r = ctx.request.post(BASE + '/propietario/bloquear', form={'csrf':csrf,'inmuebleId':str(datos['inmueble']),
        'entrada':datos['inicio'],'salida':datos['salida'],'motivo':'Intento sobre reserva', 'volverCalendario':'true','mes':mes})
    assert r.status == 200 and '/propietario/calendario' in r.url
    go(propio + '&mes=' + mes); assert estado(inicio) == 'RESERVADO'
    checks.append('Propietario accede desde Mis inmuebles y no puede bloquear una reserva activa')

    nuevo_mes = (inicio.replace(day=28) + timedelta(days=4)).replace(day=1)
    desde = nuevo_mes + timedelta(days=4); hasta = desde + timedelta(days=3)
    go(propio + '&mes=' + desde.strftime('%Y-%m'))
    day(desde).click(); day(hasta).click()
    assert page.locator('[data-calendar-start]').input_value() == str(desde)
    assert page.locator('[data-calendar-end]').input_value() == str(hasta)
    page.locator('[name=motivo]').fill('Bloqueo desde calendario')
    page.get_by_role('button', name='Bloquear fechas', exact=True).click();page.wait_for_load_state('networkidle')
    assert '/propietario/calendario' in page.url and 'mes=' + desde.strftime('%Y-%m') in page.url
    assert estado(desde) == 'BLOQUEADO' and estado(hasta) == 'DISPONIBLE'
    page.locator('.calendar-blocks li').filter(has_text='Bloqueo desde calendario').get_by_role('button',name='Liberar bloqueo').click()
    page.wait_for_load_state('networkidle');assert estado(desde) == 'DISPONIBLE'
    checks.append('Propietario selecciona, bloquea y libera un rango manteniendo el mes')
    for width in [390,768,1440]:
        page.set_viewport_size({'width':width,'height':950});assert page.evaluate('document.documentElement.scrollWidth <= innerWidth')
    checks.append('Calendario y formulario del propietario se adaptan a tres anchuras')
    salir();login('propietario2@renta.test','Propietario2026!')
    r=ctx.request.get(BASE+propio);assert r.status == 400 and 'no pertenece' in r.text()
    salir();login('cliente1@renta.test','Cliente2026!')
    assert ctx.request.get(BASE+propio).status == 403
    csrf=page.locator('[name=csrf]').first.input_value()
    assert ctx.request.post(BASE+'/cliente/cancelar',form={'csrf':csrf,'id':str(datos['reserva'])}).status == 200
    go(detalle);assert estado(inicio) == 'DISPONIBLE'
    checks.append('Permisos de propietario y cliente correctos; cancelar libera el calendario')

    simple=browser.new_context(java_script_enabled=False,viewport={'width':390,'height':844});p=simple.new_page()
    assert p.goto(BASE+detalle,wait_until='networkidle').status == 200
    assert p.locator('.calendar-day').count() >= 28
    p.get_by_role('link',name='Mes siguiente',exact=True).click();p.wait_for_load_state('networkidle')
    assert p.locator('.calendar-day').count() >= 28 and 'mes=' in p.url
    checks.append('Calendario y navegación mensual funcionan sin JavaScript')
    assert not errores, errores
    browser.close()
(ROOT / '.tools/calendario-navegador.json').write_text(json.dumps({'checks':checks,'js':errores},ensure_ascii=False,indent=2),encoding='utf-8')
for check in checks:print('OK:',check)
print('Comprobaciones:',len(checks))
