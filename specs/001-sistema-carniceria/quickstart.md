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

---

# Quickstart — Fase 3: Sin conexión y anulación

Continúa la numeración de Fase 2. Reproduce US-3.1 y US-3.4 de `spec.md` sección 4 y
`fase3/plan-fase3.md`. Requiere haber hecho antes los pasos 1 a 22 (al menos una venta de
"Vacío" ya cargada, para que el decodificador offline tenga algo cacheado — ver el paso 23).

## Verificación

23. Con conexión, entrar a **Control diario** (así se cachean `cortes` y `config_etiqueta`
    para el modo offline) y esperar a que cargue el resumen del día.
24. Abrir las DevTools del navegador → pestaña **Network** → elegir **Offline** (no alcanza
    con cortar el wifi de verdad si el navegador cachea respuestas; esto fuerza que todo
    `fetch` falle igual que sin conexión real).
25. Escanear el código válido `2000012012501`.
    - **Esperado:** aparece en "Último escaneo" como antes ("Vacío · 1,250 kg") pero con el
      título "Pendiente de sincronizar" en vez de "Descontado del stock"; el indicador de
      pendientes pasa de "Sin pendientes" a "1 venta pendiente de subir"; el campo de
      escaneo sigue con el foco (FR-201 también vale offline).
26. Escanear un código con dígito verificador adulterado, ej. `2000012012509`, todavía sin
    conexión.
    - **Esperado:** mismo error "código inválido" que online; no se encola nada, el
      indicador de pendientes no cambia.
27. Volver a poner la red en **Online** en DevTools.
    - **Esperado:** en menos de unos segundos (evento `online` del navegador) el indicador
      de pendientes vuelve a "Sin pendientes"; la venta pendiente del paso 25 ahora aparece
      en "Ventas de hoy" con su hora real y descontó stock (ver la tabla de Stock).
28. Como `dueno`, hacer click en "Anular" sobre cualquier venta de la lista (puede ser de
    hace rato).
    - **Esperado:** la venta pasa a mostrarse tachada con la etiqueta "Anulada", sin botón;
      el stock de ese corte vuelve a subir; "Kg vendidos hoy" baja en lo anulado pero
      "Etiquetas escaneadas hoy" no cambia (`plan-fase3.md` 3.8).
29. Cerrar sesión, iniciar sesión como `empleado` del mismo negocio, y escanear un código
    nuevo.
    - **Esperado:** aparece el botón "Anular" en esa venta (es propia y reciente). Click →
      se anula igual que en el paso 28.
30. Como el mismo `empleado`, buscar en la lista una venta de **otro usuario** (del dueño o
    de otro empleado) o una propia de hace más de 5 minutos.
    - **Esperado:** esas filas **no muestran** el botón "Anular" (no solo deshabilitado).

## Correspondencia con tests automatizados

Los pasos 25-30 ya están cubiertos por tests automatizados: `VentaControllerTest` (los 6
casos de anulación del Bloque 2 de `fase3/tasks-fase3.md`), `ResumenDiaControllerTest`
(paso 28), `decodificadorEtiqueta.test.ts`/`ean13.test.ts` (los mismos códigos de error del
paso 26), `colaVentas.test.ts` (la cola misma) y `VentasDeHoy.test.tsx` (cuándo se muestra
el botón, paso 29/30). Lo que el test automatizado no puede probar es la integración real
con DevTools → Network → Offline y el evento `online` del navegador — por eso siguen siendo
pasos manuales.

---

# Quickstart — Fase 6: Tipo de entrada ("Corte") en Despostado

Continúa la numeración de Fase 5. Reproduce `fase6/tasks-fase6.md` (FR-601/FR-602).

## Verificación

47. Como `dueno`, ir a **Despostado** y mirar el selector "Corte", a la derecha de
    "Categoría del animal".
    - **Esperado:** arranca en "Media res"; con ese valor, ningún corte de la tabla
      "Cortes vendibles" aparece atenuado (igual que antes de esta fase).
48. Cambiar el selector a "Pecho".
    - **Esperado:** en la tabla "Cortes vendibles", solo Paleta/Roast beef/Cogote/Falda
      quedan con su campo de kg habilitado; el resto de las filas se atenúa (gris) y su
      campo de kg no deja tipear. Las filas atenuadas se pueden igual tocar/seleccionar
      (se ve su zona en el mapa), solo no se puede cargar kg.
49. Con "Pecho" todavía elegido, cargar algo de kg en Paleta y guardar la entrada; recargar
    la página y volver a abrir esa misma entrada (o cargar una nueva y mirar el selector).
    - **Esperado:** "Pecho" sigue seleccionado — el tipo de entrada persiste.
