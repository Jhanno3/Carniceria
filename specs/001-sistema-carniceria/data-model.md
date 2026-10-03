# Modelo de datos — Fase 1: Despostado

Subconjunto del modelo completo (`especificacion-carniceria.md`, sección 8.1) que esta fase necesita: `perfiles`, `cortes`, `medias_reses`, `despostado`, `perdidas`. `ventas`, `config_etiqueta` y las políticas de empleado llegan en fases posteriores.

`perfiles` se crea ya en esta fase, aunque la invitación de empleados (Fase 3) todavía no exista, porque las políticas de RLS necesitan saber el rol del usuario desde la primera migración (Principio II: "RLS activado en todas las tablas desde la primera migración").

## perfiles

| Columna | Tipo | Regla |
|---|---|---|
| id | uuid | PK, `references auth.users(id)` |
| nombre | text | |
| rol | text | `check (rol in ('admin','dueno','empleado'))`, not null — `admin` agregado en `V9__rol_admin.sql` |
| estado | text | `check (estado in ('pendiente','aprobado','rechazado'))`, not null, `default 'pendiente'` — agregada en `V7__perfiles_estado.sql` |

El primer dueño se siembra a mano (hoy, `rol = 'admin'`, `estado = 'aprobado'`). A partir de ahí, el registro es abierto (`POST` de Supabase Auth, sin pasar por el backend) pero toda cuenta nueva nace en `estado = 'pendiente'` — ver constitution.md, Principio II (revisado) y `GET/PUT /perfiles` en `contracts/despostado-api.md`.

**`admin` vs `dueno` (`V9__rol_admin.sql`):** originalmente `dueno` era el único nivel con acceso total. Se dividió en dos para que la gestión de cuentas quede acotada a una sola persona: `admin` tiene todo lo que tenía `dueno` (vía `is_dueno()`, que ahora acepta `rol in ('dueno','admin')`) más la administración de `perfiles` (`is_admin()`, exclusivo); `dueno` queda como un nivel intermedio pensado para socios/ayudantes con acceso operativo (Despostado, Inicio) pero sin poder tocar usuarios. `rol_solicitado` del registro (`PerfilService.parsearRolOEmpleado`) nunca puede resultar en `admin`: promover a alguien a `admin` es siempre una acción explícita de otro `admin` ya existente.

**Pivot a multi-negocio (`V10__multi_negocio.sql`):** la aplicación pasó de ser para una sola carnicería a admitir varios negocios independientes, cada uno con su propio `dueno`. Columna nueva:

| Columna | Tipo | Regla |
|---|---|---|
| dueno_id | uuid | null permitido, `references auth.users(id)` |

`dueno_id` es "a qué negocio pertenece esta cuenta": para un `dueno`, es su propio `id` (es dueño de sí mismo — simplifica toda política de ahí en más a "¿esta fila es de mi negocio?"); para un `empleado`, es el `id` del dueño que lo invitó; `null` para `admin` (no opera ningún negocio, por decisión explícita — ver sección 9 de `spec.md`... *nota: esta decisión es posterior a esa sección, no está reflejada ahí todavía*) y para una cuenta todavía `pendiente`.

Función `mi_negocio_id()` (security definer): devuelve el `dueno_id` del usuario autenticado — la usan todas las políticas de las tablas de negocio (`cortes`, `medias_reses`, `despostado`, `perdidas`) en vez de `is_dueno()` a secas, para que un dueño nunca vea los datos de otro.

**Hallazgo de seguridad corregido en `V11__fix_dueno_todo_exige_rol.sql`:** las políticas `*_dueno_todo` de `V10` acotaban por negocio pero no por rol — un empleado comparte el mismo `mi_negocio_id()` que su dueño, así que esas políticas `for all` también lo dejaban hacer `INSERT`/`UPDATE`/`DELETE`, no solo los `SELECT` que le corresponden. Se les agregó `is_dueno()` además del chequeo de negocio. No lo detectaron los tests de Fase 1 porque el test de bloqueo a `empleado` usa un desconocido sin perfil (`mi_negocio_id()` da `null`, nunca matchea), no un empleado real del mismo negocio — ver Fase 2 (`tasks-fase2.md`) para los tests que sí cubren ese caso.

**Invitación de empleados:** no hay tabla de invitaciones — el código de invitación es directamente el `id` del dueño (un link `?invita=<id>` que el frontend arma, ver `InvitarEmpleado.tsx`). Al registrarse con ese código en `user_metadata.dueno_invitador_id`, `PerfilService.crearDesdeMetadata` valida con la función `es_dueno_valido(id)` (security definer, no expone la fila del dueño) que ese `id` sea un `dueno` aprobado, y si es así crea al empleado **directo en `estado = 'aprobado'`** con ese `dueno_id` — el dueño ya lo vouch-eó al invitarlo, no hace falta que nadie más apruebe un segundo paso. Sin código válido, la cuenta nace `pendiente` sin `dueno_id` (caso borde raro: no hay ningún flujo de la UI que registre un empleado sin link de invitación).

