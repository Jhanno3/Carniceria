# Tareas — Fase 3: Sin conexión y anulación

**Entradas:** `plan-fase3.md`, `../fase2/data-model-fase2.md` (sección "Agregado de Fase 3"), `../fase2/contracts/control-diario-api.md`, `../spec.md` (sección 4).
**Orden:** TDD — en cada bloque, los tests se escriben y deben fallar antes de la implementación que los hace pasar (Principio III de la constitución).
**Capas (mismo criterio que Fases 1 y 2):** `controller/` → `service/` → `modelo/` (dominio puro, sin Spring/JPA) → `repository/` → `entity/` (JPA), con `dto/` para los contratos. Frontend: `modelo/` (funciones puras) separado de `cola/` (acceso a IndexedDB), `components/` y `api/`.
**`[P]`** = se puede hacer en paralelo con las otras tareas `[P]` del mismo bloque (archivos distintos, sin dependencia entre ellas).

Numeración continúa desde `../fase2/tasks-fase2.md` (Fase 2 llegó hasta T116). Los números de
migración arrancan en `V18` (`V1`-`V17` ya los usaron las fases anteriores).

**Recordatorio de alcance (ver `plan-fase3.md` sección 1):** de las 4 historias de
`../spec.md` sección 4, US-3.2 (el empleado no ve costos) y US-3.3 (registro con aprobación)
**ya están hechas** desde el pivot a multi-negocio de Fase 2 — ninguna tarea de este
documento las vuelve a tocar. Esta fase es exclusivamente US-3.1 (cola offline) y US-3.4
(anulación).

---

## Bloque 1 — Migraciones (Flyway)

- [x] **T117** `V18__ventas_anulacion.sql` — política `ventas_empleado_anular` (`for update`, `using (usuario_id = auth.uid() and fecha_hora > now() - interval '5 minutes')`, `with check (usuario_id = auth.uid())`). El `dueno` no necesita política nueva: `ventas_dueno_todo` (`V13`) ya cubre `update` sin límite.
- [x] **T118** `V19__config_etiqueta_empleado_select.sql` — política `config_etiqueta_empleado_select` (`for select`, `using (dueno_id = mi_negocio_id())`), para que el decodificador offline del Bloque 7 funcione también en la sesión de un empleado (`plan-fase3.md` 3.1b).
- [x] **T119** Verificado contra Supabase real: 19 migraciones aplicadas (`V18`/`V19` corrieron limpio), suite completa 61/61 OK.

## Bloque 2 — Backend: anular una venta (usa el Bloque 1)

- [x] **T120 [P]** `VentaControllerTest.java`, tests nuevos para `POST /ventas/{id}/anular`: el dueño anula cualquier venta de su negocio sin límite de tiempo; un empleado anula una venta propia de menos de 5 minutos; un empleado **no** puede anular una venta ajena (misma antigüedad); un empleado **no** puede anular una venta propia de más de 5 minutos; una venta inexistente (o de otro negocio) da `404 VENTA_NO_ENCONTRADA`; anular dos veces seguidas la misma venta (dentro de la ventana) es idempotente, `200` las dos veces.
- [x] **T121 [P]** `VentaControllerTest.escanear_yListar_incluyenUsuarioId`: la respuesta de `GET /ventas` (y la de `POST /ventas`) incluye `usuarioId`.
- [x] **T122** `dto/VentaResponse.java` — agrega el campo `usuarioId`; `VentaResponse.de(...)` lo toma de `VentaEntity.getUsuarioId()`.
- [x] **T123** `repository/VentaRepository.java` — `anular(UUID id): int`, `@Modifying` `UPDATE ventas set anulada = true where id = :id` explícito (mismo motivo que `ConfigEtiquetaRepository.actualizar`, Fase 2: que un `UPDATE` que RLS bloquea se note como `0` filas afectadas, no como éxito silencioso).
- [x] **T124** `service/VentaNoEncontradaException.java` (404), `service/VentaNoSePuedeAnularException.java` (403).
- [x] **T125** `service/VentaService.anular(UUID id)` — `findById` (acotado por la política de `select`, que sí deja ver ventas ajenas del propio negocio) para distinguir "no existe" de "existe pero no se puede"; si `anular(id)` afecta `0` filas, lanza `VentaNoSePuedeAnularException`; si no, vuelve a leer la fila y devuelve `construirRespuesta` con `anulada = true`.
- [x] **T126** `controller/VentaController.java` — `POST /ventas/{id}/anular`, sin body.

