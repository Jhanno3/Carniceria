# Plan de implementación: Fase 1 — Despostado

**Spec de origen:** `spec.md`, sección 2
**Constitución:** `constitution.md` v1.0.0
**Fecha:** 2026-10-02
**Estado:** Listo para implementar — `tasks.md` generado

## 1. Resumen

Esta fase construye: alta de cortes (con PLU), alta y edición de medias reses, carga de kilos por corte y por tipo de pérdida, los cálculos en vivo de rendimiento y costo, el mapa SVG de la media res y el guardado atómico del despostado. No incluye escaneo, ventas, stock ni roles de empleado operando la pantalla (eso es Fase 2 y 3) — solo lo necesario para que RLS y roles existan desde el día uno, como exige la constitución.

## 2. Contexto técnico

| Punto | Valor |
|---|---|
| Lenguaje backend | Java 21 |
| Framework backend | Spring Boot 4.1.x (Web, Data JPA, Validation, OAuth2 Resource Server) |
| Build backend | Maven (ver `research.md`) |
| Lenguaje frontend | TypeScript + React 18 |
| Build frontend | Vite, npm (ver `research.md`) |
| Estilos | Tailwind CSS |
| Estado del servidor (frontend) | TanStack Query |
| Base de datos | Supabase Postgres, región Canada Central (ver `constitution.md` — desvío del São Paulo original, decisión del dueño) |
| Acceso a datos | Spring Data JPA + Hibernate, con propagación de JWT a RLS (ver `research.md`) |
| Migraciones | Flyway |
| Testing backend | JUnit 5 + Mockito |
| Testing frontend | Vitest + React Testing Library |
| Plataforma | Web responsive (celular y compu) |
| Tipo de proyecto | Monorepo, `/backend` + `/frontend` |
| Alcance de datos | `perfiles`, `cortes`, `medias_reses`, `despostado`, `perdidas` únicamente |

## 3. Chequeo contra la constitución

| Principio | Cumplimiento en este plan |
|---|---|
| I. Integridad de los datos | `numeric`/`BigDecimal` en toda columna de kg o $ (sección 2 de `data-model.md`); nada de `vendible_kg`/`costo_*` se persiste, se derivan (sección "Valores calculados"). El guardado atómico (`POST /medias-reses`) evita cualquier estado intermedio inconsistente en la base. No hay ventas en esta fase, así que la regla de anulación/idempotencia de escaneos no aplica todavía. |
| II. Seguridad | RLS activado en las 5 tablas desde la primera migración, incluida `perfiles` (necesaria solo para que RLS sepa el rol); solo `dueno` puede tocar `medias_reses`/`despostado`/`perdidas`; el JWT se propaga a Postgres en vez de usar `service_role` (`research.md`). |
| III. Cálculos confiables | Las fórmulas de la sección 6 de la especificación se implementan dos veces como funciones/clases puras (TypeScript en el frontend, Java sin anotaciones de framework en la capa `modelo/` del backend), cada una con tests que reproducen el ejemplo de 100 kg / $5.200 → $6.420 (`quickstart.md`, paso 5). |
| IV. El mostrador no se detiene | No aplica en esta fase (no hay escaneo); se retoma en el plan de Fase 2. |
| V. Hecho para Argentina | Todo kg/$ mostrado pasa por el formateador es-AR; campos aceptan coma o punto (el contrato de `POST /medias-reses` recibe `numeric` como string, no `number`, para no perder precisión ni formato). |
| VI. Usabilidad y accesibilidad | Componentes de esta fase (tarjetas, tabla, mapa) siguen los tokens de `especificacion-carniceria.md` sección 5; sin checklist adicional en este plan, se verifica al construir cada componente. |
| VII. Simplicidad | No se construye nada de Fase 2+ (ni políticas RLS de `empleado` sobre `despostado`) hasta que la fase correspondiente lo necesite. No hay columna de estado ni de modo de carga: el modo manual/automático es solo una pantalla previa al guardado atómico (`data-model.md`). Única excepción documentada: las fórmulas de cálculo sí se duplican (frontend y backend) a pedido explícito del dueño, por la arquitectura en capas — ver "Complejidad justificada" más abajo. |
| VIII. Cambios controlados | Toda tabla de esta fase nace en una migración Flyway versionada; credenciales de Supabase y JWT solo en variables de entorno del backend. |

**Complejidad justificada:** el Principio VII prefiere una sola implementación por regla. Esta fase se desvía en un punto puntual: las fórmulas de cálculo viven en el frontend (TypeScript) **y** en el backend (Java, capa `modelo/`), cada una con sus propios tests. Es una decisión explícita del dueño (no una conveniencia de implementación) para sostener la arquitectura en capas que pidió — Controller → Service → Model → Repository → Entity, con el Model como dueño de las reglas de negocio y cálculos en el propio backend — pensando en que el servidor sea la fuente de verdad para reportes y otros clientes futuros. El riesgo (que las dos implementaciones se desincronicen) se mitiga testeando ambas contra los mismos casos numéricos de la especificación.

