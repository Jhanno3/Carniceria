# Plan de implementación: Fase 3 — Sin conexión y anulación

**Spec de origen:** `../spec.md`, sección 4
**Constitución:** `../../constitution.md` v1.0.0
**Fecha:** 2026-10-04
**Estado:** Listo para implementar — `tasks-fase3.md` generado

## 1. Resumen

De las 4 historias de usuario de `../spec.md` sección 4, **dos ya están hechas** desde el
pivot a multi-negocio de Fase 2 (no son trabajo de esta fase, se documentan acá solo para
que quede explícito qué falta y qué no):

- **US-3.2** (el empleado no ve precios de compra ni costos) — ya lo garantiza RLS desde
  `V10__multi_negocio.sql`: ningún DTO de `medias_reses`/`despostado` expone esas columnas
  fuera de la sesión del dueño.
- **US-3.3** (registro abierto con aprobación del dueño) — ya existe (`perfiles.estado`,
  flujo de aprobación, `PerfilService`), construido durante el pivot.

Lo que esta fase **sí** construye:

- **US-3.1 / FR-301-303** — cola local (IndexedDB) de escaneos cuando no hay conexión, con
  el mismo feedback visual que un escaneo online, sincronización automática al volver la
  conexión, y un contador de pendientes siempre visible.
- **US-3.4 / FR-307-308** — anular una venta: el dueño sin límite, el empleado solo la
  propia y dentro de los 5 minutos, nunca como `DELETE`.

## 2. Contexto técnico (delta sobre `../fase2/plan-fase2.md`)

| Punto | Valor |
|---|---|
| Alcance de datos nuevo | Ninguna tabla nueva. Dos políticas RLS nuevas sobre tablas existentes (`ventas`, `config_etiqueta`) — ver 3.5 y 3.1b. |
| Librería nueva (frontend) | `idb` (wrapper fino y tipado sobre IndexedDB, ~1 KB) para la cola; `fake-indexeddb` como dev-dependency (jsdom no implementa IndexedDB, hace falta para testear la cola). |
| Contrato nuevo | `POST /ventas/{id}/anular`. `VentaResponse` gana el campo `usuarioId` (3.7). |

## 3. Decisiones de esta fase

### 3.1 El decodificador se duplica en el frontend, solo para el modo offline