**Hallazgo de RLS/AOP, no estaba en el plan:** para testear la ventana de 5 minutos hace falta fabricar una venta con `fecha_hora` pasada (un `POST /ventas` real siempre usa `Instant.now()`), insertándola directo por repositorio bajo la identidad del dueño. La primera versión de ese helper vivía como método privado de `VentaControllerTest`, haciendo `jwtClaimsHolder.set(...)` y llamando a `corteRepository.findByPlu()`/`ventaRepository.save()`/`.flush()` directo — y fallaba con "new row violates row-level security policy" **solo** cuando el test anterior había hecho otra llamada `@Transactional` real (p. ej. `registrarComoEmpleado` de un segundo usuario) entre medio. Causa: `RlsSessionAspect` necesita que el método invocado esté anotado `@Transactional` de forma que Spring AOP lo intercepte correctamente para volver a propagar el JWT (`SET LOCAL`) — una llamada a un método de un repositorio de Spring Data invocado directo desde el test (no desde un bean `@Service` propio) no lo garantiza de forma confiable, y el `SET LOCAL` de la **última** llamada que sí disparó el aspecto (la del otro usuario) seguía activo en la conexión. Se corrigió moviendo la inserción a un bean nuevo y dedicado, `VentaTestFixtures.crearVentaDirecta(...)` (`@Transactional` real, mismo patrón que `NegocioTestFixtures`), llamado con `jwtClaimsHolder` puesto a la identidad correcta justo antes — igual criterio que el resto del proyecto ya usa para sembrar fixtures.

## Bloque 3 — Backend: `kgVendidosHoy` excluye ventas anuladas (usa el Bloque 2 para su test)

- [x] **T127 [P]** `ResumenDiaControllerTest.ventaAnulada_noCuentaParaKgVendidosHoyPeroSiParaEtiquetasEscaneadasHoy`: carga dos ventas, anula una (vía el endpoint del Bloque 2), y verifica que `kgVendidosHoy` baja en lo anulado pero `etiquetasEscaneadasHoy` no cambia (sigue contando el escaneo como actividad, `plan-fase3.md` 3.8).
- [x] **T128** `service/ResumenDiaService.calcular` — filtra `kgVendidosHoy` por `!anulada`; `etiquetasEscaneadasHoy` no se toca.

Verificado: suite completa del backend 69/69 OK (61 antes de este bloque + 8 tests nuevos).

## Bloque 4 — Frontend: decodificador EAN-13 duplicado (dominio puro, sin cola ni React — usa nada de los bloques anteriores, en paralelo)

- [x] **T129 [P]** `modelo/ean13.test.ts` — mismos vectores que `Ean13Test.java`: dígito verificador correcto e incorrecto contra `2000012012501`, un código de 13 dígitos armado a mano con otro checksum válido, largo distinto de 13, caracteres no numéricos, `null`/vacío.
- [x] **T130 [P]** `modelo/decodificadorEtiqueta.test.ts` — mismos vectores que `DecodificadorEtiquetaTest.java`: `2000012012501` con la config de ejemplo → PLU 12, 1,250 kg; dígito verificador inválido; prefijo fuera de rango; valor en cero. No conoce `cortes` (igual que su contraparte Java): devuelve el PLU crudo, no un `corteId`.
- [x] **T131** `modelo/ean13.ts` — `validarDigitoVerificador(codigo: string): boolean`, algoritmo GS1 (idéntico a `Ean13.java`).
- [x] **T132** `modelo/decodificadorEtiqueta.ts` + `modelo/configEtiqueta.ts` (tipos: `ConfigEtiqueta`, `ResultadoDecodificacion` como *discriminated union* en vez del `sealed interface` de Java — mismas 4 variantes de error más `Exito`).

