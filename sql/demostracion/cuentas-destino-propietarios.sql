-- Datos ficticios para demostracion. Completa solo propietarios con inmuebles sin cuenta.
-- Reejecutable: conserva cuentas existentes y no cambia datos bancarios historicos.
-- Ejecutar con search_path apuntando al esquema operativo de DayNest.
BEGIN;

INSERT INTO banco(nom_b,id_ps)
SELECT DISTINCT 'Banco ficticio destino DayNest',c.id_ps
FROM asocia a JOIN inmuebles i USING(id_i) JOIN ciudades c USING(id_cd)
WHERE NOT EXISTS(SELECT 1 FROM relaciona r WHERE r.id_p=a.id_p)
ON CONFLICT DO NOTHING;

WITH destinos AS (
 SELECT DISTINCT ON(a.id_p) a.id_p,b.id_b
 FROM asocia a JOIN inmuebles i USING(id_i) JOIN ciudades c USING(id_cd)
 JOIN banco b ON b.id_ps=c.id_ps AND b.nom_b='Banco ficticio destino DayNest'
 WHERE NOT EXISTS(SELECT 1 FROM relaciona r WHERE r.id_p=a.id_p)
 ORDER BY a.id_p,i.id_i
)
INSERT INTO relaciona(id_p,id_b,num_cta,tipo_cta)
SELECT id_p,id_b,
 '99'||lpad(id_p::text,10,'0')||lpad(floor(random()*100000000)::bigint::text,8,'0'),
 CASE WHEN random()<0.5 THEN 'AHORROS' ELSE 'CORRIENTE' END
FROM destinos
ON CONFLICT(id_p) DO NOTHING;

COMMIT;
