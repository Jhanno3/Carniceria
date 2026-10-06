# Tareas — Fase 4: Reportes

**Entradas:** `plan-fase4.md`, `contracts/reportes-api.md`, `../spec.md` (sección 5).
**Orden:** TDD — en cada bloque, los tests se escriben y deben fallar antes de la implementación que los hace pasar (Principio III de la constitución).
**Capas (mismo criterio que fases anteriores):** `controller/` → `service/` → `modelo/` (dominio puro, sin Spring/JPA) → `repository/` → `entity/`, con `dto/` para los contratos. Frontend: `modelo/` (si hace falta) separado de `components/` y `api/`.
**`[P]`** = se puede hacer en paralelo con las otras tareas `[P]` del mismo bloque (archivos distintos, sin dependencia entre ellas).

Numeración continúa desde `../fase3/tasks-fase3.md` (Fase 3 llegó hasta T154). Una sola
migración en toda la fase, `V20__perfiles_pausado.sql` (Bloque 5) — los 3 reportes en sí
no necesitan ninguna (`plan-fase4.md`, sección 2).

**Alcance (ver `plan-fase4.md` sección 1):** los 3 reportes de rendimiento de `spec.md`
sección 5 (por proveedor, por categoría, por período), **más** dos agregados a pedido
explícito del dueño, ninguno es un reporte de rendimiento pero se agrupan acá: la pantalla
"Usuarios" (`admin`) pasa a listar todas las cuentas, no solo las pendientes de aprobación
(US-4.3/FR-405), y el `admin` puede pausar/reactivar el acceso de cualquier cuenta
(US-4.4/FR-406).

---

## Bloque 1 — Backend: capa `modelo/` pura (dominio, sin Spring/JPA)

- [x] **T155 [P]** `CalculadorPeriodoTest.java` (sin Spring): una fecha con `periodo = dia` devuelve la misma fecha; con `semana` devuelve el lunes de esa semana (probado con una fecha que ya es lunes y otra que es domingo de la misma semana); con `mes` devuelve el día 1 de ese mes (probado con el último día de un mes de 31 días).
- [x] **T156 [P]** `AgregadorRendimientoTest.java` (sin Spring): un grupo de 2 entradas con distinto `pesoKg`/`vendibleKg` da el promedio ponderado correcto de `rendimientoPorc` (90 %/50 % individuales → ponderado 86,36 %, no el promedio simple de 70 %); `costoKgVendiblePromedio` ponderado igual (1.263, no el promedio simple de 2.556), excluyendo del cálculo una entrada sin `precioKg` (pero sin excluirla de `rendimientoPromedioPorc`); un grupo donde ninguna entrada tiene `precioKg` da `costoKgVendiblePromedio = null`.
- [x] **T157** `reportes/modelo/CalculadorPeriodo.java` — `enum Periodo { dia, semana, mes }`, `inicioDelBucket(LocalDate fecha, Periodo periodo): LocalDate` (`plan-fase4.md` 3.4).
- [x] **T158** `reportes/modelo/AgregadorRendimiento.java` — recibe una lista de `(pesoKg, precioKg nullable, vendibleKg)` por entrada y devuelve `cantidadEntradas`, `rendimientoPromedioPorc`, `costoKgVendiblePromedio` (`plan-fase4.md` 3.2), mismas escalas/redondeo que `ResumenDespostado` (porcentaje 2 decimales, pesos sin decimales, `RoundingMode.HALF_UP`).

Verificado: 8/8 tests nuevos OK (ambos pasaron al primer intento, sin ajustes).

## Bloque 2 — Backend: `vendibleKg` agregado por media res (usa nada del Bloque 1, en paralelo)

- [x] **T159 [P]** `DespostadoRepositoryTest.sumaElVendibleKgDeCadaMediaRes_sinProductoCartesiano`: con 2 medias reses, una con 2 filas de `despostado` y otra con 1, devuelve un `vendibleKg` por `mediaResId` que es la suma correcta de cada una, no un producto cartesiano.
- [x] **T160** `despostado/modelo/VendibleKgPorMediaRes.java` (record: `mediaResId`, `vendibleKg`) + `DespostadoRepository.sumarVendibleKgPorMediaRes(Collection<UUID> mediaResIds): List<VendibleKgPorMediaRes>` (proyección JPQL con constructor-expression, mismo patrón que `RegistroHistorico` de Fase 1).

