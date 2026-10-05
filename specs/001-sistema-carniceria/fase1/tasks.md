# Tareas — Fase 1: Despostado

**Entradas:** `plan.md`, `research.md`, `data-model.md`, `contracts/despostado-api.md`, `../quickstart.md`.
**Orden:** TDD — en cada bloque, los tests se escriben y deben fallar antes de la implementación que los hace pasar (Principio III de la constitución).
**Capas (a pedido del dueño, ver `research.md`):** en cada feature del backend, `controller/` → `service/` → `modelo/` (dominio puro, sin Spring/JPA) → `repository/` → `entity/` (JPA), con `dto/` para los contratos de request/response. El frontend usa la misma idea: `modelo/` (funciones puras) separado de `components/` (UI) y `api/` (hooks + tipos de los DTO).
**`[P]`** = se puede hacer en paralelo con las otras tareas `[P]` del mismo bloque (tocan archivos distintos, sin dependencia entre ellas). Las tareas sin `[P]` del mismo bloque son secuenciales entre sí.

---

## Bloque 0 — Setup del monorepo

- [x] **T001** Crear `/backend` con Spring Initializr (Maven, Java 21): dependencias `spring-boot-starter-webmvc`, `spring-boot-starter-data-jpa`, `spring-boot-starter-validation`, `spring-boot-starter-security-oauth2-resource-server`, `flyway-database-postgresql`, `postgresql`. *(Spring Boot 4.1.1 — Initializr ya no ofrece 3.x; ver constitution.md actualizado.)*
- [x] **T002** Crear `/frontend` con Vite (plantilla `react-ts`); agregar Tailwind CSS v4, TanStack Query, Vitest, React Testing Library.
- [x] **T003 [P]** `backend/src/main/resources/application.yml`: datasource a Supabase Postgres (región Canada Central, ver `../../constitution.md`) y configuración de resource server JWT (`jwk-set-uri`), todo leído de variables de entorno (ninguna credencial literal en el archivo).
- [x] **T004 [P]** Tokens de color y tipografía de `especificacion-carniceria.md` sección 5 en `frontend/src/index.css` vía `@theme` de Tailwind v4 (no hay `tailwind.config.js`: la v4 usa configuración en CSS).
- [x] **T005 [P]** `.env.example` en la raíz, y `backend/.env`/`frontend/.env` (gitignorados) con las variables en blanco, listas para completar.
- [x] **T006 [P]** Paquetes del backend creados: `com.carniceria.cortes.{controller,service,entity,dto,repository}`, `com.carniceria.despostado.{controller,service,modelo,entity,dto,repository}`, `com.carniceria.perfiles.{entity,repository}`, `com.carniceria.shared.{security,error}`; y en el frontend `features/despostado/{modelo,components,api}`, `shared/{formato,ui}`.

## Bloque 1 — Migraciones (Flyway)

Orden según `data-model.md`; cada migración crea su tabla y activa RLS en el mismo archivo (Principio II).

- [x] **T007** `backend/src/main/resources/db/migration/V1__perfiles.sql` — tabla `perfiles` + RLS (un usuario solo lee su propia fila) + helper `is_dueno()`.
- [x] **T008** `V2__cortes.sql` — tabla `cortes` + RLS (`dueno`: todo; `empleado`: `select` donde `activo = true`).
- [x] **T009** `V3__seed_cortes.sql` — los 21 cortes de la sección 2.2 de la especificación, con PLU provisorio (Vacío = 12, igual que el ejemplo de la sección 7).
- [x] **T010** `V4__medias_reses.sql` — tabla `medias_reses` (sin columna `estado`) + RLS solo `dueno`.
- [x] **T011** `V5__despostado.sql` — tabla `despostado` (`unique(media_res_id, corte_id)`) + RLS solo `dueno`.
- [x] **T012** `V6__perdidas.sql` — tabla `perdidas` (`unique(media_res_id, tipo)`) + RLS solo `dueno`.
- [x] **T013** Ejecutado levantando el backend (`spring-boot:run`, Flyway migra al arrancar); confirmado con `psql` contra Supabase: 6 migraciones aplicadas, RLS activado en las 5 tablas + `perfiles`, 21 cortes cargados, 6 políticas creadas.

