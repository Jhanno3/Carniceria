# Plan de implementación: Fase 4 — Reportes

**Spec de origen:** `../spec.md`, sección 5
**Constitución:** `../../../constitution.md` v1.0.0
**Fecha:** 2026-10-05
**Estado:** Listo para implementar — `tasks-fase4.md` generado

## 1. Resumen

Esta fase construye los 3 reportes de rendimiento de `spec.md` sección 5 (por proveedor,
por categoría de animal, por período) — todos de **solo lectura**, sobre datos que ya
existen desde Fase 1 (`medias_reses`/`despostado`). No agrega ninguna tabla nueva: es una
capa de agregación sobre lo que ya hay. No incluye exportar a Excel (FR-404, descartado)
ni ningún reporte nuevo más allá de los 3 de la spec.

**Agregado fuera del alcance original de "Reportes" (a pedido explícito del dueño,
2026-10-05):** la pantalla "Usuarios" (exclusiva de `admin`) hoy solo lista cuentas
*pendientes* de aprobación. Se suman dos cosas, ninguna es un reporte de rendimiento, pero
se agrupan en esta fase porque se pidieron a continuación de planificarla:
- Un listado de **todas** las cuentas creadas, cualquier estado (US-4.3/FR-405 — ver 3.6).
- Poder **pausar** el acceso de cualquier cuenta (dueño o empleado) sin borrarla, y
  reactivarla después (US-4.4/FR-406 — ver 3.7).

## 2. Contexto técnico (delta sobre `../fase3/plan-fase3.md`)

| Punto | Valor |
|---|---|
| Alcance de datos nuevo | Ninguna tabla. Una sola migración, `V20__perfiles_pausado.sql` (3.7: agrega `pausado` al `check` de `perfiles.estado` y corrige `mi_negocio_id()`) — los 3 reportes en sí no necesitan ninguna. |
| Acceso (reportes) | Mismo que Fase 1 para costos: solo `dueno`/`admin`-operando-su-negocio, nunca `empleado` (FR-305 ya lo definía; las políticas RLS de `medias_reses`/`despostado` desde `V11__fix_dueno_todo_exige_rol.sql` ya acotan la lectura a `is_dueno()`, así que un `empleado` que le pegue a estos endpoints simplemente no ve filas — no hace falta ningún chequeo de rol nuevo en Java). |
| Paquete nuevo (backend) | `com.carniceria.reportes` (`controller/`, `service/`, `modelo/`, `dto/`), mismo criterio de capas que el resto. |
| Feature nueva (frontend) | `features/reportes/`. |

## 3. Decisiones de esta fase

### 3.1 Qué columna de fecha se usa para el rango y para agrupar por período

**Decisión: `medias_reses.fecha`** (la columna `date`, fecha de negocio de la entrada —
`especificacion-carniceria.md`), no `creado_en` (el timestamp de auditoría que usa
`MediaResService.listar` de Fase 1 para "hoy"). Para un reporte que cubre semanas o meses,
la fecha de negocio es lo que el dueño espera ver agrupado, y evita toda conversión de
zona horaria: comparar dos `LocalDate` es directo, no hace falta construir instantes de
inicio/fin del día en `America/Argentina/Buenos_Aires` como si hace `listar()`.

### 3.2 `rendimiento_%` y `costo_kg_vendible` promedio: ponderado, no un promedio simple

**Decisión: promedio ponderado por los kilos de cada entrada**, no el promedio aritmético
de los porcentajes/costos ya calculados de cada media res. Es decir, para un grupo (un
proveedor, una categoría, un período):

```
rendimientoPromedioPorc = (Σ vendibleKg del grupo) / (Σ pesoKg del grupo) × 100
costoKgVendiblePromedio = (Σ costoTotal del grupo, solo entradas con precioKg) / (Σ vendibleKg de esas mismas entradas)
```

Promediar los porcentajes/costos **ya calculados** de cada media res (en vez de los
totales) le daría el mismo peso a una entrada de 20 kg que a una de 200 kg, lo cual
distorsiona el número que el dueño necesita para "decidir a quién comprarle" (US-4.1): una
sola entrada chica con mal rendimiento no debería pesar lo mismo que una grande. Es el
mismo criterio que agregar tasas/ratios correctamente (evita el equivalente a la paradoja
de Simpson). Una entrada sin `precioKg` (costo desconocido) entra al cálculo de
`rendimientoPromedioPorc` pero se excluye del cálculo de `costoKgVendiblePromedio` (ni su
costo ni su `vendibleKg` suman ahí) — mismo criterio que `ResumenDespostado.costoKgVendible()`
de Fase 1, que ya devuelve `null` si no hay precio.

### 3.3 `proveedor`/`categoria` ausentes no se excluyen, se agrupan aparte

