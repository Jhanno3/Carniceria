# Tareas — Fase 6: Tipo de entrada ("Corte") en Despostado

**Entradas:** `../spec.md` (Fases 1-5), `../../constitution.md`, `../fase5/tasks-fase5.md`.
Igual que la Fase 5, esta fase se pidió directo por chat — los FR-6xx de abajo son nuevos,
documentados acá mismo (no están en `spec.md` todavía, ver nota al final).

**Orden:** TDD — en cada bloque, los tests se escriben y deben fallar antes de la
implementación que los hace pasar (Principio III de la constitución).
**Capas:** `controller/` → `service/` → `modelo/` (dominio puro, sin Spring/JPA) →
`repository/` → `entity/`, con `dto/` para los contratos. Frontend: `modelo/` (si hace
falta) separado de `components/` y `api/`.
**`[P]`** = se puede hacer en paralelo con las otras tareas `[P]` del mismo bloque (archivos
distintos, sin dependencia entre ellas).

Numeración continúa desde `../fase5/tasks-fase5.md` (Fase 5 llegó hasta T220). Una
migración nueva: `V25__medias_reses_tipo_entrada.sql` (Bloque 1).

## Alcance y origen (pedido del dueño, verbatim resumido)

En la pantalla "Despostado", agregar una selección nueva a la derecha de "Categoría del
animal", llamada **"Corte"** en la UI (puertas adentro le decimos **`tipoEntrada`**, para no
confundirlo con la tabla `cortes` ya existente — el catálogo de piezas con PLU como "Vacío"
o "Asado"). Representa **qué parte de la media res llegó** como entrada: a veces llega la
media res entera, a veces el carnicero ya compró un corte comercial más chico (un "pecho",
un "mocho", etc.), y en ese caso no tiene sentido mostrarle como cargables los cortes que no
están en lo que compró.

Según el tipo elegido, la tabla "Cortes vendibles" atenúa (gris, sin poder tipear kg) los
cortes del catálogo que no pertenecen a ese tipo. "Media res" (el valor por defecto, igual
al comportamiento actual) no atenúa nada.

## Decisiones ya tomadas con el dueño (no volver a preguntar)

- **Se persiste**: `medias_reses` gana una columna `tipo_entrada`, igual criterio que
  `categoria` — sobrevive a un refresh y sirve a futuro para filtrar/reportar por tipo de
  entrada. A diferencia de `categoria` (nullable, "no especificado" es un valor legítimo),
  `tipo_entrada` es **`not null` con default `'MediaRes'`**: toda entrada es "algo", y "media
  res" es justamente el caso "sin restricción, todo disponible" — no un "no sé". Las filas
  viejas (anteriores a esta fase) quedan en `'MediaRes'` por el default de la columna, que es
  el comportamiento correcto: la UI vieja nunca restringía nada.
- **Gris, no oculto**: los cortes fuera del tipo elegido siguen en la tabla "Cortes
  vendibles", atenuados visualmente (mismo criterio que los cortes inactivos de "Editar
  cortes", Fase 5) y con el campo de kg deshabilitado. No se ocultan filas.
- **Es solo una restricción de interfaz**: el backend sigue aceptando kg para cualquier
  corte activo, sin importar el `tipoEntrada` guardado (Principio IV — no bloquear al
  mostrador/carnicero; mismo espíritu que "venta sin precio_venta se registra igual" de
  Fase 5). `tipoEntrada` no participa de ninguna validación de `despostado`/`CorteKgDto` en
  el backend — el mapeo tipo→cortes habilitados vive **solo en el frontend**.
- **Nombre interno `tipoEntrada`** (Java: enum `MediaResEntity.TipoEntrada`; columna
  `tipo_entrada`; frontend: tipo `TipoEntrada`), para no pisar el concepto ya existente de
  "corte" (catálogo con PLU). La UI sigue mostrando la palabra **"Corte"** como etiqueta del
  selector — es una decisión de producto, no hace falta que el código use esa palabra.
- **"Bife entero" (Parrillero) = "Bife ancho" + "Bife angosto"**: el catálogo no tiene un
  corte llamado "Bife entero"; Parrillero incluye los dos cortes que sí existen.
