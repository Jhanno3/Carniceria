# Modelo de datos — Fase 2: Escaneo y stock

Agrega a `../fase1/data-model.md` (Fase 1) las dos tablas que faltan del modelo completo
(`especificacion-carniceria.md`, sección 8.1): `ventas` y `config_etiqueta`. No toca
ninguna tabla de la Fase 1, salvo para agregarles la política de lectura del `empleado`
que ya estaba prevista (y comentada) en `cortes` desde `V2__cortes.sql`.

**Nota de arquitectura (post-pivot a multi-negocio):** este documento se escribió antes de
`V9__rol_admin.sql`/`V10__multi_negocio.sql` (ver `../fase1/data-model.md`). Todo lo de acá abajo
ya está adaptado a ese modelo: `ventas` y `config_etiqueta` son **por negocio**, igual que
`cortes`/`medias_reses`/`despostado`/`perdidas`. Las migraciones de esta fase arrancan en
`V11` (la `V10` real ya la usó el pivot de negocio, no la Fase 2 original).

## ventas

| Columna | Tipo | Regla |
|---|---|---|
| id | uuid | PK, `default gen_random_uuid()` |
| fecha_hora | timestamptz | not null, `default now()` |
| corte_id | uuid | not null, `references cortes(id)` |
| kg | numeric(8,3) | not null, `check (kg > 0)` |
| codigo_leido | text | not null |
| id_cliente_local | uuid | `unique`, not null — lo genera el **frontend** al decodificar el escaneo (no el backend), para poder generarlo también sin conexión en Fase 3 sin cambiar el contrato. Evita duplicar la venta si el lector manda el código dos veces (doble "Enter"), caso borde de la sección 3.3 de `../spec.md`. |
| anulada | boolean | not null, `default false` |
| usuario_id | uuid | not null, `references auth.users(id)` — quién escaneó (dueño o empleado). |
| dueno_id | uuid | not null, `references auth.users(id)` — a qué negocio pertenece la venta (`mi_negocio_id()` de quien la registra). Separado de `usuario_id`: a diferencia de `medias_reses` (donde solo el dueño carga y los dos valores coinciden siempre), acá un empleado puede ser quien escanea sin ser el dueño del negocio. |

**No hay `UPDATE` en esta fase**: anular una venta es Fase 3 (FR-307/308, con la ventana de 5 minutos del empleado). La columna `anulada` existe desde ya (está en el DDL completo de la especificación) para no migrar el tipo de dato más adelante, pero ningún endpoint de esta fase la toca — nace siempre en `false`.

**RLS (acotada al propio negocio, `mi_negocio_id()` de `V10__multi_negocio.sql`):**
- `dueno`: todo (`select/insert/update/delete`), `dueno_id = mi_negocio_id()` — por si necesita corregir algo a mano antes de que exista la anulación de Fase 3.
- `empleado`: `select` donde `dueno_id = mi_negocio_id()` (todas las ventas de su propio negocio, no solo las propias — no hay todavía noción de "mías" sin roles finos de Fase 3) e `insert` con `with check (dueno_id = mi_negocio_id() and usuario_id = auth.uid())`. Sin `update`/`delete`.

## config_etiqueta

Una fila por negocio (antes de este pivot iba a ser una tabla singleton global; con varios
negocios, cada uno configura su propia balanza).

| Columna | Tipo | Regla |
|---|---|---|
| dueno_id | uuid | PK, `references auth.users(id)` — es la fila de configuración *de ese negocio*, no una fila con id propio. |
| prefijo_desde | integer | not null, `check (prefijo_desde between 20 and 29)` |
| prefijo_hasta | integer | not null, `check (prefijo_hasta between 20 and 29)` |
| inicio_plu | integer | not null, `check (inicio_plu >= 0)` — posición 0-based donde empieza el PLU dentro de los 13 dígitos. |
| largo_plu | integer | not null, `check (largo_plu > 0)` |
| inicio_valor | integer | not null, `check (inicio_valor >= 0)` |
| largo_valor | integer | not null, `check (largo_valor > 0)` |
| tipo_valor | text | `check (tipo_valor in ('peso','importe'))`, not null |
| decimales | integer | not null, `check (decimales >= 0)` |

Se siembra una fila con el ejemplo de `especificacion-carniceria.md` sección 7
(`20 00012 01250 1`: prefijo 20-29, PLU en posiciones 2-6, valor en 7-11, peso en gramos →
3 decimales) la primera vez que un dueño pide `GET /perfiles/yo` después de ser aprobado —
mismo momento y mismo mecanismo que `CatalogoInicialService` para `cortes` (Fase 1,
`../fase1/data-model.md`), no una migración de seed única como se planeaba originalmente (no hay
"la" fila global que sembrar: cada negocio nuevo necesita la suya).

**RLS:** solo `dueno`, acotado a `dueno_id = mi_negocio_id()` (`select/update`; el `insert`
lo hace el sembrado inicial bajo la sesión del propio dueño, igual que `cortes`). El
`empleado` nunca necesita leerla directo: el decodificado del código escaneado pasa
siempre por el backend (ver "Dónde vive el decodificador" en `plan-fase2.md`), nunca por
el navegador.

## Stock por corte (vista, no tabla)

```sql
create view stock_por_corte
with (security_invoker = true) as   -- sin esto, la vista corre como su dueño y se salta RLS (Principio II)
select
  c.id as corte_id,
  coalesce(d.entrado_kg, 0) as entrado_kg,
  coalesce(v.vendido_kg, 0) as vendido_kg,
  coalesce(d.entrado_kg, 0) - coalesce(v.vendido_kg, 0) as stock_kg
from cortes c
left join (select corte_id, sum(kg) as entrado_kg from despostado group by corte_id) d on d.corte_id = c.id
left join (select corte_id, sum(kg) as vendido_kg from ventas where anulada = false group by corte_id) v
  on v.corte_id = c.id;
```

