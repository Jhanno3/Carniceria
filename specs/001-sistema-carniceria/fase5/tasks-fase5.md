# Tareas — Fase 5: Editar cortes + parte contable (precio de venta, caja diaria, beneficio)

**Entradas:** `../spec.md` (Fases 1-4), `../../constitution.md`. No hay `plan-fase5.md` ni
`contracts/` todavía — esta fase se pidió directo por chat, no por el flujo spec→plan→tasks
de las fases anteriores. Los FR-5xx de abajo son nuevos, documentados acá mismo (no están
en `spec.md` todavía); si en algún momento se quiere mantener la paridad con las otras
fases, falta todavía trasladarlos a una sección nueva de `spec.md` sin romper la numeración
de secciones existente (ver nota al final de este archivo).

**Orden:** TDD — en cada bloque, los tests se escriben y deben fallar antes de la
implementación que los hace pasar (Principio III de la constitución).
**Capas:** `controller/` → `service/` → `modelo/` (dominio puro, sin Spring/JPA) →
`repository/` → `entity/`, con `dto/` para los contratos. Frontend: `modelo/` (si hace
falta) separado de `components/` y `api/`.
**`[P]`** = se puede hacer en paralelo con las otras tareas `[P]` del mismo bloque (archivos
distintos, sin dependencia entre ellas).

Numeración continúa desde `../fase4/tasks-fase4.md` (Fase 4 llegó hasta T186). Dos
migraciones nuevas: `V23__cortes_precio_venta.sql` (Bloque 1) y
`V24__ventas_precio_total.sql` (Bloque 4).

## Alcance y origen de cada bloque (pedido del dueño, verbatim resumido)

El dueño pidió dos cosas, acá repartidas en bloques más chicos para poder hacerlas en
paralelo donde no dependen entre sí:

1. **"Bloque 1" del pedido** → Bloques 1-2 de abajo: la pantalla "Editar cortes" que
   faltaba (gap detectado en la revisión de `spec.md`/`constitution.md` contra el código:
   FR-109 ya estaba completo en el backend pero no tenía ninguna pantalla en el frontend).
2. **"Bloque 2" del pedido** → Bloques 3-8: la parte contable. Resumida, con las decisiones
   ya tomadas (ver "Decisiones" abajo):
   - Cada corte tiene un `precio_venta` ($/kg), editable en la misma pantalla "Editar
     cortes" (FR-501).
   - El decodificador de etiquetas ya tenía un modo `importe` sin implementar de verdad
     (`DecodificadorEtiqueta.java:30-33`, comentario explícito del hueco) — con
     `precio_venta` cargado, se cierra: si la etiqueta trae el peso, el importe se calcula;
     si trae el importe, el peso se calcula (FR-502).
   - Cada venta guarda su importe (`precio_total`), para poder cerrar caja: "Control
     diario" muestra dinero recaudado hoy (FR-503) y "Inicio" dinero recaudado del mes
     (FR-504).
   - Reportes (por proveedor/categoría/período) agregan la columna "Beneficio por kg
     vendible" = precio de venta promedio ponderado por los kilos de cada corte, menos el
     `costoKgVendiblePromedio` que esas tablas ya muestran (FR-505).

## Decisiones ya tomadas con el dueño (no volver a preguntar)

- **Beneficio por kg vendible** = precio de venta promedio ponderado − costo_kg_vendible
  (ganancia real por kg, no solo el ingreso).
- **Un solo valor en la etiqueta** (peso **o** importe, nunca los dos a la vez en el mismo
  código): el que falta se deriva con `precio_venta` del corte. Si la balanza real termina
  imprimiendo ambos valores por separado en el mismo código, hay que volver a `config_etiqueta`
  para agregar un segundo rango de posición/largo — no es parte de esta fase.
- **Venta sin `precio_venta` cargado para ese corte:**
  - Si la etiqueta trae el **peso** (`tipoValor = peso`): la venta se registra igual, con
    `precio_total = null`. No frena el mostrador (Principio IV).
  - Si la etiqueta trae el **importe** (`tipoValor = importe`): sin `precio_venta` no hay
    forma de calcular los kilos (FR-204 exige `kg` siempre) — ahí sí se rechaza, con un
    error claro y distinto de "peso cero" (`CORTE_SIN_PRECIO_VENTA`).
