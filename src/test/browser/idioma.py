"""Bilingual UI smoke test against the isolated availability fixture on port 8081."""
from pathlib import Path
import json, sys

root=Path(__file__).resolve().parents[3]
sys.path.insert(0,str(root/'.tools/daynest-python'))
from playwright.sync_api import sync_playwright

base='http://localhost:8081/DayNest'
fixture=json.loads((root/'.tools/disponibilidad-http.json').read_text())

with sync_playwright() as p:
    browser=p.chromium.launch(executable_path='C:/Program Files/Google/Chrome/Application/chrome.exe',headless=True)
    context=browser.new_context(viewport={'width':1440,'height':1000})
    page=context.new_page()
    errors=[]
    page.on('pageerror',lambda error:errors.append(str(error)))
    assert page.goto(base+'/inicio').status==200
    assert page.locator('html').get_attribute('lang')=='es'
    assert page.locator('#titulo-inicio').inner_text()=='Tu espacio,\ntus días.'
    page.locator('#daynest-language').select_option('en')
    page.wait_for_url('**lang=en')
    assert page.locator('html').get_attribute('lang')=='en'
    assert page.locator('#titulo-inicio').inner_text()=='Your space,\nyour days.'
    assert page.locator('.brand-word').first.inner_text()=='DayNest'
    assert page.goto(base+'/buscar?capacidad=2').status==200
    assert page.locator('html').get_attribute('lang')=='en'
    page.locator('[name=minimo]').fill('100')
    page.locator('#daynest-language').select_option('es')
    page.wait_for_url('**lang=es')
    assert 'minimo=100' in page.url and 'capacidad=2' in page.url
    assert page.locator('[name=minimo]').input_value()=='100'
    assert page.goto(base+'/inmueble?id='+str(fixture['inmueble'])+'&lang=en').status==200
    assert page.locator('.calendar-status').inner_text().startswith('Choose check-in')
    assert page.locator('.calendar-weekdays').inner_text().startswith('Mon')
    page.goto(base+'/login')
    page.locator('[name=correo]').fill('cliente1@renta.test')
    page.locator('[name=password]').fill('incorrect-password')
    page.locator('form[action$="/login"] button[type=submit]').click()
    assert page.locator('.alert.error').inner_text()=='Incorrect email or password.'
    page.locator('[name=correo]').fill('cliente1@renta.test')
    page.locator('[name=password]').fill('Cliente2026!')
    page.locator('form[action$="/login"] button[type=submit]').click()
    page.wait_for_url('**/cliente/reservas')
    assert page.locator('h1').inner_text()=='My bookings'
    assert page.locator('.alert').inner_text().startswith('Welcome, ')
    row=page.locator('[data-reserva-id="'+str(fixture['reserva'])+'"]')
    assert row.count()==1
    row.get_by_role('link',name='View booking').click()
    assert page.locator('h1').inner_text().startswith('Your booking #')
    assert page.get_by_text('Booking reference',exact=True).count()==1
    assert page.get_by_text('Guests',exact=True).count()==1
    assert page.locator('.booking-summary').inner_text().find('COP')>=0
    page.screenshot(path=str(root/'.tools/reserva-idioma-en-1440.png'),full_page=True)
    assert page.goto(base+'/cliente/reserva?id=99999999').status==404
    assert page.locator('h1').inner_text()=='We could not find this page'
    page.goto(base+'/cliente/reservas?lang=es')
    assert page.locator('h1').inner_text()=='Mis reservas'
    page.set_viewport_size({'width':390,'height':844})
    page.goto(base+'/inicio')
    page.locator('.nav-toggle').click()
    assert page.locator('#daynest-language').is_visible()
    page.locator('#daynest-language').select_option('en')
    page.wait_for_url('**lang=en')
    assert page.evaluate('document.documentElement.scrollWidth<=innerWidth')
    page.screenshot(path=str(root/'.tools/idioma-en-390.png'),full_page=True)
    assert not errors,errors
    context.close()
    for email,password,routes in [
        ('propietario1@renta.test','Propietario2026!',[
            '/propietario/inmuebles','/propietario/publicar','/propietario/reservas',
            '/propietario/ingresos','/propietario/giros','/cuenta/datos','/cuenta/perfiles',
            '/propietario/calendario?id='+str(fixture['inmueble']),
            '/propietario/editar?id='+str(fixture['inmueble'])]),
        ('admin@renta.test','Admin2026!',[
            '/admin','/admin/bancos','/admin/inmuebles','/admin/reservas',
            '/admin/pagos','/admin/giros','/admin/ingresos'])]:
        context=browser.new_context()
        page=context.new_page()
        page.goto(base+'/login?lang=en')
        page.locator('[name=correo]').fill(email)
        page.locator('[name=password]').fill(password)
        page.locator('form[action$="/login"] button[type=submit]').click()
        page.wait_for_url('**/'+('admin' if email.startswith('admin') else 'propietario/inmuebles'))
        for route in routes:
            for lang in ['en','es']:
                response=page.goto(base+route+('&' if '?' in route else '?')+'lang='+lang)
                assert response.status==200,(route,lang,response.status)
                assert page.locator('html').get_attribute('lang')==lang
                assert page.locator('h1').inner_text().strip(),(route,lang)
                assert page.locator('.brand-word').first.inner_text()=='DayNest'
        context.close()
    # The selector also works without JS.
    context=browser.new_context(java_script_enabled=False)
    page=context.new_page()
    page.goto(base+'/login')
    page.locator('#daynest-language').select_option('en')
    page.locator('.language-picker button').click()
    assert page.locator('html').get_attribute('lang')=='en'
    assert page.locator('h1').inner_text()=='Good to see you again.'
    browser.close()
print('PASS: ES/EN, persistence, filters, calendar, booking, access control, mobile and no JS')
