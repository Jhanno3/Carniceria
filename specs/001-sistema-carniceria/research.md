# Investigación — Fase 1: Despostado

Resuelve los puntos técnicos que `plan.md` deja abiertos para esta fase. Ninguno es una decisión de negocio: son detalles de herramienta que no afectan a `constitution.md`.

## Arquitectura en capas (a pedido del dueño)

**Decisión:** el backend se organiza en capas explícitas dentro de cada feature (`cortes`, `despostado`, `perfiles`):

- **`controller/`** — `@RestController`. Solo HTTP: recibe/devuelve DTO, traduce excepciones de negocio a códigos de error (`contracts/despostado-api.md`). Sin lógica de negocio.
- **`dto/`** — clases planas de request/response, el contrato exacto de la API. No llevan anotaciones de JPA.
- **`service/`** — orquesta transacciones (`@Transactional`), convierte DTO ↔ Entity/Model, llama al Model para validar y calcular, llama al Repository para persistir.
- **`modelo/`** — clases de dominio puro, **sin ninguna anotación de Spring ni de JPA**, testeables con JUnit simple (sin levantar contexto de Spring). Acá viven las reglas de negocio y las fórmulas de cálculo (`ResumenDespostado`, `EstimacionCalculator`). Solo existe en los features donde hay cálculo o regla real — `cortes` no tiene `modelo/` en esta fase porque no calcula nada (sus reglas son restricciones de base: PLU único, cuarto válido).
- **`entity/`** — clases JPA, solo persistencia (`CorteEntity`, `MediaResEntity`, `DespostadoEntity`, `PerdidaEntity`, `PerfilEntity`). No llevan métodos de negocio.
- **`repository/`** — interfaces de Spring Data JPA.

El frontend refleja la misma idea: la carpeta `calculos/` de cada feature se renombra a `modelo/`, para usar el mismo nombre de capa en los dos lenguajes.

**Por qué:** así lo pidió el dueño explícitamente, pensando en que el código escale (más features, más reportes) sin que la lógica de negocio quede mezclada con anotaciones de framework o con el mapeo HTTP. El costo es más clases por feature que un CRUD mínimo — aceptado a cambio de esa escalabilidad.

## Región real de Supabase (hallazgo al cargar las credenciales)

**Hallazgo:** el proyecto de Supabase que el dueño ya había creado está en **Canada Central** (`ca-central-1`), no en São Paulo como pedía `especificacion-carniceria.md` originalmente. Se le preguntó al dueño y decidió seguir con este proyecto en vez de crear uno nuevo en São Paulo — ver `constitution.md`, sección de restricciones técnicas, para la decisión y su motivo.

**Cadena de conexión confirmada (probada con `psql`):** Supabase no expone más el host directo `db.<project-ref>.supabase.co` para este proyecto; la conexión funciona vía el *pooler* (Supavisor) de la región:

```
host:     aws-0-ca-central-1.pooler.supabase.com
puerto:   5432
usuario:  postgres.<project-ref>
base:     postgres
sslmode:  require
```

`postgres.<project-ref>` es el usuario de **migraciones** (dueño de las tablas, con DDL). El backend en runtime usa un rol distinto, más restringido — ver la siguiente sección.

## RLS puede dejar que un UPDATE "tenga éxito" sin cambiar nada (hallazgo de seguridad)

**Problema encontrado escribiendo el test de `PUT /perfiles/{id}`:** `CorteService.actualizar` y `PerfilService.actualizar` hacían `repository.findById(id)` (SELECT, permitido por una política de solo lectura) y después mutaban la entidad con `setters` + `save()`. Cuando quien llama puede **leer** la fila pero no tiene ninguna política que le permita **escribirla**, Postgres/RLS no tira ningún error: como `PerfilEntity`/`CorteEntity` no tienen `@Version`, Hibernate no verifica cuántas filas tocó el `UPDATE` — el endpoint devolvía `200 OK` con los valores "nuevos" (que en realidad eran solo el objeto Java en memoria, nunca escrito) mientras la base se quedaba exactamente igual. Un empleado podía pedir `PUT /cortes/{id}` sobre un corte activo (que sí puede leer) y recibir una respuesta de éxito que no cambiaba nada — ni un error ni el dato real, lo peor de los dos mundos.