**Hallazgo corregido en `V16__fix_stock_por_corte_fanout.sql`:** la primera versión (`V15`)
hacía `left join` a `despostado` **y** a `ventas` en la misma consulta, ambos sobre
`cortes.id`, sin relación entre sí — un corte con N filas de despostado y M de ventas
generaba N×M filas combinadas antes del `group by`, así que `sum(d.kg)` contaba cada fila
de despostado una vez por cada venta de ese corte (producto cartesiano clásico de un doble
`JOIN` uno-a-muchos). Se corrigió agregando cada lado por separado (subconsulta) antes de
unirlo a `cortes`.

Sin `dueno_id` propio en la vista: no hace falta, el aislamiento por negocio ya lo dan
`cortes`/`despostado`/`ventas` (cada una con su propia política de `V10`/esta fase) más
`security_invoker` — un dueño o empleado que consulte la vista solo ve filas de `cortes`
de su propio negocio, y los `left join` a `despostado`/`ventas` quedan igual de acotados.

`entrado_kg` es acumulado de **todas** las medias reses cargadas hasta la fecha (FR-205 no acota a "hoy"), igual que `vendido_kg` con **todas** las ventas no anuladas. "Queda poco" (FR-208, `stock_kg < 15 % de entrado_kg`) se calcula en el backend al armar la respuesta, no en la vista — es una regla de presentación, no de datos (mismo criterio que `ResumenDespostado` en Fase 1: la vista da los números crudos, el `modelo/` de arriba decide el semáforo).

**RLS de la vista:** no lleva política propia — hereda la de `despostado` (dueño, vía `creado_por = mi_negocio_id()`) y `ventas` (dueño + empleado, vía `dueno_id = mi_negocio_id()`) por `security_invoker`. Antes de esta fase, `despostado` no tenía ninguna política de lectura para `empleado` (solo dueño podía verla) — un empleado consultando la vista hubiera visto `despostado` vacío por RLS y `entrado_kg`/`stock_kg` mal. Por eso esta fase agrega esa política, ya anticipada en `V2__cortes.sql`, acotada a `select` (nunca escritura) y al propio negocio (`m.creado_por = mi_negocio_id()`, igual criterio que la política de `dueno`).

## Resumen del día y "ventas de hoy" (no persistidos)

Igual que `ResumenDespostado` en Fase 1: `kg_vendidos_hoy`, `etiquetas_escaneadas_hoy`, `stock_vendible_total`, `entradas_hoy` (FR-206) se calculan en el backend a partir de `ventas`/`stock_por_corte`/`medias_reses`, acotando por `fecha_hora`/`creado_en` dentro del día actual en `America/Argentina/Buenos_Aires` (mismo patrón que `MediaResService.listar`, Fase 1). Ninguno es columna ni vista propia.

---

# Agregado de Fase 3: anulación y cola offline (ver `../fase3/plan-fase3.md`)

## `ventas` — política de anulación (`V18__ventas_anulacion.sql`)

No agrega columnas (`anulada` ya existía desde `V13__ventas.sql`, sin usar hasta ahora).
Agrega la política que le faltaba para que un `empleado` pueda anular su propia venta:

```sql
create policy "ventas_empleado_anular" on ventas
  for update
  using (usuario_id = auth.uid() and fecha_hora > now() - interval '5 minutes')
  with check (usuario_id = auth.uid());
```

El `dueno` no necesita una política nueva: `ventas_dueno_todo` (`for all`, `V13`) ya cubre
`update` sin límite de tiempo sobre cualquier venta de su negocio (FR-308). La ventana de 5
minutos se evalúa con `fecha_hora` (columna, hora del servidor), nunca con el reloj de
quien hace el pedido — ver `../fase3/plan-fase3.md` 3.5.

## `config_etiqueta` — lectura para `empleado` (`V19__config_etiqueta_empleado_select.sql`)

```sql
create policy "config_etiqueta_empleado_select" on config_etiqueta
  for select
  using (dueno_id = mi_negocio_id());
```

Revierte la restricción de `plan-fase2.md` ("el empleado nunca necesita leerla directo")
porque ahora sí la necesita: el decodificador offline de Fase 3 corre en el navegador de
quien escanea, dueño o empleado (`../fase3/plan-fase3.md` 3.1/3.1b). No compromete el Principio II:
ninguna columna de `config_etiqueta` es un precio, costo, ni dato sensible.

## Cola local de escaneos sin conexión (IndexedDB, no es parte de la base de datos)

No es una tabla de Postgres — vive enteramente en el navegador del dispositivo que escanea,
vía la librería `idb`. Documentado acá porque cumple el mismo rol que una tabla de "staging"
pendiente de subir, aunque Postgres nunca la ve directamente.

| Campo | Tipo | Nota |
|---|---|---|
| idCienteLocal | uuid | clave primaria del object store `escaneos-pendientes` (misma semántica que `ventas.id_cliente_local`) |
| codigo | string | el código crudo tal cual lo leyó el lector |
| decodificado | `{ corteId, corteNombre, kg }` | resultado del decodificador local (`../fase3/plan-fase3.md` 3.1), para no tener que volver a decodificar al dibujar la lista de pendientes |
| creadoEn | string (ISO, reloj del dispositivo) | solo para ordenar la sincronización (3.4); nunca viaja al backend ni reemplaza a `fecha_hora` del servidor |

Se vacía exclusivamente por sincronización exitosa o por un error de negocio real al
reintentar (`../fase3/plan-fase3.md` 3.4) — nunca por una limpieza manual ni por expiración.
