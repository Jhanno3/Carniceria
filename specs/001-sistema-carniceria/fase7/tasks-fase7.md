# Tareas — Fase 7: Catálogo de Achuras/Embutidos y Cerdo + alta rápida de stock

**Entradas:** `../spec.md` (Fases 1-6), `../../constitution.md`, `../fase6/tasks-fase6.md`.
Igual que las Fases 5 y 6, esta fase se pidió directo por chat — los FR-7xx de abajo son
nuevos, documentados acá mismo (no están en `spec.md` todavía, ver nota al final).

**Orden:** TDD — en cada bloque, los tests se escriben y deben fallar antes de la
implementación que los hace pasar (Principio III de la constitución).
**Capas:** `controller/` → `service/` → `modelo/` (dominio puro, sin Spring/JPA) →
`repository/` → `entity/`, con `dto/` para los contratos. Frontend: `modelo/`/`api/`
separado de `components/`.
**`[P]`** = se puede hacer en paralelo con las otras tareas `[P]` del mismo bloque (archivos
distintos, sin dependencia entre ellas).

Numeración continúa desde `../fase6/tasks-fase6.md` (Fase 6 llegó hasta T237). Migraciones
nuevas: `V27__cortes_tipo_producto.sql`, `V28__tipo_entrada_achuras_cerdo.sql`,
`V29__seed_achuras_embutidos_cerdo.sql` (Bloques 1, 2 y 3).

## Alcance y origen (pedido del dueño, verbatim resumido)

Dos pedidos del dueño en el mismo mensaje:

1. En "Control diario", un botón a la derecha del título "Stock por corte" para añadir
   stock directo, sin pasar por "Despostado".
2. Hay cortes que no salen de despostar una media res — se agrupan en dos catálogos
   nuevos:
   - **Achuras y Embutidos**: Chinchulín, Molleja, Rabo, Mondongo, Lengua, Riñón, Corazón,
     Hígado, Fresca, Morcilla Vasca, Morcilla, Chorizo.
   - **Cerdo**: Bondiola, Pechito, Carré, Cordero.

Más el bug de los `<select>` en modo oscuro (ver más abajo — **ya se arregló**, no es
tarea de esta fase, queda documentado para no perder el rastro).

## Decisiones ya tomadas con el dueño (no volver a preguntar)

- **Flujo elegido: modal nuevo en "Control diario", separado de "Despostado"** (pregunta
  hecha y confirmada por el dueño). "Despostado" está armado alrededor de "entra una media
  res, se reparte en cortes con una pérdida de hueso/grasa/merma" — Achuras/Embutidos/Cerdo
  no se despostan, se compran ya terminados (ej. "llegaron 5 kg de chorizo"), así que no
  tiene sentido pedir peso de media res ni mostrar pérdida. El botón "Añadir stock" abre un
  modal chico: Corte (solo los de estos dos catálogos) + Kg + Precio de compra ($/kg,
  opcional).
- **Por dentro, reutiliza `POST /medias-reses` tal cual existe** — no hace falta ningún
  endpoint nuevo. El modal arma una `MediaRes` "degenerada": `pesoKg` = el mismo kg
  cargado, un solo corte en `cortes` con ese kg, `perdidas` en cero. Con esos valores el
  cálculo existente da `rendimientoPorc = 100%` y `sinAsignarKg = 0` — correcto de verdad
  (no hay pérdida porque no hay despostado), no es un hack numérico.
- **`medias_reses.tipo_entrada` gana dos valores nuevos**: `AchurasEmbutidos` y `Cerdo`
  (Bloque 2), para que estas entradas queden etiquetadas en el historial. **No se ofrecen
  en el selector "Corte" de la pantalla Despostado** (Fase 6) — ese selector sigue con sus
  7 valores de siempre; estos dos los pone el modal nuevo por su cuenta, el usuario no los
  elige ahí.
- **`cortes` gana una columna `tipo_producto`** (`Vacuno` default | `AchurasEmbutidos` |
  `Cerdo`) para distinguir estos cortes de los de la media res. **`cuarto` pasa a ser
  nullable**: solo tiene sentido para `Vacuno` (es una clasificación anatómica de la media
  res); para los otros dos tipos, `cuarto` viaja `null` siempre — no se inventa un cuarto
  falso. Mismo criterio para `zona_mapa` (ya era nullable, sigue siéndolo: estos cortes no
  tienen polígono en `MapaCortes`).
- **PLU nuevos: 29 a 44**, continuando la numeración después de Cogote (28, `V22`) —
  ver tabla de abajo. El dueño puede reasignarlos después desde "Editar cortes" si no
  coinciden con lo que tiene cargado en la balanza (son el mismo tipo de dato editable que
  cualquier otro corte, FR-109).