- **El empleado ve `precio_venta`** de los cortes (es el precio que se le cobra al cliente,
  no un costo de compra — la constitución solo restringe precio de **compra**/costos,
  Principio II). Sigue sin ver `precio_kg`, `costo_total` ni `costo_kg_vendible`.

## Requisitos funcionales nuevos (FR-5xx, numeración propia de esta fase)

| ID | Requisito |
|---|---|
| FR-501 | El dueño puede editar `precio_venta` ($/kg, opcional, > 0) de cualquier corte desde la misma pantalla "Editar cortes" de FR-109. El empleado puede leerlo; ningún rol más que el dueño/admin-dueño puede escribirlo (misma RLS que el resto del corte). |
| FR-502 | El sistema deriva, al registrar una venta, el valor que la etiqueta no trae directamente: `tipoValor = peso` → `precio_total = kg × cortes.precio_venta` (null si el corte no tiene precio); `tipoValor = importe` → `kg = precio_total / cortes.precio_venta` (rechaza con `CORTE_SIN_PRECIO_VENTA` si el corte no tiene precio, porque sin él no hay forma de calcular `kg`). |
| FR-503 | "Control diario" muestra, además de lo que ya mostraba (FR-206), el dinero recaudado hoy (suma de `precio_total` de ventas no anuladas de hoy, ignorando las que tienen `precio_total = null`) y la cantidad de ventas de hoy sin precio registrado. |
| FR-504 | "Inicio" muestra, además de lo que ya mostraba, el dinero recaudado en lo que va del mes (mismo criterio de exclusión que FR-503). |
| FR-505 | Los 3 reportes de Fase 4 (por proveedor, por categoría, por período) agregan una columna "Beneficio por kg vendible" = precio de venta promedio ponderado por los kilos de cada corte en las medias reses de ese grupo, menos `costoKgVendiblePromedio`. Una media res cuyos cortes no tienen ningún `precio_venta` cargado no aporta a ese promedio (ni al numerador ni al denominador) — igual criterio que `costoKgVendiblePromedio` con `precioKg` nulo. |

## Casos borde

- Un corte tenía `precio_venta` cargado, se vendió, y después el dueño le cambió el precio:
  la venta vieja conserva el `precio_total` que tenía al momento de venderse (es una
  columna propia de `ventas`, nunca un cálculo al vuelo desde `cortes.precio_venta` — mismo
  criterio que "una venta no se borra", Principio I: no se reescribe historial).
- `tipoValor = importe`, el valor decodificado da `0`: se rechaza como `PESO_CERO` (no
  llega a intentar dividir por `precio_venta`).
- Un reporte incluye una media res cuyo despostado tiene algunos cortes con `precio_venta`
  y otros sin él: solo los kilos de los cortes con precio entran al promedio ponderado (ni
  numerador ni denominador suman los kilos de los cortes sin precio) — no se los trata
  como precio 0, que arrastraría el promedio para abajo sin sentido.
- Ningún corte de ningún grupo tiene `precio_venta`: "Beneficio por kg vendible" se muestra
  como "—", igual que ya pasa hoy con `costoKgVendiblePromedio` sin `precioKg`.

---

## Bloque 1 — Backend: `cortes.precio_venta`

- [ ] **T187 [P]** `CorteControllerTest`, casos nuevos: `PUT /cortes/{id}` con
  `precioVenta: "9000.00"` lo persiste y lo devuelve en la respuesta; con `precioVenta: null`
  lo deja sin precio; con `precioVenta: "-100"` devuelve `400` (validación, no llega a la
  base); un `empleado` autenticado contra `GET /cortes` recibe `precioVenta` en cada fila
  (no queda oculto como si fuera un costo).
- [ ] **T188** `V23__cortes_precio_venta.sql` — `alter table cortes add column precio_venta
  numeric(12,2) null`, `check (precio_venta is null or precio_venta > 0)`.
