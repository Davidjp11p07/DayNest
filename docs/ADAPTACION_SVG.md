# Correspondencia con el diagrama del profesor

Fuente: Proyecto - Airbnb.pdf suministrado por el usuario. modelo_er_daynest.svg es una recreacion vectorial editable de las seis entidades. Incluye numero y tipo de cuenta en las relaciones cliente-banco y propietario-banco, segun la aclaracion del usuario; no se presenta como el archivo original del profesor.

| Diagrama | Tabla y atributos principales |
|---|---|
| Propietarios | propietarios: id_p, nom_p, tel_p, emai_p |
| Clientes | clientes: id_cl, nom_cl, tel_cl, emai_cl, id_cd |
| Inmuebles | inmuebles: id_i, dir_i, area, n_hab, capacd, id_cd |
| Ciudades | ciudades: id_cd, nom_cd, id_ps |
| Paises | paises: id_ps, nom_ps |
| Banco | banco: id_b, nom_b, id_ps |
| Asocia | id_i, id_p, prec_nch |
| Vincula | id_cl, id_b, num_cta, tipo_cta |
| Relaciona propietario-banco | id_p, id_b, num_cta, tipo_cta |
| Renta | PK id_cl, id_i, fch_e, num_intento; fch_s y datos historicos |

Las cardinalidades 1:N de ciudad-pais, inmueble-ciudad, cliente-ciudad y banco-pais se materializan mediante FK. La cuenta actual es unica por perfil; se permite que ambos perfiles usen el mismo numero y banco.

El modelo logico distingue las diez tablas del profesor de nueve extensiones tecnicas en la version financiera actual. Las vistas no se cuentan como tablas. Los datos antiguos conservan sus IDs y valores; los campos desconocidos quedan pendientes. datos-demo-profesor.sql es opcional y separado: solo completa datos ficticios de usuarios demo.

La nomenclatura ya esta aplicada. La automatizacion financiera aun requiere autorizacion para aplicar su migracion operativa. Los detalles completos estan en esquema_relacional.md y GIROS_AUTOMATICOS.md.