- **"Restablecer ejemplo" fuerza `tipoEntrada` a "Media res"**: el ejemplo canónico de 100 kg
  (sección 2.2 de la especificación) reparte kilos en cortes de distintos tipos a la vez
  (lomo, cuadril, asado, etc.), así que solo tiene sentido mostrado sin ninguna atenuación.
- **El modo automático (estimación histórica) no filtra por `tipoEntrada`**: sigue
  estimando sobre el % histórico de todos los cortes, igual que hoy. Si el usuario eligió un
  tipo que no es "Media res", puede llegar a prellenar un corte atenuado — queda tal cual
  (no se le borra el valor, el usuario decide si lo corrige o lo deja).

## Notas sobre nombres que podrían parecer inconsistencias (no lo son)

- **"Pecho" es ambiguo a propósito**: existe un corte individual "Pecho" en el catálogo
  (PLU propio) y, separado, un tipo de entrada comercial "Pecho" que *no* incluye ese corte
  (incluye Paleta/Roast beef/Cogote/Falda). Es terminología real del oficio — ambos se
  llaman igual y son cosas distintas, no es un bug.
- **"Delantero" (tipo de entrada) no coincide con `cortes.cuarto = 'Delantero'`**: el tipo
  de entrada "Delantero" incluye Matambre y Vacío, que en el catálogo tienen
  `cuarto = 'Trasero'`. Son dos clasificaciones independientes (una es anatómica, la otra es
  cómo se vende comercialmente un corte grande) — no hay que "corregir" `cuarto` ni derivar
  un mapeo del otro.

## Mapeo de cortes por tipo de entrada (nombres tal como están en el catálogo)

| `tipoEntrada` | Etiqueta en la UI | Cortes habilitados |
|---|---|---|
| `MediaRes` | Media res | *(todos — sin atenuar nada)* |
| `Delantero` | Delantero | Paleta, Roast beef, Matambre, Asado, Vacío, Falda, Cogote |
| `Pecho` | Pecho | Paleta, Roast beef, Cogote, Falda |
| `Parrillero` | Parrillero | Asado, Falda, Vacío, Bife ancho, Bife angosto, Lomo |
| `AsadoCompleto` | Asado completo | Asado, Vacío, Matambre |
| `Mocho` | Mocho | Nalga, Cuadrada, Peceto, Bola de lomo, Cuadril, Tortuga, Osobuco |
| `Rueda` | Rueda | Nalga, Cuadrada, Peceto, Bola de lomo, Tortuga, Osobuco *(= Mocho sin Cuadril)* |

Esta tabla es la fuente de verdad para `frontend/.../modelo/tiposDeEntrada.ts` (Bloque 2) —
cualquier corte del catálogo que no aparezca en una fila queda atenuado para ese tipo
(ej. Aguja, Asado americano, Carne picada/recortes, Chingolo, Colita de cuadril, Entraña,
Espinazo, Marucha, Tapa de asado, Tapa de nalga, Pecho (el corte) no pertenecen a ningún
tipo salvo "Media res").

## Requisitos funcionales nuevos (FR-6xx)

| ID | Requisito |
|---|---|
| FR-601 | Al cargar o editar una entrada en "Despostado", el dueño elige un "Corte" (tipo de entrada): Media res (default), Delantero, Pecho, Parrillero, Asado completo, Mocho o Rueda. Se persiste en `medias_reses.tipo_entrada`. |
| FR-602 | Según el tipo de entrada elegido, la tabla "Cortes vendibles" atenúa (gris, campo de kg deshabilitado) los cortes del catálogo que no pertenecen a ese tipo (ver tabla de mapeo); "Media res" no atenúa ninguno. Es una restricción de interfaz nada más — el backend acepta kg para cualquier corte activo sin importar `tipoEntrada` (Principio IV). |

## Casos borde

- Un corte del catálogo fue creado por el dueño después de esta fase (no está en la tabla de
  mapeo de ningún tipo salvo "Media res"): queda atenuado en cualquier tipo que no sea
  "Media res", igual que los cortes "sueltos" ya listados arriba — no hace falta que el
  dueño haga nada para que esto pase.