## Bloque 2 — Propagación del JWT a RLS (research.md)

- [x] **T013b** Rol de base de datos `app_backend` creado en Supabase: sin `BYPASSRLS`, sin ser dueño de ninguna tabla, `SELECT/INSERT/UPDATE/DELETE` sobre las 5 tablas + `ALTER DEFAULT PRIVILEGES` para las futuras, miembro de `authenticated` (para poder usar `auth.uid()`). Migraciones separadas de runtime: Flyway sigue usando `postgres.<project-ref>` (`spring.flyway.url/user/password`), el datasource de la app usa `app_backend` (`spring.datasource.*`). Verificado con `psql`: sin claims bloquea todo, con claims de un dueño real permite — ver `research.md`, "Rol de base de datos del backend — hallazgo crítico de seguridad" (se había planeado usar directamente `postgres`, que tiene `BYPASSRLS=true` en Supabase y hubiera vuelto cosmético todo el RLS).
- [x] **T014** `RlsPropagationIT.sinClaimsPropagadas_noVeNingunaFila` — corrido contra Supabase real: `0` filas sin claims, aunque ya exista un `dueno` de prueba (`87b585e4-...`) con una entrada cargada. Confirma fail-closed.
- [x] **T015** `backend/src/main/java/com/carniceria/shared/security/JwtClaimsContextFilter.java` + `JwtClaimsHolder.java` + `SecurityConfig.java` — `OncePerRequestFilter` que guarda `sub`/`role`/`email` del JWT validado en un holder de la petición. Cubierto por `JwtClaimsContextFilterTest` (2/2 OK, sin DB).
- [x] **T016** `backend/src/main/java/com/carniceria/shared/security/RlsSessionAspect.java` — `@Aspect` sobre métodos `@Transactional` que ejecuta `select set_config('request.jwt.claims', ?, true)` al abrir la transacción. *(Sin `SET LOCAL role`: no hace falta — ver ajuste en research.md.)*
- [x] **T017** `RlsPropagationIT.conClaimsDeUnDuenoReal_veLasFilasQueInserta` — con claims del dueño de prueba, inserta y cuenta `1` fila; Spring Test hace rollback solo al final (verificado con `psql`: la tabla queda en `0` filas después del test). 2/2 tests de `RlsPropagationIT` OK contra la base real.
- [x] **T018 [P]** `backend/src/main/java/com/carniceria/shared/error/GlobalExceptionHandler.java` + `ErrorResponse.java` + `ApiException.java` — formato común `{ error, mensaje }` para todas las excepciones de negocio.

## Bloque 3 — Feature `cortes` (CRUD simple, sin capa `modelo/`)

- [x] **T019 [P]** Test de integración `GET /cortes`, `POST /cortes` (incluye `409` por PLU duplicado), `PUT /cortes/{id}` — `backend/src/test/java/com/carniceria/cortes/CorteControllerTest.java`. 4/4 OK contra Supabase real (autenticación simulada con `SecurityMockMvcRequestPostProcessors.jwt()`, rollback automático por test).
- [x] **T020** `entity/CorteEntity.java` (enum `Cuarto` mapeado `EnumType.STRING`, `@GeneratedValue(strategy = GenerationType.UUID)`) + `repository/CorteRepository.java`.
- [x] **T021** `dto/CorteRequest.java`, `dto/CorteResponse.java` (shape de `contracts/despostado-api.md`).
- [x] **T022** `service/CorteService.java` (+ `PluDuplicadoException`, `CorteNoEncontradoException`, `CuartoInvalidoException`) + `controller/CorteController.java`.

## Bloque 4 — Feature `despostado`: capa `modelo/` (dominio puro, Java sin Spring/JPA)