**Decisión:** los métodos `actualizar(...)` que pueden ser llamados por alguien que solo tiene permiso de lectura (no de escritura) sobre esa fila usan un `UPDATE` explícito vía `@Modifying @Query(...)` que devuelve la cantidad de filas tocadas, en vez de mutar la entidad y confiar en el flush automático de Hibernate. Si da `0`, el servicio comprueba con `existsById(...)` si la fila existe (si no, `404`) y si existe, `403 ACCESO_DENEGADO` (nueva excepción en `shared/error/`) — ahí sí se sabe que RLS la bloqueó de verdad.

**Segundo hallazgo, encadenado al primero:** al agregar el `@Modifying`, dos tests seguían fallando — devolvían el valor **viejo** después de un `UPDATE` que sí había funcionado en la base. Causa: una consulta anterior en la misma transacción (el chequeo de PLU duplicado, o un listado) ya había cargado esa misma fila en la sesión de Hibernate; un `UPDATE` JPQL va directo a la base y no actualiza esa copia en memoria, así que el `findById` posterior devolvía la copia vieja cacheada. Se resuelve con `@Modifying(clearAutomatically = true)`, que limpia la sesión después del `UPDATE`.

**Por qué importa:** es un ejemplo concreto de por qué el Principio I exige que ninguna operación "mienta" sobre si persistió. Vale para cualquier `actualizar(...)` futuro donde el que escribe no sea necesariamente el dueño de la fila.

## Build tools

**Decisión:** Maven (backend), npm (frontend).
**Por qué:** son los valores por defecto de Spring Initializr y de las plantillas de Vite; no hay ninguna razón propia del proyecto para desviarse, y sumar una herramienta distinta (Gradle, pnpm) sería una dependencia más sin beneficio concreto (Principio VII).

## Versión real de Spring Boot (hallazgo al generar el proyecto)

**Hallazgo:** al generar el backend, Spring Initializr ya no ofrece ninguna versión 3.x — solo 4.0.x y 4.1.x. `constitution.md` decía "Spring Boot 3.x"; se le preguntó al dueño y eligió actualizar a **Spring Boot 4.1.1** (ver `constitution.md`, actualizado). Como consecuencia, los starters cambiaron de nombre (p. ej. `spring-boot-starter-webmvc` en vez de `spring-boot-starter-web`, `spring-boot-starter-security-oauth2-resource-server` en vez de `spring-boot-starter-oauth2-resource-server`, y cada starter tiene su propio artefacto `-test`). El `pom.xml` generado usa estos nombres nuevos.

## Jackson 3 en Spring Boot 4 (hallazgo al implementar los DTO de despostado)

**Hallazgo:** se había planeado un `@Bean ObjectMapper` (Jackson 2, `com.fasterxml.jackson.databind`) con un serializador custom para que `BigDecimal` viaje como string JSON (contracts/despostado-api.md). Al probarlo contra un endpoint real, el campo seguía viajando como número (`"pesoKg":100.000` en vez de `"pesoKg":"100.000"`). La causa: Spring Boot 4 usa por dentro **Jackson 3** (`tools.jackson.*`, nuevo groupId del proyecto Jackson) a través de `spring-boot-starter-jackson`, con un mecanismo de configuración basado en *builder customizers* — un bean de `ObjectMapper` de Jackson 2 simplemente no es el objeto que Spring MVC usa para serializar.

**Decisión:** en vez de aprender/pelear contra la configuración nueva de Jackson 3 (todavía muy reciente), los campos de kilos e importes en los DTO del backend son **`String`**, no `BigDecimal`. La conversión a/desde `BigDecimal` se hace a mano, explícita, con `shared/BigDecimals.java` (`parse`/`aTexto`). Es más verboso que un serializador global, pero no depende de qué versión de Jackson esté activa por debajo, y el `Model` (`ResumenDespostado`, capa `modelo/`) sigue trabajando 100 % en `BigDecimal` — la conversión a texto es solo en el borde (DTO).

