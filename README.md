# Carnicería — Despostado y control diario

Aplicación web interna para una carnicería argentina: registra el despostado de medias reses, calcula el rendimiento y el costo real por kilo vendible, y lleva el control diario de ventas y stock escaneando etiquetas EAN-13 de balanza. La usan el dueño y empleados de mostrador.

Documentos de referencia:
- [`especificacion-carniceria.md`](especificacion-carniceria.md) — producto, diseño y modelo de datos.
- [`constitution.md`](constitution.md) — principios que rigen el desarrollo y el stack decidido.
- [`specs/001-sistema-carniceria/spec.md`](specs/001-sistema-carniceria/spec.md) — spec funcional de las 4 fases (historias de usuario, requisitos funcionales, casos borde).
- [`specs/001-sistema-carniceria/quickstart.md`](specs/001-sistema-carniceria/quickstart.md) — guión de verificación manual, continuo a través de las fases.
- `specs/001-sistema-carniceria/fase{1,2,3}/` — plan, modelo de datos, contrato de API y tareas de cada fase ya planificada o implementada.

## Stack tecnológico

| Capa | Tecnología |
|---|---|
| Backend | Java 21 + Spring Boot 4.1.x (Web, Data JPA, Validation, OAuth2 Resource Server), Maven |
| Frontend | React 19 + TypeScript, Vite, Tailwind CSS v4, TanStack Query |
| Base de datos | Supabase (Postgres, Auth, Realtime), región Canada Central — Row Level Security activado en todas las tablas |
| Migraciones | Flyway |
| Testing backend | JUnit 5 + Mockito |
| Testing frontend | Vitest + React Testing Library |
| Hosting | Frontend en Vercel/Netlify, backend en Railway |

Arquitectura: el frontend nunca accede a Supabase directamente (salvo el login contra Supabase Auth con la clave anónima). Todo lo demás pasa por la API propia en Spring Boot, que valida el JWT y lo propaga a Postgres para que las políticas de RLS se apliquen por usuario real. Cada feature del backend se organiza en capas (`controller` → `service` → `modelo` → `repository` → `entity`, con `dto` para los contratos de la API) — ver [`specs/001-sistema-carniceria/fase1/research.md`](specs/001-sistema-carniceria/fase1/research.md) para el detalle.

## Requisitos previos

- Java 21
- Node.js 20+ y npm
- Un proyecto de Supabase (Postgres, región Canada Central) con Auth configurado

## Variables de entorno

Copiar `.env.example` como referencia y completar:
- `backend/.env` — credenciales de conexión a Postgres y el JWKS de Supabase Auth.
- `frontend/.env` — URL y clave anónima de Supabase, y la URL base de la API propia.

Ninguno de los dos se versiona (están en `.gitignore`). `frontend/.env` lo lee Vite automáticamente. `backend/.env` no lo lee Spring Boot solo: hay que exportarlo al entorno antes de levantar el backend (ver paso 1 más abajo).

## Cómo levantar el proyecto

### 1. Backend (aplica las migraciones de Flyway automáticamente al arrancar)

```bash
cd backend
set -a; source .env; set +a
./mvnw spring-boot:run
```

Queda escuchando en `http://localhost:8080` (configurable con `SERVER_PORT` en `backend/.env`).

### 2. Frontend

```bash
cd frontend
npm install
npm run dev
```

Queda escuchando en `http://localhost:5173`.

## Tests

```bash
# Backend
cd backend && ./mvnw test

# Frontend
cd frontend && npm run test
```

## Estado actual

- **Fase 1 (Despostado)** y **Fase 2 (Escaneo y stock)** implementadas — ver [`fase1/tasks.md`](specs/001-sistema-carniceria/fase1/tasks.md) y [`fase2/tasks-fase2.md`](specs/001-sistema-carniceria/fase2/tasks-fase2.md) para el detalle y su progreso. Pendiente de esta última: la verificación manual de `quickstart.md` (T115).
- **Fase 3 (Sin conexión y anulación)** planificada, implementación no empezada — ver [`fase3/tasks-fase3.md`](specs/001-sistema-carniceria/fase3/tasks-fase3.md).
- **Fase 4 (Reportes)** sin planificar todavía.