- Cambiar el tipo de entrada después de haber tipeado kilos en cortes que con el nuevo tipo
  quedan atenuados: esos valores **no se borran** (se deshabilita el input, no se limpia su
  contenido) y, si el backend no los rechaza, se guardan igual al cargar la entrada.
- Un valor de `tipoEntrada` inválido en el body de `POST`/`PUT /medias-reses` (typo, valor
  viejo que ya no existe): `400 TIPO_ENTRADA_INVALIDO`, mismo patrón que
  `CuartoInvalidoException`/`CategoriaInvalidaException`.
- `tipoEntrada` ausente en el body: se asume `MediaRes` (no es obligatorio tipearlo desde
  afuera de la UI, ej. un script o Postman).

---

## Bloque 1 — Backend: persistir `medias_reses.tipo_entrada`

- [x] **T221 [P]** `MediaResControllerTest`, casos nuevos: `POST /medias-reses` con
  `tipoEntrada: "Pecho"` lo persiste y lo devuelve en la respuesta; sin `tipoEntrada` en el
  body, lo guarda como `"MediaRes"`; con `tipoEntrada: "Invalido"` devuelve `400
  TIPO_ENTRADA_INVALIDO` y no se crea ninguna entrada; `PUT /medias-reses/{id}` también lo
  actualiza (cambiar de `"MediaRes"` a `"Rueda"` y confirmar que el `GET` posterior lo
  refleja).
- [x] **T222** `V25__medias_reses_tipo_entrada.sql` — `alter table medias_reses add column
  tipo_entrada text not null default 'MediaRes' check (tipo_entrada in ('MediaRes',
  'Delantero', 'Pecho', 'Parrillero', 'AsadoCompleto', 'Mocho', 'Rueda'))`. Sin backfill
  explícito: el `default` ya resuelve las filas viejas.
- [x] **T223** `MediaResEntity` — enum `TipoEntrada` (`MediaRes, Delantero, Pecho,
  Parrillero, AsadoCompleto, Mocho, Rueda`) + campo `tipoEntrada` (`@Enumerated(STRING)`,
  `@Column(nullable = false)`, mismo estilo que `Categoria` pero sin permitir null).
  `CargarEntradaRequest` agrega `tipoEntrada: String` (opcional — ausente/blank se trata
  como `"MediaRes"`, mismo criterio permisivo que el resto de la API). `MediaResResponse`
  agrega `tipoEntrada: String` (siempre presente, nunca null). `MediaResService.cargarEntrada`/
  `actualizar` parsean con `parsearTipoEntrada` (default `MediaRes` si viene null/blank,
  `TipoEntradaInvalidoException` si no matchea ningún valor del enum — mismo patrón que
  `parsearCategoria`/`parsearCuarto` de `CorteService`). `TipoEntradaInvalidoException`
  (`400`, mismo patrón que `CategoriaInvalidaException`).
  **Nota:** también hubo que actualizar 2 call sites que construían `CargarEntradaRequest`/
  `MediaResEntity` directamente por posición (`EstimacionControllerTest`,
  `ReporteControllerTest`, `DespostadoRepositoryTest`) — mismo tipo de ajuste mecánico que
  en Fase 5 con `CorteEntity`.

---

## Bloque 2 — Frontend: mapeo de cortes por tipo de entrada

- [x] **T224 [P]** `features/despostado/modelo/tiposDeEntrada.test.ts`: `cortesHabilitados
  ('MediaRes')` devuelve `null` (sin restricción); `cortesHabilitados('Pecho')` devuelve un
  `Set` con exactamente `{Paleta, Roast beef, Cogote, Falda}`; `cortesHabilitados('Rueda')`
  no incluye `Cuadril` pero sí los demás cortes de `Mocho`; `TIPOS_DE_ENTRADA` tiene las 7
  entradas en el orden de la tabla de mapeo, cada una con su `etiqueta` para la UI.
- [x] **T225** `features/despostado/modelo/tiposDeEntrada.ts` — `TipoEntrada` (type union de
  los 7 valores), `TIPOS_DE_ENTRADA: { valor: TipoEntrada; etiqueta: string }[]` (fuente de
  la tabla de mapeo de este documento), `cortesHabilitados(tipo: TipoEntrada): Set<string> |
  null`.