## Tailwind CSS v4 (hallazgo al instalar)

**Hallazgo:** `npm install tailwindcss` instaló la v4, que no usa `tailwind.config.js`: el plugin `@tailwindcss/vite` se registra en `vite.config.ts` y los tokens de diseño (colores, tipografías, tamaños) se declaran con la directiva `@theme` directo en `frontend/src/index.css`. No es una decisión de producto, es cómo funciona la herramienta actual — se ajustó `plan.md` para reflejarlo.

## Rol de base de datos del backend — hallazgo crítico de seguridad

**Problema encontrado probando la conexión (con `psql`, antes de escribir código):** `postgres` (el rol que usamos para las migraciones) tiene el atributo `rolbypassrls = true` en Supabase. Esto significa que **ninguna política de RLS se aplica jamás** mientras una conexión use ese rol — sin importar qué JWT propaguemos ni qué `SET LOCAL` ejecutemos. Si el backend en runtime hubiera seguido usando las credenciales de `postgres` (como se planeaba originalmente), el mecanismo de RLS completo habría sido cosmético: cualquier bug, excepción silenciada, o método `@Transactional` nuevo que el aspecto no intercepte correctamente, habría expuesto **todos los datos de todos los usuarios** (fail-open) en vez de bloquear (fail-closed).

**Decisión:** se creó un rol de Postgres separado, `app_backend`, sin `BYPASSRLS`, sin ser dueño de ninguna tabla, con permisos `SELECT/INSERT/UPDATE/DELETE` otorgados explícitamente sobre las 5 tablas de esta fase (más una regla de `ALTER DEFAULT PRIVILEGES` para que las tablas de fases futuras hereden el mismo permiso), y miembro del rol `authenticated` de Supabase (necesario para poder ejecutar `auth.uid()`, que vive en el schema `auth`, cuyo uso no es público). Es el usuario que usa el backend en runtime (`SUPABASE_DB_USERNAME`/`SUPABASE_DB_PASSWORD`).

**Las migraciones siguen corriendo con `postgres.<project-ref>`** (necesita DDL: `CREATE TABLE`, etc.), configurado por separado en Spring Boot (`spring.flyway.url/user/password`), distinto del datasource de runtime (`spring.datasource.*`). Así, el rol con más privilegios (`postgres`) solo se usa en el momento controlado de aplicar una migración, nunca para atender pedidos de usuarios.

**Verificado de punta a punta con `psql`** (sin escribir código Java todavía, para no dar por sentado que el diseño funciona):
1. Conectado como `app_backend`, **sin** `request.jwt.claims` seteado: `select count(*) from medias_reses` → `0` filas, `is_dueno()` → `false`. Bloqueo confirmado (fail-closed).
2. Mismo rol, con `select set_config('request.jwt.claims', '{"sub":"<uuid-de-un-dueño-real>"}', true)`: `auth.uid()` resuelve el UUID, `is_dueno()` → `true`, un `INSERT` en `medias_reses` funciona y la fila se ve. Acceso confirmado.

**Mecanismo de propagación (sin cambios en la idea, más simple de lo planeado):** un filtro de Spring Security (`JwtClaimsContextFilter`, `OncePerRequestFilter`), ejecutado después de validar el JWT como resource server, guarda `sub`/`role`/`email` del token en un holder de la petición (`JwtClaimsHolder`). Un `@Aspect` (`RlsSessionAspect`) con `@Before` sobre los métodos `@Transactional`, ordenado para ejecutar **dentro** de la transacción ya abierta (`@EnableTransactionManagement(order = HIGHEST_PRECEDENCE)` en `BackendApplication` + `@Order(HIGHEST_PRECEDENCE + 10)` en el aspecto), ejecuta:

```sql
select set_config('request.jwt.claims', '<claims-json-del-token>', true);
```

**Ajuste sobre el plan original:** no hace falta `SET LOCAL role = 'authenticated'`. Nuestras políticas RLS no restringen `TO authenticated`, evalúan `is_dueno()` directamente — y como `app_backend` ya no tiene `BYPASSRLS`, alcanza con el `set_config` de las claims. Un paso menos (Principio VII).

