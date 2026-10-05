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

- [ ] **T129 [P]** `modelo/ean13.test.ts` — mismos vectores que `Ean13Test.java`: dígito verificador correcto e incorrecto contra `2000012012501`, un código de 13 dígitos armado a mano con otro checksum válido, largo distinto de 13, caracteres no numéricos, `null`/vacío.
- [ ] **T130 [P]** `modelo/decodificadorEtiqueta.test.ts` — mismos vectores que `DecodificadorEtiquetaTest.java`: `2000012012501` con la config de ejemplo → PLU 12, 1,250 kg; dígito verificador inválido; prefijo fuera de rango; valor en cero. No conoce `cortes` (igual que su contraparte Java): devuelve el PLU crudo, no un `corteId`.
- [ ] **T131** `modelo/ean13.ts` — `validarDigitoVerificador(codigo: string): boolean`, algoritmo GS1 (idéntico a `Ean13.java`).
- [ ] **T132** `modelo/decodificadorEtiqueta.ts` + `modelo/configEtiqueta.ts` (tipos: `ConfigEtiqueta`, `ResultadoDecodificacion` como *discriminated union* en vez del `sealed interface` de Java — mismas 4 variantes de error más `Exito`).

## Bloque 5 — Frontend: cachés compartidos de `cortes` y `config_etiqueta` (refactor — necesario para que el Bloque 7 pueda decodificar offline)

- [ ] **T133** Mover `useCortes` de `features/despostado/api/useCortes.ts` a `shared/api/useCortes.ts` (mismo `queryKey`, mismo comportamiento) — ya lo necesitan dos features (`despostado` y, desde esta fase, `control-diario`). `features/despostado/api/useCortes.ts` queda com un simple re-export para no tocar cada punto donde ya se importa.
- [ ] **T134** Mover `useConfigEtiqueta` (el `GET`, de solo lectura) de `features/ajustes/api/useConfigEtiqueta.ts` a `shared/api/useConfigEtiqueta.ts`, mismo criterio. `useActualizarConfigEtiqueta` (el `PUT`, sigue siendo cosa de `ajustes` nada más) no se mueve.
- [ ] **T135** Build + typecheck + suite de frontend completa tras el refactor: nada debería cambiar de comportamiento, solo de ubicación.

## Bloque 6 — Frontend: cola local de escaneos sin conexión (IndexedDB)

- [ ] **T136** Agregar `idb` a `dependencies` y `fake-indexeddb` a `devDependencies` (jsdom no implementa IndexedDB; hace falta para testear la cola). Registrar `import 'fake-indexeddb/auto'` en el setup de Vitest, acotado a los tests que la necesitan.
- [ ] **T137 [P]** `cola/colaVentas.test.ts` — agregar un escaneo pendiente y poder listarlo; listar devuelve en orden de creación; eliminar por `idClienteLocal`; contar refleja cuántos quedan; agregar dos veces el mismo `idClienteLocal` no duplica (mismo criterio que la unicidad del backend).
- [ ] **T138** `cola/colaVentas.ts` — `abrir()` (vía `openDB` de `idb`, store `escaneos-pendientes`, `keyPath: "idClienteLocal"`), `agregar(escaneo)`, `listar(): Promise<EscaneoPendiente[]>` (ordenado por `creadoEn`), `eliminar(idClienteLocal)`, `contar(): Promise<number>`.

## Bloque 7 — Frontend: hooks de escaneo offline, sincronización y anulación (usa los Bloques 4, 5 y 6)