FR-403 ya lo pide explícitamente para `categoria`; se aplica el mismo criterio a
`proveedor` por simetría (no estaba escrito para `proveedor`, pero excluir silenciosamente
entradas sin proveedor cargado escondería datos reales sin que la spec lo haya pedido). En
la respuesta, el grupo sin valor viaja como `null` (`proveedor`/`categoria`); el frontend
lo rotula "Sin proveedor"/"Sin categoría".

### 3.4 Período (FR-402): un parámetro que elige el dueño, no un reporte fijo

**Decisión:** `GET /reportes/por-periodo` recibe `desde`, `hasta` **y** `periodo`
(`dia`/`semana`/`mes`) — el dueño elige la granularidad para el mismo rango, en vez de que
el sistema devuelva los tres a la vez. Función pura `modelo/CalculadorPeriodo.java` (sin
Spring, con tests): dada una `fecha` y un `Periodo`, devuelve el inicio del "bucket" al que
pertenece (`dia` → la fecha misma; `semana` → el lunes de esa semana, semana argentina de
lunes a domingo; `mes` → el día 1 de ese mes). Se agrupa por ese valor.

### 3.5 Cómo se calcula `vendibleKg` por entrada sin repetir `ResumenDespostado` completo

`ResumenDespostado` (Fase 1) necesita `Perdidas` para construirse, pero ni
`rendimientoPorc()` ni `costoKgVendible()` usan `perdidas` para nada — solo `pesoKg`,
`precioKg` y la suma de `despostado.kg` de esa media res. Para no cargar perdidas que
nadie va a usar (ni construir un objeto pensado para una sola media res, cuando acá hace
falta agregar muchas), se agrega una consulta nueva en `DespostadoRepository`,
`sumarVendibleKgPorMediaRes(Collection<UUID> mediaResIds)` (proyección JPQL a un record
`VendibleKgPorMediaRes(mediaResId, vendibleKg)`, mismo patrón que `RegistroHistorico` ya
usa para la estimación automática), y `modelo/AgregadorRendimiento.java` hace la cuenta de
3.2 directo sobre `(pesoKg, precioKg, vendibleKg)` por entrada — sin instanciar
`ResumenDespostado` ni tocar `perdidas`/`despostado` entidad por entidad.

### 3.6 Admin: listado completo de cuentas en "Usuarios"

`GET /perfiles` (Fase 1, `PerfilController`) hoy exige `estado` (`defaultValue =
"pendiente"`). Pasa a ser **opcional, sin default**: con `estado` devuelve lo de siempre
(filtrado); sin `estado`, devuelve **todas** las cuentas (`PerfilRepository.findAllByOrderByNombreAsc()`,
método nuevo). Nada cambia en RLS (`perfiles_admin_todo` de `V9__rol_admin.sql` ya deja a
`admin` leer cualquier fila; el filtro por estado siempre fue un `WHERE` de Java, nunca una
restricción de RLS) — es estrictamente un reporte más completo sobre datos que `admin` ya
podía ver fila por fila. `UsuariosPage.tsx` pasa a pedir el listado completo una sola vez
(`usePerfilesTodos`, reemplaza a `usePerfilesPendientes`) y arma la sección de "pendientes
de aprobación" (con los botones Aprobar/Rechazar que ya existían) filtrando en el cliente,
más una tabla nueva, "Todas las cuentas", con nombre/rol/estado de cada una.

### 3.7 Pausar el acceso de una cuenta: un `estado` nuevo, no una columna booleana aparte

**Decisión: agregar `pausado` al `enum`/`check` de `perfiles.estado`** (que hoy es
`pendiente`/`aprobado`/`rechazado`), no una columna `activo boolean` separada. Evita que
las dos señales puedan contradecirse (¿qué significa `estado = 'aprobado'` y `activo =
false`?) y reutiliza exactamente el mismo endpoint que ya existe para todo lo demás de
esta pantalla: `PUT /perfiles/{id}` con el mismo `rol` y `estado: "pausado"` pausa;
`estado: "aprobado"` reactiva. Cero endpoints nuevos.

**Hallazgo de diseño, no es solo agregar un valor al enum:** `is_dueno()`/`is_admin()` ya
exigen `estado = 'aprobado'` desde `V7__perfiles_estado.sql` — pausar a un `dueno` o a un
`admin` ya les saca el acceso con el solo agregado del valor nuevo. Pero
`mi_negocio_id()` (`V10__multi_negocio.sql`, la función que usan **todas** las políticas
por-negocio: `cortes`, `medias_reses`, `despostado`, `perdidas`, `ventas`,
`config_etiqueta`) nunca chequeó `estado` — un `empleado` pausado seguiría resolviendo el
`dueno_id` de su negocio igual que uno activo, porque esa función solo mira
`dueno_id`/`id`, nunca `estado`. Sin corregir esto, "pausar" a un empleado no le sacaría
ningún acceso real. Se corrige en `V20__perfiles_pausado.sql` agregando `and estado =
'aprobado'` a `mi_negocio_id()` — un único cambio que revoca el acceso de **cualquier**
cuenta no aprobada (pendiente, rechazada o pausada) en todas esas tablas a la vez, sin
tocar ninguna política una por una.

