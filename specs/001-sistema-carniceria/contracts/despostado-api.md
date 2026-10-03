# Contrato de API — Fase 1: Despostado

Base: `/api/v1`. Todas las rutas requieren el JWT de Supabase en `Authorization: Bearer <token>`; todas, salvo que se indique lo contrario, exigen rol `dueno` (lo exige RLS en la base, no solo el backend).

Los kilos y los importes viajan como `string` con formato `numeric` de Postgres (ej. `"3.300"`), nunca como `number` de JSON, para no perder precisión decimal en el cliente ni en el servidor.

## Cortes

### `GET /cortes?incluirInactivos=false`
Lista los cortes **del propio negocio** (RLS, `V10__multi_negocio.sql` — cada dueño tiene su catálogo, un empleado ve el de su dueño). `incluirInactivos=true` solo lo respeta el backend si el usuario es `dueno`.

```json
200 OK
[
  { "id": "uuid", "nombre": "Vacío", "plu": 12, "cuarto": "Trasero", "zonaMapa": "vacio", "activo": true }
]
```

### `POST /cortes`
```json
// request
{ "nombre": "Vacío", "plu": 12, "cuarto": "Trasero", "zonaMapa": "vacio" }
// 201 Created → mismo shape que GET, con "id"
```
`409 Conflict` si el `plu` ya existe **en el propio negocio** (el PLU es único por `dueno_id`, no global — dos negocios distintos pueden usar el mismo número).

### `PUT /cortes/{id}`
Mismo body que `POST`, incluye `activo`. `200 OK` con el corte actualizado.

## Estimación automática (FR-113)

### `GET /medias-reses/estimacion?pesoKg=100.000`
Solo lectura — no persiste nada. Devuelve el kilaje estimado por corte, calculado como el promedio histórico de `kg / peso_kg` de las entradas ya cargadas **por el usuario autenticado que hace el pedido** (no las de otros usuarios), multiplicado por el `pesoKg` recibido.

```json
200 OK
{
  "cortes": [ { "corteId": "uuid", "kgEstimado": "3.150" }, ... ]
}
```
`409 Conflict` con `{ "error": "SIN_HISTORIAL", "mensaje": "Todavía no hay ninguna entrada cargada para estimar." }` si el usuario autenticado no tiene ninguna entrada previa propia, aunque otros usuarios ya tengan historial — así el frontend sabe que debe ofrecer solo el modo manual (FR-113).

## Medias reses (entradas de carne)

Una media res no existe en la base hasta que se completa la carga entera: no hay `POST` que la cree sola. El flujo es: el dueño completa peso/precio/proveedor y la tabla de cortes en el frontend (sin pegarle al backend salvo para pedir la estimación), y recién al tocar "Cargar entrada" se manda todo junto.

### `POST /medias-reses` — "Cargar entrada" (FR-111)
Crea, en una sola transacción, la media res, sus filas de `despostado` y sus `perdidas`. Todo o nada.

```json
// request
{
  "proveedor": "Frigorífico Sur",
  "pesoKg": "100.000",
  "precioKg": "5200.00",
  "cortes": [ { "corteId": "uuid", "kg": "3.300" }, ... ],
  "perdidas": { "hueso": "11.000", "grasa": "6.000", "merma": "2.000" }
}
// 201 Created
{
  "id": "uuid", "fecha": "2026-10-02", "proveedor": "Frigorífico Sur",
  "pesoKg": "100.000", "precioKg": "5200.00",
  "despostado": [ { "corteId": "uuid", "kg": "3.300" } ],
  "perdidas": { "hueso": "11.000", "grasa": "6.000", "merma": "2.000" },
  "resumen": {
    "vendibleKg": "81.000", "perdidaKg": "19.000", "sinAsignarKg": "0.000",
    "rendimientoPorc": "81.00", "costoTotal": "520000", "costoKgVendible": "6420"
  }
}
```
`precioKg` es opcional; si se omite, se guarda `null` (la tarjeta de costo muestra "—" en el frontend, y `resumen.costoTotal`/`costoKgVendible` viajan en `null`). `cortes` puede tener cualquier subconjunto de los cortes activos (los que no se cargaron, simplemente no tienen fila). `resumen` lo calcula el `Model` del backend (capa `modelo/`, ver `research.md`) con las mismas fórmulas que el frontend usa para la vista en vivo antes de guardar — es la versión "oficial" una vez persistido.

**Validaciones (backend, además de las de la base):**
- Cada `corteId` debe existir y estar `activo`.
- Cada `kg` debe ser `> 0` si está presente.
- No se bloquea el guardado si `sin_asignar_kg ≠ 0`: esa comparación es solo informativa en el frontend (spec, sección 2.2, US-1.2).