Verificado: 11/11 tests nuevos OK, suite completa de frontend 71/71 OK, build y typecheck limpios.

## Bloque 5 — Frontend: cachés compartidos de `cortes` y `config_etiqueta` (refactor — necesario para que el Bloque 7 pueda decodificar offline)

- [x] **T133** Mover `useCortes` de `features/despostado/api/useCortes.ts` a `shared/api/useCortes.ts` (mismo `queryKey`, mismo comportamiento) — ya lo necesitan dos features (`despostado` y, desde esta fase, `control-diario`). `features/despostado/api/useCortes.ts` queda con un simple re-export para no tocar cada punto donde ya se importa. `CorteResponse` se movió junto (a `shared/api/types.ts`), por el mismo motivo.
- [x] **T134** Mover `useConfigEtiqueta` (el `GET`, de solo lectura) de `features/ajustes/api/useConfigEtiqueta.ts` a `shared/api/useConfigEtiqueta.ts`, mismo criterio (`ConfigEtiquetaDto` también a `shared/api/types.ts`). `useActualizarConfigEtiqueta` (el `PUT`, sigue siendo cosa de `ajustes` nada más) no se mueve.
- [x] **T135** Build + typecheck + suite de frontend completa tras el refactor: nada cambió de comportamiento, solo de ubicación. `FormularioConfigEtiqueta.test.tsx` sigue haciendo `vi.mock('../api/useConfigEtiqueta')` sin cambios — el mock intercepta el módulo re-exportado igual que si fuera la implementación real.

## Bloque 6 — Frontend: cola local de escaneos sin conexión (IndexedDB)

- [x] **T136** Agregadas `idb` (`^8.0.3`) a `dependencies` y `fake-indexeddb` (`^6.2.5`) a `devDependencies`. `import 'fake-indexeddb/auto'` acotado al propio `colaVentas.test.ts` (no al setup global de Vitest): con el pool por defecto cada archivo de test corre en su propio entorno, así que no hace falta tocar nada más.
- [x] **T137 [P]** `cola/colaVentas.test.ts` — agregar un escaneo pendiente y poder listarlo; listar devuelve en orden de creación; eliminar por `idClienteLocal`; contar refleja cuántos quedan; agregar dos veces el mismo `idClienteLocal` no duplica (mismo criterio que la unicidad del backend). `beforeEach` borra la base entera (`indexedDB.deleteDatabase`) porque fake-indexeddb la conserva entre tests del mismo archivo.
- [x] **T138** `cola/colaVentas.ts` — `abrir()` (vía `openDB` de `idb`, store `escaneos-pendientes`, `keyPath: "idClienteLocal"`), `agregar(escaneo)` (con `put`, no `add`: idempotente ante el mismo `idClienteLocal`), `listar(): Promise<EscaneoPendiente[]>` (ordenado por `creadoEn`), `eliminar(idClienteLocal)`, `contar(): Promise<number>`.

**Hallazgo, no estaba en el plan:** la primera versión abría una conexión por llamada y nunca la cerraba. Dentro del test no se notaba en cada `it` individual, pero el `beforeEach` del test siguiente (`indexedDB.deleteDatabase(...)`) se quedaba colgado esperando a que esas conexiones viejas se cerraran solas (nunca pasa) — el hook terminaba en timeout. Se corrigió con un helper `conLaBaseAbierta` que abre, hace el trabajo y cierra (`db.close()`) en un `finally`, en cada una de las 4 funciones exportadas.

Verificado: 5/5 tests nuevos OK, suite completa de frontend 76/76 OK, build limpio.

## Bloque 7 — Frontend: hooks de escaneo offline, sincronización y anulación (usa los Bloques 4, 5 y 6)