- **Se siembran para todos los negocios ya existentes** (migración `V29`, mismo patrón
  `cross join` + `on conflict do nothing` que `V21`/`V22`) y se agregan a
  `CatalogoInicialService.CATALOGO_DE_EJEMPLO` para que un negocio nuevo de acá en más los
  reciba de entrada.
- **Es solo catálogo + alta de stock, no un circuito nuevo de ventas**: el control diario
  (escaneo EAN-13, stock por corte, "queda poco") ya funciona genérico por corte — no hace
  falta tocar nada ahí, estos cortes nuevos entran al mismo cálculo de stock
  (`despostado − ventas no anuladas`) que cualquier otro.
- **Reportes (Fase 4) no se tocan**: estas entradas van a aparecer mezcladas con las de
  media res en los reportes existentes, sin ninguna distinción especial por
  `tipoProducto`. Filtrar reportes por tipo de producto queda fuera de esta fase.

## Nota sobre un nombre que podría parecer un error (no lo es)

El dueño agrupó **"Cordero"** dentro del catálogo **"Cerdo"**, aunque el cordero es oveja,
no cerdo. Se implementa tal cual se pidió (no se "corrige" a una tercera categoría sin
que el dueño lo pida) — si fue sin querer, se mueve después con un `PUT /cortes/{id}`
común y corriente, no hace falta ninguna migración para recategorizarlo.

## Catálogo nuevo (PLU y `tipo_producto`)

| Nombre | PLU | `tipo_producto` |
|---|---|---|
| Chinchulín | 29 | `AchurasEmbutidos` |
| Molleja | 30 | `AchurasEmbutidos` |
| Rabo | 31 | `AchurasEmbutidos` |
| Mondongo | 32 | `AchurasEmbutidos` |
| Lengua | 33 | `AchurasEmbutidos` |
| Riñón | 34 | `AchurasEmbutidos` |
| Corazón | 35 | `AchurasEmbutidos` |
| Hígado | 36 | `AchurasEmbutidos` |
| Fresca | 37 | `AchurasEmbutidos` |
| Morcilla Vasca | 38 | `AchurasEmbutidos` |
| Morcilla | 39 | `AchurasEmbutidos` |
| Chorizo | 40 | `AchurasEmbutidos` |
| Bondiola | 41 | `Cerdo` |
| Pechito | 42 | `Cerdo` |
| Carré | 43 | `Cerdo` |
| Cordero | 44 | `Cerdo` |

Los 16 nacen `activo = true`, `cuarto = null`, `zona_mapa = null`, `precio_venta = null`
(igual que cualquier corte recién sembrado — el dueño lo completa en "Editar cortes"
cuando quiera cobrar un precio distinto por kg).

## Requisitos funcionales nuevos (FR-7xx)

| ID | Requisito |
|---|---|
| FR-701 | `cortes` tiene una columna `tipoProducto` (`Vacuno` default, `AchurasEmbutidos`, `Cerdo`). `cuarto` es obligatorio solo cuando `tipoProducto = Vacuno`; para los otros dos valores, `cuarto` debe ser `null` (el backend lo rechaza si viene con un valor). |
| FR-702 | El catálogo de cada negocio (los ya existentes y los nuevos de acá en más) incluye los 16 cortes de "Achuras y Embutidos" y "Cerdo" de la tabla de arriba. |
| FR-703 | "Control diario" muestra un botón "Añadir stock" a la derecha del título "Stock por corte". Abre un modal con Corte (limitado a cortes activos con `tipoProducto` ≠ `Vacuno`), Kg y Precio de compra ($/kg, opcional). Al guardar, crea una `MediaRes` con ese kg como `pesoKg`, un único corte cargado con ese mismo kg y pérdidas en cero, etiquetada con el `tipoEntrada` que corresponda (`AchurasEmbutidos` o `Cerdo` según el corte elegido) — reusando `POST /medias-reses` tal cual existe. |
| FR-704 | `medias_reses.tipo_entrada` admite dos valores nuevos, `AchurasEmbutidos` y `Cerdo`, además de los 7 de la Fase 6. No aparecen en el selector "Corte" de "Despostado" — solo los usa el modal de FR-703. |

## Casos borde

