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

---

# Quickstart — Fase 2: Escaneo y stock

Continúa la numeración de la fase anterior. Reproduce US-2.1 a US-2.3 de `spec.md` y el
contrato de `fase2/contracts/control-diario-api.md`. Requiere haber hecho antes los pasos 1 a 14
(ya existe al menos una media res cargada, si no "Vacío" no tiene stock que mostrar).

Los códigos de ejemplo usan la configuración de etiqueta por defecto (prefijo 20-29, PLU en
las posiciones 2-6, peso en las posiciones 7-11 con 3 decimales) y el PLU 12 = Vacío
sembrado por `CatalogoInicialService` — los mismos valores que usan
`VentaControllerTest`/`StockControllerTest`.

## Verificación

15. Ir a **Control diario** y escanear (tipear + Enter, simulando el lector) el código `2000012012501`.
    - **Esperado:** aparece en "Último escaneo" como "Vacío · 1,250 kg", con fecha/hora actual; el resumen del día suma 1 a "Etiquetas escaneadas hoy" y 1,250 kg a "Kg vendidos hoy".
16. Escanear de nuevo el **mismo** evento (mismo `idClienteLocal` — en la práctica, doble "Enter" del lector sobre la misma lectura).
    - **Esperado:** no se duplica la venta ni se descuenta stock dos veces; el resumen no cambia.
17. Escanear un código con dígito verificador adulterado, ej. `2000012012509`.
    - **Esperado:** error visible "código inválido" (`DIGITO_VERIFICADOR_INVALIDO`), nada se persiste, el resumen no cambia.
18. Escanear un código con PLU inexistente, ej. `2000099010001`.
    - **Esperado:** error `PLU_INEXISTENTE`, nada se persiste.
19. Ir a la tabla de **Stock** y ubicar "Vacío".
    - **Esperado:** `vendidoKg` subió en 1,250 kg respecto de antes del paso 15; `stockKg = entradoKg - vendidoKg`. Si queda por debajo del 15 % de lo entrado, la fila se marca "queda poco".
20. Ir a **Ajustes** → configuración de etiqueta, y guardar un rango inconsistente (ej. que el largo de PLU se superponga con el inicio de valor).
    - **Esperado:** `400 CONFIGURACION_ETIQUETA_INVALIDA`, no se guarda; el formulario muestra el error.
21. Corregir y guardar una configuración válida (puede ser la misma por defecto).
    - **Esperado:** se guarda; un escaneo posterior con un código que respete esa configuración se sigue decodificando bien.
22. Cerrar sesión, iniciar sesión como `empleado` del mismo negocio y repetir el paso 15.
    - **Esperado:** el empleado puede escanear y ver stock/ventas del negocio, pero no ve `precio_kg` ni ningún costo en ninguna pantalla, y no tiene acceso a **Ajustes** (solo `dueno`, FR-209).

## Correspondencia con tests automatizados

Los pasos 15 a 20 ya están cubiertos por tests de integración existentes:
`VentaControllerTest` (escaneo válido/duplicado/4 tipos de error), `StockControllerTest`
(descuento de stock y umbral "queda poco"), `ResumenDiaControllerTest` (conteo del día) y
`ConfigEtiquetaControllerTest` (validación de rangos). El paso 22 lo cubre
`VentaControllerTest`/`StockControllerTest` en su variante con usuario `empleado`.