- [x] **T139** `features/control-diario/api/types.ts` — `VentaResponse` gana `usuarioId`; nuevo tipo `VentaConfirmadaOPendiente` (`{ corteNombre, kg: number, pendiente: boolean }`, una sola forma para los dos casos en vez de distinguir "pendiente" por la ausencia de `id`).
- [x] **T140** `components/CampoEscaneo.tsx` / `ResultadoEscaneo` — aplanado (`{ tipo: 'exito' } & VentaConfirmadaOPendiente` en vez de `venta: VentaResponse`). Se actualizaron también `UltimoEscaneo.tsx` (nuevo título "Pendiente de sincronizar" cuando `pendiente === true`, en vez de siempre "Descontado del stock") y los 3 tests existentes que construían el shape viejo (`CampoEscaneo.test.tsx`, `UltimoEscaneo.test.tsx`, `VentasDeHoy.test.tsx` por el `usuarioId` nuevo de T139).
- [x] **T141** `api/useEscanear.ts`, reescrito (`plan-fase3.md` 3.3): intenta `POST /ventas` igual que antes; si la promesa se resuelve (éxito o `ApiError` real de negocio), se comporta exactamente igual que en Fase 2, **sin** tocar el decodificador local. Si `fetch` falla por red (no es una `ApiError`), recién ahí decodifica localmente con `decodificadorEtiqueta.ts` + la `ConfigEtiqueta`/`cortes` ya cacheados vía `useConfigEtiqueta`/`useCortes` de `shared/api` (Bloque 5): si el decodificador local rechaza el código o el PLU no está entre los cortes activos cacheados, lanza un `ApiError` sintético con el mismo código que usaría el backend (`DIGITO_VERIFICADOR_INVALIDO`/`PREFIJO_INVALIDO`/`PESO_CERO`/`PLU_INEXISTENTE`) para que `UltimoEscaneo` no necesite saber si vino de la red o de acá; si no hay nada cacheado todavía (plan-fase3.md 3.1b), `SIN_CONEXION_SIN_CACHE`. Si decodifica y encuentra el corte, guarda el escaneo en la cola (Bloque 6) y devuelve el resultado "pendiente". **No** invalida `stock`/`resumen-dia`/`ventas` en ese camino (nada cambió todavía del lado del servidor), solo `cola-pendiente`.
- [x] **T142** `api/useSincronizarCola.ts` — nuevo. Recorre la cola **en orden de creación**, una venta a la vez: éxito → la saca de la cola e invalida `cola-pendiente` al toque (y `stock`/`resumen-dia`/`ventas` al terminar toda la pasada); `ApiError` real → la saca igual y la agrega a `noSincronizadas` (estado que devuelve el hook, para que `ControlDiarioPage` lo muestre en el Bloque 8); falla de red de nuevo → corta el recorrido de esa pasada. Se dispara con el evento `online`, cada 30 s mientras `navigator.onLine` y al montar.
- [x] **T143** `api/useColaPendiente.ts` — nuevo, `useQuery` sobre `colaVentas.contar()` con `refetchInterval: 2000`.
- [x] **T144** `api/useAnularVenta.ts` — nuevo, mutation sobre `POST /ventas/{id}/anular`; invalida `ventas`/`stock`/`resumen-dia` al tener éxito.

Verificado: suite completa de frontend 77/77 OK (76 de antes + el caso "pendiente" agregado a `UltimoEscaneo.test.tsx`), build y typecheck limpios.

## Bloque 8 — Frontend: componentes de UI