**Errores:**
```json
400 Bad Request
{ "error": "CORTE_INEXISTENTE", "mensaje": "El corte uuid no existe o está inactivo." }
```

### `GET /medias-reses/{id}`
Mismo shape que la respuesta de `POST`, incluido `resumen` (se recalcula en cada lectura, no se persiste — ver `data-model.md`, "Valores calculados").

```json
200 OK
{
  "id": "uuid", "fecha": "2026-10-02", "proveedor": "Frigorífico Sur",
  "pesoKg": "100.000", "precioKg": "5200.00",
  "despostado": [ { "corteId": "uuid", "kg": "3.300" } ],
  "perdidas": { "hueso": "11.000", "grasa": "6.000", "merma": "2.000" },
  "resumen": { "vendibleKg": "81.000", "perdidaKg": "19.000", "sinAsignarKg": "0.000",
    "rendimientoPorc": "81.00", "costoTotal": "520000", "costoKgVendible": "6420" }
}
```

### `PUT /medias-reses/{id}`
Corrige una entrada ya cargada: `proveedor`, `pesoKg`, `precioKg`, y/o la lista completa de `cortes`/`perdidas` (mismo body que `POST`, reemplaza lo anterior en una transacción). Para correcciones posteriores, no para la carga inicial.

### `GET /medias-reses?desde=2026-10-02&hasta=2026-10-02`
Lista entradas de carne, filtrable por rango de `creado_en` (usado por FR-206 para contar las cargadas hoy). Paginada (`page`, `size`) si la lista crece.

## Perfiles (multi-negocio, `V10__multi_negocio.sql`)

El alta de cuenta (email+contraseña) no pasa por esta API: el frontend llama directo a `supabase.auth.signUp(...)`, con `nombre`/`rol_solicitado`/`dueno_invitador_id` en `options.data` (quedan en el `user_metadata` del JWT). Estos endpoints son lo que pasa *después* de eso.

Dos caminos de registro:
- **Sin link de invitación** (formulario genérico): `rol_solicitado: "dueno"`. Nace `pendiente`, sin `dueno_id`, a la espera de que un `admin` lo apruebe (ver `PUT /perfiles/{id}`). "Empleado" no es una opción acá — un empleado siempre llega por el link de su dueño.
- **Con link de invitación** (`?invita=<id-del-dueño>` en el frontend): `rol_solicitado: "empleado"`, `dueno_invitador_id: "<ese id>"`. Si ese `id` corresponde a un `dueno` aprobado, la cuenta nace **directo en `estado: "aprobado"`** con `dueno_id` = ese id — nadie tiene que aprobar un segundo paso, el dueño ya lo vouch-eó al invitarlo. Si el código no es válido, cae al mismo `pendiente` sin `dueno_id` que el camino genérico (caso borde raro, sin flujo de UI para resolverlo después).

### `GET /perfiles/yo`
Cualquier usuario autenticado. Si todavía no existe un `perfiles` para este usuario, lo crea según lo de arriba. Si ya existe y es un `dueno` aprobado, de paso siembra su catálogo de cortes si todavía no tiene ninguno (`CatalogoInicialService`).

```json
200 OK
{ "id": "uuid", "nombre": "Ana Gómez", "rol": "empleado", "estado": "aprobado" }
```

### `GET /perfiles?estado=pendiente`
Solo `admin` (RLS, `V9__rol_admin.sql`). Lista cuentas por estado (`pendiente` por default). Un `dueno` no-admin no recibe `403`: RLS simplemente no le muestra ninguna fila ajena (su propia fila solo aparece si coincide con el filtro de `estado`), así que ve una lista vacía.

```json
200 OK
[ { "id": "uuid", "nombre": "Ana Gómez", "rol": "empleado", "estado": "pendiente" } ]
```

### `PUT /perfiles/{id}`
Solo `admin` (RLS). Aprueba, rechaza, o cambia el rol de cualquier cuenta — incluido promover a alguien a `admin` (algo que nadie puede pedir para sí mismo al registrarse, ver `GET /perfiles/yo`).

```json
// request
{ "rol": "empleado", "estado": "aprobado" }
// 200 OK → mismo shape que GET /perfiles/yo
```

`403 ACCESO_DENEGADO` si quien llama no es un `admin` aprobado (RLS bloquea el `UPDATE`, el backend lo traduce) — salvo que el `id` apuntado tampoco sea visible para quien llama (no es ni su propia fila ni hay política que se la muestre), en cuyo caso es `404 PERFIL_NO_ENCONTRADO` antes que nada.

## Formato de error común

```json
{ "error": "<CODIGO_EN_MAYUSCULAS>", "mensaje": "<texto en español rioplatense para mostrar al usuario>" }
```
