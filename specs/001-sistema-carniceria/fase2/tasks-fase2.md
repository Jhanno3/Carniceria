# Tareas — Fase 2: Escaneo y stock

**Entradas:** `plan-fase2.md`, `data-model-fase2.md`, `contracts/control-diario-api.md`, `../spec.md` (sección 3).
**Orden:** TDD — en cada bloque, los tests se escriben y deben fallar antes de la implementación que los hace pasar (Principio III de la constitución).
**Capas (mismo criterio que Fase 1):** `controller/` → `service/` → `modelo/` (dominio puro, sin Spring/JPA) → `repository/` → `entity/` (JPA), con `dto/` para los contratos. Frontend: `modelo/` (funciones puras, si hace falta) separado de `components/` y `api/`.
**`[P]`** = se puede hacer en paralelo con las otras tareas `[P]` del mismo bloque (archivos distintos, sin dependencia entre ellas).

Numeración continúa desde `../fase1/tasks.md` (Fase 1 llegó hasta T060). Los números de migración
arrancan en `V12`: `V9`-`V11` ya los usó el pivot a multi-negocio (`V9__rol_admin.sql`,
`V10__multi_negocio.sql`, `V11__fix_dueno_todo_exige_rol.sql`, ver `../fase1/data-model.md`) que
pasó **después** de que se escribió `tasks-fase2.md` por primera vez — toda tarea de esta
fase que toca RLS ya da por hecho ese pivot (acotar por `is_dueno() and ... = mi_negocio_id()`,
no solo por `is_dueno()` o solo por `mi_negocio_id()`).

---

## Bloque 1 — Migraciones (Flyway)

Orden según `data-model-fase2.md`.

- [x] **T061** `V12__despostado_empleado_select.sql` — agrega la política de `select` para `empleado` sobre `despostado` que ya estaba anticipada (comentada) en `V2__cortes.sql`, acotada al propio negocio (`m.creado_por = mi_negocio_id()`, mismo criterio que la política de `dueno`); necesaria para que la vista de stock le dé números correctos a un `empleado`.
- [x] **T062** `V13__ventas.sql` — tabla `ventas` (con `dueno_id`, no solo `usuario_id` — ver `data-model-fase2.md`) + RLS (`dueno`: todo, acotado a `is_dueno() and dueno_id = mi_negocio_id()`; `empleado`: `select` acotado a `dueno_id = mi_negocio_id()`, `insert` con `check (dueno_id = mi_negocio_id() and usuario_id = auth.uid())`).
- [x] **T063** `V14__config_etiqueta.sql` — tabla `config_etiqueta` **por negocio** (PK `dueno_id`, no un `id` fijo — ver `data-model-fase2.md` y `plan-fase2.md` 3.1b) + RLS solo `dueno`, acotada a `is_dueno() and dueno_id = mi_negocio_id()`. Sin migración de seed: cada negocio siembra la suya (T088b, Bloque 5).
- [x] **T064** `V15__stock_por_corte.sql` — vista `stock_por_corte` con `security_invoker = true`.
- [x] **T065** Verificado contra Supabase real vía los tests de Fase 1 (que ya ejercitan Flyway al arrancar el contexto): 15 migraciones aplicadas, suite completa 30/30 OK.

**Hallazgo de seguridad de paso (`V11__fix_dueno_todo_exige_rol.sql`, no estaba en el plan original):** las políticas `*_dueno_todo` de `V10__multi_negocio.sql` (cortes, medias_reses, despostado, perdidas) acotaban por negocio pero no por rol — un empleado comparte el mismo `mi_negocio_id()` que su dueño, así que esas políticas `for all` también lo dejaban hacer `INSERT`/`UPDATE`/`DELETE`, no solo los `SELECT` que le corresponden. Se corrigió agregándoles `is_dueno()`, y `ventas`/`config_etiqueta` de esta fase ya nacieron con ese criterio. Test de regresión: `CorteControllerTest.empleadoDelMismoNegocio_puedeLeerPeroNoCrearCortes`.