- [x] **T145 [P]** `CampoEscaneo.test.tsx`, caso nuevo: con la red caída (simulada mockeando lo que `useEscanear` ya resuelve), un código válido muestra éxito marcado `pendiente: true` y mantiene el foco. El caso de error offline no necesitó un test aparte: `CampoEscaneo` no distingue de dónde vino un `ApiError`, así que el test de error de Fase 2 ya lo cubre (mismo `MENSAJES_DE_ERROR`, sin uno nuevo en el componente).
- [x] **T146 [P]** `IndicadorPendientes.test.tsx` (nuevo componente): con la cola vacía muestra "Sin pendientes" (FR-303 pide que se muestre "en todo momento", no que se oculte); con pendientes muestra la cantidad, en singular para 1.
- [x] **T147 [P]** `VentasDeHoy.test.tsx`, casos nuevos del botón "Anular": visible para el dueño en cualquier venta no anulada (aunque sea vieja y ajena); visible para un empleado solo en una venta propia de menos de 5 minutos; **no se renderiza** (no solo deshabilitado) para un empleado en una venta ajena o de más de 5 minutos; una venta ya anulada se muestra distinguible (texto "Anulada" + tachado, no solo color) y sin botón; el click dispara la mutación con el id correcto.
- [x] **T148** `components/UltimoEscaneo.tsx` — estado "pendiente de sincronizar" (ya resuelto en el Bloque 7 junto con el aplanado del tipo: título "Pendiente de sincronizar" en vez de "Descontado del stock", Principio VI cumplido con texto, no solo color).
- [x] **T149** `components/IndicadorPendientes.tsx` — implementación, usa `useColaPendiente` (Bloque 7).
- [x] **T150** `components/VentasDeHoy.tsx` — botón "Anular" condicional (`usuarioActualId`/`puedeAnularCualquiera` como props, resueltos en `ControlDiarioPage` vía `usePerfilPropio`, mismo hook que ya usa `App.tsx`) + `useAnularVenta`.
- [x] **T151** `ControlDiarioPage.tsx` — agrega `IndicadorPendientes`, llama a `useSincronizarCola()` una vez al montar la pantalla y muestra `noSincronizadas` en un aviso si la sincronización encontró ventas que no se pudieron subir.

Verificado: 28/28 tests de componentes de `control-diario` OK, suite completa de frontend 87/87 OK, build y typecheck limpios.

## Bloque 9 — Cierre de fase

- [x] **T152** Agregados a `../quickstart.md` los pasos 23-30 de Fase 3, continuando la numeración: cortar la red de verdad vía DevTools → Network → Offline, escanear un código válido offline y ver "Pendiente de sincronizar" + el indicador de pendientes subir, un error offline que no encola nada, reconectar y ver que sincroniza solo, anular como dueño y como empleado (propia y reciente), y confirmar que el botón "Anular" no aparece para una venta ajena o vieja. De paso se reordenó la sección "Correspondencia con tests automatizados" de Fase 2, que había quedado despegada de sus propios pasos al insertar la de Fase 3.
- [ ] **T153** Ejecutar `../quickstart.md` completo (Fases 1+2+3) de punta a punta; corregir cualquier desvío. Pendiente: requiere clickear la UI real (y usar DevTools para simular offline) con el back y el front levantados — no lo puede hacer esta sesión por sí sola.
- [x] **T154** Revisado: no existe ningún feature/paquete `reportes` ni endpoint de reportes en el código (`frontend/src/features/` y `backend/.../com/carniceria/` listados completos). Las únicas coincidencias de "FR-40"/"categoría" son la columna `categoria` de `medias_reses` (`V17__categoria_animal.sql`), ya documentada como preparación explícita para Fase 4, no una implementación de reportes.

---

## Dependencias entre bloques

```
Bloque 1 (migraciones)
  → Bloque 2 (anulación backend)
      → Bloque 3 (kgVendidosHoy excluye anuladas, usa el endpoint de anular para su test)
Bloque 4 (decodificador offline, puro)         ─┐
Bloque 5 (cachés compartidos de cortes/config) ─┼→ Bloque 7 (hooks) → Bloque 8 (UI) → Bloque 9 (cierre)
Bloque 6 (cola IndexedDB)                      ─┘
```

Los Bloques 4, 5 y 6 no dependen entre sí ni del backend (2/3): pueden arrancar todos en
paralelo apenas termina el Bloque 1 (que ni siquiera los bloquea a ellos — el frontend no
necesita las migraciones para nada de esto; se las puso después solo por convención de
"backend primero" de las fases anteriores). El Bloque 7 sí necesita que los tres (4, 5, 6)
estén terminados.

## Ejemplo de ejecución en paralelo

Apenas arranca la fase, lanzar juntos (sin dependencia entre sí):

```
T117, T118                  (migraciones, Bloque 1)
T129, T130                  (tests del decodificador offline, Bloque 4)
T133, T134                  (refactor de cachés compartidos, Bloque 5)
```

Y dentro del Bloque 8, los 3 tests de componente (T145-T147) en paralelo, igual que en
Fases 1 y 2.
