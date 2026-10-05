# Tareas — Fase 4: Reportes

**Entradas:** `plan-fase4.md`, `contracts/reportes-api.md`, `../spec.md` (sección 5).
**Orden:** TDD — en cada bloque, los tests se escriben y deben fallar antes de la implementación que los hace pasar (Principio III de la constitución).
**Capas (mismo criterio que fases anteriores):** `controller/` → `service/` → `modelo/` (dominio puro, sin Spring/JPA) → `repository/` → `entity/`, con `dto/` para los contratos. Frontend: `modelo/` (si hace falta) separado de `components/` y `api/`.
**`[P]`** = se puede hacer en paralelo con las otras tareas `[P]` del mismo bloque (archivos distintos, sin dependencia entre ellas).

Numeración continúa desde `../fase3/tasks-fase3.md` (Fase 3 llegó hasta T154). **Sin
migraciones en esta fase** (`plan-fase4.md`, sección 2: los 3 reportes son una capa de
agregación sobre `medias_reses`/`despostado`, que ya existen; el único cambio de contrato
es que `GET /perfiles` deja de exigir `estado`).

**Alcance (ver `plan-fase4.md` sección 1):** los 3 reportes de rendimiento de `spec.md`
sección 5 (por proveedor, por categoría, por período), **más** un agregado a pedido
explícito del dueño: la pantalla "Usuarios" (`admin`) pasa a listar todas las cuentas, no
solo las pendientes de aprobación (US-4.3/FR-405).

---

## Bloque 1 — Backend: capa `modelo/` pura (dominio, sin Spring/JPA)

- [ ] **T155 [P]** `CalculadorPeriodoTest.java` (sin Spring): una fecha con `periodo = dia` devuelve la misma fecha; con `semana` devuelve el lunes de esa semana (probar con una fecha que ya es lunes y otra que es domingo, para cubrir los dos extremos de la semana argentina); con `mes` devuelve el día 1 de ese mes (probar con el último día de un mes de 31 días).
- [ ] **T156 [P]** `AgregadorRendimientoTest.java` (sin Spring): un grupo de 2 entradas con distinto `pesoKg`/`vendibleKg` da el promedio ponderado correcto de `rendimientoPorc` (no el promedio simple de los dos porcentajes — el test tiene que elegir números donde ambos promedios den distinto, para probar que es el ponderado); `costoKgVendiblePromedio` ponderado igual, excluyendo del cálculo una entrada sin `precioKg` (pero sin excluirla de `rendimientoPromedioPorc`); un grupo donde ninguna entrada tiene `precioKg` da `costoKgVendiblePromedio = null`; un grupo vacío no explota (aunque en la práctica `ReporteService` nunca arme un grupo vacío).
- [ ] **T157** `reportes/modelo/CalculadorPeriodo.java` — `enum Periodo { dia, semana, mes }`, `inicioDelBucket(LocalDate fecha, Periodo periodo): LocalDate` (`plan-fase4.md` 3.4).
- [ ] **T158** `reportes/modelo/AgregadorRendimiento.java` — recibe una lista de `(pesoKg, precioKg nullable, vendibleKg)` por entrada y devuelve `cantidadEntradas`, `rendimientoPromedioPorc`, `costoKgVendiblePromedio` (`plan-fase4.md` 3.2), mismas escalas/redondeo que `ResumenDespostado` (porcentaje 2 decimales, pesos sin decimales, `RoundingMode.HALF_UP`).

## Bloque 2 — Backend: `vendibleKg` agregado por media res (usa nada del Bloque 1, en paralelo)

- [ ] **T159 [P]** Test de `DespostadoRepository.sumarVendibleKgPorMediaRes`: con 2 medias reses, cada una con varias filas de `despostado`, devuelve un `vendibleKg` por `mediaResId` que es la suma correcta de cada una (no un producto cartesiano — mismo tipo de bug que `V15__stock_por_corte.sql` en Fase 2, ya corregido ahí con subconsultas; acá se evita desde el vamos con un `group by` simple sobre una sola tabla).
- [ ] **T160** `despostado/modelo/VendibleKgPorMediaRes.java` (record: `mediaResId`, `vendibleKg`) + `DespostadoRepository.sumarVendibleKgPorMediaRes(Collection<UUID> mediaResIds): List<VendibleKgPorMediaRes>` (proyección JPQL con constructor-expression, mismo patrón que `RegistroHistorico` de Fase 1).

## Bloque 3 — Backend: los 3 reportes (usa los Bloques 1 y 2)

- [ ] **T161 [P]** `ReporteControllerTest.porProveedor` (varios `@Test` en el mismo archivo): agrupa correctamente por `proveedor`; una entrada sin `proveedor` cargado cae en un grupo `null` (no se excluye); un rango sin medias reses da `200 OK` con `[]`; un `empleado` del mismo negocio recibe `[]` (RLS, no un 403); una media res de otro negocio no contamina el reporte del propio.
- [ ] **T162 [P]** `ReporteControllerTest.porCategoria`: mismos casos que por-proveedor, agrupando por `categoria` (incluida una entrada sin categoría cargada).
- [ ] **T163 [P]** `ReporteControllerTest.porPeriodo`: con `periodo=dia`/`semana`/`mes` agrupa correctamente (al menos un caso con 2 entradas que caen en el mismo bucket de semana/mes y deben sumarse, no aparecer como 2 filas); `periodo` inválido (ej. `"anual"`) da `400 PERIODO_INVALIDO`.
- [ ] **T164** `reportes/dto/ReporteProveedorItem.java`, `ReporteCategoriaItem.java`, `ReportePeriodoItem.java`.
- [ ] **T165** `reportes/service/ReporteService.java` — cada método carga las `medias_reses` del rango (filtrando por `fecha`, no `creado_en` — `plan-fase4.md` 3.1), pide los `vendibleKg` del Bloque 2, agrupa con `AgregadorRendimiento`/`CalculadorPeriodo` (Bloque 1) y arma la respuesta ordenada (alfabético para proveedor/categoría con el grupo `null` al final; cronológico para período). `reportes/service/PeriodoInvalidoException.java` (400).
- [ ] **T166** `reportes/controller/ReporteController.java` — `GET /reportes/por-proveedor`, `/por-categoria`, `/por-periodo`.