Verificado: test nuevo OK al primer intento.

## Bloque 3 — Backend: los 3 reportes (usa los Bloques 1 y 2)

- [x] **T161 [P]** `ReporteControllerTest.porProveedor_agrupaCorrectamenteYDejaLosSinProveedorAlFinal` + `rangoSinDatos_devuelve200ConListaVacia` + `empleado_noVeNingunaEntradaDelReporte` + `unaMediaResDeOtroNegocio_noContaminaElReportePropio` (estos 3 últimos quedaron una sola vez, no repetidos por endpoint, porque comparten el mismo código RLS/de rango vacío que `ReporteService` usa igual para los 3 reportes).
- [x] **T162 [P]** `ReporteControllerTest.porCategoria_agrupaCorrectamenteYDejaLasSinCategoriaAlFinal`.
- [x] **T163 [P]** `ReporteControllerTest.porPeriodo_agrupaPorSemanaYSumaLasQueCaenEnElMismoBucket` + `porPeriodo_conPeriodoInvalido_devuelve400`.
- [x] **T164** `reportes/dto/ReporteProveedorItem.java`, `ReporteCategoriaItem.java`, `ReportePeriodoItem.java`.
- [x] **T165** `reportes/service/ReporteService.java` + `MediaResRepository.findByFechaBetween` (método nuevo, no estaba en el plan original pero hacía falta para filtrar por `fecha` en vez de `creado_en`) + `reportes/service/PeriodoInvalidoException.java` (400).
- [x] **T166** `reportes/controller/ReporteController.java` — `GET /reportes/por-proveedor`, `/por-categoria`, `/por-periodo`.

**Hallazgo, no estaba en el plan:** `Collectors.groupingBy` de Java lanza `NullPointerException` si la función clasificadora devuelve `null` — y `plan-fase4.md` 3.3 pide agrupar las entradas sin `proveedor`/`categoria` en un grupo aparte, no excluirlas, así que la clasificación SÍ puede devolver `null`. Se corrigió envolviendo la clave en `Optional.ofNullable(...)` para agrupar (evita además el riesgo de un sentinel tipo `""`/`"Sin proveedor"` que colisione con un valor real) y `.orElse(null)` al armar cada `dto`.

Verificado: 7/7 tests nuevos OK, suite completa del backend 85/85 OK.

## Bloque 4 — Backend: `admin` ve todas las cuentas (independiente de los Bloques 1-3)

- [x] **T167 [P]** `PerfilControllerTest.adminVeTodasLasCuentas_sinFiltrarPorEstado`: `GET /perfiles` sin `estado` devuelve cuentas en los 3 estados que ya existían (pendiente/aprobado/rechazado) ordenadas por nombre; un no-admin sin `estado` sigue viendo únicamente su propia fila (RLS vía `perfiles_select_propio`, nunca el listado completo de `perfiles_admin_todo`), no el total de cuentas.
- [x] **T168** `PerfilRepository.findAllByOrderByNombreAsc(): List<PerfilEntity>`.
- [x] **T169** `PerfilService`: el método de listado acepta `estado` nulo (sin filtrar, usa el Bloque anterior) o un valor concreto (comportamiento sin cambios).
- [x] **T170** `PerfilController.listar`: `estado` pasa de `@RequestParam(defaultValue = "pendiente")` a `@RequestParam(required = false)`.

Verificado: 5/5 tests de `PerfilControllerTest` OK (incluido el nuevo).

## Bloque 5 — Backend: pausar/reactivar una cuenta (independiente de los Bloques 1-3, después del 4 por tocar el mismo archivo)

- [x] **T171 [P]** `PerfilControllerTest`, casos nuevos: un `admin` pausa (`PUT` con `estado: "pausado"`) a un `empleado` aprobado y, **de verdad** (verificado con `GET /cortes`, no solo el campo `estado` de la respuesta), ese empleado deja de poder leer los cortes de su negocio — y los recupera al reactivarlo; lo mismo con un `dueno` pausado, que pierde el acceso a sus propios cortes; un `admin` que intenta `PUT /perfiles/{su propio id}` da `403 NO_PUEDE_MODIFICAR_SU_PROPIA_CUENTA`.
- [x] **T172** `V20__perfiles_pausado.sql` — agrega `'pausado'` al `check` de `perfiles.estado` (buscando el nombre real de la constraint con un bloque `do $$ ... $$`, no hardcodeado) y redefine `mi_negocio_id()` agregando `and estado = 'aprobado'`.
- [x] **T173** `PerfilEntity.Estado` — agrega `pausado` al enum.
- [x] **T174** `PerfilController.actualizar` pasa a recibir `@AuthenticationPrincipal Jwt jwt`; `PerfilService.actualizar` rechaza con `NoPuedeModificarSuPropiaCuentaException` (403) si el `id` del objetivo es igual al `sub` del JWT de quien llama, antes de tocar nada.