**Efecto inmediato, no hace falta esperar a que expire la sesión:** `estado = 'aprobado'`
se vuelve a consultar contra la base en cada pedido (vía `is_dueno()`/`is_admin()`/
`mi_negocio_id()`, todas `stable` pero no cacheadas entre pedidos) — el primer pedido
después de pausar ya falla, sin importar que el JWT de Supabase Auth siga siendo válido
por su cuenta.

**Un admin no puede pausarse/rechazarse/cambiarse el rol a sí mismo:** sin este chequeo, un
`admin` único (el caso típico: el dueño real de la plataforma es el único `admin`) podría
dejarse bloqueado sin nadie que lo revierta. `PerfilController.actualizar` pasa a recibir
el JWT de quien llama (antes no lo necesitaba) y `PerfilService.actualizar` rechaza con
`403 NO_PUEDE_MODIFICAR_SU_PROPIA_CUENTA` si `id` (el objetivo) es igual al `sub` del JWT
de quien llama — sin importar qué rol tenga, por simetría y simplicidad (no vale la pena
una excepción "salvo que haya otro admin").

**Frontend, un detalle que si no se corrige deja una pantalla rota:** `App.tsx` hoy
distingue `perfil.estado === 'pendiente'`/`'rechazado'` con un mensaje propio antes de
mostrar la aplicación — un `'pausado'` que cae por ningún `if` seguiría de largo hacia la
app normal (que fallaría en silencio contra el backend, RLS de por medio) en vez de
mostrar un aviso claro. Se agrega el tercer caso explícito.

### 3.8 Fuera de alcance de esta fase (Principio VII)

- **Exportar a Excel** (FR-404): descartado, mismo criterio que Fases 2 y 3.
- **Gráficos o visualizaciones**: la spec solo pide ver los números (tablas), no pide
  ningún gráfico — no se agrega ninguna librería de charting.
- **Reportes para `empleado`**: FR-305 nunca se lo dio; las políticas RLS existentes ya lo
  bloquean sin código nuevo (3.2 de la tabla de contexto técnico).
- **Filtrar "todas las cuentas" por rol/estado en el backend**: la tabla nueva de
  `UsuariosPage` filtra en el cliente sobre la misma lista que ya trajo completa — no hace
  falta un parámetro de búsqueda en el servidor para esta escala (una sola carnicería con
  un puñado de cuentas).

## 4. Chequeo contra la constitución

| Principio | Cumplimiento en este plan |
|---|---|
| I. Integridad de los datos | Ningún reporte persiste nada (se recalcula siempre desde `medias_reses`/`despostado`, igual que `ResumenDespostado` en Fase 1). |
| II. Seguridad | Los 3 reportes heredan el acoplamiento `is_dueno()` + `mi_negocio_id()` que ya tienen `medias_reses`/`despostado` desde `V11`, sin política nueva. El listado completo de cuentas (3.6) sigue exclusivo de `admin` vía `perfiles_admin_todo`, sin cambios. Pausar (3.7) sí toca seguridad de fondo: corrige que `mi_negocio_id()` nunca chequeaba `estado`, cerrando un hueco real para cuando exista una cuenta no aprobada con `dueno_id` ya asignado. |
| III. Cálculos confiables | `modelo/AgregadorRendimiento.java` y `modelo/CalculadorPeriodo.java` son funciones puras con tests propios, sin Spring/JPA. |
| IV. El mostrador no se detiene | No aplica — Reportes y Usuarios son pantallas de consulta/administración, no de mostrador, sin presupuesto de 1 segundo. |
| V. Hecho para Argentina | Agrupación "por semana" definida como semana argentina de lunes a domingo (3.4); toda fecha sigue siendo `LocalDate`/`America/Argentina/Buenos_Aires`, sin cambios de convención. |
| VI. Usabilidad y accesibilidad | El caso borde "sin datos" (sección 5.3 de la spec) se muestra como mensaje neutro, no como error ni tabla vacía sin explicación; una cuenta pausada ve un mensaje propio al iniciar sesión (3.7), no una pantalla rota. |
| VII. Simplicidad | Un endpoint por tipo de reporte, sin un mega-endpoint genérico de "agrupar por X"; `AgregadorRendimiento` se reutiliza para los 3 (misma cuenta, distinta clave de agrupación) en vez de triplicar la lógica. Pausar reutiliza `PUT /perfiles/{id}` entero, cero endpoints nuevos. |
| VIII. Cambios controlados | Una sola migración nueva (`V20__perfiles_pausado.sql`); los demás cambios de contrato (`GET /perfiles` con `estado` opcional, los 3 reportes, `PUT /perfiles/{id}` exige el JWT del actor) quedan documentados en `contracts/reportes-api.md`. |

