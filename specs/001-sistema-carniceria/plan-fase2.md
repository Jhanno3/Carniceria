# Plan de implementación: Fase 2 — Escaneo y stock

**Spec de origen:** `spec.md`, sección 3
**Constitución:** `constitution.md` v1.0.0
**Fecha:** 2026-10-03
**Estado:** Listo para implementar — `tasks-fase2.md` generado

**Nota:** este plan se escribió antes del pivot a multi-negocio (`V9__rol_admin.sql`,
`V10__multi_negocio.sql` — ver `data-model.md`). Ya está actualizado para reflejarlo:
`ventas` y `config_etiqueta` son por negocio, y las migraciones de esta fase arrancan en
`V11` (la `V10` real la usó el pivot).

## 1. Resumen

Esta fase construye: el decodificador de etiquetas EAN-13 (configurable, FR-202),
el registro de ventas por escaneo con feedback en menos de 1 segundo (FR-201/FR-203/FR-204),
el stock por corte calculado por vista (FR-205), el resumen del día y "ventas de hoy"
(FR-206/FR-207), el umbral de "Queda poco" (FR-208), la pantalla de configuración de
etiqueta (FR-209) y la exportación a Excel (FR-210). No incluye cola offline ni anulación
de ventas (Fase 3) ni roles finos más allá de los que ya distinguía RLS desde la Fase 1.

## 2. Contexto técnico (delta sobre `plan.md`)

| Punto | Valor |
|---|---|
| Alcance de datos nuevo | `ventas`, `config_etiqueta` (`data-model-fase2.md`); `despostado` gana una política de `select` para `empleado` que ya estaba anticipada en `V2__cortes.sql`. |
| Generación de Excel | Apache POI (`poi-ooxml`), nueva dependencia Maven. |
| Nada nuevo en el frontend a nivel de librerías | El escaneo por lector USB es un `<input>` de texto normal + `onKeyDown`/`onChange` (el lector "escribe" y manda Enter, no hay API de hardware involucrada). |

## 3. Decisiones de esta fase (equivalente a `research.md` de la Fase 1)

### 3.1 Dónde vive el decodificador de EAN-13

**Decisión: solo en el backend.** El frontend manda el código crudo tal cual lo entrega
el lector; el backend lee la `config_etiqueta` **del negocio de quien escanea**
(`mi_negocio_id()`, ver 3.1b) y decodifica. Alternativa descartada: decodificar en el
frontend (necesitaría exponer `config_etiqueta` al navegador, inclusive a cuentas
`empleado`, y duplicaría la lógica de validación en dos lugares sin que el dueño lo haya
pedido — a diferencia de las fórmulas de Fase 1, acá no hay un pedido explícito de
arquitectura en capas que lo justifique, así que rige el Principio VII: una sola
implementación). Consecuencia: `POST /ventas` es el único lugar que conoce el formato de
etiqueta vigente; cambiar `config_etiqueta` no requiere ningún cambio ni redeploy de
frontend.

### 3.1b `config_etiqueta` por negocio, no singleton global

El diseño original de esta fase (anterior al pivot a multi-negocio) la planteaba como una
tabla de una sola fila para todo el sistema. Con varios negocios independientes, cada
carnicería puede tener una balanza distinta: `config_etiqueta` pasa a tener **una fila por
negocio** (`dueno_id` como PK, no un `id` fijo). Se siembra con el mismo mecanismo que el
catálogo de cortes (`CatalogoInicialService`, Fase 1): la primera vez que un dueño recién
aprobado pide `GET /perfiles/yo`, además de sus cortes de ejemplo se le crea su fila de
`config_etiqueta` con los valores de ejemplo de la sección 7. No hay migración de seed
(no existe "la" fila global a sembrar una sola vez).

### 3.1c Resolver "mi negocio" desde Java, no solo desde SQL

Hasta Fase 1, todo lo que escribía en una tabla de negocio era siempre el propio dueño
(`usuarioId == tenant`), así que el backend nunca necesitó preguntarse "¿de qué negocio es
este usuario?" — se lo dejaba enteramente a `mi_negocio_id()` del lado de Postgres (RLS).
Fase 2 rompe ese supuesto: un **empleado** también escanea, y su propio `id` no es el
`dueno_id` del negocio (es el de quien lo invitó). El `VentaService` necesita saber el
`dueno_id` del negocio para dos cosas que no puede dejarle solo a RLS: elegir **qué fila**
de `config_etiqueta` leer (podría haber una por negocio visible, pero acá no hay ambigüedad
porque RLS ya la acota a una sola) y para loguear `dueno_id` en la fila nueva de `ventas`
antes de insertarla. Se agrega un método compartido, `PerfilRepository.findById(usuarioId)`
→ `.getDuenoId()` (ya es una entidad de Fase 1, visible por `perfiles_select_propio` para
el propio usuario), usado desde `escaneo/service/VentaService` y `ConfigEtiquetaService`.
No hace falta replicar la función SQL `mi_negocio_id()` en Java — alcanza con leer la
misma columna por JPA.