- [ ] **T189** `CorteEntity` (campo + getter/setter), `CorteRequest`/`CorteResponse`
  (agregan `precioVenta`, validado con `@Positive` — Bean Validation ya permite null y solo
  valida cuando está presente, sin exception nueva), `CorteRepository.actualizar(...)` (el
  `@Query` de `UPDATE` explícito suma `c.precioVenta = :precioVenta`), `CorteService.crear`/
  `actualizar` pasan el valor nuevo. `CorteService.crear` nace siempre con `precioVenta =
  request.precioVenta()` (no hay restricción especial al crear, a diferencia de `activo`).

---

## Bloque 2 — Frontend: pantalla "Editar cortes" (cierra el gap de FR-109, incluye FR-501)

- [ ] **T190 [P]** `features/cortes/api/types.ts` — `Corte` (refleja `CorteResponse` ya
  extendido: `id, nombre, plu, cuarto, zonaMapa, activo, precioVenta`), `CorteRequest` para
  el body de `POST`/`PUT`.
- [ ] **T191 [P]** `features/cortes/api/useCortes.ts` (`GET /cortes?incluirInactivos=true` —
  esta pantalla necesita ver los inactivos para poder reactivarlos), `useCrearCorte.ts`,
  `useActualizarCorte.ts` (invalidan la query de `useCortes` al éxito, mismo patrón que
  `useActualizarPerfil`).
- [ ] **T192 [P]** `components/FormularioCorte.test.tsx`: un formulario (alta o edición,
  mismo componente) con campos nombre/PLU/cuarto/zona de mapa/precio de venta/activo;
  valida que PLU y nombre no queden vacíos; al confirmar llama `onGuardar` con los valores
  tal cual los tipeó (sin tocar formato es-AR → BigDecimal-string, eso lo hace el hook de
  arriba, mismo criterio que `FormularioConfigEtiqueta`).
- [ ] **T193 [P]** `components/TablaDeCortes.test.tsx`: lista todos los cortes (activos e
  inactivos, estos últimos visualmente atenuados) con nombre/PLU/cuarto/zona/precio de
  venta/estado; cada fila tiene un botón "Editar" (abre el formulario con esos valores
  precargados) y un botón "Desactivar"/"Activar" según `activo`; sin cortes, mensaje
  neutro.
- [ ] **T194** `components/FormularioCorte.tsx`, `components/TablaDeCortes.tsx`.
- [ ] **T195** `CortesPage.tsx` — tabla + botón "Nuevo corte" que abre el formulario vacío;
  reusa `useCortes`/`useCrearCorte`/`useActualizarCorte`. Sin test propio (página de
  ensamblado, mismo criterio que `DespostadoPage`/`AjustesPage`).
- [ ] **T196** `App.tsx` — nueva sección `'cortes'` ("Editar cortes"), visible junto a
  `'ajustes'`/`'reportes'` para quien opera su propio negocio (`operaNegocio`); el empleado
  no la ve en la navegación pero si llamara al endpoint de todos modos solo podría leer
  (RLS ya lo impide escribir, T187).

---

## Bloque 3 — Backend: modelo puro — deriva kg/importe según `tipoValor` [P] (en paralelo con Bloques 1-2)

- [ ] **T197 [P]** `CalculadorVentaTest.java` (sin Spring):
  - `tipoValor=peso`, `valor=1.250`, `precioVentaCorte=6500` → `Exito(kg=1.250,
    importe=8125)`.
  - `tipoValor=peso`, `valor=1.250`, `precioVentaCorte=null` → `Exito(kg=1.250,
    importe=null)` (venta sin precio, permitida).
  - `tipoValor=importe`, `valor=8120`, `precioVentaCorte=6500` → `Exito(kg=1.249,
    importe=8120)` (8120/6500 = 1,24923... → redondeo HALF_UP a 3 decimales = 1,249, no
    1,250: verifica que redondea de verdad, no trunca).
  - `tipoValor=importe`, `valor=8125`, `precioVentaCorte=null` → `FaltaPrecioVenta` (sin
    precio no hay forma de recuperar los kilos).
  - `valor=0` en cualquiera de los dos modos → `PesoCero` (en modo importe, antes de
    intentar ninguna división).