- [x] **T023 [P]** `ResumenDespostadoTest.java` (6 tests, sin Spring): ejemplo obligatorio 100 kg/$5.200 → 81 kg vendibles, $6.420 por kg vendible, + casos sin precio, faltan/sobran kilos, sin vendible, peso inválido. *(Falló primero: con `costoTotal()`/`costoKgVendible()` redondeados a 2 decimales daba `$6.419,75`, no `$6.420` — ver hallazgo en `research.md`.)*
- [x] **T024 [P]** `EstimacionCalculatorTest.java` (3 tests, sin Spring): promedio histórico de `kg/pesoKg` por corte escalado a un `pesoKg` nuevo, un solo registro histórico, sin historial → mapa vacío.
- [x] **T025** `modelo/ResumenDespostado.java` + `modelo/Perdidas.java` — dominio puro. Redondeo corregido a **0 decimales** para `costoTotal()`/`costoKgVendible()` (no 2): ningún ejemplo de moneda de la especificación muestra centavos, y era la única forma de reproducir el `$6.420` exacto del ejemplo obligatorio.
- [x] **T026** `modelo/EstimacionCalculator.java` + `modelo/RegistroHistorico.java`. Sin historial devuelve un mapa vacío (no lanza excepción); el Service de Bloque 5 decide qué significa eso para la API (`409 SIN_HISTORIAL`).

## Bloque 5 — Feature `despostado`: persistencia y API (usa la capa `modelo/` del Bloque 4)

- [x] **T027 [P]** `MediaResControllerTest.java` (4 tests): éxito con `resumen` calculado, `400 CORTE_INEXISTENTE`, `400 KG_INVALIDO` sin persistir nada, bloqueo a `empleado`. Todos OK contra Supabase real.
- [x] **T028 [P]** Cubierto por el mismo archivo (`empleado_noPuedeCargarNiLeerEntradas`): RLS deniega el `INSERT` (SQLSTATE `42501`) → `GlobalExceptionHandler` lo traduce a `403 ACCESO_DENEGADO` (hallazgo nuevo, ver abajo).
- [x] **T029 [P]** `EstimacionControllerTest.java` (2 tests): `409 SIN_HISTORIAL` sin entradas, estimación correcta con una entrada histórica.
- [x] **T030** `entity/MediaResEntity.java`, `entity/DespostadoEntity.java`, `entity/PerdidaEntity.java` (enum `Tipo` en minúscula, igual al `CHECK` de la migración) + sus `repository/`. Sin `@ManyToOne` entre ellas a propósito (research.md): el cruce para el historial de estimación es una consulta JPQL explícita (`DespostadoRepository.buscarHistoricoCompleto()`).
- [x] **T031** DTOs de `contracts/despostado-api.md` — con un cambio importante: los campos de kilos/importes son **`String`**, no `BigDecimal` (ver hallazgo de Jackson 3 abajo). `BigDecimals.aTexto()/.parse()` (en `shared/`) hacen la conversión en los dos sentidos.
- [x] **T032** `service/MediaResService.cargarEntrada(...)` + `actualizar(...)`: valida peso/kg/cortes (4 excepciones de negocio nuevas: `PESO_INVALIDO`, `KG_INVALIDO`, `CORTE_INEXISTENTE`, `MEDIA_RES_NO_ENCONTRADA`), arma un `ResumenDespostado` y persiste todo en una transacción. `fecha` se calcula en hora Argentina (`ZoneId.of("America/Argentina/Buenos_Aires")`, Principio V), no con el default de Postgres.
- [x] **T033** `service/MediaResService.estimar(...)`: cuando no hay historial, lanza `SinHistorialException` (409) en vez de devolver el mapa vacío que da `EstimacionCalculator` — la traducción HTTP vive acá, no en el Model.
- [x] **T034** `controller/MediaResController.java`: las 5 rutas. El usuario (`creadoPor`) sale del propio JWT (`@AuthenticationPrincipal Jwt`), no del body.