**Hallazgo de Hibernate, también de paso:** `CatalogoInicialService.sembrarSiHaceFalta` y `CorteService.crear` guardan entidades con id generado en memoria (`GenerationType.UUID`), que Hibernate puede dejar sin volcar a la base hasta el próximo flush. Si para ese momento la sesión de RLS ya cambió de identidad (pasa en tests que alternan usuarios en la misma transacción; en producción nunca, cada pedido HTTP tiene una sola identidad) el INSERT se evalúa con las claims equivocadas. Se agregó `.flush()` explícito en los dos lugares.

## Bloque 2 — Backend: capa `modelo/` del decodificador (dominio puro, sin Spring/JPA)

- [x] **T067 [P]** `Ean13Test.java` (sin Spring, 6 tests): dígito verificador correcto e incorrecto contra `2000012012501` (sección 7 de la especificación), un código de 13 dígitos armado a mano con otro checksum válido, largo distinto de 13, caracteres no numéricos y `null` — todos inválidos sin excepción.
- [x] **T068 [P]** `DecodificadorEtiquetaTest.java` (sin Spring, 5 tests): decodifica `2000012012501` con la config de ejemplo → PLU 12, 1,250 kg; dígito verificador inválido, largo distinto de 13 (mismo caso, sin excepción), prefijo fuera de rango y valor en cero, cada uno con su variante de `ResultadoDecodificacion`.
- [x] **T069** `escaneo/modelo/Ean13.java` — `validarDigitoVerificador(String codigo): boolean`, algoritmo GS1 estándar (3.2 de `plan-fase2.md`).
- [x] **T070** `escaneo/modelo/DecodificadorEtiqueta.java` + `ResultadoDecodificacion.java` (`sealed interface` con variantes `Exito`/`DigitoVerificadorInvalido`/`PrefijoInvalido`/`PesoCero` — tipo resultado, no excepción, mismo criterio que `EstimacionCalculator` en Fase 1) + `ConfigEtiqueta.java` (record de dominio, no la entity JPA). `tipoValor == importe` queda documentado como pregunta abierta: sin un precio por kg configurado (no existe ese campo hoy) no hay forma de recuperar el peso real a partir del importe; se lo trata igual que `peso` a falta de una definición mejor.

## Bloque 3 — Feature `escaneo`: ventas (usa el Bloque 2)

- [x] **T071 [P]** `VentaControllerTest.java` (10 tests): código válido → `201`; mismo `idClienteLocal` repetido → `200` sin duplicar; los 4 errores `400`, incluida la prioridad (dígito verificador antes que prefijo); `PLU_INEXISTENTE` para un PLU no sembrado; empleado del mismo negocio puede escanear y el dueño ve la venta; una venta de un negocio no aparece en el listado de otro.
- [x] **T072 [P]** `VentaControllerTest.listarVentas` (incluido en el mismo archivo): más recientes primero, `limite` corta la lista, rango sin ventas devuelve `[]`.
- [x] **T073** `entity/VentaEntity.java` (con `duenoId` y `usuarioId` por separado) + `repository/VentaRepository.java`.
- [x] **T074** `dto/EscanearRequest.java`, `dto/VentaResponse.java` — kilos como `String`, `fechaHora` como `OffsetDateTime` formateado en hora Argentina (primer campo de fecha/hora que serializa esta API, no solo `LocalDate`).
- [x] **T075** `service/VentaService.escanear(...)` — `resolverNegocioId` vía `PerfilRepository.findById(usuarioId).getDuenoId()` (3.1c de `plan-fase2.md`). Las 4 excepciones (`DigitoVerificadorInvalidoException`, `PrefijoInvalidoException`, `PluInexistenteException`, `PesoCeroException`) en `escaneo/service/`.
- [x] **T076** `service/VentaService.listar(desde, hasta, limite)`.
- [x] **T077** `controller/VentaController.java`: `POST /ventas` (`201`/`200` según `ResultadoEscaneo.yaRegistrada()`), `GET /ventas`.