- [x] **T226 [P]** `features/despostado/api/types.ts` — importa/re-exporta `TipoEntrada` de
  `modelo/tiposDeEntrada.ts`; `MediaResResponse.tipoEntrada: TipoEntrada` (siempre presente);
  `CargarEntradaRequest.tipoEntrada?: TipoEntrada | null`.
- [x] **T227** `DespostadoPage.tsx` — nuevo `<select id="tipo-entrada">` a la derecha de
  "Categoría del animal" (envolver ambos en un `flex flex-wrap gap-4`, mismo patrón de
  tamaño `max-w-sm` que ya usan los campos de arriba); estado `tipoEntrada` (default
  `'MediaRes'`), se manda en `cargarLaEntrada()`; "Restablecer ejemplo" fuerza
  `setTipoEntrada('MediaRes')`. Sin test propio (página de ensamblado, mismo criterio que
  `CortesPage`/`AjustesPage`).

---

## Bloque 3 — Frontend: `TablaCortes` atenúa los cortes no habilitados

- [x] **T228 [P]** `components/TablaCortes.test.tsx`, casos nuevos: sin la prop
  `corteNombresHabilitados` (o pasada como `null`), ningún corte se atenúa (comportamiento
  actual, "Media res"); con `corteNombresHabilitados` un `Set` chico, las filas de cortes
  fuera del set quedan atenuadas visualmente y su input de kg queda `disabled`; el usuario
  igual puede *seleccionar* esa fila (ver su zona en el mapa) — solo se bloquea cargar kg,
  no la lectura.
- [x] **T229** `components/TablaCortes.tsx` — nueva prop opcional `corteNombresHabilitados:
  Set<string> | null`; fila atenuada con `opacity` (mismo criterio que `TablaDeCortes` de
  Fase 5 para cortes inactivos) cuando `corteNombresHabilitados` no es `null` y no incluye
  `corte.nombre`; en ese caso el input de kg de esa fila lleva `disabled`.
- [x] **T230** `DespostadoPage.tsx` — pasa `cortesHabilitados(tipoEntrada)` (de
  `modelo/tiposDeEntrada.ts`) a `TablaCortes` como `corteNombresHabilitados`.

---

## Bloque 4 — Cierre de fase

- [x] **T231** Agregar a `../quickstart.md` los pasos de Fase 6: elegir "Pecho" en el
  selector "Corte" y confirmar que la tabla atenúa todo salvo Paleta/Roast beef/Cogote/
  Falda; cargar la entrada y recargar la página, confirmar que "Pecho" sigue seleccionado;
  volver a "Media res" y confirmar que ya no queda nada atenuado; intentar tipear kg en una
  fila atenuada y confirmar que el campo no deja escribir.
- [x] **T232** Revisión final: confirmar que ninguna entrada vieja (anterior a esta fase)
  quedó con un `tipo_entrada` distinto de `"MediaRes"` (el `default` de la columna debe
  haber alcanzado, sin backfill manual) y que `spec.md` recibió, aunque sea como nota, la
  referencia a esta fase (sección 12, mismo criterio que la sección 11 de Fase 5).

---

## Bloque 5 — "Ganancia estimada" en Despostado (agregado a la fase, pedido por chat)

Tarjeta nueva en `TarjetasResumen`, a la derecha de "Costo real por kg vendible": la
ganancia total en pesos de la entrada que se está cargando ahora mismo, no un $/kg
promedio. Para cada corte con kg cargado y `precioVenta` cargado en "Editar cortes",
resta el mismo `costoKgVendible` uniforme de la media res (no hay forma de saber el costo
"real" de un corte individual dentro de una compra conjunta) y multiplica por los kg de ese
corte — distinto del "Beneficio por kg vendible" de Reportes (Fase 5), que es un $/kg
histórico agregado, no un total por entrada.

| ID | Requisito |
|---|---|
| FR-603 | "Despostado" muestra, junto a las demás tarjetas de resumen, "Ganancia estimada": `Σ (precioVenta_corte − costoKgVendible) × kg_corte`, sumando solo los cortes con `kg` cargado y `precioVenta` cargado. `null` (se muestra "—") si no hay `costoKgVendible` (falta precio de compra) o si ningún corte con kg tiene `precioVenta` — nunca se trata como $0. |