50. Volver a "Media res".
    - **Esperado:** ya no queda ninguna fila atenuada.
51. Click "Restablecer ejemplo" (estando en cualquier otro tipo que no sea "Media res").
    - **Esperado:** el selector vuelve solo a "Media res" (el ejemplo de 100 kg reparte
      kilos en cortes de varios tipos a la vez, no tendría sentido mostrado atenuado).

## Correspondencia con tests automatizados

Los pasos 47-51 ya están cubiertos por tests automatizados: `MediaResControllerTest`
(persistencia de `tipoEntrada`, default `"MediaRes"`, `400 TIPO_ENTRADA_INVALIDO`),
`tiposDeEntrada.test.ts` (el mapeo puro de cada tipo a sus cortes), `TablaCortes.test.tsx`
(atenuado/deshabilitado de las filas fuera del tipo elegido, y que igual se pueden
seleccionar). Lo que el test automatizado no cubre es la experiencia visual real de ver la
tabla atenuarse en el navegador — por eso sigue siendo un paso manual.

---

# Quickstart — Fase 4: Reportes, listado completo de cuentas y pausar acceso

Continúa la numeración de Fase 3. Reproduce US-4.1/4.3/4.4 de `spec.md` sección 5 y
`fase4/plan-fase4.md`. Requiere haber hecho antes los pasos 1 a 30 (medias reses cargadas
en varias fechas/proveedores/categorías para que los reportes tengan algo que agrupar).

## Verificación

31. Como `dueno`, ir a **Reportes** → sección "Por proveedor", con el rango de fechas por
    defecto (últimos 30 días).
    - **Esperado:** una fila por proveedor cargado en ese rango, con cantidad de entradas,
      rendimiento promedio y costo por kg vendible; las entradas sin proveedor cargado
      aparecen agrupadas como "Sin proveedor" al final de la tabla, no excluidas.
32. Elegir un rango de fechas sin ninguna media res cargada (ej. el año pasado) y tocar
    "Aplicar", en cualquiera de las 3 secciones.
    - **Esperado:** mensaje neutro ("No hay datos para el rango elegido"), nunca un error
      ni una tabla vacía sin explicación (`spec.md` 5.3).
33. En "Por período", cambiar el selector de "Semana" a "Día" y a "Mes", sin tocar las
    fechas.
    - **Esperado:** la tabla se reagrupa (más filas con "Día", menos con "Mes", para el
      mismo rango) sin tener que tocar "Aplicar" de nuevo.
34. Como `empleado`, intentar entrar a **Reportes**.
    - **Esperado:** la pestaña ni siquiera aparece en la navegación (FR-305: reportes es
      solo para quien opera el negocio).
35. Como `admin`, entrar a **Usuarios** y mirar la tabla "Todas las cuentas".
    - **Esperado:** aparecen cuentas en cualquier estado (aprobadas, rechazadas,
      pendientes), no solo las pendientes de aprobación de la sección de arriba.
36. En esa misma tabla, hacer click en "Pausar" sobre una cuenta aprobada (de prueba, no la
    propia).
    - **Esperado:** la fila pasa a mostrar estado "pausado" y el botón cambia a
      "Reactivar". Si esa cuenta tiene una sesión abierta en otra pestaña/navegador, su
      próximo pedido al backend falla (sin esperar a que expire su sesión de Supabase).
37. Iniciar sesión con la cuenta pausada del paso 36 (o recargar su pestaña si seguía
    abierta).
    - **Esperado:** mensaje "Tu cuenta fue pausada. Consultá con el administrador.", no la
      aplicación normal ni una pantalla rota.
38. Como `admin`, click en "Reactivar" sobre esa misma cuenta.
    - **Esperado:** vuelve a aparecer "Pausar"; la cuenta recupera el acceso al toque.
39. Como `admin`, en la propia fila (la cuenta con la que está logueado) buscar los botones
    de Pausar/Aprobar/Rechazar.
    - **Esperado:** no existen — no hay forma de tocarse la propia cuenta desde esta
      pantalla (FR-406, evita quedar bloqueado sin otro admin que lo revierta).

## Correspondencia con tests automatizados