**Hallazgos, no estaban en el plan:**
- El guardia de auto-modificación es universal (aplica a cualquier actor, no solo a un `admin`), porque es más simple que restringirlo por rol y de todos modos ningún no-admin podía escribir su propia fila antes tampoco. Esto cambió el código de error de **dos tests ya existentes** que apuntaban a sí mismos como atajo para esquivar la ambigüedad 404-vs-403 de RLS (`unaCuentaPendiente_noPuedeAprobarseASiMisma`, `unDueno_noPuedeGestionarCuentas_soloAdmin`): ahora dan `NO_PUEDE_MODIFICAR_SU_PROPIA_CUENTA` en vez de `ACCESO_DENEGADO`, más preciso para lo que describen. Se agregó un test nuevo, `unDuenoNoAdmin_noPuedeModificarOtraCuenta_devuelve404`, para no perder cobertura de "un no-admin apuntando a **otra** cuenta" (ahora sí da `404`, no `403` — RLS le esconde la fila ajena por completo).
- La cuenta real de Facundo (`94b9f75e-...`) usada como "dueño flexible" en los tests de pausado ya opera su propio negocio de prueba con el catálogo de 17 cortes sembrado (PLU 1-21) — el primer intento de `crearCorteDirecto` usaba `plu = 1` y chocó con `cortes_dueno_id_plu_key`. Se corrigió con un PLU aleatorio alto (100.000-999.999).

Verificado: 9/9 tests de `PerfilControllerTest` OK, suite completa del backend 90/90 OK.

## Bloque 6 — Frontend: acceso a datos (`api/`)

- [x] **T175 [P]** `features/reportes/api/types.ts` — tipos que reflejan `contracts/reportes-api.md`.
- [x] **T176 [P]** `api/useReportePorProveedor.ts`, `useReportePorCategoria.ts`, `useReportePorPeriodo.ts` (el último con `periodo` como parámetro del hook).
- [x] **T177 [P]** `features/perfiles/api/usePerfilesTodos.ts` (`GET /perfiles` sin `estado`) — también se agregó `'pausado'` al tipo `PerfilResponse.estado` (el backend ya lo puede devolver desde el Bloque 5). `usePerfilesPendientes.ts` **todavía no se borra**: sigue en uso por `UsuariosPage.tsx` hasta que el Bloque 7 la reescriba; borrarlo ahora rompería el build a mitad de camino.

Verificado: build y typecheck limpios, suite completa de frontend 87/87 OK (sin tests nuevos — estos hooks no tienen test propio, mismo criterio que fases anteriores).

## Bloque 7 — Frontend: componentes de UI