**Catálogo inicial por negocio:** cuando una cuenta pasa a `rol = 'dueno'` (sea por aprobación de un `admin`, FR futuro, o por lo que sea), no tiene ningún corte propio. `CatalogoInicialService.sembrarSiHaceFalta`, llamado desde `PerfilService.obtenerOCrearPropio` (bajo la sesión del propio dueño — sembrar "para otro" no pasa RLS), le carga el catálogo de ejemplo de la sección 2.2 de la especificación (los 17 cortes que quedan después de `V8__eliminar_cortes_redundantes.sql`) la primera vez que pide `GET /perfiles/yo` después de ser aprobado.

## cortes

| Columna | Tipo | Regla |
|---|---|---|
| id | uuid | PK, `default gen_random_uuid()` |
| nombre | text | not null |
| plu | integer | not null, `unique (dueno_id, plu)` — por negocio, no global (`V10__multi_negocio.sql`: dos dueños pueden usar el mismo PLU) |
| cuarto | text | `check (cuarto in ('Delantero','Trasero','Ambos'))`, not null |
| zona_mapa | text | null permitido (ej. carne picada/recortes) |
| activo | boolean | not null, `default true` |
| dueno_id | uuid | not null, `references auth.users(id)` — agregada en `V10__multi_negocio.sql` |

**RLS (`V10__multi_negocio.sql`):**
- `dueno`: todo (`select/insert/update/delete`), acotado a `dueno_id = mi_negocio_id()`.
- `empleado`: `select` donde `activo = true and dueno_id = mi_negocio_id()` — ve el catálogo de su propio dueño, nunca el de otro negocio.

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

**RLS:** solo `dueno`, acotado a `creado_por = mi_negocio_id()` (`V10__multi_negocio.sql`; antes era `is_dueno()` a secas, lo que hubiera dejado a cualquier dueño ver las medias reses de cualquier otro negocio). No hace falta una columna `dueno_id` propia: nadie salvo el dueño carga medias reses (nunca un empleado, FR-101), así que `creado_por` **ya es** el tenant. El empleado no tiene ninguna política sobre esta tabla: no ve precio_kg ni ninguna otra columna (Principio II).

**No hay columna `estado`.** No existe el concepto de media res "abierta"/"cerrada": una fila de `medias_reses` solo se crea junto con su despostado y pérdidas completos, en una única transacción (ver FR-111/FR-112 de `spec.md` y "Carga atómica" más abajo). Nunca hay una media res a medio cargar en la base.

## despostado

| Columna | Tipo | Regla |
|---|---|---|
| id | uuid | PK, `default gen_random_uuid()` |
| media_res_id | uuid | not null, `references medias_reses(id) on delete cascade` |
| corte_id | uuid | not null, `references cortes(id)` |
| kg | numeric(8,3) | not null, `check (kg > 0)` |
| | | `unique (media_res_id, corte_id)` — cargar de nuevo un corte para la misma media res es un `UPDATE`, nunca una segunda fila. |

**RLS:** solo `dueno`, acotado a que la `media_res_id` pertenezca a su propio negocio (`exists (select 1 from medias_reses m where m.id = despostado.media_res_id and m.creado_por = mi_negocio_id())`, `V10__multi_negocio.sql`). La política de lectura para el cálculo de stock (Fase 2) se agrega en la migración de esa fase, no antes (Principio VII).

**No hay ninguna columna que distinga "manual" de "automático" (FR-112/FR-113).** El modo de carga es un detalle de la pantalla antes de guardar: una vez que se toca "Cargar entrada", todas las filas de `despostado` son iguales, hayan arrancado precargadas por la estimación o tipeadas a mano.

## Carga atómica (FR-111) y estimación automática (FR-113)

- **Guardado:** el backend expone una única operación transaccional que inserta `medias_reses` y, en la misma transacción, todas las filas de `despostado` y `perdidas` correspondientes. Si cualquier `CHECK` o `UNIQUE` falla, se revierte todo (no queda la media res sin sus cortes).
- **Estimación automática:** es una consulta de **lectura**, no persiste nada. Para cada corte activo, calcula `AVG(despostado.kg / medias_reses.peso_kg)` sobre las filas históricas de `despostado` cargadas por el **mismo usuario autenticado** (`medias_reses.creado_por = auth.uid()`, join con `medias_reses`), y lo multiplica por el `peso_kg` de la media res que se está cargando. Es por usuario (no global) porque cada carnicero suele trabajar siempre con la misma raza/proveedor y el mismo criterio de corte, así que su propio historial estima mejor que el promedio de todos los usuarios. Si el usuario no tiene ninguna fila histórica propia de `despostado`, la consulta devuelve vacío y el modo automático no se ofrece (FR-113), aunque sí exista historial de otros usuarios.

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

La migración de datos iniciales (`V3__seed_cortes.sql`) cargó los 21 cortes de la tabla de la sección 2.2, con su `cuarto`, `zona_mapa` y `plu` provisorio (a reemplazar por el dueño cuando configure la balanza real). Sirve para que "Restablecer ejemplo" tenga algo para cargar desde el primer arranque.

`V8__eliminar_cortes_redundantes.sql` sacó después Aguja, Marucha, Pecho y Cogote (PLU 16/18/19/20): en esta carnicería no se despostan como cortes aparte, van incluidos dentro de otros cortes ya existentes. Catálogo actual: 17 cortes.