**Hallazgos de esta fase (documentados en research.md):**
- **Jackson 3 en Spring Boot 4:** un `@Bean ObjectMapper` (Jackson 2, `com.fasterxml.jackson`) no tiene ningún efecto sobre lo que Spring MVC serializa — Boot 4 usa por dentro `tools.jackson` (Jackson 3) vía un mecanismo de *builder customizers* nuevo. En vez de pelear contra eso, los DTOs usan `String` directo para kilos/importes (conversión explícita con `BigDecimals`), que no depende de qué versión de Jackson esté actuando.
- **RLS deniega un `INSERT` con SQLSTATE `42501`:** `GlobalExceptionHandler` ahora atrapa `DataAccessException`, mira la causa raíz, y si es ese SQLSTATE devuelve `403 ACCESO_DENEGADO` en vez de un `500` genérico.
- Al escribir un test que insertaba un registro histórico llamando al `service` directo (sin pasar por HTTP), el `JwtClaimsContextFilter` no corre (es un filtro de servlet) — hubo que simular su trabajo seteando `JwtClaimsHolder` a mano en el test, igual que en `RlsPropagationIT` del Bloque 2.

## Bloque 6 — Frontend: capa `modelo/` (espejo del Bloque 4, mismos casos numéricos)

- [x] **T035 [P]** `resumen.test.ts` (7 tests): ejemplo obligatorio 100 kg/$5.200 → $6.420 (0 decimales, mismo hallazgo que el backend), sin precio, faltan/sobran kilos, sin vendible, 21 cortes reales sin arrastre de error de punto flotante, peso inválido.
- [x] **T036 [P]** `colorZonaMapa.test.ts` (4 tests): `t = 0, 0.5, 1` y valores fuera de `[0,1]` acotados.
- [x] **T037 [P]** `formatoEsAr.test.ts` (8 tests): `parsearNumero` acepta coma, punto, y el es-AR completo (`"1.250,5"`); `formatearKg`/`formatearPesos`.
- [x] **T038** `modelo/resumen.ts` — `calcularResumen`, función pura. Suma kilos en gramos enteros (`Math.round(kg*1000)`) para no acumular error de punto flotante al sumar varios cortes; mismo redondeo a 0 decimales que el backend para `costoTotal`/`costoKgVendible`.
- [x] **T039** `modelo/colorZonaMapa.ts`.
- [x] **T040** `shared/formato/formatoEsAr.ts` — usa `Intl.NumberFormat('es-AR', ...)` en vez de armar el formato a mano.

**19/19 tests OK** (`npm run test`), build y typecheck limpios (`npm run build`).

## Bloque 7 — Frontend: acceso a datos (`api/`)

- [x] **T041 [P]** `api/types.ts` — tipos que reflejan los DTO de `contracts/despostado-api.md`; kilos/importes como `string`, igual que el backend.
- [x] **T042 [P]** `api/useCortes.ts` — hook TanStack Query sobre `GET /cortes`.
- [x] **T043 [P]** `api/useEstimacion.ts` — hook sobre `GET /medias-reses/estimacion`, expone `disponible: boolean` (`false` ante `409 SIN_HISTORIAL` o si todavía no hay `pesoKg`).
- [x] **T044** `api/useCargarEntrada.ts` — mutation sobre `POST /medias-reses`; invalida la query de estimación al tener éxito (una entrada nueva cambia el % histórico).

**Plumbing agregado, no estaba en una tarea propia pero hacía falta para que los hooks funcionen:**
- `shared/supabase/cliente.ts` — cliente de Supabase con la clave anónima, solo para leer la sesión (login en sí no está en el alcance de esta fase — ver nota abajo).
- `shared/api/apiFetch.ts` — wrapper de `fetch` que adjunta `Authorization: Bearer <token>` desde la sesión de Supabase y traduce `{error, mensaje}` a una `ApiError` tipada.
- `vite-env.d.ts` — tipado de las variables `VITE_*`.
- Instalado `@supabase/supabase-js`.

**Hallazgo:** el `tsconfig` de esta plantilla de Vite tiene `erasableSyntaxOnly` activado (TypeScript 5.x), que prohíbe el azúcar sintáctico de "parameter properties" en constructores (`constructor(public readonly x: string)`) porque no es puramente borrable en la transpilación. Se escribió `ApiError` con los campos declarados aparte.

**Pendiente para cuando se arme `DespostadoPage` (Bloque 8):** ningún bloque de esta fase incluye una pantalla de login, pero `../quickstart.md` (paso 1) asume que el dueño ya inició sesión. ~~Falta decidir cómo se obtiene esa sesión en la práctica~~ — resuelto en el Bloque 7b.