- [ ] **T198** `escaneo/modelo/CalculadorVenta.java` — `sealed interface ResultadoVenta`
  (`Exito(BigDecimal kg, BigDecimal importe)`, `PesoCero()`, `FaltaPrecioVenta()`);
  `calcular(BigDecimal valor, ConfigEtiqueta.TipoValor tipoValor, BigDecimal
  precioVentaCorte): ResultadoVenta`. Mismo redondeo que el resto del dominio: kg a 3
  decimales, importe a 0 decimales (pesos sin centavos, igual que
  `AgregadorRendimiento`), `RoundingMode.HALF_UP`.
- [ ] **T199** `DecodificadorEtiqueta.decodificar(...)` deja de devolver `kg` y de hacer el
  chequeo de `PesoCero` — pasa a devolver `Exito(int plu, BigDecimal valor)` (el valor
  crudo, sin interpretar todavía si es peso o importe: eso ahora lo decide `CalculadorVenta`
  una vez que el Service sabe qué corte es). Se borra el caso `PesoCero` de
  `ResultadoDecodificacion` (se movió a `ResultadoVenta`). Actualizar
  `DecodificadorEtiquetaTest` existente a la firma nueva.

---

## Bloque 4 — Backend: la venta persiste `precio_total` (usa Bloques 1 y 3)

- [ ] **T200 [P]** `VentaControllerTest`, casos nuevos:
  - Corte con `precioVenta=6500`, `config_etiqueta.tipoValor=peso`, código que decodifica
    `kg=1.250` → la venta creada tiene `precioTotal="8125"`.
  - Corte sin `precioVenta`, mismo escenario → `precioTotal=null`, la venta se crea igual
    (`201`).
  - `config_etiqueta.tipoValor=importe`, corte sin `precioVenta` → `400
    CORTE_SIN_PRECIO_VENTA`, no se crea ninguna venta.
  - `idClienteLocal` repetido sigue devolviendo la misma venta ya creada (caso existente,
    confirmar que sigue pasando con el flujo nuevo).
- [ ] **T201** `V24__ventas_precio_total.sql` — `alter table ventas add column precio_total
  numeric(12,2) null` (sin `check`: puede ser null a propósito, a diferencia de
  `cortes.precio_venta`).
- [ ] **T202** `VentaEntity` (campo + getter), `VentaResponse` (agrega `precioTotal`,
  nullable), `escaneo/service/CorteSinPrecioVentaException.java` (`400`, mismo patrón que
  `PesoCeroException`).
- [ ] **T203** `VentaService.escanear`: después de encontrar el `CorteEntity` por PLU (ya
  estaba, antes de construir la `VentaEntity`), llama a `CalculadorVenta.calcular(...)` con
  el `valor` crudo del decodificador, `config.tipoValor()` y `corte.getPrecioVenta()`;
  traduce `PesoCero`/`FaltaPrecioVenta` a excepción (mismo `switch` exhaustivo que ya existe
  para `ResultadoDecodificacion`); guarda `kg` e `importe` en la `VentaEntity` nueva.

---

## Bloque 5 — Backend: resumen diario — dinero recaudado hoy (usa Bloque 4)

- [ ] **T204 [P]** `ResumenDiaServiceTest` (o controller, según dónde ya estén los tests de
  Fase 2/3), caso nuevo: 3 ventas de hoy no anuladas con `precioTotal` 8125/6000/null, una
  anulada con `precioTotal=5000` → `dineroRecaudadoHoy="14125"` (excluye la anulada y la
  null), `ventasSinPrecioHoy=1`.
- [ ] **T205** `ResumenDiaResponse` agrega `dineroRecaudadoHoy: String`,
  `ventasSinPrecioHoy: int`. `ResumenDiaService.calcular` los suma a partir de
  `ventasDelDia` que ya tenía cargada (sin ninguna consulta nueva a la base).

---

## Bloque 6 — Backend: reportes — "Beneficio por kg vendible" (usa Bloque 1, independiente de los Bloques 3-5)

- [ ] **T206 [P]** `DespostadoRepositoryTest`, caso nuevo (mismo ejemplo canónico de la
  especificación: media res de 100 kg, 81 kg vendibles): de esos 81 kg, 60 kg son de un
  corte con `precioVenta=9000` y 21 kg son de otro corte sin `precioVenta` →
  `sumarKgYValorVentaPorMediaRes` devuelve, para esa media res, `kgConPrecioVenta=60`,
  `importeConPrecioVenta=540000` (60 × 9000) — los 21 kg sin precio no entran a ninguno de
  los dos.