**Dependencia no anticipada en el plan original:** `VentaService.escanear` necesita leer la `ConfigEtiqueta` del negocio para decodificar — eso es trabajo de `config_etiqueta` (Bloque 5), que el plan tenía como "independiente". Se adelantó de Bloque 5 lo mínimo indispensable: `entity/ConfigEtiquetaEntity.java`, `repository/ConfigEtiquetaRepository.java` y `service/ConfigEtiquetaInicialService.sembrarSiHaceFalta(duenoId)` (mismo criterio que `CatalogoInicialService` de Fase 1, enganchado en el mismo punto de `PerfilService.obtenerOCrearPropio`). Falta todavía de Bloque 5: `dto/ConfigEtiquetaDto.java`, `ConfigEtiquetaService` (GET/PUT, con la validación de rangos superpuestos) y `ConfigEtiquetaController.java`.

**Hallazgo de Hibernate, mismo patrón que en Bloque 1:** `VentaService.escanear` y `ConfigEtiquetaInicialService.sembrarSiHaceFalta` también guardan entidades con id asignado/generado en memoria — se les agregó `.flush()` explícito por la misma razón que `CorteService.crear`/`CatalogoInicialService`.

## Bloque 4 — Feature `stock` y resumen del día (usa el Bloque 1, no depende del Bloque 3)

- [x] **T078 [P]** `StockControllerTest.java` (2 tests): `GET /stock` devuelve `entradoKg`/`vendidoKg`/`stockKg` correctos después de cargar un despostado y varias ventas; `quedaPoco = true` cuando el stock baja de 15 % de lo despostado, `false` para un corte sin ventas; `empleado` puede leer.
- [x] **T079 [P]** `ResumenDiaControllerTest.java` (2 tests): sin `fecha` usa hoy (Argentina) y cuenta ventas/entradas del día; con `fecha` de un día sin actividad, todo en cero sin error.
- [x] **T080** `entity/StockPorCorteEntity.java` (`@Immutable` de Hibernate) + `repository/StockRepository.java`.
- [x] **T081** `dto/StockCorteResponse.java`, `dto/ResumenDiaResponse.java`.
- [x] **T082** `service/StockService.listar()` — aplica el umbral de "Queda poco" (FR-208); un corte nunca despostado (`entradoKg = 0`) no cuenta como "queda poco".
- [x] **T083** `service/ResumenDiaService.calcular(fecha)`.
- [x] **T084** `controller/StockController.java` (`/api/v1/stock`), `controller/ResumenDiaController.java` (`/api/v1/control-diario/resumen`) — dos controllers separados, los paths del contrato no comparten prefijo.

**Hallazgo de SQL, no estaba en el plan:** la vista `stock_por_corte` de `V15` hacía `left join` a `despostado` **y** a `ventas` en la misma consulta, ambos sobre `cortes.id` sin relación entre sí — un corte con N filas de despostado y M de ventas generaba N×M filas combinadas antes del `group by` (producto cartesiano clásico), así que `sum(d.kg)` contaba cada fila de despostado una vez por cada venta de ese corte. Se corrigió en `V16__fix_stock_por_corte_fanout.sql` agregando cada lado por separado (subconsulta) antes de unirlo a `cortes`. Lo encontró el primer test que cargaba más de una venta sobre un corte ya despostado — con una sola venta por corte el bug no se nota (N×1 = N).

## Bloque 5 — Feature `config_etiqueta` (independiente de los Bloques 2-4)

- [x] **T086** `entity/ConfigEtiquetaEntity.java` (PK `duenoId`, `@Id` sin `@GeneratedValue`) + `repository/ConfigEtiquetaRepository.java`. Adelantado en Bloque 3 (`VentaService` los necesitaba para decodificar).
- [x] **T088b** `service/ConfigEtiquetaInicialService.sembrarSiHaceFalta(duenoId)` — adelantado en Bloque 3, enganchado en `PerfilService.obtenerOCrearPropio` junto al sembrado de cortes.
- [x] **T085 [P]** `ConfigEtiquetaControllerTest.java` (5 tests): `GET` devuelve la fila sembrada; `PUT` actualiza y el `GET` siguiente refleja el cambio; `PUT` con `inicioPlu`/`largoPlu` superpuesto a `inicioValor`/`largoValor` → `400 CONFIGURACION_ETIQUETA_INVALIDA`; `empleado` no puede leer ni escribir; un segundo negocio no ve ni puede pisar la config del primero.
- [x] **T087** `dto/ConfigEtiquetaDto.java`.
- [x] **T088** `service/ConfigEtiquetaService.java` (GET/PUT sobre la fila ya sembrada, `ConfiguracionEtiquetaInvalidaException` para rangos inválidos/superpuestos, `ConfigEtiquetaNoEncontradaException` para el caso defensivo de un dueño sin fila todavía) + `controller/ConfigEtiquetaController.java`. `duenoId` sale siempre del propio JWT, nunca del body ni de la URL. `ConfigEtiquetaRepository.actualizar` es un `UPDATE` explícito (mismo motivo que `CorteRepository.actualizar`: que un `UPDATE` bloqueado por RLS se note como `0` filas, no como éxito silencioso).