## Bloque 7b — Registro con aprobación previa (a pedido del dueño, fuera del plan original)

No estaba en el alcance original de la Fase 1 (la invitación de empleados era Fase 3), pero sin esto no hay manera de entrar a la aplicación. El dueño pidió explícitamente: cualquiera puede registrarse, pero nadie entra hasta que él lo apruebe.

- [x] **Migración `V7__perfiles_estado.sql`**: columna `estado` (`pendiente`/`aprobado`/`rechazado`, default `pendiente`) en `perfiles`; el/los dueño(s) ya sembrados pasan a `aprobado` (si no, quedarían bloqueados por su propia migración); `is_dueno()` ahora exige `estado = 'aprobado'` además de `rol = 'dueno'` (si no, pedir "dueño" al registrarse alcanzaría para auto-aprobarse); políticas nuevas `perfiles_insert_propio` (cualquiera crea su propia fila, nunca la de otro) y `perfiles_dueno_todo`.
- [x] **Backend, feature `perfiles`** (packages que ya existían vacíos desde el Bloque 0): `PerfilEntity`/`PerfilRepository`, `PerfilService`, `PerfilController` — `GET /perfiles/yo` (crea el perfil en `pendiente` la primera vez, leyendo `nombre`/`rol_solicitado` de `user_metadata` del JWT), `GET /perfiles?estado=` (dueño, RLS), `PUT /perfiles/{id}` (aprobar/rechazar/cambiar rol, dueño, RLS). 3 tests de integración, todos contra Supabase real.
- [x] **Hallazgo de seguridad (ver research.md, "RLS puede dejar que un UPDATE 'tenga éxito' sin cambiar nada"):** `CorteService.actualizar` y el nuevo `PerfilService.actualizar` mutaban una entidad JPA leída de antes y confiaban en el flush automático de Hibernate. Como ninguna de las dos entidades tiene `@Version`, cuando RLS bloqueaba el `UPDATE` (alguien con permiso de lectura pero no de escritura sobre esa fila), Hibernate no se enteraba — el endpoint devolvía `200 OK` con datos que nunca se guardaron. Se corrigieron **las dos** (no solo `perfiles`) con `@Modifying @Query(...)` que devuelve filas afectadas, más `clearAutomatically = true` (sin esto, una consulta anterior en la misma transacción podía dejar cacheada la versión vieja). Nueva excepción compartida `shared/error/AccesoDenegadoException` (403 `ACCESO_DENEGADO`).
- [x] **Frontend:** `features/auth/` (`LoginForm`, `RegisterForm` — el registro pide nombre/email/contraseña/rol y llama directo a `supabase.auth.signUp`, nada pasa por el backend todavía; `AuthPage` alterna entre los dos); `features/perfiles/` (`api/usePerfilPropio`, `usePerfilesPendientes`, `useActualizarPerfil`, y `UsuariosPage` con la lista de pendientes y botones Aprobar/Rechazar); `App.tsx` reescrito para decidir, según la sesión de Supabase y `GET /perfiles/yo`, si mostrar login/registro, un aviso de "pendiente"/"rechazado", `UsuariosPage` (dueño) o un placeholder (empleado aprobado — Control diario es Fase 2).
- [x] Build y typecheck limpios, 19/19 tests de frontend siguen OK, 27/27 tests de backend OK contra Supabase real.

**No verificado (sin herramienta de navegador en esta sesión):** no pude clickear el flujo real en un browser. Tampoco pude probar el login real de punta a punta con un usuario confirmado por email, porque confirmar un email hecho a mano requeriría la clave `service_role` que el proyecto decidió no usar/guardar (constitution.md, Principio II) — los tests de integración simulan el JWT con el soporte de test de Spring Security, que no depende de esto, pero el flujo real de "confirmá tu mail" de Supabase no se ejecutó de punta a punta.

## Bloque 8 — Frontend: componentes de UI