- [x] **T233 [P]** `modelo/gananciaEstimada.test.ts`: ejemplo del dueño (asado a $18.500/kg,
  espinazo a $5.000/kg, costo $12.319/kg) calcula la suma correcta, incluido el margen
  negativo de espinazo; sin `costoKgVendible` da `null`; sin ningún corte con `precioVenta`
  da `null` (no `0`); un corte sin `precioVenta` no aporta ni resta, el resto sigue sumando;
  un corte con `precioVenta` pero kg en 0 no aporta.
- [x] **T234** `modelo/gananciaEstimada.ts` — `calcularGananciaEstimada(kgPorCorte,
  costoKgVendible, precioVentaPorCorte): number | null`.
- [x] **T235 [P]** `components/TarjetasResumen.test.tsx`, casos nuevos: sin
  `costoKgVendible`, la tarjeta nueva también muestra "—" y "Cargá el precio de compra"
  (mismo motivo que "Costo real por kg vendible"); con `costoKgVendible` pero
  `gananciaEstimada` null, pide cargar precios en "Editar cortes"; con `gananciaEstimada`
  numérica, la muestra formateada es-AR (verde si es positiva, el mismo criterio de color
  que ya usa "Costo real por kg vendible" para "vendible").
- [x] **T236** `components/TarjetasResumen.tsx` — nueva prop `gananciaEstimada: number |
  null` (requerida), tarjeta "Ganancia estimada" a la derecha de "Costo real por kg
  vendible" (grid pasa de `lg:grid-cols-4` a `lg:grid-cols-5`).
- [x] **T237** `DespostadoPage.tsx` — arma `precioVentaPorCorte` (de `cortes`, ya trae
  `precioVenta` desde Fase 5) y calcula `gananciaEstimada` con `calcularGananciaEstimada`,
  se lo pasa a `TarjetasResumen`.

## Bug preexistente encontrado y arreglado (no era de esta fase)

T221 agregó el primer test de `PUT /medias-reses/{id}` que existió — hasta ahora ese
endpoint no tenía ningún test. Surgió un `500` real: `MediaResService.actualizar` borra
despostado/pérdidas y los vuelve a insertar en la misma transacción, pero sin flush
explícito entre el delete y el insert — si la edición manda el mismo corte que ya tenía,
Hibernate puede flushear el INSERT antes que el DELETE (el orden de acciones no respeta el
orden del código) y choca contra la unique constraint `(media_res_id, corte_id)`. Se
arregló agregando `despostadoRepository.flush()`/`perdidaRepository.flush()` después de los
deletes, mismo patrón que ya usan `CorteService.crear`/`CatalogoInicialService` para este
tipo exacto de problema. No afecta a Fase 6 en sí — quedó documentado acá porque lo
encontró un test de esta fase.

---

## Nota sobre `spec.md`

Mismo criterio que Fase 5: no se renumeran las secciones existentes. Si se quiere agregar
esta fase con FR formales, va como sección nueva al final (después de la sección 11 de
Fase 5).

---

## Dependencias entre bloques

- Bloque 1 (backend `tipo_entrada`): sin dependencias.
- Bloque 2 (mapeo + selector): el mapeo (T224/T225) no depende del Bloque 1; el selector
  (T227) necesita el campo en la API (`CargarEntradaRequest.tipoEntrada`), así que depende
  del Bloque 1 para poder guardar de verdad (podría construirse en paralelo y enchufarse al
  final).
- Bloque 3 (`TablaCortes` atenúa): depende del mapeo del Bloque 2 (T225), no del Bloque 1.
- Bloque 4 (cierre): depende de todo lo anterior.

## Ejemplo de ejecución en paralelo

Al arrancar la fase, en paralelo:
```
T221                    (test de tipo_entrada en MediaResController, Bloque 1)
T224                    (test de tiposDeEntrada, Bloque 2, no necesita el Bloque 1)
```
Una vez cerrado el mapeo (T225), en paralelo:
```
T226                    (tipos de API, Bloque 2)
T228                    (test de TablaCortes atenuada, Bloque 3)
```