**Alternativa descartada:** usar la clave `service_role` desde el backend y reimplementar la autorización por rol en Java. Se descarta porque RLS dejaría de ser la autoridad real sobre los datos, violando el Principio II de la constitución — y el hallazgo de arriba es, justamente, un ejemplo concreto de por qué ese principio importa.

## Escalas y redondeo de `BigDecimal`

**Decisión:** kilos con `scale = 3`, `RoundingMode.HALF_UP`. Los importes **guardados** (`precio_kg` en la base) son `numeric(12,2)`, tal como define `especificacion-carniceria.md` sección 8.1. Los importes **calculados y mostrados** (`costo_total`, `costo_kg_vendible`) se redondean a **0 decimales** (pesos enteros, sin centavos) — no a 2.

**Hallazgo al escribir el test obligatorio (Principio III):** con el ejemplo exacto de la especificación (100 kg, $5.200/kg, 81 kg vendibles), `520.000 / 81 = 6419,753...`, que redondeado a 2 decimales da `$6.419,75`, **no** `$6.420` como dice la sección 6. Redondeando a 0 decimales sí da `$6.420` exacto. Esto coincide con que *ningún* ejemplo de moneda en toda la especificación muestra centavos (`$ 6.420`, `$ 520.000`, sección 5.4) — los pesos argentinos de este negocio siempre se muestran enteros. Se corrigió `ResumenDespostado` para redondear `costoTotal()`/`costoKgVendible()` a 0 decimales; `rendimiento_%` sigue en 2 decimales (ningún ejemplo lo contradice). `costo_kg_vendible` igual se calcula con una escala intermedia mayor (`MathContext` de al menos 10 dígitos) antes de ese redondeo final, para no perder precisión en la división.

## Color de zona en el mapa SVG

**Decisión:** interpolación lineal en el espacio RGB entre `rgb(246,228,224)` y `rgb(110,20,20)`, con `t = kg_zona / kg_zona_más_pesada` (si no hay ninguna zona con kilos cargados, todas quedan en el extremo claro). Función pura en TypeScript, con tests para `t = 0`, `t = 0.5`, `t = 1`.

## Dónde viven las fórmulas de cálculo (Principio III)

**Decisión (revisada a pedido del dueño):** las fórmulas de la sección 6 de la especificación (`vendible_kg`, `perdida_kg`, `sin_asignar_kg`, `rendimiento_%`, `costo_total`, `costo_kg_vendible`) se implementan **dos veces**, cada una con sus propios tests que reproducen el ejemplo de 100 kg / $5.200 → $6.420:

- **Frontend (TypeScript, capa `modelo/`):** alimenta la vista en vivo mientras el dueño edita, antes de guardar — todavía no hay nada para pedirle al servidor.
- **Backend (Java, capa `modelo/`, clases sin anotaciones de Spring/JPA):** es la fuente de verdad una vez que los datos están guardados. El backend expone el resumen calculado (`vendibleKg`, `perdidaKg`, `sinAsignarKg`, `rendimientoPorc`, `costoTotal`, `costoKgVendible`) en las respuestas de `GET /medias-reses/{id}` y `POST /medias-reses`, en vez de obligar a cada cliente (web, futuros reportes, un eventual cliente mobile) a recalcularlo.

**Por qué el cambio:** la decisión original (una sola implementación, solo frontend) priorizaba simplicidad, pero el dueño pidió explícitamente una arquitectura por capas con un `Model` de dominio en el backend dedicado a reglas de negocio y cálculos — pensando en que crezca (reportes de Fase 4, otros clientes). Se acepta el costo de mantener dos implementaciones sincronizadas.

**Cómo se evita que se desincronicen:** ambas implementaciones se testean contra los mismos casos numéricos de la especificación (sección 6 y el ejemplo de 100 kg). Si una de las dos cambia sin el mismo resultado, su test correspondiente falla.

**Lo que no cambia:** el guardado (`POST /medias-reses`) sigue sin bloquearse si `sinAsignarKg ≠ 0` — es informativo, no una validación dura (spec, sección 2.2, US-1.2).