Decisión tomada con el dueño: a diferencia de Fase 2 ("el decodificador vive solo en el
backend"), FR-301 exige mostrar éxito (corte + kg) **sin** poder llamar al backend. Se
reimplementa `Ean13`/`DecodificadorEtiqueta`/`ConfigEtiqueta` como funciones puras en
`frontend/src/features/control-diario/modelo/` (TypeScript, con sus propios tests —
mismos vectores que `Ean13Test.java`/`DecodificadorEtiquetaTest.java`), igual patrón que
ya existe para las fórmulas de Despostado (`../fase1/research.md`, Fase 1: una fórmula, dos
implementaciones con tests propios cada una).

El backend sigue siendo la autoridad: cuando hay conexión, `POST /ventas` nunca usa el
decodificador del frontend, solo lo usa para decidir si encolar y qué mostrar mientras no
hay conexión. Al sincronizar, el backend vuelve a decodificar desde cero — si hubo un
desvío (p. ej. la config de etiqueta cambió mientras el dispositivo estaba offline), la
venta sincronizada puede fallar con un error real; ver 3.4.

### 3.1b `config_etiqueta` pasa a ser legible también por `empleado` (nueva política)

Para que el decodificador offline del punto anterior funcione en la sesión de un
**empleado** (no solo del dueño), su navegador necesita poder leer la `config_etiqueta` de
su negocio al menos una vez mientras hay conexión, para cachearla. Esto revierte una
decisión explícita de `../fase2/plan-fase2.md` ("el empleado nunca necesita leerla directo"), pero
no compromete el Principio II: `config_etiqueta` no tiene ningún precio, costo ni dato
sensible — son posiciones y rangos del formato de etiqueta, igual de visibles para
cualquiera que mire físicamente una etiqueta de la balanza. Se agrega
`config_etiqueta_empleado_select` (`select`, acotada a `dueno_id = mi_negocio_id()`, sin
`insert`/`update`/`delete`) en `V19__config_etiqueta_empleado_select.sql`.

**Caso borde documentado (no resuelto con más código, es una limitación aceptada):** si la
pantalla de Control diario se abre por primera vez ya sin conexión (nunca se cacheó
`config_etiqueta` ni el catálogo de `cortes`), el frontend no tiene con qué decodificar
localmente. En ese caso el campo de escaneo se deshabilita con un aviso ("Sin conexión y
sin datos para trabajar offline — conectate al menos una vez") en vez de intentar encolar
un escaneo que no se puede ni mostrar. No bloquea FR-301 (que asume que hubo conexión antes
en el turno, caso normal de un mostrador que arranca el día online).

### 3.2 Diseño de la cola local (IndexedDB)

- Base `carniceria-cola-ventas`, un único object store `escaneos-pendientes`, `keyPath: "idClienteLocal"`.
- Cada registro: `{ idClienteLocal, codigo, decodificado: { corteId, corteNombre, kg }, creadoEn }`.
  `decodificado` es el resultado del decodificador local (3.1) — se guarda ya resuelto para
  no tener que volver a decodificar al dibujar la lista de pendientes.
- Acceso vía `idb` (`openDB`, `.add`/`.getAll`/`.delete`), envuelto en
  `frontend/src/features/control-diario/cola/colaVentas.ts` (funciones puras async, sin
  React) — mismo criterio de capas que el resto del repo: la lógica no vive en un
  componente ni en un hook, el hook solo la invoca.
- Tests: `fake-indexeddb/auto` importado en el setup de Vitest (`vitest.setup.ts` o
  equivalente) solo para los tests de `colaVentas`, igual que `jsdom` ya cubre el DOM.

### 3.3 Cuándo se encola (detección de "sin conexión")

**Decisión: intento de red primero (igual que Fase 2), el decodificador local (3.1) solo
entra en juego si esa llamada falla por red** — no se decodifica dos veces en el camino
feliz (Principio VII: no agregar trabajo que nadie pidió; FR-301 solo exige el mismo
feedback *cuando no hay conexión*, no una respuesta más rápida cuando sí la hay). Tampoco
se confía únicamente en `navigator.onLine` para decidir si hay conexión: es una señal de
"hay una interfaz de red", no de "el backend responde" (puede dar `true` con el wifi
prendido pero sin internet real). Flujo de `useEscanear`:

1. Intentar `POST /ventas` igual que en Fase 2.
   - Responde (`200`/`201`/`4xx` real del backend): se usa esa respuesta tal cual, sin
     tocar el decodificador local para nada — mismo comportamiento que hoy.
   - Falla por red (`fetch` tira `TypeError`, timeout, etc. — nunca llegó respuesta):
     recién ahí se decodifica localmente (3.1).
2. Si el decodificador local rechaza el código (dígito verificador, prefijo, PLU
   inexistente, peso cero), se muestra ese error al toque — mismo criterio que si hubiera
   respondido el backend, no se encola nada.
3. Si decodifica bien, se guarda en la cola (3.2) y se muestra éxito con ese resultado,
   marcado como "pendiente de sincronizar".

### 3.4 Sincronización al volver la conexión

- Dispara con el evento `online` del navegador y, además, cada 30 segundos mientras
  `navigator.onLine` sea `true` y la cola no esté vacía (red de seguridad: el evento
  `online` no siempre dispara de forma confiable en todos los navegadores/redes).
- Recorre la cola **en orden de creación** (campo `creadoEn`), una venta a la vez (no en
  paralelo — simplicidad y para no saturar si hay muchas pendientes tras un corte largo).
- Por cada una, `POST /ventas` con el mismo `codigo`/`idClienteLocal` originales:
  - `200`/`201` → se borra de la cola (éxito, con o sin duplicado — la unicidad de
    `idClienteLocal` en el backend, FR-302, ya la cubre `V13__ventas.sql` desde Fase 2).
  - `4xx` real → se borra igual de la cola (reintentarla no la va a arreglar) y se agrega a
    una lista de "no se pudieron sincronizar" que se le muestra al usuario para que decida
    qué hacer a mano (ningún reintento infinito de algo que ya sabemos que va a fallar
    siempre igual).
  - Falla de red de nuevo → se deja en la cola, se corta el recorrido de esta pasada (si la
    primera de la cola no pudo salir, asumimos que seguimos sin conexión real y no tiene
    sentido intentar las demás ahora).

### 3.5 Anulación de ventas (FR-307/308)

- `V18__ventas_anulacion.sql`: política nueva `ventas_empleado_anular` (`for update`,
  `using (usuario_id = auth.uid() and fecha_hora > now() - interval '5 minutes')`,
  `with check (usuario_id = auth.uid())`). El dueño ya puede actualizar cualquier fila de
  `ventas` de su negocio por la política `ventas_dueno_todo` de `V13__ventas.sql` (`for
  all`), no hace falta una política nueva para él.
- **Defense in depth, mismo patrón que el resto del proyecto** (ver `ConfigEtiquetaRepository.actualizar`,
  Fase 2): `VentaService.anular(id, usuarioId)` hace primero un `findById` (ya acotado por
  RLS de `select`, que sí deja ver ventas ajenas del propio negocio) para distinguir
  `404 VENTA_NO_ENCONTRADA` (no existe o es de otro negocio) de un intento bloqueado por la
  regla de negocio; después ejecuta un `UPDATE ... where id = :id` explícito (no vía
  `save()` de una entidad ya cargada — mismo motivo que `CorteRepository.actualizar`: que un
  `UPDATE` que RLS bloquea se note como `0` filas afectadas) y, si afectó `0` filas, lanza
  `403 VENTA_NO_SE_PUEDE_ANULAR` ("la venta es de otro empleado o pasaron más de 5
  minutos"). El `dueno` nunca cae en ese camino porque su propia política ya lo deja pasar
  siempre.
- **Idempotente dentro de la ventana:** anular una venta ya anulada vuelve a poner
  `anulada = true` sin error (no hay ningún estado intermedio que proteger). Fuera de la
  ventana de 5 minutos con la venta ya anulada, un empleado recibe el mismo
  `403 VENTA_NO_SE_PUEDE_ANULAR` de siempre — no es un caso que la spec pida distinguir.
- **Nunca un `DELETE`** (FR-307, Principio I): no se agrega ningún endpoint ni método que
  borre una fila de `ventas`.
- La ventana de 5 minutos se evalúa siempre contra `ventas.fecha_hora` (hora del
  **servidor**, columna ya persistida), nunca contra el reloj del dispositivo que hace el
  pedido — cubre el caso borde de `../spec.md` 4.3 sin ningún código extra (ya es así por
  construcción: la comparación vive en la política SQL, no en Java).

### 3.6 Habilitar o esconder el botón "Anular" sin intentos fallidos

`VentaResponse` (ver `../fase2/contracts/control-diario-api.md`) gana el campo `usuarioId`. El
frontend decide cuándo mostrar el botón sin necesidad de intentarlo primero: el dueño lo ve
siempre; el empleado solo si `usuarioId` es el suyo **y** `fechaHora` tiene menos de 5
minutos (comparado con el reloj del propio dispositivo, solo para la UI — la autoridad
real sigue siendo la política SQL de 3.5, que usa la hora del servidor). Si el reloj del
dispositivo está adelantado/atrasado, en el peor caso se le muestra u oculta el botón de
más, pero el backend igual decide bien.

### 3.8 `kgVendidosHoy` pasa a excluir ventas anuladas (ajuste a `ResumenDiaService`)

`ResumenDiaService.calcular` (Fase 2) sumaba **todas** las ventas del día sin filtrar,
con un comentario explícito de que era un stopgap ("no hay anulación todavía en esta
fase" — `../fase2/data-model-fase2.md`). Ahora que la anulación existe, dejarlo así generaría una
inconsistencia con `stockVendibleTotal` (que sale de `stock_por_corte`, una vista que **ya**
excluye `anulada = true` desde `V15`): el stock volvería a subir al anular, pero
"Kg vendidos hoy" seguiría contando la venta anulada como si nada. Se corrige filtrando
`kgVendidosHoy` por `!anulada`. `etiquetasEscaneadasHoy` **no** cambia: sigue contando todos
los escaneos válidos del día, anulados incluidos — es una métrica de actividad del
mostrador ("cuántas veces se escaneó"), no de ventas netas, y la propia spec ya las
distingue como dos números separados en FR-206.

### 3.9 Fuera de alcance de esta fase (Principio VII)

- **Reintento con backoff exponencial o cola persistida entre pestañas/dispositivos:** no
  hace falta — un mostrador es un único dispositivo con una única pestaña abierta todo el
  turno; `localStorage`/IndexedDB del navegador ya sobrevive a un refresh de página, que es
  el único caso real a cubrir.
- **Notificación push o sonido cuando termina de sincronizar:** no lo pide ningún FR de
  esta fase; el contador de pendientes (FR-303) ya es suficiente feedback visual.
- **Reportes (Fase 4):** sin cambios acá.

## 4. Chequeo contra la constitución

| Principio | Cumplimiento en este plan |
|---|---|
| I. Integridad de los datos | Anular nunca es un `DELETE` (3.5); `id_cliente_local unique` (ya desde Fase 2) sigue dando idempotencia también para lo que sale de la cola (3.4). |
| II. Seguridad | Nueva política RLS para anulación acotada a dueño propio de la venta y ventana de tiempo (3.5), evaluada con hora de servidor; `config_etiqueta` ahora legible por `empleado` pero sin ningún dato sensible (3.1b); ningún precio/costo nuevo se expone en `VentaResponse` (`usuarioId` no es sensible). |
| III. Cálculos confiables | El decodificador offline (3.1) es función pura con tests propios, mismos vectores que su contraparte backend. |
| IV. El mostrador no se detiene | Es el objetivo central de esta fase: seguir vendiendo sin conexión (3.3), sin esperar a que vuelva internet. |
| V. Hecho para Argentina | La ventana de anulación y toda fecha siguen en `America/Argentina/Buenos_Aires`, sin cambios sobre Fase 1/2. |
| VI. Usabilidad y accesibilidad | El contador de pendientes y el estado "pendiente de sincronizar" nunca dependen solo de color (mismo criterio que "Queda poco" en Fase 2); el botón "Anular" deshabilitado/oculto evita clicks que solo van a fallar. |
| VII. Simplicidad | Sincronización secuencial simple, sin colas distribuidas ni reintentos infinitos (3.4, 3.7); la anulación reutiliza el mismo patrón de "`UPDATE` explícito + chequeo de filas afectadas" que ya existe en el proyecto, no inventa uno nuevo. |
| VIII. Cambios controlados | `V18`/`V19`, versionadas con Flyway, cada una con un único propósito. |

## 5. Estructura del proyecto (nuevo sobre `../fase2/plan-fase2.md`)

```
backend/src/main/resources/db/migration/
  V18__ventas_anulacion.sql
  V19__config_etiqueta_empleado_select.sql
backend/src/main/java/com/carniceria/escaneo/
  service/VentaService.java           (+ anular(id, usuarioId))
  service/VentaNoEncontradaException.java, VentaNoSePuedeAnularException.java   (nuevas)
  repository/VentaRepository.java     (+ anular(id): UPDATE explícito)
  controller/VentaController.java     (+ POST /ventas/{id}/anular)
  dto/VentaResponse.java              (+ usuarioId)

frontend/src/features/control-diario/
  modelo/ean13.ts, decodificadorEtiqueta.ts, configEtiqueta.ts   (puros, con tests — 3.1)
  cola/colaVentas.ts                  (acceso a IndexedDB vía `idb`, con tests — 3.2)
  api/useEscanear.ts                  (reescrito: intento de red + fallback a cola — 3.3)
  api/useSincronizarCola.ts           (nuevo — 3.4)
  api/useAnularVenta.ts               (nuevo)
  api/useColaPendiente.ts             (nuevo — cuenta la cola para FR-303)
  components/IndicadorPendientes.tsx  (nuevo)
  components/UltimoEscaneo.tsx        (+ estado "pendiente de sincronizar")
  components/VentasDeHoy.tsx          (+ botón "Anular" condicional — 3.6)
```

## 6. Diseño

- **Datos:** sin tablas nuevas; dos políticas RLS nuevas sobre `ventas`/`config_etiqueta`
  (ver 3.1b y 3.5). No hace falta un `data-model-fase3.md` separado — se documentan como
  una sección añadida a `../fase2/data-model-fase2.md`.
- **API:** `../fase2/contracts/control-diario-api.md` — se agrega `POST /ventas/{id}/anular` y el
  campo `usuarioId` en `VentaResponse`.
- **Cola local:** no es una API, es puramente del lado del navegador — diseño en 3.2-3.4 de
  este documento, no hay contrato HTTP nuevo para ella (reusa `POST /ventas` tal cual).
- **Verificación:** se agrega a `../quickstart.md` en `tasks-fase3.md` (último bloque),
  continuando la numeración de Fase 1+2.

## 7. Enfoque para generar tareas (no se ejecuta en este documento)

Mismo orden TDD que las fases anteriores:
1. Migraciones (`V18`, `V19`).
2. Backend: anulación (`VentaService.anular`, excepciones, endpoint, `usuarioId` en
   `VentaResponse`) — tests de integración primero (dueño sin límite, empleado propio
   dentro de la ventana, empleado fuera de la ventana, empleado sobre venta ajena, venta
   inexistente).
3. Frontend: decodificador offline duplicado (`modelo/`) — tests primero, mismos vectores
   que el backend.
4. Frontend: cola IndexedDB (`cola/colaVentas.ts`) — tests primero con `fake-indexeddb`.
5. Frontend: `useEscanear` reescrito (intento de red + fallback), `useSincronizarCola`,
   `useColaPendiente`.
6. Frontend: componentes (`IndicadorPendientes`, `UltimoEscaneo` con estado "pendiente",
   `VentasDeHoy` con botón "Anular" condicional, `useAnularVenta`).
7. Cierre: pasos manuales agregados a `../quickstart.md` (incluye desconectar la red de
   verdad, ej. DevTools "Offline", para probar la cola).

## 8. Seguimiento de progreso

- [x] Spec revisada (`../spec.md`, sección 4)
- [x] Chequeo contra la constitución (sección 4 de este documento)
- [x] Decisiones de diseño (sección 3 de este documento)
- [x] `../fase2/data-model-fase2.md` (sección agregada para `ventas`/`config_etiqueta` de esta fase)
- [x] `../fase2/contracts/control-diario-api.md` (actualizado)
- [x] `tasks-fase3.md`