## Bloque 6 — Descartado (decisión del dueño, 2026-10-04)

- [~] ~~T089-T092~~ Exportar a Excel: no se hace. No se agrega `poi-ooxml` ni ningún
`ExportService`/`ExportController`; `GET /control-diario/exportar` (FR-210) se saca del
contrato. Ver `../spec.md` sección 3.2 y `contracts/control-diario-api.md`.

## Bloque 7 — Frontend: acceso a datos (`api/`)

- [x] **T093 [P]** `features/control-diario/api/types.ts` — tipos que reflejan `contracts/control-diario-api.md`.
- [x] **T094 [P]** `api/useEscanear.ts` — mutation sobre `POST /ventas`. El `idClienteLocal` lo genera quien captura el escaneo (`CampoEscaneo`, Bloque 8), no el hook: si lo generara acá, cada llamada tendría un id distinto y un "doble Enter" del lector nunca se podría deduplicar. Invalida `stock`/`resumen-dia`/`ventas` al tener éxito.
- [x] **T095 [P]** `api/useVentas.ts`, `api/useStock.ts`, `api/useResumenDia.ts` — hooks de lectura con `refetchInterval` de 15 s para que el mostrador vea ventas de otro dispositivo sin recargar.
- [x] **T096 [P]** `features/ajustes/api/useConfigEtiqueta.ts`, `useActualizarConfigEtiqueta.ts`, `types.ts`.

Build y typecheck limpios, 40/40 tests de frontend siguen OK (estos hooks no tienen test propio, mismo criterio que el Bloque 7 de la Fase 1).

## Bloque 8 — Frontend: componentes de UI

- [x] **T097 [P]** `CampoEscaneo.test.tsx` (5 tests): foco al montar; `Enter` dispara el escaneo con el valor acumulado y limpia el campo; mantiene el foco después de éxito, de error, y después de un click en cualquier otro lado de la pantalla.
- [x] **T098 [P]** `UltimoEscaneo.test.tsx` (6 tests): "Descontado del stock" en verde con corte y peso; motivo en rojo para cada uno de los 4 errores (con mensajes propios por código, no el `mensaje` crudo del backend).
- [x] **T099 [P]** `ResumenDia.test.tsx`: las 4 cifras de FR-206 con formato es-AR.
- [x] **T100 [P]** `VentasDeHoy.test.tsx` (3 tests): respeta el orden recibido del backend (no reordena), "Ver todas" expande la lista completa sin pedir de nuevo al backend (recorte puramente local, `slice` sobre lo que ya se tiene).
- [x] **T101 [P]** `TablaStock.test.tsx`: fila con `quedaPoco` se marca en naranja **y** con la etiqueta de texto "Queda poco" (nada depende solo del color).
- [x] **T102 [P]** `FormularioConfigEtiqueta.test.tsx` (3 tests): carga los valores actuales, guarda, muestra el error de rango superpuesto si el backend lo rechaza.
- [x] **T103–T108** Los 6 componentes, implementación. `CampoEscaneo` y `FormularioConfigEtiqueta` son dueños de sus propios hooks (como `DespostadoPage` en Fase 1); `ResumenDia`/`VentasDeHoy`/`TablaStock`/`UltimoEscaneo` son presentacionales puros, reciben los datos por props.
- [x] **T109** `ControlDiarioPage.tsx`: ensambla `CampoEscaneo` + `UltimoEscaneo` + `ResumenDia` + `VentasDeHoy` + `TablaStock`.
- [x] **T110** `AjustesPage.tsx`: `FormularioConfigEtiqueta`.
- [x] **T111** Cableado en `App.tsx`: la condición para mostrar Inicio/Despostado/Ajustes es "opera su propio negocio" (`duenoId === id`, no `rol === 'dueno'` a secas — cubre también al admin con negocio de prueba); "Control diario" la ve cualquiera (admin-con-negocio, dueño, o empleado); un empleado ya **no** ve la pantalla de placeholder — entra directo a Control diario.
- [x] **T112** Checklist de accesibilidad: campo de escaneo en 56 px (`h-14`), "Queda poco" con texto además del color, `<label>` en todo campo nuevo (incluido el `select` de `tipoValor`), ningún `outline-none` que tape el foco por defecto del navegador (mismo criterio que el resto del repo, que no sobreescribe el foco en ningún lado).