- [ ] **T207** `despostado/modelo/IngresoCortesPorMediaRes.java` (record: `mediaResId,
  kgConPrecioVenta, importeConPrecioVenta`) + `DespostadoRepository
  .sumarKgYValorVentaPorMediaRes(Collection<UUID> mediaResIds)`: join explícito por
  igualdad (`from DespostadoEntity d, CorteEntity c where c.id = d.corteId and
  c.precioVenta is not null and d.mediaResId in :mediaResIds group by d.mediaResId`, mismo
  estilo sin `@ManyToOne` que `buscarHistoricoPorUsuario`), seleccionando `sum(d.kg)` y
  `sum(d.kg * c.precioVenta)`.
- [ ] **T208 [P]** `AgregadorRendimientoTest`, caso nuevo, mismo ejemplo canónico
  encadenado con T206: `Entrada(pesoKg=100, precioKg=5200, vendibleKg=81,
  kgConPrecioVenta=60, importeConPrecioVenta=540000)` → `costoKgVendiblePromedio=6420`
  (ya lo calculaba así, sin cambios), `precioVentaPromedioPonderado=9000` (540000/60),
  `beneficioPorKgVendiblePromedio=2580` (9000−6420). Un segundo caso: agregar a ese mismo
  grupo una entrada sin ningún `kgConPrecioVenta` (0/0) no cambia el
  `precioVentaPromedioPonderado` (sigue dando 9000, prueba que no se la trata como kilos a
  precio 0). Un tercer caso: ningún corte del grupo tiene precio →
  `precioVentaPromedioPonderado=null` y por lo tanto `beneficioPorKgVendiblePromedio=null`.
- [ ] **T209** `AgregadorRendimiento.Entrada` agrega `kgConPrecioVenta`/
  `importeConPrecioVenta` (ambos `BigDecimal`, se tratan como `0` si vienen `null` al
  sumar — a diferencia de `precioKg`, que si es `null` excluye toda la entrada);
  `Resultado` agrega `precioVentaPromedioPonderado`/`beneficioPorKgVendiblePromedio`, mismo
  redondeo (`ESCALA_PESOS`, `HALF_UP`) que `costoKgVendiblePromedio`.
- [ ] **T210** `ReporteProveedorItem`/`ReporteCategoriaItem`/`ReportePeriodoItem` agregan
  `beneficioPorKgVendible: String`. `ReporteService` carga
  `sumarKgYValorVentaPorMediaRes` una sola vez por llamada (igual patrón que
  `cargarVendibleKgPorMediaRes`, mapeado por `mediaResId`) y lo mezcla en las `Entrada` de
  los 3 métodos (`porProveedor`/`porCategoria`/`porPeriodo`).

---

## Bloque 7 — Frontend: caja diaria y resumen del mes (usa Bloques 4 y 5)

- [ ] **T211 [P]** `features/control-diario/components/ResumenDelDia.test.tsx` (o el
  componente que ya muestre el resumen — extender sus tests existentes): agrega "Recaudado
  hoy" (formateado con `formatearPesos`) y, si `ventasSinPrecioHoy > 0`, una aclaración
  visible ("N ventas de hoy sin precio registrado").
- [ ] **T212** `features/control-diario/api/types.ts` (agrega los 2 campos nuevos de
  `ResumenDiaResponse`) + el componente de arriba.
- [ ] **T213 [P]** `features/despostado/api/types.ts` (o donde viva `VentaResponse` en el
  frontend) agrega `precioTotal: string | null`.
- [ ] **T214** `features/inicio/modelo/resumenMes.ts` — agrega `dineroRecaudadoMes` y
  `ventasSinPrecioMes`, calculados sobre `GET /ventas?desde&hasta` del mes (mismo patrón ya
  usado ahí para medias reses: trae la lista completa del rango y suma en el cliente, sin
  endpoint nuevo — un mes de ventas de una sola carnicería no justifica una agregación en
  el backend). Actualizar `resumenMes.test.ts`.