- [x] **T178 [P]** `TablaReporte.test.tsx` (nuevo componente genérico, reutilizado por los 3 reportes — misma estructura, distinta etiqueta de columna de grupo): fila por grupo con cantidad/rendimiento/costo; grupo `null` se muestra como "Sin {lo que corresponda}" y el costo `null` como "—"; sin filas, mensaje neutro (caso borde de `spec.md` 5.3). 3/3 al primer intento.
- [x] **T179 [P]** `UsuariosPage.test.tsx` (nuevo — no existía): la sección de pendientes conserva sus botones Aprobar/Rechazar; la tabla nueva "Todas las cuentas" lista nombre/rol/estado de cualquier cuenta, incluidas las ya aprobadas/rechazadas; una cuenta `aprobada` muestra un botón "Pausar", una `pausada` muestra "Reactivar" en su lugar; la fila de la **propia cuenta del admin logueado** no muestra ningún botón.
- [x] **T180** `components/TablaReporte.tsx`, `components/SelectorDeRango.tsx` (dos campos de fecha + botón "Aplicar", reutilizado por los 3 reportes vía `idPrefix` para no duplicar ids de `<label>`/`<input>` en la misma página).
- [x] **T181** `ReportesPage.tsx` — tres secciones (proveedor/categoría/período), cada una con su `SelectorDeRango` (rango por defecto: últimos 30 días) y su `TablaReporte`; la de período agrega el selector `dia`/`semana`/`mes`. Sin test propio (página de ensamblado, mismo criterio que `ControlDiarioPage`/`DespostadoPage`).
- [x] **T182** `UsuariosPage.tsx` — cambia a `usePerfilesTodos`, deriva la lista de pendientes en el cliente (`estado === 'pendiente'`) para la sección existente, agrega la tabla "Todas las cuentas" debajo con los botones Pausar/Reactivar, y esconde esos botones en la fila del propio admin (`usePerfilPropio`). `usePerfilesPendientes.ts` se borró (ya no lo usa nadie).
- [x] **T183** `App.tsx` — nueva sección "Reportes", visible para quien opera su propio negocio (`operaNegocio`); agrega el caso `perfil.estado === 'pausado'` junto a `'pendiente'`/`'rechazado'`, con su propio mensaje, antes de que el resto de la app intente renderizarse.

Verificado: 8 tests nuevos OK (3 de `TablaReporte` + 5 de `UsuariosPage`), suite completa de frontend 95/95 OK, build y typecheck limpios.

## Bloque 8 — Cierre de fase

- [x] **T184** Agregados a `../quickstart.md` los pasos 31-39 de Fase 4, continuando la numeración: ver los 3 reportes, rango sin datos (mensaje neutro), cambiar `periodo`; "Usuarios" con cuentas en cualquier estado; pausar/reactivar una cuenta de prueba (incluido confirmar el mensaje que ve esa cuenta al loguearse pausada); confirmar que la propia fila del admin no tiene botones para tocarse a sí mismo.
- [ ] **T185** Ejecutar `../quickstart.md` completo (Fases 1-4) de punta a punta; corregir cualquier desvío. Pendiente: requiere clickear la UI real con el back y el front levantados — no lo puede hacer esta sesión por sí sola.
- [x] **T186** Revisión final contra `../spec.md`: las 4 fases cubren FR-101 a FR-406 completos (las únicas tachadas, FR-210/FR-404, son descartes explícitos del dueño, no huecos). Se resolvió de paso la pregunta abierta de la sección 10 sobre la agrupación por período de FR-402 (quedó como parámetro elegible, no un reporte fijo) — queda una sola pregunta abierta real, la marca/modelo de balanza, que la spec ya marca como no bloqueante por diseño (`config_etiqueta`).

---

## Dependencias entre bloques

```
Bloque 1 (modelo/ puro)                    ─┐
Bloque 2 (vendibleKg agregado)              ─┼→ Bloque 3 (los 3 reportes) ─┐
Bloque 4 (admin ve todas las cuentas) ──────────────────────────────────┼→ Bloque 6 (hooks) → Bloque 7 (UI) → Bloque 8 (cierre)
Bloque 5 (pausar/reactivar, toca los mismos archivos que el 4) ─────────┘
```

Los Bloques 1 y 2 no dependen entre sí: pueden arrancar en paralelo desde el principio de
la fase. El Bloque 4 y el Bloque 5 tocan los mismos archivos (`PerfilController`,
`PerfilService`, `PerfilRepository`) — no son estrictamente dependientes uno del otro en
diseño, pero conviene hacer el 4 primero y el 5 después para no pisarse en el mismo
archivo a la vez. El Bloque 3 necesita que 1 y 2 estén listos. El Bloque 6 necesita que 3,
4 y 5 estén listos (los hooks de reportes dependen del 3; `usePerfilesTodos` y el resto de
`UsuariosPage`, del 4 y el 5 juntos).

## Ejemplo de ejecución en paralelo

Apenas arranca la fase, lanzar juntos (sin dependencia entre sí):

```
T155, T156              (tests del modelo/ puro, Bloque 1)
T159                    (test de vendibleKg agregado, Bloque 2)
T167                    (test de admin ve todas las cuentas, Bloque 4)
```

Y dentro del Bloque 3, los 3 tests de controller (T161-T163) en paralelo; dentro del
Bloque 7, T178/T179 en paralelo, igual que en fases anteriores.