### 3.2 Algoritmo del dígito verificador EAN-13

Estándar GS1: sobre los primeros 12 dígitos, suma los de posición impar (1ª, 3ª, ...) tal
cual y los de posición par ×3; el dígito verificador es lo que le falta a esa suma para
llegar al siguiente múltiplo de 10. Función pura en
`backend/.../escaneo/modelo/Ean13.java` (sin Spring), con tests contra el ejemplo de la
especificación (`2000012012501`) y un dígito adulterado.

### 3.3 Foco persistente del campo de escaneo (FR-201, Principio IV)

El campo usa una `ref` de React; un `useEffect` lo re-enfoca cada vez que cambia
`ultimoEscaneo` (éxito o error) y también en un listener de `blur` del propio input. No
hace falta ninguna librería: es el mismo patrón que cualquier campo de "siempre con foco"
en un POS web.

### 3.4 Vista de stock y `security_invoker`

Postgres ejecuta una vista por defecto con los permisos de su dueño, no de quien la
consulta — eso saltearía RLS por completo (mismo tipo de hallazgo de seguridad que ya
apareció en Fase 1 con los `UPDATE` sin `@Version`, ver `research.md`). La vista
`stock_por_corte` se crea explícitamente con `security_invoker = true` (`data-model-fase2.md`)
para que las políticas de `despostado` y `ventas` se apliquen con el rol **y el negocio**
de quien pregunta, no con el del dueño de la vista — sin esto, no solo se saltearía RLS
por rol, sino que cualquier negocio vería el stock de cualquier otro.

### 3.5 Exportar a Excel

Apache POI, en una clase de servicio nueva (`controldiario/service/ExportService.java`),
sin capa `modelo/` propia: arma el `.xlsx` a partir de los mismos DTOs que ya devuelven
`GET /ventas` y `GET /stock`, no recalcula nada. Un test de integración alcanza (contenido
binario simple de verificar: cantidad de filas por hoja).

### 3.6 Fuera de alcance de esta fase (Principio VII)

- **Escaneo por cámara** ("Usar cámara del celular", sección 4.2 de la especificación):
  no tiene requisito funcional propio más allá del botón alternativo. Se deja marcada como
  tarea opcional en `tasks-fase2.md`, fuera del camino crítico — agregarla implica sumar
  una librería de lectura de barcode por cámara que hoy no tiene ningún FR que la exija
  con detalle (ej. qué pasa si la cámara no decodifica, qué formatos soporta).
- **Anulación de ventas:** Fase 3 (FR-307/308). `ventas.anulada` existe en el DDL desde ya
  (para no migrar el tipo de dato después) pero ningún endpoint de esta fase la escribe.
- **Cola offline (IndexedDB):** Fase 3 (FR-301-303). `id_cliente_local` ya se genera en el
  frontend desde esta fase (lo pide un caso borde de la Fase 2 misma, no solo la Fase 3),
  pero no hay cola ni sincronización todavía: si no hay conexión, el escaneo simplemente
  falla.

## 4. Chequeo contra la constitución

| Principio | Cumplimiento en este plan |
|---|---|
| I. Integridad de los datos | `ventas.kg` como `numeric`; stock como vista, nunca columna (3.4); `id_cliente_local unique` da idempotencia ante el doble "Enter" del lector. |
| II. Seguridad | RLS en `ventas` y `config_etiqueta` desde su migración; `security_invoker` en la vista de stock (3.4); el decodificador no expone `config_etiqueta` a ningún rol vía el navegador (3.1); `empleado` nunca recibe `precio_kg`/costos en ningún DTO nuevo. |
| III. Cálculos confiables | `Ean13.java` (dígito verificador) es función pura con tests (3.2); el umbral de "Queda poco" es una regla simple en el Service, cubierta por test de integración del endpoint `GET /stock`. |
| IV. El mostrador no se detiene | Foco persistente (3.3); `POST /ventas` es una sola llamada HTTP (decodificar + validar + persistir juntos) para entrar cómodo en el presupuesto de 1 segundo de FR-201. |
| V. Hecho para Argentina | `fecha_hora` y los cortes de "hoy" en `America/Argentina/Buenos_Aires`, igual que Fase 1. |
| VI. Usabilidad y accesibilidad | Campo de escaneo de 56 px (sección 5.3 de la especificación); fila de stock "Queda poco" no depende solo del color (mismo criterio ya aplicado en Fase 1 a la tabla de cortes). |
| VII. Simplicidad | Decodificador en un solo lugar (3.1); cámara y anulación quedan explícitamente afuera (3.6); la vista de stock resuelve el cálculo en la base, no se duplica en Java. |
| VIII. Cambios controlados | `ventas`/`config_etiqueta`/vista/política nueva de `despostado`, todo en migraciones Flyway versionadas (`V12`-`V15` en `tasks-fase2.md`). |

