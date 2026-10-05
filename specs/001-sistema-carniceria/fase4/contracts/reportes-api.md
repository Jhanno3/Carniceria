# Contrato de API — Fase 4: Reportes

Mismas convenciones que `../../fase1/contracts/despostado-api.md`: kilos/importes como
`string` (nunca `number`), errores como `{ "error": "CODIGO", "mensaje": "..." }`, toda
fecha `America/Argentina/Buenos_Aires`. Solo `dueno`/`admin`-operando-su-negocio: un
`empleado` recibe `[]` en los tres reportes (RLS, no un error — ver `plan-fase4.md` 2).

## `GET /reportes/por-proveedor?desde=2026-09-01&hasta=2026-09-30`

```json
200 OK
[
  { "proveedor": "Frigorífico Sur", "cantidadEntradas": 4, "rendimientoPromedioPorc": "81.50", "costoKgVendiblePromedio": "6420" },
  { "proveedor": null, "cantidadEntradas": 1, "rendimientoPromedioPorc": "78.00", "costoKgVendiblePromedio": null }
]
```

`proveedor: null` agrupa las entradas sin proveedor cargado (no se excluyen, `plan-fase4.md`
3.3). `costoKgVendiblePromedio` es `null` si ninguna entrada del grupo tiene `precioKg`
cargado. Ordenado alfabéticamente por `proveedor`, con el grupo `null` al final. Rango
vacío (sin medias reses en esas fechas) → `200 OK` con `[]`, nunca un error.

## `GET /reportes/por-categoria?desde=2026-09-01&hasta=2026-09-30`

```json
200 OK
[
  { "categoria": "Novillo", "cantidadEntradas": 3, "rendimientoPromedioPorc": "82.10", "costoKgVendiblePromedio": "6500" },
  { "categoria": null, "cantidadEntradas": 2, "rendimientoPromedioPorc": "79.40", "costoKgVendiblePromedio": "6300" }
]
```

Mismo criterio que `por-proveedor`, agrupando por `medias_reses.categoria` (FR-403).

## `GET /reportes/por-periodo?desde=2026-09-01&hasta=2026-09-30&periodo=semana`

`periodo`: `dia` | `semana` | `mes` (FR-402). Agrupa por el inicio de cada bucket
(`plan-fase4.md` 3.4: `semana` = lunes de esa semana; `mes` = día 1 de ese mes).

```json
200 OK
[
  { "periodoInicio": "2026-09-01", "cantidadEntradas": 2, "rendimientoPromedioPorc": "80.00", "costoKgVendiblePromedio": "6400" },
  { "periodoInicio": "2026-09-08", "cantidadEntradas": 3, "rendimientoPromedioPorc": "81.20", "costoKgVendiblePromedio": "6450" }
]
```

Ordenado cronológicamente por `periodoInicio`. `periodo` inválido (no es `dia`/`semana`/`mes`):

```json
400 Bad Request
{ "error": "PERIODO_INVALIDO", "mensaje": "\"anual\" no es un período válido (dia, semana o mes)." }
```

## `GET /perfiles` — cambio sobre Fase 1

`estado` pasa a ser **opcional** (antes tenía `defaultValue = "pendiente"`). Sin `estado`,
devuelve **todas** las cuentas del sistema, cualquier estado, ordenadas por nombre — sigue
exclusivo de `admin` (RLS, sin cambios). Con `estado` (`pendiente`/`aprobado`/`rechazado`),
se comporta exactamente igual que antes.

```json
GET /perfiles
200 OK
[
  { "id": "uuid", "nombre": "Juan Pérez", "rol": "dueno", "estado": "aprobado", "duenoId": "uuid" },
  { "id": "uuid", "nombre": "Ana Gómez", "rol": "empleado", "estado": "pendiente", "duenoId": null },
  ...
]
```
