-- OPCIONAL. Datos ficticios academicos; ejecutar solamente por decision explicita.
-- Requiere schema.sql final y, para completar perfiles/inmuebles, data.sql.
-- No modifica cuentas existentes ni propiedades de usuarios ajenos al demo.
BEGIN;
INSERT INTO paises(nom_ps,codigo_iso2) VALUES('Pais ficticio DayNest','XX') ON CONFLICT DO NOTHING;
INSERT INTO division_administrativa(id_pais,nombre,codigo_oficial)
 SELECT id_ps,'Region ficticia','DEMO' FROM paises WHERE codigo_iso2='XX' ON CONFLICT DO NOTHING;
INSERT INTO ciudades(id_division,nom_cd,codigo_oficial)
 SELECT id_division,'Ciudad ficticia DayNest','DEMO-01' FROM division_administrativa d JOIN paises p ON p.id_ps=d.id_pais WHERE p.codigo_iso2='XX' ON CONFLICT DO NOTHING;
INSERT INTO banco(nom_b,id_ps) SELECT 'Banco ficticio DayNest',id_ps FROM paises WHERE codigo_iso2='XX' ON CONFLICT DO NOTHING;
INSERT INTO vincula(id_cl,id_b,num_cta,tipo_cta)
 SELECT c.id_cl,b.id_b,'9000'||lpad(c.id_cl::text,8,'0'),'AHORROS' FROM clientes c CROSS JOIN banco b JOIN paises p USING(id_ps)
 WHERE c.emai_cl LIKE '%@renta.test' AND p.codigo_iso2='XX' AND b.nom_b='Banco ficticio DayNest' ON CONFLICT DO NOTHING;
INSERT INTO relaciona(id_p,id_b,num_cta,tipo_cta)
 SELECT o.id_p,b.id_b,'8000'||lpad(o.id_p::text,8,'0'),'CORRIENTE' FROM propietarios o CROSS JOIN banco b JOIN paises p USING(id_ps)
 WHERE o.emai_p LIKE '%@renta.test' AND p.codigo_iso2='XX' AND b.nom_b='Banco ficticio DayNest' ON CONFLICT DO NOTHING;
UPDATE clientes SET id_cd=(SELECT id_cd FROM ciudades WHERE codigo_oficial='DEMO-01' AND id_ps=(SELECT id_ps FROM paises WHERE codigo_iso2='XX')) WHERE emai_cl LIKE '%@renta.test' AND id_cd IS NULL;
UPDATE inmuebles i SET area=coalesce(i.area,80.00),n_hab=coalesce(i.n_hab,2)
 FROM asocia a JOIN propietarios p USING(id_p) WHERE a.id_i=i.id_i AND p.emai_p LIKE '%@renta.test' AND (i.area IS NULL OR i.n_hab IS NULL);
COMMIT;
