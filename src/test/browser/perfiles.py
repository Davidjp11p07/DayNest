"""Prueba web de perfiles; requiere Jetty 8081 con esquema HTTP UUID aislado."""
from pathlib import Path
import sys,re,subprocess,json
ROOT=Path(__file__).resolve().parents[3]
sys.path.insert(0,str(ROOT/'.tools/daynest-python'))
from playwright.sync_api import sync_playwright
BASE='http://localhost:8081/DayNest'
schema=(ROOT/'.tools/perfiles-http-schema.txt').read_text().strip()
assert re.fullmatch(r'renta_http_[a-f0-9]{32}',schema)
cmd=subprocess.check_output(['powershell','-NoProfile','-Command',"$listener=Get-NetTCPConnection -LocalPort 8081 -State Listen -ErrorAction Stop; (Get-CimInstance Win32_Process -Filter \"ProcessId=$($listener[0].OwningProcess)\").CommandLine"],text=True)
assert '-Drenta.db.schema='+schema in cmd,'Se rechazan pruebas contra la base operativa'
checks=[]
with sync_playwright() as pw:
    browser=pw.chromium.launch(executable_path='C:/Program Files/Google/Chrome/Application/chrome.exe',headless=True)
    for correo,password,perfil,boton in [
        ('cliente1@renta.test','Cliente2026!','PROPIETARIO','Activar perfil propietario'),
        ('propietario2@renta.test','Propietario2026!','CLIENTE','Activar perfil cliente')]:
        context=browser.new_context(viewport={'width':390,'height':844})
        context.route('**/*',lambda route:route.continue_() if route.request.url.startswith('http://localhost:8081/') else route.abort())
        page=context.new_page()
        page.goto(BASE+'/login');page.locator('[name=correo]').fill(correo);page.locator('[name=password]').fill(password)
        page.locator('form[action$="/login"] button[type=submit]').click();page.wait_for_load_state('networkidle')
        assert page.goto(BASE+'/cuenta/perfiles').status==200
        assert page.get_by_role('button',name=boton).count()==1
        token=page.locator('input[name=csrf]').first.get_attribute('value')
        anterior=next(c['value'] for c in context.cookies() if c['name']=='JSESSIONID')
        bad=context.request.post(BASE+'/cuenta/perfiles',form={'perfil':perfil,'csrf':'INVALIDO'},max_redirects=0)
        assert bad.status==403
        checks.append(perfil+': CSRF incorrecto rechazado')
        bad=context.request.post(BASE+'/cuenta/perfiles',form={'perfil':'ADMINISTRADOR','csrf':token},max_redirects=0)
        assert bad.status==303
        page.goto(BASE+'/cuenta/perfiles');assert page.get_by_role('button',name=boton).count()==1
        assert context.request.get(BASE+'/admin').status==403
        checks.append(perfil+': petición de administrador rechazada sin otorgar permisos')
        page.get_by_role('button',name=boton).click();page.wait_for_load_state('networkidle')
        assert page.url==BASE+'/cuenta/perfiles'
        assert 'Ya tienes ambos perfiles activos.' in page.locator('main').inner_text()
        assert page.get_by_role('button',name='Activar perfil',exact=False).count()==0
        despues=next(c['value'] for c in context.cookies() if c['name']=='JSESSIONID')
        assert anterior!=despues
        checks.append(perfil+': activación por formulario, sesión renovada y ambos perfiles visibles')
        for ruta in ['/cliente/reservas','/propietario/inmuebles']:
            assert page.goto(BASE+ruta).status==200
        assert context.request.get(BASE+'/admin').status==403
        checks.append(perfil+': acceso inmediato a cliente/propietario, administración denegada')
        assert page.goto(BASE+'/cuenta/perfiles').status==200
        repeated=context.request.post(BASE+'/cuenta/perfiles',form={'perfil':perfil,'csrf':token},max_redirects=0)
        assert repeated.status==303
        page.goto(BASE+'/cuenta/perfiles');assert 'Ya tienes ambos perfiles activos.' in page.locator('main').inner_text()
        page.screenshot(path=str(ROOT/f'.tools/perfiles-{perfil.lower()}-390.png'),full_page=True)
        assert page.evaluate('document.documentElement.scrollWidth <= innerWidth')
        checks.append(perfil+': repetición idempotente y pantalla móvil sin desbordamiento')
        context.close()
    context=browser.new_context();page=context.new_page()
    page.goto(BASE+'/cuenta/perfiles');assert page.url.endswith('/login')
    checks.append('Sin sesión: redirección a login')
    browser.close()
(ROOT/'.tools/verificacion-perfiles-http.json').write_text(json.dumps({'checks':checks},ensure_ascii=False,indent=2),encoding='utf-8')
print(f'OK: {len(checks)} comprobaciones web en esquema temporal.')