- [ ] **T215** `InicioPage.tsx` — nueva tarjeta "Recaudado este mes"; si
  `ventasSinPrecioMes > 0`, mismo tipo de aclaración que T211.

---

## Bloque 8 — Frontend: reportes — columna "Beneficio por kg vendible" (usa Bloque 6)

- [ ] **T216 [P]** `features/reportes/components/TablaReporte.test.tsx`, caso nuevo: una
  fila con `beneficioPorKgVendible` numérico lo muestra formateado con `formatearPesos`;
  con `null` muestra "—" (mismo criterio que `costoKgVendiblePromedio` ya tiene).
- [ ] **T217** `features/reportes/api/types.ts` (agrega el campo a los 3 tipos de item) +
  `components/TablaReporte.tsx` (nueva columna, mismo orden en las 3 tablas ya que
  reusan el componente).

---

## Bloque 9 — Cierre de fase

- [ ] **T218** Agregar a `../quickstart.md` los pasos de Fase 5: cargar un precio de venta
  desde "Editar cortes"; escanear una venta de ese corte y confirmar "Recaudado hoy" en
  Control diario; escanear una venta de un corte **sin** precio y confirmar que igual se
  registra (aparece en "ventas sin precio"); revisar "Recaudado este mes" en Inicio; abrir
  Reportes y confirmar la columna "Beneficio por kg vendible" en las 3 tablas.
- [ ] **T219** Ejecutar `../quickstart.md` completo (Fases 1-5) de punta a punta; corregir
  cualquier desvío. Pendiente: requiere clickear la UI real con el back y el front
  levantados — no lo puede hacer esta sesión por sí sola.
- [ ] **T220** Revisión final: confirmar que ningún corte existente quedó con `precio_total`
  de ventas viejas recalculado retroactivamente (deben seguir en `null`, Bloque 4 no hace
  ningún backfill) y que `spec.md` recibió, aunque sea como nota, la referencia a esta
  fase (ver nota abajo) para que la próxima sesión no la pierda de vista.

---

## Nota sobre `spec.md`

Esta fase no tocó `../spec.md`: agregarle una sección "Fase 5" formal implicaría
renumerar las secciones 6-10 actuales (y revisar cada referencia cruzada a
"sección 9", "sección 5.3", etc. en otros archivos del proyecto), algo que no se pidió y
que no vale la pena arriesgar solo por esta tanda de tasks. Si en algún momento se quiere
esa paridad, la forma más segura es agregar una sección nueva al final (después de
"10. Preguntas abiertas") en vez de insertarla entre las fases existentes.

---

## Dependencias entre bloques

- Bloque 1 (backend `precio_venta`): sin dependencias.
- Bloque 2 (frontend "Editar cortes"): depende del Bloque 1 (necesita el campo en la API).
- Bloque 3 (modelo puro `CalculadorVenta`): sin dependencias, en paralelo con 1-2.
- Bloque 4 (venta persiste `precio_total`): depende de los Bloques 1 y 3.
- Bloque 5 (resumen diario): depende del Bloque 4.
- Bloque 6 (reportes, "Beneficio por kg vendible"): depende solo del Bloque 1 (no de
  `ventas` en absoluto — usa `despostado`/`cortes`), en paralelo con los Bloques 3-5.
- Bloque 7 (frontend caja/resumen del mes): depende de los Bloques 4 y 5.
- Bloque 8 (frontend reportes): depende del Bloque 6.
- Bloque 9 (cierre): depende de todo lo anterior.

## Ejemplo de ejecución en paralelo

Al arrancar la fase, en paralelo:
```
T187                    (test de precio_venta en CorteController, Bloque 1)
T197                    (test de CalculadorVenta, Bloque 3, no necesita el Bloque 1)
```
Una vez cerrado el Bloque 1, en paralelo:
```
T190, T191, T192, T193  (frontend "Editar cortes", Bloque 2)
T206, T208              (tests de reportes, Bloque 6, no necesita los Bloques 3-5)
```
Una vez cerrados los Bloques 4 y 5, en paralelo:
```
T211, T213              (frontend control diario / tipos de venta, Bloque 7)
T216                    (frontend reportes, Bloque 8, si el Bloque 6 ya cerró)
```