## 4. Estructura del proyecto

```
specs/001-sistema-carniceria/
  spec.md
  plan.md            ← este archivo
  research.md
  data-model.md
  contracts/
    despostado-api.md
  quickstart.md

backend/
  pom.xml
  src/main/java/com/carniceria/
    cortes/
      controller/CorteController.java
      service/CorteService.java
      entity/CorteEntity.java
      dto/CorteRequest.java, CorteResponse.java
      repository/CorteRepository.java
      (sin modelo/ en esta fase: no hay cálculo propio, solo restricciones de base)
    despostado/              (incluye medias_reses y perdidas: es un solo agregado, se guarda junto)
      controller/MediaResController.java
      service/MediaResService.java
      modelo/ResumenDespostado.java, EstimacionCalculator.java   (dominio puro, sin Spring/JPA, con sus tests)
      entity/MediaResEntity.java, DespostadoEntity.java, PerdidaEntity.java
      dto/CargarEntradaRequest.java, MediaResResponse.java, EstimacionResponse.java, ResumenDto.java
      repository/MediaResRepository.java, DespostadoRepository.java, PerdidaRepository.java
    perfiles/
      entity/PerfilEntity.java
      repository/PerfilRepository.java   (sin endpoint propio todavía — solo lo necesita RLS)
    shared/
      security/              (JwtClaimsContextFilter, RlsSessionAspect — propagación a RLS)
      error/                 (GlobalExceptionHandler, ErrorResponse { error, mensaje })
  src/main/resources/
    db/migration/
      V1__perfiles.sql
      V2__cortes.sql
      V3__seed_cortes.sql
      V4__medias_reses.sql
      V5__despostado.sql
      V6__perdidas.sql
    application.yml
  src/test/java/com/carniceria/...   (mismo árbol de paquetes; los tests de modelo/ no usan @SpringBootTest)

frontend/
  package.json
  vite.config.ts
  (Tailwind v4: sin tailwind.config.js — tokens vía @theme en src/index.css)
  src/
    features/despostado/
      modelo/             (funciones puras + tests: ResumenDespostado, colorZonaMapa — espejo del modelo/ backend)
      components/          (TarjetasResumen, BarraComposicion, MapaCortes, TablaCortes, TarjetasPerdida, SelectorModoCarga)
      api/                  (types.ts con los DTO de despostado-api.md + hooks de TanStack Query)
      DespostadoPage.tsx
    shared/
      formato/              (es-AR: kg, $, parseo coma/punto)
      ui/                    (botones, tarjetas, campos — tokens de la especificación)
```

## 5. Fase 0 — Investigación

Resuelta en `research.md`: arquitectura en capas del backend y del frontend, build tools, propagación de JWT a RLS, escalas de `BigDecimal`, color de zona en el mapa, y dónde viven las fórmulas de cálculo (ahora en ambas capas `modelo/`, frontend y backend). Ningún punto queda abierto para empezar a codear esta fase.

## 6. Fase 1 — Diseño

- **Datos:** `data-model.md` — 5 tablas, con sus RLS y el seed de los 21 cortes de la sección 2.2 de la especificación.
- **API:** `contracts/despostado-api.md` — CRUD de cortes, el endpoint de solo lectura `GET /medias-reses/estimacion` (promedio histórico para la carga automática) y `POST /medias-reses` como guardado atómico único (media res + cortes + pérdidas juntos).
- **Verificación:** `quickstart.md` — 11 pasos manuales que cubren las 4 historias de usuario de esta fase y el ejemplo numérico obligatorio.

## 7. Fase 2 — Enfoque para generar tareas (no se ejecuta en este documento)

Cuando se pida `tasks.md`, se derivará de este plan en orden TDD:
1. Migraciones Flyway (una tarea por tabla, en el orden de `data-model.md`) + seed de cortes.
2. Propagación de JWT a RLS (test que falla sin ella, luego el filtro/aspecto que la hace pasar).
3. Capa `modelo/` del backend (tests de `ResumenDespostado`/`EstimacionCalculator`, sin Spring, deben fallar primero) → implementación.
4. Tests de integración de cada endpoint de `contracts/despostado-api.md` (incluido el rechazo por RLS a un usuario `empleado`) → entity/repository/service/controller de cada feature.
5. Capa `modelo/` del frontend (mismos casos numéricos que el paso 3) → implementación.
6. Hooks de datos (`api/`) y componentes de UI (tarjetas, barra, mapa, tabla, selector de modo) con sus tests → ensamblado en `DespostadoPage`.
7. Ejecución manual de `quickstart.md` como cierre de la fase.

## 8. Seguimiento de progreso

- [x] Spec revisada (`spec.md`, sección 2)
- [x] Chequeo contra la constitución (sección 3 de este documento)
- [x] `research.md`
- [x] `data-model.md`
- [x] `contracts/despostado-api.md`
- [x] `quickstart.md`
- [x] `tasks.md`
