# Contrato de API — Fase 2: Escaneo y stock

Mismas convenciones que `despostado-api.md`: kilos/importes como `string` (nunca `number`,
ver el hallazgo de Jackson 3 en `research.md`), errores como `{ "error": "CODIGO", "mensaje": "..." }`,
toda fecha/hora en `America/Argentina/Buenos_Aires`.

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
  "codigoLeido": "2000012012501"
}
```

`idClienteLocal` repetido → `200 OK` con la misma venta ya registrada (no se vuelve a
insertar ni a descontar stock — caso borde de `spec.md`, sección 3.3).

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
  { "id": "uuid", "fechaHora": "...", "corteNombre": "Vacío", "kg": "1.250", "anulada": false },
  ...
]
```

`empleado`: ve todas las ventas del rango **de su propio negocio** (no solo las propias —
no hay todavía noción de "mías" sin los roles finos de Fase 3), nunca `precio_kg` ni
costos (no están en este DTO).

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
anuladas incluidas — no hay anulación todavía en esta fase); `stockVendibleTotal` es la
suma de `stockKg` de `GET /stock`, no acotada al día (el stock es acumulado histórico).

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

## `GET /control-diario/exportar?fecha=2026-10-10` (FR-210)

Solo `dueno`. Devuelve un `.xlsx` (`Content-Type: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`)
con dos hojas: "Ventas" (mismas columnas que `GET /ventas` de ese día) y "Stock" (mismas
columnas que `GET /stock`, calculado al momento de exportar — no es una foto histórica del
`fecha` pedido, el stock siempre es "ahora").
