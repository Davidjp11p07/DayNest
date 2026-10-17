"""Recorrido destructivo SOLO para el fixture HTTP UUID de transferencias en 8081.

El fixture .tools/transferencias-http.json identifica tres reservas sin pagar:
registrada/ocasional de propietario1@renta.test (ambos perfiles) y sinCuenta de
cliente2@renta.test, que no tiene VINCULA. Incluye banco del catalogo ficticio.
No ejecutar sobre datos operativos ni reutilizar el fixture despues de pagar.
"""
from pathlib import Path
import sys, json, re, subprocess
ROOT = Path(__file__).resolve().parents[3]
sys.path.insert(0, str(ROOT / '.tools/daynest-python'))
from playwright.sync_api import sync_playwright

datos = json.loads((ROOT / '.tools/transferencias-http.json').read_text())
schema = datos['schema']
assert re.fullmatch(r'renta_http_[a-f0-9]{32}', schema)
cmd = subprocess.check_output(['powershell', '-NoProfile', '-Command',
    '$p=Get-NetTCPConnection -LocalPort 8081 -State Listen -ErrorAction Stop; '
    '(Get-CimInstance Win32_Process -Filter "ProcessId=$($p[0].OwningProcess)").CommandLine'], text=True)
assert '-Drenta.db.schema=' + schema in cmd
BASE = 'http://localhost:8081/DayNest'
checks, errors = [], []
with sync_playwright() as pw:
    browser = pw.chromium.launch(executable_path='C:/Program Files/Google/Chrome/Application/chrome.exe', headless=True)
    ctx = browser.new_context(locale='es-CO')
    ctx.route('**/*', lambda r: r.continue_() if r.request.url.startswith('http://localhost:8081/') else r.abort())
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

    login('propietario1@renta.test', 'Propietario2026!')
    go('/cuenta/datos')
    original = {rol: page.locator(f'form[data-cuenta={rol}] [name=numeroCuenta]').input_value()
                for rol in ['CLIENTE', 'PROPIETARIO']}
    go('/cliente/pagar?id=' + str(datos['registrada']))
    assert page.locator('[name=metodo]').count() == 0
    assert page.locator('[name=cuentaPago]').input_value() == 'REGISTRADA'
    assert not page.locator('[data-cuenta-ocasional]').is_visible()
    page.locator('form[data-cuenta-pago] button[type=submit]').click()
    page.wait_for_load_state('networkidle')
    assert '/cliente/reservas' in page.url
    checks.append('Transferencia con cuenta registrada; tarjeta y efectivo retirados')

    go('/cliente/pagar?id=' + str(datos['ocasional']))
    page.locator('[name=cuentaPago]').select_option('OCASIONAL')
    assert page.locator('[data-cuenta-ocasional]').is_visible()
    csrf = page.locator('form[data-cuenta-pago] [name=csrf]').input_value()
    payload = {'csrf': csrf, 'id': str(datos['ocasional']), 'cuentaPago': 'OCASIONAL',
               'bancoId': '99999999', 'numeroCuenta': '000055556666', 'tipoCuenta': 'CORRIENTE'}
    response = ctx.request.post(BASE + '/cliente/pagar', form=payload)
    assert response.status == 200 and 'value="OCASIONAL" selected' in response.text()
    page.reload(wait_until='networkidle')
    checks.append('Banco inexistente rechazado sin confirmar reserva')
    for width in [390, 768, 1440]:
        page.set_viewport_size({'width': width, 'height': 900})
        assert page.evaluate('document.documentElement.scrollWidth <= innerWidth'), width
    checks.append('Formulario sin desbordamiento en 390, 768 y 1440 px')
    page.locator('[name=cuentaPago]').select_option('OCASIONAL')
    page.locator('[name=bancoId]').select_option(str(datos['banco']))
    page.locator('[name=numeroCuenta]').fill('000055556666')
    page.locator('[name=tipoCuenta]').select_option('CORRIENTE')
    page.locator('form[data-cuenta-pago] button[type=submit]').click()
    page.wait_for_load_state('networkidle')
    assert '/cliente/reservas' in page.url
    go('/cuenta/datos')
    for rol, numero in original.items():
        assert page.locator(f'form[data-cuenta={rol}] [name=numeroCuenta]').input_value() == numero
    checks.append('Cuenta ocasional no modifica cliente ni propietario')
    go('/cliente/historial')
    assert 'TRANSFERENCIA' in page.locator('main').inner_text()
    assert '****6666' in page.locator('main').inner_text()
    assert '000055556666' not in page.locator('main').inner_text()
    checks.append('Historial muestra metodo y cuenta usada enmascarada')
    page.locator('form[action$="/salir"]').evaluate('(f)=>f.requestSubmit()')
    page.wait_for_url('**/login')
    login('admin@renta.test', 'Admin2026!')
    go('/admin/pagos')
    assert 'TRANSFERENCIA' in page.locator('main').inner_text()
    assert 'OCASIONAL' in page.locator('main').inner_text()
    assert '****6666' in page.locator('main').inner_text()
    checks.append('Administracion consulta metodo, origen y cuenta historica')

    # Otro contexto comprueba el formulario sin JavaScript y sin cuenta predeterminada.
    simple = browser.new_context(java_script_enabled=False)
    other = simple.new_page()
    other.goto(BASE + '/login')
    other.locator('[name=correo]').fill('cliente2@renta.test')
    other.locator('[name=password]').fill('Cliente2026!')
    other.locator('form[action$="/login"] button[type=submit]').click()
    other.wait_for_load_state('networkidle')
    other.goto(BASE + '/cliente/pagar?id=' + str(datos['sinCuenta']))
    assert other.locator('[name=cuentaPago] option').count() == 1
    other.locator('[name=bancoId]').select_option(str(datos['banco']))
    other.locator('[name=numeroCuenta]').fill('000099991111')
    other.locator('[name=tipoCuenta]').select_option('AHORROS')
    other.locator('form[data-cuenta-pago] button[type=submit]').click()
    other.wait_for_load_state('networkidle')
    assert '/cliente/reservas' in other.url
    other.goto(BASE + '/cuenta/datos')
    assert other.locator('form[data-cuenta=CLIENTE] [name=numeroCuenta]').input_value() == ''
    checks.append('Pago ocasional sin predeterminada y sin JavaScript; no crea cuenta de perfil')
    assert not errors, errors
    browser.close()
for check in checks:
    print('OK:', check)
print('Comprobaciones:', len(checks))