Los pasos 31-39 ya están cubiertos por tests automatizados: `ReporteControllerTest` (los 3
reportes, el caso "sin datos" del paso 32, el ponderado por kilos), `AgregadorRendimientoTest`/
`CalculadorPeriodoTest` (el cálculo puro detrás del paso 33), `PerfilControllerTest` (el
listado completo del paso 35, el efecto real de pausar sobre `GET /cortes` del paso 36, y
el guardia de auto-modificación del paso 39), `TablaReporte.test.tsx`/`UsuariosPage.test.tsx`
(qué se muestra y qué botón aparece en cada caso). Lo que el test automatizado no puede
probar es la experiencia real de punta a punta con dos sesiones de navegador distintas
(pasos 36-37) — por eso siguen siendo pasos manuales.

---

# Quickstart — Fase 5: Editar cortes + parte contable

Continúa la numeración de Fase 4. Reproduce `fase5/tasks-fase5.md` (FR-501 a FR-505).
Requiere haber hecho antes los pasos 1 a 39 (al menos el corte "Vacío" ya sembrado y, si se
quiere ver "Beneficio por kg vendible" con un valor real en vez de "—", conviene usar el
mismo despostado de 100 kg / 81 kg vendibles del paso 2).

## Verificación

40. Como `dueno`, ir a **Editar cortes** y editar "Vacío": cargar precio de venta `6500`.
    - **Esperado:** se guarda; la tabla muestra "$ 6.500" en la columna de precio de venta.
    El empleado, si entra a esta misma pantalla por URL directa, puede ver ese precio (no es
    un costo) pero no tiene el link en la navegación ni puede guardar cambios.
41. Ir a **Control diario** y escanear el código `2000012012501` (PLU 12 = Vacío, 1,250 kg).
    - **Esperado:** la venta se registra igual que antes; la tarjeta "Recaudado hoy" sube en
      `$ 8.125` (1,250 kg × $6.500).
42. Editar otro corte (ej. "Asado") y dejarlo **sin** precio de venta cargado (o usar uno que
    ya no lo tenga). Escanear una venta de ese corte.
    - **Esperado:** la venta se registra igual (no se bloquea el mostrador, Principio IV); no
      suma a "Recaudado hoy", y aparece la aclaración "N ventas de hoy sin precio
      registrado".
43. Ir a **Ajustes** y cambiar `tipoValor` a `importe`. Escanear un código cuyo corte **no**
    tenga precio de venta cargado.
    - **Esperado:** `400 CORTE_SIN_PRECIO_VENTA` (distinto de "peso cero"); no se registra
      ninguna venta. Volver `tipoValor` a `peso` para seguir probando el resto del quickstart.
44. Ir a **Inicio** y revisar la tarjeta "Recaudado este mes".
    - **Esperado:** suma el total de los escaneos con precio de los pasos 41 (y cualquier
      otro del mes); si hubo ventas sin precio, aparece la misma aclaración que en Control
      diario, pero en términos del mes.
45. Ir a **Reportes** y abrir las 3 secciones (por proveedor, por categoría, por período).
    - **Esperado:** las 3 tablas tienen la columna "Beneficio por kg vendible". Para el grupo
      que incluye la media res de 100 kg/81 kg vendibles del paso 2 (con "Vacío" a $6.500 el
      kg, el resto de los cortes de esa media res sin precio cargado): el valor se acerca al
      ejemplo canónico (`$9.000 − $6.420 = $2.580`, ponderado solo por los kilos de los
      cortes con precio). Un grupo sin ningún corte con precio cargado muestra "—".
46. Cambiar el precio de venta de "Vacío" (paso 40) a otro valor y revisar una venta **ya
    registrada** de ese corte (ej. la del paso 41) en "Ventas de hoy" o en el listado.
    - **Esperado:** esa venta vieja conserva el `precioTotal` que tenía al momento de
      venderse — no se recalcula retroactivamente (Principio I, no se reescribe historial).

## Correspondencia con tests automatizados

Los pasos 40-46 ya están cubiertos por tests automatizados: `CorteControllerTest` (precio de
venta editable y visible para el empleado, paso 40), `CalculadorVentaTest` (el cálculo puro
peso↔importe detrás de los pasos 41-43), `VentaControllerTest` (persistencia de
`precioTotal`, venta sin precio que igual se registra, y el `400 CORTE_SIN_PRECIO_VENTA` del
paso 43), `ResumenDiaControllerTest` (paso 41/42), `DespostadoRepositoryTest`/
`AgregadorRendimientoTest` (el ejemplo canónico del paso 45), `ResumenDia.test.tsx`/
`TablaReporte.test.tsx` (qué se muestra en cada tarjeta/columna). El paso 46 (que una venta
vieja no se recalcule) es más una garantía de diseño —`VentaEntity.precioTotal` nunca se
reescribe después de creada— que algo que un test de integración verifique directamente
tocando el reloj; queda como paso manual.
