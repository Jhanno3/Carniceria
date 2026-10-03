# Modelo de datos — Fase 1: Despostado

Subconjunto del modelo completo (`especificacion-carniceria.md`, sección 8.1) que esta fase necesita: `perfiles`, `cortes`, `medias_reses`, `despostado`, `perdidas`. `ventas`, `config_etiqueta` y las políticas de empleado llegan en fases posteriores.

`perfiles` se crea ya en esta fase, aunque la invitación de empleados (Fase 3) todavía no exista, porque las políticas de RLS necesitan saber el rol del usuario desde la primera migración (Principio II: "RLS activado en todas las tablas desde la primera migración").

## perfiles

| Columna | Tipo | Regla |
|---|---|---|
| id | uuid | PK, `references auth.users(id)` |
| nombre | text | |
| rol | text | `check (rol in ('dueno','empleado'))`, not null |
| estado | text | `check (estado in ('pendiente','aprobado','rechazado'))`, not null, `default 'pendiente'` — agregada en `V7__perfiles_estado.sql` |

El primer dueño se siembra a mano (`rol = 'dueno'`, `estado = 'aprobado'`). A partir de ahí, el registro es abierto (`POST` de Supabase Auth, sin pasar por el backend) pero toda cuenta nueva nace en `estado = 'pendiente'` — ver constitution.md, Principio II (revisado) y `GET/PUT /perfiles` en `contracts/despostado-api.md`.

## cortes

| Columna | Tipo | Regla |
|---|---|---|
| id | uuid | PK, `default gen_random_uuid()` |
| nombre | text | not null |
| plu | integer | `unique`, not null |
| cuarto | text | `check (cuarto in ('Delantero','Trasero','Ambos'))`, not null |
| zona_mapa | text | null permitido (ej. carne picada/recortes) |
| activo | boolean | not null, `default true` |

**RLS:**
- `dueno`: todo (`select/insert/update/delete`).
- `empleado`: `select` donde `activo = true` (anticipa la Fase 2 — el mostrador necesita leer la lista de cortes para mostrar stock —, ya definido en la especificación sección 8.2; no se le da ningún otro permiso en esta fase).

## medias_reses

| Columna | Tipo | Regla |
|---|---|---|
| id | uuid | PK, `default gen_random_uuid()` |
| fecha | date | not null, `default current_date` |
| proveedor | text | null |
| peso_kg | numeric(8,3) | not null, `check (peso_kg > 0)` |
| precio_kg | numeric(12,2) | null, `check (precio_kg is null or precio_kg > 0)` |
| creado_por | uuid | not null, `references auth.users(id)` |
| creado_en | timestamptz | not null, `default now()` |

**RLS:** solo `dueno` (`select/insert/update/delete`). El empleado no tiene ninguna política sobre esta tabla: no ve precio_kg ni ninguna otra columna (Principio II).

**No hay columna `estado`.** No existe el concepto de media res "abierta"/"cerrada": una fila de `medias_reses` solo se crea junto con su despostado y pérdidas completos, en una única transacción (ver FR-111/FR-112 de `spec.md` y "Carga atómica" más abajo). Nunca hay una media res a medio cargar en la base.

## despostado

| Columna | Tipo | Regla |
|---|---|---|
| id | uuid | PK, `default gen_random_uuid()` |
| media_res_id | uuid | not null, `references medias_reses(id) on delete cascade` |
| corte_id | uuid | not null, `references cortes(id)` |
| kg | numeric(8,3) | not null, `check (kg > 0)` |
| | | `unique (media_res_id, corte_id)` — cargar de nuevo un corte para la misma media res es un `UPDATE`, nunca una segunda fila. |

**RLS:** solo `dueno`, en esta fase. La política de lectura para el cálculo de stock (Fase 2) se agrega en la migración de esa fase, no antes (Principio VII).

**No hay ninguna columna que distinga "manual" de "automático" (FR-112/FR-113).** El modo de carga es un detalle de la pantalla antes de guardar: una vez que se toca "Cargar entrada", todas las filas de `despostado` son iguales, hayan arrancado precargadas por la estimación o tipeadas a mano.

## Carga atómica (FR-111) y estimación automática (FR-113)

- **Guardado:** el backend expone una única operación transaccional que inserta `medias_reses` y, en la misma transacción, todas las filas de `despostado` y `perdidas` correspondientes. Si cualquier `CHECK` o `UNIQUE` falla, se revierte todo (no queda la media res sin sus cortes).
- **Estimación automática:** es una consulta de **lectura**, no persiste nada. Para cada corte activo, calcula `AVG(despostado.kg / medias_reses.peso_kg)` sobre todas las filas históricas de `despostado` (join con `medias_reses`), y lo multiplica por el `peso_kg` de la media res que se está cargando. Si no existe ninguna fila histórica de `despostado`, la consulta devuelve vacío y el modo automático no se ofrece (FR-113).

## perdidas

| Columna | Tipo | Regla |
|---|---|---|
| id | uuid | PK, `default gen_random_uuid()` |
| media_res_id | uuid | not null, `references medias_reses(id) on delete cascade` |
| tipo | text | `check (tipo in ('hueso','grasa','merma'))`, not null |
| kg | numeric(8,3) | not null, `check (kg > 0)` |
| | | `unique (media_res_id, tipo)` — como máximo una fila por tipo de pérdida y media res. |

**RLS:** solo `dueno`.

## Valores calculados (no persistidos)

`vendible_kg`, `perdida_kg`, `sin_asignar_kg`, `rendimiento_%`, `costo_total`, `costo_kg_vendible` **no son columnas**: ningún `CREATE TABLE` de esta fase los incluye. Se derivan dos veces, cada una con sus tests (ver `research.md`, "Dónde viven las fórmulas de cálculo"):
- en el frontend (capa `modelo/`), para la vista en vivo mientras se edita, antes de guardar;
- en el backend (capa `modelo/`, clase `ResumenDespostado`), recalculados en cada `GET`/`POST` de `medias-reses` a partir de las filas de `despostado` y `perdidas` ya guardadas — nunca se guardan ellos mismos.

## Seed de cortes (sección 2.2 de la especificación)

La migración de datos iniciales carga los 21 cortes de la tabla de la sección 2.2, con su `cuarto`, `zona_mapa` y `plu` provisorio (a reemplazar por el dueño cuando configure la balanza real). Sirve para que "Restablecer ejemplo" tenga algo para cargar desde el primer arranque.