Build y typecheck limpios, 60/60 tests de frontend (40 de antes + 20 nuevos). No pude probarlo en un navegador real desde esta sesión — recomiendo abrir `http://localhost:5173` y escanear `2000012012501` (PLU 12 del catálogo de ejemplo) para confirmar el flujo de punta a punta antes de darlo por cerrado.

## Bloque 9 — Descartado (decisión del dueño, 2026-10-04)

- [~] ~~T113~~ Botón "Usar cámara del celular": no se hace. El mostrador escanea siempre con un lector de código de barras USB físico (el mismo supuesto de FR-201/`CampoEscaneo`, sección 4.2 de la especificación), nunca con la cámara del celular. No evaluar `@zxing/browser` ni ninguna librería de lectura por cámara.

## Bloque 10 — Cierre de fase

- [x] **T114** Agregar a `../quickstart.md` los pasos manuales de Control diario (escanear un código válido, uno inválido, ver stock bajar) continuando la numeración existente. Pasos 15-22, con los mismos códigos de ejemplo que usan `VentaControllerTest`/`StockControllerTest` (PLU 12 = Vacío).
- [ ] **T115** Ejecutar `../quickstart.md` completo (Fase 1 + los pasos nuevos) de punta a punta; corregir cualquier desvío. Pendiente: requiere clickear la UI real con el back y el front levantados — no lo puede hacer esta sesión por sí sola.
- [x] **T116** Revisado: grep sin resultados de `anular`/`IndexedDB`/`offline`/`cola` en `backend/.../escaneo` y `frontend/.../control-diario`. `VentaEntity.anulada` es solo la columna que ya preveía `data-model-fase2.md`, sin ningún endpoint ni lógica de anulación — no hay nada de Fase 3 (anulación, cola offline) ni de Fase 4 (reportes) en esta implementación.

---

## Dependencias entre bloques

```
Bloque 1 (migraciones)
  → Bloque 2 (modelo/ decodificador, puro) ──────────────┐
  → Bloque 4 (stock/resumen, no depende del Bloque 3)    │
  → Bloque 5 (config_etiqueta, independiente)             │
                                                            → Bloque 3 (ventas, usa Bloque 2)
Bloque 7 (hooks, apunta a la API real de 3/4/5)
Bloque 8 (UI, usa Bloque 7) → Bloque 10 (cierre) — Bloques 6 y 9 descartados, no se hacen
```

El Bloque 2 (modelo/ del decodificador) no toca Spring ni la base: puede arrancar apenas
termina el Bloque 1, en paralelo con los Bloques 4 y 5. El Bloque 3 sí depende del Bloque 2
(usa `Ean13`/`DecodificadorEtiqueta`) pero no de los Bloques 4/5.

## Ejemplo de ejecución en paralelo

Apenas termina el Bloque 1, lanzar juntos (sin dependencia entre sí):

```
T067, T068           (modelo/ decodificador)
T078, T079           (tests de stock/resumen — Bloque 4)
T085                 (test de config_etiqueta — Bloque 5)
```

Y dentro del Bloque 8, los 6 tests de componente (T097-T102) en paralelo, igual que en
Fase 1.
