"""Pago inmediato en un fixture HTTP UUID. PrepararTransferenciasHttp.java lo crea.
El puerto se configura mediante DAYNEST_HTTP_PORT (8081 por defecto).
Cada ejecucion consume periodos del fixture: crear uno nuevo para repetir.
"""
from pathlib import Path
from datetime import datetime, timedelta, timezone
import os, sys, json, re, subprocess
ROOT = Path(__file__).resolve().parents[3]
sys.path.insert(0, str(ROOT / '.tools/daynest-python'))
from playwright.sync_api import sync_playwright

datos = json.loads((ROOT / '.tools/transferencias-http.json').read_text())
schema = datos['schema']
assert re.fullmatch(r'renta_http_[a-f0-9]{32}', schema)
port = int(os.environ.get('DAYNEST_HTTP_PORT', '8081'))
assert 1024 <= port <= 65535
cmd = subprocess.check_output(['powershell', '-NoProfile', '-Command',
    f'$p=Get-NetTCPConnection -LocalPort {port} -State Listen -ErrorAction Stop; '
    '(Get-CimInstance Win32_Process -Filter "ProcessId=$($p[0].OwningProcess)").CommandLine'], text=True)
assert '-Drenta.db.schema=' + schema in cmd
BASE = f'http://localhost:{port}/DayNest'
checks, errors = [], []
hoy = datetime.now(timezone(timedelta(hours=-5))).date()
with sync_playwright() as pw:
    browser = pw.chromium.launch(executable_path='C:/Program Files/Google/Chrome/Application/chrome.exe', headless=True)
    ctx = browser.new_context(locale='es-CO')
    ctx.route('**/*', lambda r: r.continue_() if r.request.url.startswith(BASE + '/') else r.abort())
    page = ctx.new_page()
    page.on('pageerror', lambda e: errors.append(str(e)))

    def go(path):
        response = page.goto(BASE + path, wait_until='networkidle')
        assert response.status == 200, (path, response.status)

    def login(email, password):
        go('/login')
        page.locator('[name=correo]').fill(email)
        page.locator('[name=password]').fill(password)
        page.locator('form[action$="/login"] button[type=submit]').click()
        page.wait_for_load_state('networkidle')
        assert '/login' not in page.url

    def preparar(dias):
        go('/cliente/reservar?id=' + str(datos['inmueble']))
        page.locator('[name=entrada]').fill((hoy + timedelta(days=dias)).isoformat())
        page.locator('[name=salida]').fill((hoy + timedelta(days=dias+3)).isoformat())
        page.locator('[name=huespedes]').fill('1')

    def confirmar():
        page.locator('form[data-cuenta-pago] button[type=submit]').click()
        page.wait_for_load_state('networkidle')
        assert '/cliente/reservas' in page.url
        assert 'Pago realizado' in page.locator('main').inner_text()

    login('propietario1@renta.test', 'Propietario2026!')
    go('/cuenta/datos')
    original = {rol: page.locator(f'form[data-cuenta={rol}] [name=numeroCuenta]').input_value()
                for rol in ['CLIENTE', 'PROPIETARIO']}
    preparar(90)
    assert page.locator('[name=cuentaPago]').input_value() == 'REGISTRADA'
    assert not page.locator('[data-cuenta-ocasional]').is_visible()
    confirmar()
    checks.append('Reservar cobra y confirma de inmediato con cuenta registrada')

    preparar(100)
    page.locator('[name=cuentaPago]').select_option('OCASIONAL')
    csrf = page.locator('form[data-cuenta-pago] [name=csrf]').input_value()
    payload = {'csrf': csrf, 'inmuebleId': str(datos['inmueble']), 'cuentaPago': 'OCASIONAL',
               'entrada': (hoy+timedelta(days=100)).isoformat(), 'salida': (hoy+timedelta(days=103)).isoformat(),
               'huespedes': '1', 'bancoId': '99999999', 'numeroCuenta': '000055556666', 'tipoCuenta': 'CORRIENTE'}
    response = ctx.request.post(BASE + '/cliente/reservar', form=payload)
    assert response.status == 200 and 'Pago/reserva fallado' in response.text()
    preparar(100)
    checks.append('Banco inexistente falla sin dejar reserva ni retener fechas')
    for width in [390, 768, 1440]:
        page.set_viewport_size({'width': width, 'height': 900})
        assert page.evaluate('document.documentElement.scrollWidth <= innerWidth'), width
    checks.append('Reserva con pago sin desbordamiento en 390, 768 y 1440 px')
    page.locator('[name=cuentaPago]').select_option('OCASIONAL')
    page.locator('[name=bancoId]').select_option(str(datos['banco']))
    page.locator('[name=numeroCuenta]').fill('000055556666')
    page.locator('[name=tipoCuenta]').select_option('CORRIENTE')
    confirmar()
    go('/cuenta/datos')
    for rol, numero in original.items():
        assert page.locator(f'form[data-cuenta={rol}] [name=numeroCuenta]').input_value() == numero
    checks.append('Reintento con cuenta ocasional confirma y conserva ambas cuentas de perfil')
    go('/cliente/historial')
    assert 'TRANSFERENCIA' in page.locator('main').inner_text()
    assert '****6666' in page.locator('main').inner_text()
    assert '000055556666' not in page.locator('main').inner_text()
    go('/cliente/pagar?id=1')
    assert page.url.endswith('/cliente/reservas')
    checks.append('Historial enmascarado y ruta antigua sin pantalla de pago')
    page.locator('form[action$="/salir"]').evaluate('(f)=>f.requestSubmit()')
    page.wait_for_url('**/login')
    login('admin@renta.test', 'Admin2026!')
    go('/admin/pagos')
    assert 'TRANSFERENCIA' in page.locator('main').inner_text()
    assert 'OCASIONAL' in page.locator('main').inner_text()
    assert '****6666' in page.locator('main').inner_text()
    checks.append('Administracion conserva metodo y cuenta historica')

    simple = browser.new_context(java_script_enabled=False)
    other = simple.new_page()
    other.goto(BASE + '/login')
    other.locator('[name=correo]').fill('cliente2@renta.test')
    other.locator('[name=password]').fill('Cliente2026!')
    other.locator('form[action$="/login"] button[type=submit]').click()
    other.wait_for_load_state('networkidle')
    other.goto(BASE + '/cliente/reservar?id=' + str(datos['inmueble']))
    other.locator('[name=entrada]').fill((hoy+timedelta(days=110)).isoformat())
    other.locator('[name=salida]').fill((hoy+timedelta(days=113)).isoformat())
    other.locator('[name=huespedes]').fill('1')
    assert other.locator('[name=cuentaPago] option').count() == 1
    other.locator('[name=bancoId]').select_option(str(datos['banco']))
    other.locator('[name=numeroCuenta]').fill('000099991111')
    other.locator('[name=tipoCuenta]').select_option('AHORROS')
    other.locator('form[data-cuenta-pago] button[type=submit]').click()
    other.wait_for_load_state('networkidle')
    assert '/cliente/reservas' in other.url
    assert 'Pago realizado' in other.locator('main').inner_text()
    other.goto(BASE + '/cuenta/datos')
    assert other.locator('form[data-cuenta=CLIENTE] [name=numeroCuenta]').input_value() == ''
    checks.append('Reserva pagada sin JavaScript ni cuenta predeterminada; no crea cuenta de perfil')
    assert not errors, errors
    browser.close()
for check in checks:
    print('OK:', check)
print('Comprobaciones:', len(checks))