## Bloque 4 — Backend: `admin` ve todas las cuentas (independiente de los Bloques 1-3)

- [ ] **T167 [P]** `PerfilControllerTest`, caso nuevo: `GET /perfiles` sin `estado` devuelve cuentas en los 3 estados (pendiente/aprobado/rechazado) ordenadas por nombre; sigue devolviendo `[]` (RLS) para quien no es `admin`, igual que la variante con `estado`.
- [ ] **T168** `PerfilRepository.findAllByOrderByNombreAsc(): List<PerfilEntity>`.
- [ ] **T169** `PerfilService`: el método de listado acepta `estado` nulo (sin filtrar, usa el Bloque anterior) o un valor concreto (comportamiento sin cambios).
- [ ] **T170** `PerfilController.listar`: `estado` pasa de `@RequestParam(defaultValue = "pendiente")` a `@RequestParam(required = false)`.

## Bloque 5 — Frontend: acceso a datos (`api/`)

- [ ] **T171 [P]** `features/reportes/api/types.ts` — tipos que reflejan `contracts/reportes-api.md`.
- [ ] **T172 [P]** `api/useReportePorProveedor.ts`, `useReportePorCategoria.ts`, `useReportePorPeriodo.ts` (el último con `periodo` como parámetro del hook).
- [ ] **T173 [P]** `features/perfiles/api/usePerfilesTodos.ts` (`GET /perfiles` sin `estado`) — reemplaza a `usePerfilesPendientes.ts`, que se borra (nada más lo usa fuera de `UsuariosPage`).

## Bloque 6 — Frontend: componentes de UI

- [ ] **T174 [P]** `TablaReporte.test.tsx` (nuevo componente genérico, reutilizado por los 3 reportes — misma estructura, distinta etiqueta de columna de grupo): fila por grupo con cantidad/rendimiento/costo; grupo `null` se muestra como "Sin {lo que corresponda}"; sin filas, mensaje neutro (caso borde de `spec.md` 5.3).
- [ ] **T175 [P]** `UsuariosPage.test.tsx` (nuevo — no existía): la sección de pendientes conserva sus botones Aprobar/Rechazar; la tabla nueva "Todas las cuentas" lista nombre/rol/estado de cualquier cuenta, incluidas las ya aprobadas/rechazadas.
- [ ] **T176** `components/TablaReporte.tsx`, `components/SelectorDeRango.tsx` (dos campos de fecha + botón "Aplicar", reutilizado por los 3 reportes).
- [ ] **T177** `ReportesPage.tsx` — tres secciones (proveedor/categoría/período), cada una con su `SelectorDeRango` y su `TablaReporte`; la de período agrega el selector `dia`/`semana`/`mes`.
- [ ] **T178** `UsuariosPage.tsx` — cambia a `usePerfilesTodos`, deriva la lista de pendientes en el cliente (`estado === 'pendiente'`) para la sección existente, y agrega la tabla "Todas las cuentas" debajo.
- [ ] **T179** `App.tsx` — nueva sección "Reportes", visible para quien opera su propio negocio (`operaNegocio`, mismo criterio que Despostado/Ajustes/Inicio — FR-305: el `empleado` no tiene acceso a reportes).

## Bloque 7 — Cierre de fase

- [ ] **T180** Agregar a `../quickstart.md` los pasos manuales de Fase 4, continuando la numeración: ver los 3 reportes con datos cargados en fases anteriores, probar un rango sin datos (mensaje neutro, no error), cambiar `periodo` en el reporte por período, y entrar a "Usuarios" como `admin` para confirmar que aparecen cuentas en los 3 estados (no solo pendientes).
- [ ] **T181** Ejecutar `../quickstart.md` completo (Fases 1-4) de punta a punta; corregir cualquier desvío.
- [ ] **T182** Revisión final: confirmar contra `../spec.md` que las 4 fases están completas y que ningún requisito quedó sin implementar ni sin decisión explícita de descarte.

---

## Dependencias entre bloques

```
Bloque 1 (modelo/ puro)                    ─┐
Bloque 2 (vendibleKg agregado)              ─┼→ Bloque 3 (los 3 reportes) ─┐
Bloque 4 (admin ve todas las cuentas)       ─────────────────────────────┼→ Bloque 5 (hooks) → Bloque 6 (UI) → Bloque 7 (cierre)
```

Los Bloques 1, 2 y 4 no dependen entre sí: pueden arrancar todos en paralelo desde el
principio de la fase. El Bloque 3 necesita que 1 y 2 estén listos. El Bloque 5 necesita
que 3 y 4 estén listos (los hooks de reportes dependen del 3; `usePerfilesTodos`, del 4).

## Ejemplo de ejecución en paralelo

Apenas arranca la fase, lanzar juntos (sin dependencia entre sí):

```
T155, T156              (tests del modelo/ puro, Bloque 1)
T159                    (test de vendibleKg agregado, Bloque 2)
T167                    (test de admin ve todas las cuentas, Bloque 4)
```

Y dentro del Bloque 3, los 3 tests de controller (T161-T163) en paralelo; dentro del
Bloque 6, T174/T175 en paralelo, igual que en fases anteriores.
