"""Fotos, calificaciones y calendario; solo admite el fixture UUID PrepararContenidoHttp."""
from pathlib import Path
from datetime import datetime, timedelta, timezone
import sys, re, json, subprocess, time
ROOT = Path(__file__).resolve().parents[3]
sys.path.insert(0, str(ROOT / '.tools/daynest-python'))
from playwright.sync_api import sync_playwright

datos = json.loads((ROOT / '.tools/contenido-http.json').read_text())
schema = datos['schema']
assert re.fullmatch(r'renta_http_[a-f0-9]{32}', schema)
cmd = subprocess.check_output(['powershell', '-NoProfile', '-Command',
    '$p=Get-NetTCPConnection -LocalPort 8081 -State Listen; '
    '(Get-CimInstance Win32_Process -Filter "ProcessId=$($p[0].OwningProcess)").CommandLine'], text=True)
assert '-Drenta.db.schema=' + schema in cmd
BASE = 'http://localhost:8081/DayNest'
checks, errores = [], []
with sync_playwright() as pw:
    browser = pw.chromium.launch(executable_path='C:/Program Files/Google/Chrome/Application/chrome.exe', headless=True)
    ctx = browser.new_context(locale='es-CO', timezone_id='America/Bogota')
    ctx.route('**/*', lambda r: r.continue_() if r.request.url.startswith(BASE + '/') else r.abort())
    page = ctx.new_page()
    page.on('pageerror', lambda e: errores.append(str(e)))

    def go(path):
        r = page.goto(BASE + path, wait_until='networkidle')
        assert r.status == 200, (path, r.status)

    def login(correo, password):
        go('/login'); page.locator('[name=correo]').fill(correo)
        page.locator('[name=password]').fill(password)
        page.locator('form[action$="/login"] button[type=submit]').click()
        page.wait_for_load_state('networkidle'); assert '/login' not in page.url

    def salir():
        page.locator('form[action$="/salir"]').evaluate('(f)=>f.requestSubmit()')
        page.wait_for_url('**/login')

    def token():
        return page.locator('[name=csrf]').first.input_value()

    assert ctx.request.get(BASE + '/foto?id=invalido').status == 404
    go('/inmueble?id=' + str(datos['propio']))
    assert page.locator('.gallery [data-photo]').count() == 3
    assert page.locator('.gallery').inner_text().count('Referencia') == 3
    for img in page.locator('.gallery img').all():
        assert img.evaluate('(e)=>e.complete && e.naturalWidth > 0')
    page.locator('[data-photo]').first.click()
    assert page.locator('#galeria-modal').is_visible()
    page.keyboard.press('Escape'); assert not page.locator('#galeria-modal').is_visible()
    checks.append('Interiores locales cargan y se amplían; referencias identificadas')

    login('propietario1@renta.test', 'Propietario2026!')
    go('/propietario/editar?id=' + str(datos['propio']))
    foto_ajena = ctx.request.get(BASE + '/inmueble?id=' + str(datos['ajeno'])).text()
    id_ajena = re.search(r'/foto\?id=(\d+)', foto_ajena).group(1)
    assert ctx.request.get(BASE + '/propietario/foto/ver?id=' + id_ajena).status == 404
    csrf = token()
    r = ctx.request.post(BASE + '/propietario/foto/eliminar', form={
        'csrf': csrf, 'id': str(datos['propio']), 'fotoId': id_ajena})
    assert r.status == 200 and ctx.request.get(BASE + '/foto?id=' + id_ajena).status == 200
    checks.append('Propietario no puede consultar ni eliminar fotos ajenas; error recuperable')

    payload = {'csrf': csrf, 'id': str(datos['propio']), 'tipoFoto': 'INTERIOR',
               'descripcionFoto': 'Interior subido en prueba',
               'foto': {'name': 'interior.jpg', 'mimeType': 'image/jpeg',
                        'buffer': (ROOT / 'src/main/webapp/assets/img/interior-01.jpg').read_bytes()}}
    sin_csrf = dict(payload, csrf='INVALIDO')
    assert ctx.request.post(BASE + '/propietario/foto/subir', multipart=sin_csrf).status == 403
    enorme = dict(payload, foto={'name': 'grande.jpg', 'mimeType': 'image/jpeg', 'buffer': b'x' * 6000001})
    assert ctx.request.post(BASE + '/propietario/foto/subir', multipart=enorme).status == 413
    exceso_archivo = dict(payload, foto={'name': 'exceso.jpg', 'mimeType': 'image/jpeg', 'buffer': b'x' * 5242881})
    respuesta_exceso = ctx.request.post(BASE + '/propietario/foto/subir', multipart=exceso_archivo)
    assert respuesta_exceso.status in (400, 413), (respuesta_exceso.status, respuesta_exceso.text()[:1500])
    invalido = dict(payload, foto={'name': 'falso.jpg', 'mimeType': 'image/jpeg', 'buffer': b'no es una imagen'})
    r = ctx.request.post(BASE + '/propietario/foto/subir', multipart=invalido)
    assert r.status == 200 and 'Solo se admiten' in r.text()
    assert ctx.request.post(BASE + '/propietario/foto/subir', multipart=payload).status == 200
    go('/propietario/editar?id=' + str(datos['propio']))
    assert page.locator('.owner-photo-item').count() == 4
    ultima = page.locator('.owner-photo-item').last
    foto_id = ultima.locator('[name=fotoId]').input_value()
    r = ctx.request.get(BASE + '/foto?id=' + foto_id)
    assert r.status == 200 and r.headers['content-type'].startswith('image/jpeg')
    ultima.get_by_role('button', name='Eliminar foto').click(); page.wait_for_load_state('networkidle')
    assert page.locator('.owner-photo-item').count() == 3
    assert ctx.request.get(BASE + '/foto?id=' + foto_id).status == 404
    checks.append('Subida JPEG, validación de contenido, CSRF, persistencia y eliminación funcionan')

    for _ in range(7):
        assert ctx.request.post(BASE + '/propietario/foto/subir', multipart=payload).status == 200
    r = ctx.request.post(BASE + '/propietario/foto/subir', multipart=payload)
    assert r.status == 200 and 'hasta 10 fotos' in r.text()
    go('/propietario/editar?id=' + str(datos['propio']))
    assert page.locator('.owner-photo-item').count() == 10
    checks.append('Undécima foto rechazada sin perder fotos ni producir HTTP 500')

    go('/propietario/bloqueos')
    inicio = (datetime.now(timezone(timedelta(hours=-5))).date() + timedelta(days=260)).isoformat()
    fin = (datetime.now(timezone(timedelta(hours=-5))).date() + timedelta(days=263)).isoformat()
    page.locator('[name=inmuebleId]').select_option(str(datos['propio']))
    page.locator('[name=entrada]').fill(inicio); page.locator('[name=salida]').fill(fin)
    motivo = 'Mantenimiento prueba general ' + str(time.time_ns())
    page.locator('[name=motivo]').fill(motivo)
    page.get_by_role('button', name='Crear bloqueo').click(); page.wait_for_load_state('networkidle')
    fila = page.locator('tbody tr').filter(has_text=motivo)
    assert fila.count() == 1
    fila.get_by_role('button', name='Liberar').click(); page.wait_for_load_state('networkidle')
    assert 'Liberado' in fila.inner_text()
    checks.append('Calendario registra y libera un bloqueo propio')

    go('/propietario/reservas')
    r = ctx.request.post(BASE + '/propietario/calificar', form={
        'csrf': token(), 'id': str(datos['finalizada']), 'puntuacion': '4', 'comentario': 'Buen huésped'})
    assert r.status == 200
    salir(); login('cliente1@renta.test', 'Cliente2026!')
    go('/cliente/reservas')
    csrf = token()
    for reserva, puntos in [(datos['futura'], '5'), (datos['finalizada'], '6')]:
        r = ctx.request.post(BASE + '/cliente/calificar', form={
            'csrf': csrf, 'id': str(reserva), 'puntuacion': puntos, 'comentario': 'Inválida'})
        assert r.status == 200 and ('calificación' in r.text() or 'No se pudo' in r.text() or 'finalizada' in r.text())
    comentario = '<script>window.reseñaInyectada=true</script>'
    payload = {'csrf': csrf, 'id': str(datos['finalizada']), 'puntuacion': '5', 'comentario': comentario}
    assert ctx.request.post(BASE + '/cliente/calificar', form=payload).status == 200
    assert ctx.request.post(BASE + '/cliente/calificar', form=payload).status == 200
    go('/inmueble?id=' + str(datos['propio']))
    assert page.locator('.review').count() == 1
    assert comentario in page.locator('.review').inner_text()
    assert page.evaluate('window.reseñaInyectada === undefined')
    checks.append('Califican cliente y propietario; se rechazan futuras, fuera de rango y duplicadas; comentario escapado')
    salir(); login('propietario1@renta.test', 'Propietario2026!'); go('/propietario/reservas')
    page.locator('.guest-reviews').first.locator('summary').click()
    assert 'Buen huésped' in page.locator('.guest-reviews').first.inner_text()
    checks.append('Propietario consulta el historial de calificaciones del huésped')
    assert not errores, errores
    browser.close()

(ROOT / '.tools/general-contenido.json').write_text(json.dumps({'checks': checks, 'js': errores}, ensure_ascii=False, indent=2), encoding='utf-8')
for check in checks: print('OK:', check)
print('Comprobaciones:', len(checks))