- Ningún corte con `tipoProducto` ≠ `Vacuno` activo (negocio que los desactivó todos, o
  una corrida vieja de `CatalogoInicialService` anterior a esta fase que nunca sembró
  nada): el modal muestra el select vacío con un aviso ("No tenés cortes de Achuras/
  Embutidos o Cerdo activos. Cargalos en Editar cortes.") y el botón "Guardar" queda
  deshabilitado — no se manda ningún request.
- `kg` en el modal debe ser `> 0`, misma validación que ya existe para cualquier fila de
  `despostado`.
- Precio de compra omitido en el modal: viaja `precioKg: null` en el `POST` (igual que
  Despostado) — `costoKgVendible` de esa entrada queda `null`, no bloquea el guardado.
- Editar un corte `Vacuno` existente y cambiarle `tipoProducto` a `Cerdo`/`AchurasEmbutidos`
  en "Editar cortes": el frontend limpia `cuarto` a `null` antes de mandar el `PUT` (el
  backend lo rechazaría igual si viniera con un valor, es cinturón y tirantes). Si ese
  corte tenía `zonaMapa` cargada, queda guardada pero sin efecto — `MapaCortes` nunca la va
  a dibujar porque ese corte no puede tener kg cargado en una media res real (no aparece en
  ningún `tipoEntrada` del selector de Despostado, Fase 6).
- `tipoProducto` inválido en el body de `POST`/`PUT /cortes` (typo): `400
  TIPO_PRODUCTO_INVALIDO`, mismo patrón que `CuartoInvalidoException`. `cuarto` presente
  con `tipoProducto` distinto de `Vacuno`: `400 CUARTO_NO_APLICA`.
- Un corte creado por el dueño después de esta fase con `tipoProducto` ≠ `Vacuno`: igual
  que los "sueltos" de Fase 6, no pertenece a ningún `tipoEntrada` del selector de
  Despostado, y tampoco hace falta que pertenezca — ese selector es exclusivo de `Vacuno`.

---

## Bug ya arreglado (no es tarea de esta fase, queda documentado)

Los `<select>` se veían mal en modo oscuro: el popup desplegable salía con fondo blanco y
texto apenas legible, mismo síntoma que las pestañas de navegación de esta misma rama
(un color fijo del navegador que no sabía que el tema era oscuro). Se arregló con una sola
propiedad CSS (`color-scheme: dark` en `.dark`, en `index.css`) — el navegador dibuja el
popup nativo del `<select>` (y scrollbars) con su propia variante oscura automáticamente,
sin tocar ningún componente. Ya está en la rama `darkmode/front`, con los 132 tests
existentes en verde.

---

## Bloque 1 — Backend: `cortes.tipoProducto` + `cuarto` nullable

**Nota sobre verificación:** el código de este bloque (y de los Bloques 2-3) está escrito
siguiendo exactamente los mismos patrones que el resto del módulo `cortes`/`despostado`
(mismo estilo de excepciones, mismo criterio de migraciones con `pg_constraint` dinámico
que ya usa `V20`), y compila limpio (`mvnw test-compile`). **No se pudo correr
`CorteControllerTest`/`MediaResControllerTest` contra una base real** — se encontró que
*todos* los tests de `CorteControllerTest` (incluidos los que ya existían antes de esta
fase, sin tocar nada) fallan con `ERROR: new row violates row-level security policy ... for
table "perfiles"` al confirmarse contra el Supabase real de `backend/.env`, reproducido
igual en un `git stash` al commit limpio de `agregarPicada` — es un problema preexistente
del entorno de tests (no de esta fase), no investigado más a fondo a pedido del dueño. Dar
este bloque por cerrado requiere, en algún momento, arreglar ese entorno y correr estos
tests de verdad.

- [x] **T238 [P]** `CorteControllerTest`, casos nuevos: `POST /cortes` con
  `tipoProducto: "Cerdo"` y sin `cuarto` lo crea con `cuarto: null`; con `tipoProducto:
  "Cerdo"` y `cuarto: "Trasero"` devuelve `400 CUARTO_NO_APLICA` y no crea nada; sin
  `tipoProducto` en el body, se asume `"Vacuno"` y exige `cuarto` como hoy; `tipoProducto:
  "Invalido"` devuelve `400 TIPO_PRODUCTO_INVALIDO`. `PUT /cortes/{id}` cubre los mismos
  casos al editar.
- [x] **T239** `V27__cortes_tipo_producto.sql` — `alter table cortes add column
  tipo_producto text not null default 'Vacuno' check (tipo_producto in ('Vacuno',
  'AchurasEmbutidos', 'Cerdo'))`; `alter table cortes alter column cuarto drop not null`;
  reemplazar el `check` de `cuarto` por uno que exija consistencia con `tipo_producto`:
  `check ((tipo_producto = 'Vacuno' and cuarto in ('Delantero','Trasero','Ambos')) or
  (tipo_producto <> 'Vacuno' and cuarto is null))`.
- [x] **T240** `CorteEntity` — enum `TipoProducto` (`Vacuno, AchurasEmbutidos, Cerdo`) +
  campo `tipoProducto` (`@Enumerated(STRING)`, `@Column(nullable = false)`); `cuarto` pasa
  a `@Column(nullable = true)`. `CorteService` valida: `tipoProducto` ausente → `Vacuno`;
  valor que no matchea el enum → `TipoProductoInvalidoException` (`400
  TIPO_PRODUCTO_INVALIDO`, mismo patrón que `CuartoInvalidoException`); `cuarto` presente
  con `tipoProducto ≠ Vacuno` → `CuartoNoAplicaException` (`400 CUARTO_NO_APLICA`);
  `tipoProducto = Vacuno` sin `cuarto` → sigue siendo obligatorio, mismo error que hoy.
- [x] **T241** `CorteRequest`/`CorteResponse` agregan `tipoProducto: String` (requerido en
  la respuesta, opcional en el request — default `"Vacuno"`); `cuarto` pasa a `String?`
  (nullable) en ambos.
- [x] **T242 [P]** Actualizar los call sites existentes que construyen `CorteEntity` por
  posición (tests de otras features que arman un corte de prueba) para el nuevo parámetro
  — mismo tipo de ajuste mecánico que Fase 5/6 con `CorteEntity`/`CargarEntradaRequest`.

---

## Bloque 2 — Backend: `medias_reses.tipo_entrada` gana `AchurasEmbutidos`/`Cerdo`

- [x] **T243 [P]** `MediaResControllerTest`, caso nuevo: `POST /medias-reses` con
  `tipoEntrada: "Cerdo"` (o `"AchurasEmbutidos"`) lo persiste y lo devuelve igual que
  cualquier otro valor del enum (Fase 6 ya cubre el resto de los casos — inválido, ausente
  — no hace falta repetirlos).
- [x] **T244** `V28__tipo_entrada_achuras_cerdo.sql` — `alter table medias_reses drop
  constraint` + `add constraint ... check (tipo_entrada in ('MediaRes', 'Delantero',
  'Pecho', 'Parrillero', 'AsadoCompleto', 'Mocho', 'Rueda', 'AchurasEmbutidos', 'Cerdo'))`.
  `MediaResEntity.TipoEntrada` agrega `AchurasEmbutidos, Cerdo` al enum —
  `parsearTipoEntrada` ya es genérico sobre los valores del enum, no necesita más cambios.

---

## Bloque 3 — Catálogo: seed de los 16 cortes nuevos

- [x] **T245** `V29__seed_achuras_embutidos_cerdo.sql` — inserta los 16 cortes de la tabla
  de arriba (PLU 29-44, `cuarto = null`, `zona_mapa = null`) para cada `dueno_id` que ya
  tiene catálogo (`cross join (select distinct dueno_id from cortes)`, `on conflict
  (dueno_id, plu) do nothing`, mismo patrón que `V21`/`V22`).
- [x] **T246** `CatalogoInicialService.CATALOGO_DE_EJEMPLO` — agrega las mismas 16 entradas
  (mismos PLU, `tipoProducto` según corresponda, `cuarto = null`) para que un negocio
  aprobado de acá en más las reciba de entrada. `CorteDeEjemplo` gana el campo
  `tipoProducto` (las 28 entradas existentes quedan explícitas en `Vacuno`).

---

## Bloque 4 — Frontend: `tipoProducto` en "Editar cortes"

- [x] **T247 [P]** Tests nuevos: `FormularioCorte.test.tsx` — con `tipoProducto` distinto
  de `Vacuno`, el selector "Cuarto" no se muestra (o queda deshabilitado) y `onGuardar`
  recibe `cuarto: null`; al elegir "Vacuno" vuelve a pedirlo. `TablaDeCortes.test.tsx` y
  despostado `TablaCortes.test.tsx` — una fila con `cuarto: null` muestra `"—"` en esa
  columna en vez de romper o mostrar `"null"`.
- [x] **T248** `features/cortes/api/types.ts` + `shared/api/types.ts` — `Corte`/
  `CorteResponse`/`CorteRequest` agregan `tipoProducto: 'Vacuno' | 'AchurasEmbutidos' |
  'Cerdo'`; `cuarto` pasa a admitir `null`.
- [x] **T249** `FormularioCorte.tsx` — nuevo selector "Tipo de producto" (mismo patrón que
  "Cuarto"); cuando no es `Vacuno`, oculta el selector "Cuarto" y fuerza `cuarto: null` en
  `valores` (no se le pide nada al usuario para ese campo).
- [x] **T250** `TablaDeCortes.tsx` (Editar cortes) y
  `despostado/components/TablaCortes.tsx` — la celda de "Cuarto" muestra `corte.cuarto ??
  '—'`.

---

## Bloque 5 — Frontend: modal "Añadir stock" en Control diario

- [x] **T251 [P]** `ModalAnadirStock.test.tsx`: con cortes de `tipoProducto` `Cerdo`/
  `AchurasEmbutidos` activos, el select solo lista esos (ningún corte `Vacuno`); sin
  ninguno disponible, muestra el aviso y el botón "Guardar" está deshabilitado; con un
  corte elegido y `kg` ≤ 0, tampoco deja guardar; con corte + kg válidos (+ precio de
  compra opcional), al confirmar llama a la mutación con el body esperado (`pesoKg` = `kg`,
  `cortes: [{ corteId, kg }]`, `perdidas` en cero, `tipoEntrada` = `"Cerdo"` o
  `"AchurasEmbutidos"` según el `tipoProducto` del corte elegido); al resolver, cierra el
  modal.
- [x] **T252** `features/control-diario/components/ModalAnadirStock.tsx` — usa
  `shared/ui/Modal.tsx`; trae los cortes con `useCortes()` filtrando a `tipoProducto !==
  'Vacuno'` y `activo`; campos Corte (select), Kg, Precio de compra ($/kg, opcional);
  botones Cancelar/Guardar (mismo estilo que el resto de los formularios).
- [x] **T253** `features/control-diario/api/useAnadirStock.ts` — `useMutation` que hace
  `POST /medias-reses` con el body descrito en FR-703; al resolver, invalida la query
  `['stock']` (y `['medias-reses']` si existe) para que "Stock por corte" se actualice
  solo, sin recargar la página.
- [x] **T254** `ControlDiarioPage.tsx` — el encabezado de "Stock por corte" pasa a un `flex
  items-center justify-between`, con el `<h2>` a la izquierda y el botón "Añadir stock" (`h-11`,
  mismo estilo que los demás botones secundarios) a la derecha; abre `ModalAnadirStock`.

---

## Bloque 6 — Cierre de fase

- [x] **T255** Agregar a `../quickstart.md` los pasos de Fase 7: crear un corte nuevo con
  "Tipo de producto" = Cerdo y confirmar que no pide Cuarto; abrir "Añadir stock" en
  Control diario, cargar un corte de Achuras y Embutidos con kg, confirmar que "Stock por
  corte" lo refleja sin pasar por Despostado; confirmar que ese corte nunca aparece como
  opción cargable en el selector "Corte" de Despostado.
- [ ] **T256** Revisión final: confirmar que ningún corte `Vacuno` existente (anterior a
  esta fase) quedó con `tipoProducto` distinto de `"Vacuno"` por el default de la columna
  (`V27`), y que `spec.md` recibió, aunque sea como nota, la referencia a esta fase
  (sección nueva al final, mismo criterio que Fases 5 y 6).

---

## Nota sobre `spec.md`

Mismo criterio que Fases 5 y 6: no se renumeran las secciones existentes. Si se quiere
agregar esta fase con FR formales, va como sección nueva al final (después de la sección
de Fase 6).

---

## Dependencias entre bloques

- Bloque 1 (`cortes.tipoProducto`): sin dependencias.
- Bloque 2 (`medias_reses.tipoEntrada` nuevo): sin dependencias, independiente del Bloque 1.
- Bloque 3 (seed de los 16 cortes): depende del Bloque 1 (necesita la columna
  `tipo_producto` para poder insertar con un valor válido).
- Bloque 4 (`FormularioCorte`/tablas): depende del Bloque 1 (campo en la API).
- Bloque 5 (modal "Añadir stock"): depende del Bloque 3 (necesita que existan cortes
  `tipoProducto ≠ Vacuno` para poder probarlo de punta a punta) y del Bloque 2 (el
  `tipoEntrada` que manda en el `POST`).
- Bloque 6 (cierre): depende de todo lo anterior.

## Ejemplo de ejecución en paralelo

Al arrancar la fase, en paralelo:
```
T238                    (test de tipoProducto en CorteController, Bloque 1)
T243                    (test de tipoEntrada nuevo en MediaResController, Bloque 2)
```
Una vez cerrado el Bloque 1, en paralelo:
```
T242                    (ajuste mecánico de call sites, Bloque 1)
T247                    (test de FormularioCorte/tablas, Bloque 4)
```