## 5. Estructura del proyecto (nuevo sobre `../fase3/plan-fase3.md`)

```
backend/src/main/java/com/carniceria/reportes/
  controller/ReporteController.java
  service/ReporteService.java
  modelo/AgregadorRendimiento.java, CalculadorPeriodo.java   (puros, con tests)
  dto/ReporteProveedorItem.java, ReporteCategoriaItem.java, ReportePeriodoItem.java

backend/src/main/java/com/carniceria/despostado/repository/DespostadoRepository.java
  (+ sumarVendibleKgPorMediaRes, nuevo método)
backend/src/main/java/com/carniceria/despostado/modelo/VendibleKgPorMediaRes.java   (record nuevo)

backend/src/main/resources/db/migration/
  V20__perfiles_pausado.sql   (+ 'pausado' al check de estado; mi_negocio_id() exige aprobado)

backend/src/main/java/com/carniceria/perfiles/
  entity/PerfilEntity.java                (enum Estado + pausado)
  controller/PerfilController.java        (estado opcional en GET; actualizar recibe el Jwt del actor)
  service/PerfilService.java              (listar() soporta "sin filtro"; actualizar() rechaza auto-modificación)
  service/NoPuedeModificarSuPropiaCuentaException.java   (nueva, 403)
  repository/PerfilRepository.java        (+ findAllByOrderByNombreAsc)

frontend/src/features/reportes/
  api/types.ts, useReportePorProveedor.ts, useReportePorCategoria.ts, useReportePorPeriodo.ts
  components/TablaReporte.tsx, SelectorDeRango.tsx
  ReportesPage.tsx

frontend/src/features/perfiles/
  api/usePerfilesTodos.ts   (reemplaza a usePerfilesPendientes)
  UsuariosPage.tsx           (+ tabla "Todas las cuentas" con Pausar/Reactivar)

frontend/src/App.tsx         (+ caso perfil.estado === 'pausado')
```

## 6. Diseño

- **Datos:** sin tablas nuevas — ver 3.1/3.5 de este documento para cómo se agregan los
  datos existentes. Una sola migración, `V20__perfiles_pausado.sql` (3.7).
- **API:** `contracts/reportes-api.md` (nuevo) — los 3 `GET /reportes/...`, el cambio de
  `GET /perfiles` (estado opcional) y el de `PUT /perfiles/{id}` (rechaza auto-modificación).
- **Verificación:** se agrega a `../quickstart.md` en `tasks-fase4.md` (último bloque),
  continuando la numeración de Fases 1-3.

## 7. Enfoque para generar tareas (no se ejecuta en este documento)

1. Backend, capa `modelo/` (dominio puro): `CalculadorPeriodo` y `AgregadorRendimiento` —
   tests primero.
2. Backend, acceso a datos: `VendibleKgPorMediaRes` + `DespostadoRepository.sumarVendibleKgPorMediaRes`.
3. Backend, los 3 reportes: tests de integración de cada endpoint (incluido el caso
   "sin datos" y que un `empleado` no ve nada) → `dto/` → `ReporteService` → `ReporteController`.
4. Backend: `GET /perfiles` sin `estado` lista todas las cuentas — test primero.
5. Backend: migración `V20`, el nuevo estado `pausado`, y el rechazo de auto-modificación
   en `PUT /perfiles/{id}` — tests primero (pausar revoca acceso de un empleado de verdad,
   no solo de un dueño; un admin no puede tocarse a sí mismo).
6. Frontend: hooks de los 3 reportes + `usePerfilesTodos`.
7. Frontend: componentes (`TablaReporte`, `SelectorDeRango`, `ReportesPage`), la tabla
   nueva de `UsuariosPage` (con Pausar/Reactivar) y el caso `'pausado'` en `App.tsx` —
   tests de componente primero.
8. Cierre: pasos manuales agregados a `quickstart.md`.

## 8. Seguimiento de progreso

- [x] Spec revisada (`spec.md`, sección 5)
- [x] Chequeo contra la constitución (sección 4 de este documento)
- [x] Decisiones de diseño (sección 3 de este documento)
- [x] `contracts/reportes-api.md`
- [x] `tasks-fase4.md`
