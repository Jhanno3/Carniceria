# Quickstart — Fase 1: Despostado

Guión de verificación manual. Reproduce las historias de usuario US-1.1 a US-1.5 de `spec.md` y el ejemplo numérico de la sección 6 de la especificación (obligatorio como test automatizado también, ver Principio III).

## Preparación

1. Variables de entorno (`.env`, no versionado): credenciales de conexión a Supabase Postgres (región Canada Central), clave anónima de Supabase, URL del proyecto Supabase.
2. `cd backend && mvn flyway:migrate` — aplica las migraciones de esta fase (`perfiles`, `cortes` + seed, `medias_reses`, `despostado`, `perdidas`, políticas RLS).
3. Crear a mano, en `auth.users` (Supabase Auth) y en `perfiles`, un usuario con `rol = 'dueno'`.
4. `cd backend && mvn spring-boot:run`
5. `cd frontend && npm install && npm run dev`

## Verificación

1. Iniciar sesión con el usuario `dueno`.
2. Ir a **Despostado** → "Cargar entrada nueva": peso `100` kg, precio de compra `5200` $/kg, sin proveedor.
   - **Esperado:** la tarjeta de costo muestra "—" y "Cargá el precio de compra" *antes* de tipear el precio; después de tipearlo, pasa a calcular. El botón de carga **automática** aparece deshabilitado (todavía no hay ninguna entrada cargada).
3. Elegir modo **manual** y entrar a la tabla de cortes.
   - **Esperado:** todos los campos de kilos arrancan vacíos.
4. Click "Restablecer ejemplo".
   - **Esperado:** se cargan los 21 cortes de la sección 2.2 con sus kg de ejemplo, y hueso `11,0`, grasa `6,0`, merma `2,0`.
5. Revisar las 4 tarjetas de resumen.
   - **Esperado:** Carne vendible `81,0 kg` (81 %); Pérdida `19,0 kg` (19 %); Costo real por kg vendible `$ 6.420` (`100 kg × $5.200 = $520.000`; `$520.000 / 81 kg = $6.420`).
6. Revisar el mensaje de control de la barra de composición.
   - **Esperado:** verde, "Cuadra con el peso de entrada" (la entrada de ejemplo cierra exacta).
7. Cambiar el kg del corte "Asado" de `11,0` a `9,0`.
   - **Esperado:** el mensaje pasa a naranja, "Faltan asignar 2,0 kg"; todas las tarjetas y el mapa se recalculan sin recargar la página. No se bloquea seguir adelante.
8. Revisar el mapa de cortes.
   - **Esperado:** antes del cambio del paso 7, la zona "asado" es la más oscura (11 kg es el valor más alto). Las zonas sin corte cargado quedan en el extremo claro.
9. Seleccionar "Vacío" en la tabla de cortes.
   - **Esperado:** la fila se resalta en rosado suave, la zona "vacio" del mapa se marca con borde negro grueso, y debajo del mapa aparece "Vacío · 3,3 kg · 3,3 % de la media res".
10. Click "Cargar entrada" y recargar la página.
    - **Esperado:** los valores cargados (incluido el cambio del paso 7) persisten tal como se guardaron; ahora existe una entrada en la base.
11. Repetir el paso 2 con una entrada nueva (peso `90` kg).
    - **Esperado:** el modo **automático** ya está disponible. Al elegirlo, la tabla de cortes se precarga con el kilaje estimado a partir del % histórico de la única entrada guardada hasta ahora, escalado a 90 kg (ej. Asado: `9,0 / 100 × 90 ≈ 8,1` kg).
12. Corregir a mano un corte de los precargados en el paso 11 y cargar la entrada.
    - **Esperado:** se guarda igual que cualquier despostado manual; no queda ningún rastro de que ese corte vino de una estimación.
13. Intentar tipear `-1` en el kg de un corte.
    - **Esperado:** el campo rechaza el valor o el guardado devuelve `400 CORTE_KG_INVALIDO`; nunca se persiste un kg ≤ 0.
14. Cerrar sesión, iniciar sesión como un usuario con `rol = 'empleado'` (si ya existe) e intentar abrir **Despostado**.
    - **Esperado:** sin acceso a la pantalla, o la pantalla no devuelve ningún dato (RLS bloquea `medias_reses`/`despostado`/`perdidas` para `empleado`).

## Correspondencia con tests automatizados

Los pasos 5, 6, 10, 11 y 12 deben existir también como tests automatizados (no solo manuales):
- Test de las funciones puras de cálculo, con el ejemplo de 100 kg / $5.200 → $6.420 (Principio III).
- Test de integración de `POST /medias-reses` que verifica persistencia atómica y, con un usuario `empleado`, verifica que la RLS bloquea la lectura.
- Test de integración de `GET /medias-reses/estimacion` que verifica el promedio histórico y el `409 SIN_HISTORIAL` cuando no hay ninguna entrada cargada todavía.