- [ ] **T139** `features/control-diario/api/types.ts` — `VentaResponse` gana `usuarioId`; nuevo tipo para el resultado "pendiente de sincronizar" (sin `id` propio todavía: `{ corteNombre, kg, codigoLeido, idClienteLocal, pendiente: true }`).
- [ ] **T140** `components/CampoEscaneo.tsx` / `ResultadoEscaneo` — aplanar el tipo (`corteNombre`/`kg`/`pendiente` directo en vez de `venta: VentaResponse`), para que tanto un escaneo confirmado como uno pendiente quepan en la misma forma.
- [ ] **T141** `api/useEscanear.ts`, reescrito (`plan-fase3.md` 3.3): intenta `POST /ventas` igual que antes; si la promesa se resuelve (éxito o `ApiError` real de negocio), se comporta exactamente igual que en Fase 2, **sin** tocar el decodificador local. Si `fetch` falla por red (no es una `ApiError`), recién ahí decodifica localmente con `decodificadorEtiqueta.ts` + la `ConfigEtiqueta` y los `cortes` ya cacheados (Bloque 5): si el decodificador local rechaza el código, lanza un `ApiError` sintético con el mismo `codigo`/`mensaje` que usaría el backend (para que `UltimoEscaneo` no necesite saber si vino de la red o de acá); si decodifica bien, busca el PLU entre los `cortes` activos cacheados (si no está, es el mismo caso que `PLU_INEXISTENTE`), guarda el escaneo en la cola (Bloque 6) y devuelve el resultado "pendiente" del T139. **No** invalida `stock`/`resumen-dia`/`ventas` en el camino "pendiente" (nada cambió todavía del lado del servidor).
- [ ] **T142** `api/useSincronizarCola.ts` — nuevo. Recorre la cola **en orden de creación**, una venta a la vez: `POST /ventas` con el `codigo`/`idClienteLocal` originales; éxito (`200`/`201`) → la saca de la cola e invalida `stock`/`resumen-dia`/`ventas`/el conteo de pendientes; `ApiError` real → la saca igual (no tiene sentido reintentar algo que va a fallar siempre igual) y la agrega a una lista de "no se pudieron sincronizar" para mostrarle al usuario; falla de red de nuevo → corta el recorrido de esa pasada, la deja en la cola. Se dispara con el evento `online` del navegador, cada 30 s mientras `navigator.onLine` y la cola no esté vacía, y una vez al montar (cubre el caso de reabrir la pestaña ya con conexión y una cola pendiente de la sesión anterior).
- [ ] **T143** `api/useColaPendiente.ts` — nuevo, `useQuery` sobre `colaVentas.contar()` con `refetchInterval` corto (cola local, consulta barata) para que el contador de FR-303 se actualice solo.
- [ ] **T144** `api/useAnularVenta.ts` — nuevo, mutation sobre `POST /ventas/{id}/anular`; invalida `ventas`/`stock`/`resumen-dia` al tener éxito (anular le devuelve kg al stock, mismo criterio que una venta nueva lo descuenta).

## Bloque 8 — Frontend: componentes de UI

- [ ] **T145 [P]** `CampoEscaneo.test.tsx`, casos nuevos: con la red caída, un código válido muestra éxito marcado "pendiente" y no limpia el contador en `0`; con la red caída, un código inválido localmente muestra el mismo mensaje de error que mostraría online (mismo mapa `MENSAJES_DE_ERROR`, sin uno nuevo); el campo mantiene el foco en ambos casos (FR-201 sigue valiendo offline).
- [ ] **T146 [P]** `IndicadorPendientes.test.tsx` (nuevo componente): muestra "Sin pendientes" (o se oculta, a definir en la implementación) cuando la cola está vacía; muestra la cantidad cuando no.
- [ ] **T147 [P]** `VentasDeHoy.test.tsx`, casos nuevos del botón "Anular": visible y habilitado para el dueño en cualquier venta no anulada; visible y habilitado para un empleado solo en una venta propia (`usuarioId` propio) de menos de 5 minutos; **no se renderiza** para un empleado en una venta ajena o de más de 5 minutos (no solo deshabilitado — ver `plan-fase3.md` 3.6, evita clicks que solo van a fallar); una venta ya anulada se muestra distinguible (no solo por color) y sin botón.
- [ ] **T148** `components/UltimoEscaneo.tsx` — estado "pendiente de sincronizar" (ícono/etiqueta de texto, no solo un color distinto, Principio VI).
- [ ] **T149** `components/IndicadorPendientes.tsx` — implementación, usa `useColaPendiente` (Bloque 7).
- [ ] **T150** `components/VentasDeHoy.tsx` — botón "Anular" condicional (necesita saber quién es el usuario actual y su rol, ya disponible donde sea que el resto de la app lo resuelve hoy) + `useAnularVenta`.
- [ ] **T151** `ControlDiarioPage.tsx` — agrega `IndicadorPendientes`, llama a `useSincronizarCola()` una vez al montar la pantalla.

## Bloque 9 — Cierre de fase

- [ ] **T152** Agregar a `../quickstart.md` los pasos manuales de Fase 3, continuando la numeración existente: cortar la red de verdad (DevTools → Network → Offline, no alcanza con `navigator.onLine` simulado desde la consola), escanear un código válido y ver el indicador de pendientes subir, reconectar y ver que sincroniza solo y el indicador vuelve a `0`; anular una venta como dueño (sin límite) y como empleado (propia y reciente); intentar anular como empleado una venta ajena o vieja y confirmar que no aparece el botón.
- [ ] **T153** Ejecutar `../quickstart.md` completo (Fases 1+2+3) de punta a punta; corregir cualquier desvío.
- [ ] **T154** Revisar `../spec.md` sección 5 (Fase 4 — Reportes) y confirmar que esta implementación no adelantó nada de reportes ni de exportación.

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