## 5. Estructura del proyecto (nuevo sobre `plan.md`)

```
backend/src/main/java/com/carniceria/
  escaneo/
    controller/VentaController.java, ConfigEtiquetaController.java
    service/VentaService.java, ConfigEtiquetaService.java, ExportService.java
    modelo/Ean13.java, DecodificadorEtiqueta.java   (dominio puro, con tests)
    entity/VentaEntity.java, ConfigEtiquetaEntity.java
    dto/EscanearRequest.java, VentaResponse.java, StockCorteResponse.java,
        ResumenDiaResponse.java, ConfigEtiquetaDto.java
    repository/VentaRepository.java, ConfigEtiquetaRepository.java, StockRepository.java
backend/src/main/resources/db/migration/
  V12__despostado_empleado_select.sql
  V13__ventas.sql
  V14__config_etiqueta.sql
  V15__stock_por_corte.sql
  (sin migración de seed para config_etiqueta: se siembra por negocio, ver 3.1b)

frontend/src/features/control-diario/
  modelo/                 (ninguno propio: el decodificador no vive en el frontend)
  components/CampoEscaneo.tsx, UltimoEscaneo.tsx, ResumenDia.tsx, VentasDeHoy.tsx,
             TablaStock.tsx
  api/useEscanear.ts, useStock.ts, useResumenDia.ts, useVentas.ts
  ControlDiarioPage.tsx
frontend/src/features/ajustes/
  components/FormularioConfigEtiqueta.tsx
  api/useConfigEtiqueta.ts, useActualizarConfigEtiqueta.ts
  AjustesPage.tsx
```

## 6. Diseño

- **Datos:** `data-model-fase2.md` — `ventas`, `config_etiqueta`, vista `stock_por_corte`,
  más la política de `empleado` que le faltaba a `despostado`.
- **API:** `contracts/control-diario-api.md` — `POST /ventas`, `GET /ventas`, `GET /stock`,
  `GET /control-diario/resumen`, `GET`/`PUT /config-etiqueta`, `GET /control-diario/exportar`.
- **Verificación:** se agrega a `quickstart.md` en `tasks-fase2.md` (último bloque), no se
  duplica un archivo nuevo — mismos pasos manuales, continuando la numeración de la Fase 1.

## 7. Enfoque para generar tareas (no se ejecuta en este documento)

Mismo orden TDD que la Fase 1 (`plan.md`, sección 7), adaptado:
1. Migraciones (`V12`-`V15`, en orden).
2. Capa `modelo/` del backend (`Ean13`, `DecodificadorEtiqueta`) — tests primero, sin Spring.
3. Feature `escaneo`: tests de integración de `POST /ventas` (incluye los 4 códigos de
   error en orden, el caso de `idClienteLocal` repetido, y el bloqueo a cada rol donde
   corresponda) → entity/repository/service/controller.
4. Feature `stock`/`resumen`: tests de `GET /stock` y `GET /control-diario/resumen` →
   implementación (usa la vista de 3.4).
5. `config_etiqueta`: tests de `GET`/`PUT` (incluido el `400` de rangos superpuestos) →
   implementación.
6. Exportar a Excel: test de integración mínimo → `ExportService`.
7. Frontend: `ControlDiarioPage` (campo de escaneo con foco persistente, resumen,
   ventas de hoy, stock) y `AjustesPage` (`config_etiqueta`), con sus tests de componente.
8. Cierre: pasos manuales agregados a `quickstart.md`.

## 8. Seguimiento de progreso

- [x] Spec revisada (`spec.md`, sección 3)
- [x] Chequeo contra la constitución (sección 4 de este documento)
- [x] Decisiones de diseño (sección 3 de este documento)
- [x] `data-model-fase2.md`
- [x] `contracts/control-diario-api.md`
- [x] `tasks-fase2.md`
