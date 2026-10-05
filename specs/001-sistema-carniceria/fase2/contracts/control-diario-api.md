# Contrato de API — Fase 2: Escaneo y stock

Mismas convenciones que `../../fase1/contracts/despostado-api.md`: kilos/importes como `string` (nunca `number`,
ver el hallazgo de Jackson 3 en `../../fase1/research.md`), errores como `{ "error": "CODIGO", "mensaje": "..." }`,
toda fecha/hora en `America/Argentina/Buenos_Aires`.

**Incluye los agregados de Fase 3** (`../../fase3/plan-fase3.md`): `POST /ventas/{id}/anular` y el
campo `usuarioId` en `VentaResponse`, marcados explícitamente más abajo. La cola offline de
Fase 3 no agrega ningún endpoint nuevo — reusa `POST /ventas` tal cual.

## `POST /ventas` — registrar un escaneo (FR-201 a FR-204)

El decodificador de EAN-13 vive **solo en el backend** (lee `config_etiqueta`, nunca
expuesta al navegador — ver `plan-fase2.md`, "Dónde vive el decodificador"). El frontend
manda el código crudo tal cual lo imprime el lector, más un `idClienteLocal` que genera
él mismo al capturar el evento de escaneo (antes de saber si el código es válido: así el
mismo valor sirve para detectar un "Enter" duplicado del lector).

```json
// request
{
  "codigo": "2000012012501",
  "idClienteLocal": "a1b2c3d4-...-uuid-v4"
}
```

```json
// 201 Created — primera vez que se ve este idClienteLocal
{
  "id": "uuid",
  "fechaHora": "2026-10-10T14:32:05-03:00",
  "corteId": "uuid",
  "corteNombre": "Vacío",
  "kg": "1.250",
  "codigoLeido": "2000012012501",
  "usuarioId": "uuid"
}
```

`idClienteLocal` repetido → `200 OK` con la misma venta ya registrada (no se vuelve a
insertar ni a descontar stock — caso borde de `../../spec.md`, sección 3.3).

Errores, en el orden de validación de FR-203 (el primero que falla corta la cadena):

| HTTP | `error` | Causa |
|---|---|---|
| 400 | `DIGITO_VERIFICADOR_INVALIDO` | El último dígito no coincide con el checksum EAN-13. |
| 400 | `PREFIJO_INVALIDO` | Los primeros 2 dígitos no caen en el rango de `config_etiqueta` (20-29 por defecto). |
| 400 | `PLU_INEXISTENTE` | El PLU decodificado no corresponde a ningún corte con `activo = true`. |
| 400 | `PESO_CERO` | El valor decodificado (peso o importe→peso) da `0`. |

Ninguno de estos cuatro persiste nada (ni siquiera con un `idClienteLocal` nuevo).

## `GET /ventas?desde=2026-10-10&hasta=2026-10-10&limite=20`

Mismo patrón que `GET /medias-reses` (Fase 1): `desde`/`hasta` obligatorios, más
recientes primero. `limite` opcional (lo usa el widget "Ventas de hoy"; sin él, devuelve
todas las del rango — lo que usa el link "ver todas").

```json
200 OK
[
  { "id": "uuid", "fechaHora": "...", "corteNombre": "Vacío", "kg": "1.250", "anulada": false, "usuarioId": "uuid" },
  ...
]
```

`empleado`: ve todas las ventas del rango **de su propio negocio** (no solo las propias —
no hay noción de "mías" para *ver*, solo para *anular*, ver abajo), nunca `precio_kg` ni
costos (no están en este DTO). `usuarioId` (agregado en Fase 3) no es un dato sensible —
sirve para que el frontend decida cuándo mostrar el botón "Anular" sin intentarlo primero
(`../../fase3/plan-fase3.md` 3.6).

## `POST /ventas/{id}/anular` — anular una venta (Fase 3, FR-307/308)

Sin body. Nunca borra la fila: siempre un `UPDATE anulada = true` (Principio I).

```json
200 OK
{ "id": "uuid", "fechaHora": "...", "corteNombre": "Vacío", "kg": "1.250", "anulada": true, "usuarioId": "uuid" }
```

Reglas (evaluadas con la hora del **servidor**, `ventas.fecha_hora`, nunca con el reloj de
quien hace el pedido — caso borde de `../../spec.md` 4.3):
- `dueno`: puede anular cualquier venta de su negocio, sin límite de tiempo.
- `empleado`: solo si `usuarioId` de la venta es el suyo **y** pasaron menos de 5 minutos
  desde `fechaHora`.
- Anular una venta ya anulada es idempotente (vuelve a quedar `anulada: true`, `200`), si
  todavía se cumplen las reglas de arriba.

Errores:
```json
404 Not Found
{ "error": "VENTA_NO_ENCONTRADA", "mensaje": "La venta uuid no existe o no es de tu negocio." }

403 Forbidden
{ "error": "VENTA_NO_SE_PUEDE_ANULAR", "mensaje": "No podés anular esta venta: es de otro usuario o pasaron más de 5 minutos." }
```

## `GET /stock` — stock por corte (FR-205, FR-208)

```json
200 OK
[
  { "corteId": "uuid", "corteNombre": "Vacío", "entradoKg": "15.300", "vendidoKg": "13.800", "stockKg": "1.500", "quedaPoco": true },
  ...
]
```

`quedaPoco = stockKg < 0.15 * entradoKg` (umbral de FR-208), calculado en el backend a
partir de la vista `stock_por_corte` (`data-model-fase2.md`) — la vista da los kilos
crudos, el semáforo es presentación. `dueno` y `empleado` acceden igual (US-2.3); ningún
costo se expone acá de todos modos.

## `GET /control-diario/resumen?fecha=2026-10-10` (FR-206)

Sin `fecha`, usa hoy (hora Argentina).

```json
200 OK
{
  "kgVendidosHoy": "42.800",
  "etiquetasEscaneadasHoy": 37,
  "stockVendibleTotal": "128.400",
  "entradasHoy": 2
}
```

`etiquetasEscaneadasHoy` cuenta **todos** los escaneos válidos del día (filas de `ventas`,
anuladas incluidas — es una métrica de actividad del mostrador, no de ventas netas).
`kgVendidosHoy` (desde Fase 3, `../../fase3/plan-fase3.md` 3.8) **excluye** las ventas anuladas, para no
contradecir a `stockVendibleTotal`: la suma de `stockKg` de `GET /stock`, no acotada al día
(el stock es acumulado histórico), que ya excluye anuladas desde `stock_por_corte` (Fase 2).

## `GET /config-etiqueta` / `PUT /config-etiqueta` (FR-209)

Solo `dueno`. Una fila por negocio (`data-model-fase2.md` — PK `dueno_id`, sembrada sola
al aprobar la cuenta, igual que el catálogo de cortes), sin ningún id en el body: siempre
se lee/edita la del propio negocio de quien llama.

```json
// GET 200 / PUT request y response
{
  "prefijoDesde": 20,
  "prefijoHasta": 29,
  "inicioPlu": 2,
  "largoPlu": 5,
  "inicioValor": 7,
  "largoValor": 5,
  "tipoValor": "peso",
  "decimales": 3
}
```

`PUT` con rangos inconsistentes (ej. `inicioPlu + largoPlu` se superpone con
`inicioValor`) → `400 CONFIGURACION_ETIQUETA_INVALIDA`. No se valida contra ningún
código real: es responsabilidad del dueño probar un escaneo real después de guardar.

`GET /control-diario/exportar` (FR-210, exportar a Excel) no se construye — descartado por
decisión del dueño (2026-10-04), no forma parte de esta fase.