- [x] **T045 [P]** `TarjetasResumen.test.tsx` (3 tests): sin precio muestra "—"/"Cargá el precio de compra"; con precio, valores es-AR; escribir en el campo de peso llama al callback.
- [x] **T046 [P]** `BarraComposicion.test.tsx` (3 tests): mensaje verde/naranja/rojo según `sinAsignarKg`.
- [x] **T047 [P]** `MapaCortes.test.tsx` (4 tests): zona más pesada con el color más oscuro, zona resaltada con borde grueso + detalle debajo, aclaración fija, sin datos no rompe.
- [x] **T048 [P]** `TablaCortes.test.tsx` (3 tests): editar kg llama al callback, tocar el nombre selecciona, la fila seleccionada se resalta.
- [x] **T049 [P]** `TarjetasPerdida.test.tsx` (2 tests): kg y % de las tres tarjetas, editar llama al callback con el tipo correcto.
- [x] **T050 [P]** `SelectorModoCarga.test.tsx` (3 tests): manual siempre habilitado, automático deshabilitado/habilitado según `disponibleAutomatico`.
- [x] **T051–T056** Los 6 componentes, todos pasando sus tests (37/37 en el frontend).
- [x] **T057** `DespostadoPage`: peso/precio/proveedor y las 4 tarjetas de resumen siempre visibles; `SelectorModoCarga` tapa el resto de la pantalla hasta elegir modo (automático precarga `kgPorCorte` desde `useEstimacion`); después, barra + mapa + tabla + pérdidas + "Restablecer ejemplo"/"Cargar entrada". "Restablecer ejemplo" fija `modo = 'manual'` y los valores de la sección 2.2 (peso 100, precio 5.200, kg por PLU, pérdidas 11/6/2) — matchea los cortes por `plu`, no por id (el id es distinto en cada entorno).
- [x] **T058** Checklist de accesibilidad — encontré y corregí 2 cosas en el camino:
  - **Nada depende solo del color (Principio VI):** la fila seleccionada de `TablaCortes` solo se distinguía por el fondo rosado. Se le agregó borde izquierdo grueso + negrita al nombre del corte, así se nota sin percibir el color.
  - **Responsive:** el SVG de `MapaCortes` se achicaba hasta volverse ilegible en pantallas angostas. Se envolvió en un contenedor con scroll horizontal propio (`overflow-x-auto` + ancho mínimo), consistente con "las tablas anchas se desplazan dentro de su caja, nunca la página entera" (sección 5.4).
  - El resto ya cumplía: `<label>` en todo campo (incluidos `sr-only` en los inputs de la tabla), `h-11` (44 px) en todos los botones/campos nuevos, los pares de color de éxito/aviso/error ya venían con contraste verificado desde la especificación (sección 5.1).

**Build y typecheck limpios, 37/37 tests de frontend.**

## Bloque 9 — Cierre de fase

- [ ] **T059** Ejecutar manualmente los 14 pasos de `../quickstart.md`; corregir cualquier desvío encontrado.
- [ ] **T060** Revisar `../spec.md` sección 9 (verificación contra la constitución) y confirmar que esta implementación no incorporó nada de Fase 2, 3 o 4.

---

## Dependencias entre bloques

```
Bloque 0 (setup)
  → Bloque 1 (migraciones)
    → Bloque 2 (JWT/RLS)
      → Bloque 3 (cortes) ─────────────────┐
      → Bloque 4 (modelo/ backend, puro)   │
          → Bloque 5 (persistencia/API, usa Bloque 4) ─┤
                                                         → Bloque 8 (UI, usa Bloque 6 y 7)
Bloque 6 (modelo/ frontend, sin dependencia de backend) ┘
Bloque 7 (hooks, apunta a la API real de Bloques 3/5)
Bloque 8 → Bloque 9 (cierre)
```

El Bloque 4 (modelo/ backend) no toca Spring ni la base: puede arrancar apenas termina el Bloque 0, en paralelo con los Bloques 1–3. El Bloque 6 (modelo/ frontend) tampoco depende de nada del backend y puede avanzar en paralelo desde el día uno.

## Ejemplo de ejecución en paralelo

Dentro del Bloque 8, lanzar juntos (archivos de test distintos, sin dependencia entre ellos):

```
T045, T046, T047, T048, T049, T050
```

Y luego, una vez que cada test falla como se espera, sus implementaciones correspondientes (T051–T056) también son independientes entre sí.
