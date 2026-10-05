# Especificación: Sistema de despostado y control diario para carnicería

**Feature**: `001-sistema-carniceria`
**Creado**: 2026-10-02
**Estado**: Borrador
**Entradas**: `especificacion-carniceria.md` (v0.1) · `constitution.md` (v1.0.0)

Esta spec traduce `especificacion-carniceria.md` en requisitos verificables, organizados por fase (sección 9 de la especificación), y queda sujeta a todo lo que exige `constitution.md`. Donde la especificación original dejaba algo abierto y ya se decidió (en la conversación con el dueño), esta spec usa el valor decidido. Donde sigue abierto, queda marcado al final.

---

## 1. Resumen

Una aplicación web de uso interno para una carnicería: registra el despostado de medias reses, calcula rendimiento y costo real por kilo vendible, y lleva ventas y stock diarios escaneando etiquetas EAN-13 de balanza. La usan un dueño (admin) y empleados de mostrador, en un local físico, en español rioplatense.

---

## 2. Fase 1 — Despostado

### 2.1 Historias de usuario

**US-1.1** — Como dueño, ingreso el peso de entrada, el precio de compra y elijo cómo voy a cargar los cortes, para empezar a despostar una media res.
- **Dado** que voy a cargar una media res nueva, **cuando** ingreso peso (kg), precio de compra ($/kg) y proveedor (opcional), **entonces** el sistema me deja elegir entre dos modos de carga: **manual** o **automática**, antes de pasar a la tabla de cortes.
- **Dado** que todavía no cargué el precio de compra, **cuando** veo la tarjeta de costo, **entonces** muestra "—" y el texto "Cargá el precio de compra".
- **Dado** que elijo carga **manual**, **cuando** entro a la tabla de cortes, **entonces** todos los campos de kilos arrancan vacíos.
- **Dado** que elijo carga **automática**, **cuando** entro a la tabla de cortes, **entonces** cada corte ya tiene precargado un kilaje estimado (ver US-1.5), editable como cualquier otro valor manual.
- **Dado** que estoy en la tabla de cortes (haya elegido el modo que haya elegido), **cuando** sigo editando cualquier kilo a mano, **entonces** el sistema no distingue ese valor de uno "manual" puro: ambos modos terminan en la misma tabla editable.
- **Dado** que todavía no toqué el botón final, **cuando** cierro la pantalla sin guardar, **entonces** no queda ninguna media res a medias en la base: no existe un estado intermedio guardado.

**US-1.2** — Como dueño, cargo o ajusto los kilos de cada corte y de cada pérdida (hueso, grasa, merma) y veo el rendimiento en vivo, hasta decidir cargar la entrada.
- **Dado** que edito el campo de kilos de un corte, **cuando** confirmo el valor, **entonces** se recalculan al instante vendible_kg, perdida_kg, sin_asignar_kg, rendimiento_%, costo_total y costo_kg_vendible (fórmulas de la sección 6 de la especificación).
- **Dado** que la suma de cortes y pérdidas coincide con el peso de entrada (±0,05 kg), **cuando** miro la barra de composición, **entonces** el mensaje de control está en verde ("Cuadra con el peso de entrada").
- **Dado** que falta asignar kilos, **entonces** el mensaje está en naranja ("Faltan asignar X kg"); **dado** que sobran, **entonces** está en rojo ("Sobran X kg: revisá las pesadas").
- **Dado** que toco el botón final "Cargar entrada", **cuando** el sistema guarda, **entonces** la media res, los kilos por corte y las pérdidas se persisten juntos, en una sola operación atómica (todo o nada).

**US-1.3** — Como dueño, veo en un mapa de la media res qué zonas pesan más, para detectar de un vistazo despostados raros.
- **Dado** que cargué kilos para varios cortes, **cuando** miro el mapa, **entonces** cada zona se pinta de claro a rojo oscuro según sus kilos relativos a la zona más pesada de esa media res, y el texto cambia a blanco si el fondo es oscuro.
- **Dado** que selecciono un corte en la tabla, **cuando** se marca su zona en el mapa con borde grueso, **entonces** debajo aparece "‹Corte› · X kg · Y % de la media res".
- **Dado** que un corte no tiene zona asignada (ej. carne picada/recortes), **entonces** no se pinta ninguna zona por él y se muestra la aclaración fija de la sección 4.1.

**US-1.4** — Como dueño, puedo editar la lista de cortes y sus PLU para que coincidan con los configurados en la balanza.

**US-1.5** — Como dueño, pido una estimación automática de kilos por corte para no arrancar de cero cada vez.
- **Dado** que ya guardé al menos una entrada de carne anteriormente (con sus cortes), **cuando** elijo carga **automática** para una media res nueva, **entonces** el sistema calcula, para cada corte, el porcentaje promedio que representó sobre el peso de entrada en **mis propias** entradas guardadas hasta ahora (no las de otros usuarios), y lo aplica al peso de la media res nueva para precargar su kilaje estimado.
- **Dado** que todavía no guardé ninguna entrada de carne propia (aunque otro usuario sí haya cargado entradas), **cuando** intento elegir carga **automática**, **entonces** esa opción no está disponible (solo puedo elegir manual).
- **Dado** que el sistema precargó un estimado, **cuando** reviso la tabla, **entonces** puedo corregir cualquier corte a mano antes de cargar la entrada; el sistema no guarda ninguna marca de "esto fue estimado" — una vez guardado, es un dato igual a cualquier otro.

### 2.2 Requisitos funcionales

| ID | Requisito |
|---|---|
| FR-101 | El sistema permite iniciar la carga de una media res con fecha, proveedor (opcional), peso de entrada (`numeric(8,3)`, > 0) y precio de compra por kg (`numeric(12,2)`, opcional). Este paso no persiste nada todavía: es el primer tramo de un único flujo que termina en FR-111. |
| FR-102 | El sistema permite cargar, por media res, los kilos de cada corte activo (`numeric(8,3)`, > 0 si se carga) y los kilos de cada tipo de pérdida: hueso, grasa, merma (`numeric(8,3)`, > 0 si se carga), editables en la misma tabla sin importar el modo de carga elegido (FR-112). |
| FR-103 | El sistema calcula, sin recargar la pantalla: `vendible_kg`, `perdida_kg`, `sin_asignar_kg`, `rendimiento_%`, `costo_total`, `costo_kg_vendible`, con las fórmulas de la sección 6 de la especificación. Estas funciones son puras y están cubiertas por tests (Principio III de la constitución). |
| FR-104 | `costo_kg_vendible` muestra "—" y "Cargá el precio de compra" mientras `precio_kg` sea nulo. |
| FR-105 | El mensaje de control de cuadre es verde si `\|sin_asignar_kg\| ≤ 0,05`, naranja si `sin_asignar_kg > 0,05` ("Faltan asignar X kg"), rojo si `sin_asignar_kg < -0,05` ("Sobran X kg: revisá las pesadas"). |
| FR-106 | El mapa de cortes pinta cada zona con una escala de `rgb(246,228,224)` a `rgb(110,20,20)` según los kilos de esa zona respecto de la zona más pesada de la misma media res; el color de texto cambia a blanco cuando el contraste con el fondo lo requiere. |
| FR-107 | Seleccionar un corte en la tabla resalta su fila (rosado suave), marca su zona en el mapa con borde negro grueso y muestra debajo el detalle del corte seleccionado. |
| FR-108 | Editar los kilos de un corte en la tabla recalcula de inmediato todos los totales derivados (FR-103). |
| FR-109 | El dueño puede editar nombre, PLU (único), cuarto y zona de mapa de cada corte, y activarlo o desactivarlo. |
| FR-110 | La acción "Restablecer ejemplo" carga los valores de referencia de la sección 2.2 de la especificación (incluyendo hueso 11 kg, grasa 6 kg, merma 2 kg sobre una media res de 100 kg). Es independiente de los modos manual/automático: sirve para probar el sistema, no para operar con datos reales. |
| FR-111 | La acción final "Cargar entrada" persiste, en una sola operación atómica, la media res, los kilos por corte y las pérdidas. Si falla cualquier parte, no se guarda nada (ni la media res). No existe ningún estado "a medias" guardado en la base. |
| FR-112 | Antes de entrar a la tabla de cortes, el dueño elige el modo de carga: **manual** (todos los campos de kilos arrancan vacíos) o **automática** (ver FR-113). Ambos modos llevan a la misma tabla editable; el modo elegido no se guarda ni distingue después. |
| FR-113 | El modo **automático** calcula, para cada corte activo, el porcentaje promedio que representó sobre `peso_kg` en las entradas ya cargadas por el **mismo usuario** (`creado_por`, FR-111) hasta ese momento, y lo aplica al `peso_kg` de la media res nueva para precargar su kilaje estimado en la tabla. Solo está disponible si ese usuario tiene al menos una entrada propia cargada previamente; si no, solo se ofrece el modo manual, aunque otros usuarios ya tengan historial. |

### 2.3 Casos borde

- Guardar una entrada cuyo `sin_asignar_kg` sea distinto de cero: se permite guardar (el mensaje de control queda como advertencia visible, no bloquea el guardado).
- Dos cortes con la misma `zona_mapa` (ej. Cuadrada y Tapa de nalga comparten "nalga"): el mapa suma sus kilos para pintar esa única zona.
- Corte sin `zona_mapa` (carne picada/recortes): participa en los cálculos pero no en el mapa.
- Se intenta elegir carga automática sin ninguna entrada previa propia cargada: la opción aparece deshabilitada, no se ofrece una estimación con valores de ejemplo, aun si otro usuario del sistema ya cargó entradas.
- Se agrega un corte nuevo (FR-109) después de que ya existan entradas cargadas: ese corte no tiene historial propio, así que su estimación automática parte de 0 kg hasta que se cargue al menos una vez.
- Se cierra o recarga la pantalla a mitad de la carga (antes de tocar "Cargar entrada"): no queda ningún rastro en la base; hay que empezar de nuevo.

---

## 3. Fase 2 — Escaneo y stock

### 3.1 Historias de usuario

**US-2.1** — Como empleado, escaneo la etiqueta de la balanza y veo al instante si la venta se registró.
- **Dado** que el campo de escaneo tiene el foco, **cuando** el lector USB escribe el código y manda Enter, **entonces** el sistema lo decodifica, lo valida y, si es válido, descuenta el stock y muestra "Descontado del stock" en verde en menos de 1 segundo.
- **Dado** que el código es inválido (dígito verificador, prefijo, PLU inexistente o peso cero), **entonces** no se registra ninguna venta y se muestra el motivo en rojo en menos de 1 segundo.
- **Dado** que hago clic en cualquier otro lado de la pantalla, **cuando** vuelvo a mirar el campo de escaneo, **entonces** el foco está ahí de nuevo.

**US-2.2** — Como empleado, veo de un vistazo cómo viene el día: kilos vendidos, etiquetas escaneadas, stock en cámara, entradas de carne cargadas hoy.

**US-2.3** — Como dueño o empleado, veo el stock por corte y cuáles están por agotarse.
- **Dado** que el stock de un corte cae por debajo del 15 % de lo despostado para ese corte, **entonces** su fila se marca en naranja con la etiqueta "Queda poco".

**US-2.4** — Como dueño, configuro el formato de la etiqueta EAN-13 sin que nadie toque código, porque cada balanza puede imprimir distinto.

### 3.2 Requisitos funcionales

| ID | Requisito |
|---|---|
| FR-201 | El campo de escaneo mantiene el foco en todo momento mientras la pantalla de Control diario está abierta (Principio IV). |
| FR-202 | El sistema decodifica cada código leído usando la configuración vigente en `config_etiqueta` (prefijo, posición/largo de PLU, posición/largo/tipo de valor, decimales), no un formato fijo en el código. |
| FR-203 | El sistema valida, en este orden, antes de registrar una venta: dígito verificador correcto, prefijo de peso variable, PLU existente entre los cortes activos, peso mayor a cero. Si cualquiera falla, no se registra la venta y se informa el motivo. |
| FR-204 | Una venta válida se registra con: corte, kilos (`numeric(8,3)`), código leído, `id_cliente_local` único, usuario y fecha/hora (`timestamptz`). |
| FR-205 | El stock por corte se calcula siempre como `despostado.kg` acumulado menos `ventas.kg` no anuladas acumuladas para ese corte; nunca se guarda como columna (Principio I). |
| FR-206 | El resumen del día muestra: kilos vendidos hoy, etiquetas escaneadas hoy, stock vendible total en cámara, cantidad de entradas de carne (medias reses) cargadas en el día de hoy (`creado_en` dentro del día actual, hora Argentina). |
| FR-207 | "Ventas de hoy" lista hora, corte y kilos, más reciente primero, con enlace a ver el listado completo. |
| FR-208 | La tabla de stock por corte marca en naranja, con la etiqueta "Queda poco", todo corte cuyo stock sea menor al 15 % de lo despostado para ese corte. |
| FR-209 | Existe una pantalla de ajustes donde el dueño edita `config_etiqueta` (prefijo, posición y largo del PLU, posición y largo del valor, tipo de valor, decimales) sin necesidad de un cambio de código. |
| ~~FR-210~~ | ~~"Exportar a Excel" genera un archivo con las ventas y el stock del día.~~ Descartado (decisión del dueño, 2026-10-04): no se construye. |

### 3.3 Casos borde

- Mismo código escaneado dos veces por error de lector (doble Enter): si genera el mismo `id_cliente_local`, no se duplica la venta ni el descuento de stock (Principio I).
- PLU leído que no corresponde a ningún corte activo: error "PLU inexistente", venta no registrada.
- Peso leído igual a cero: error, venta no registrada.
- `config_etiqueta` mal configurada (ej. posiciones que se superponen): el dígito verificador no da y el escaneo se rechaza con error, no con un valor incorrecto silencioso.

---

## 4. Fase 3 — Sin conexión y roles

### 4.1 Historias de usuario

**US-3.1** — Como empleado, sigo vendiendo aunque se corte internet en el local.
- **Dado** que no hay conexión, **cuando** escaneo una etiqueta válida, **entonces** se guarda en una cola local (IndexedDB) con `id_cliente_local`, se muestra el mismo éxito que si hubiera conexión, y la pantalla indica cuántas ventas están pendientes de subir.
- **Dado** que vuelve la conexión, **cuando** el sistema sincroniza la cola, **entonces** cada venta pendiente se sube una sola vez, sin duplicarse aunque se reintente la sincronización.

**US-3.2** — Como empleado, no veo precios de compra ni costos en ninguna pantalla.

**US-3.3** — Como dueño, soy el único que puede aprobar cuentas nuevas; cualquiera puede pedir una, pero nadie entra sin mi aprobación. *(Revisado durante la implementación — ver constitution.md, Principio II: la especificación original decía "sin registro público, el dueño invita"; se cambió a registro abierto con aprobación previa.)*

**US-3.4** — Como dueño o empleado, puedo anular una venta mal cargada, dentro de las reglas que me corresponden.
- **Dado** que soy el dueño, **cuando** anulo cualquier venta, **entonces** se marca `anulada = true` sin límite de tiempo.
- **Dado** que soy empleado y la venta es mía y tiene menos de 5 minutos, **cuando** la anulo, **entonces** se marca `anulada = true`.
- **Dado** que soy empleado y la venta es de otro usuario, o la mía tiene más de 5 minutos, **cuando** intento anularla, **entonces** el sistema lo rechaza.

### 4.2 Requisitos funcionales

| ID | Requisito |
|---|---|
| FR-301 | Sin conexión, cada escaneo válido se guarda en una cola local (IndexedDB) con `id_cliente_local` único, generado en el cliente, y muestra éxito sin esperar respuesta del servidor. |
| FR-302 | Al recuperar la conexión, el sistema sincroniza automáticamente la cola local; el backend rechaza sin duplicar cualquier `id_cliente_local` ya recibido antes (restricción `UNIQUE`). |
| FR-303 | La pantalla muestra en todo momento la cantidad de ventas pendientes de subir. |
| FR-304 | El rol `empleado` puede: registrar ventas por escaneo, leer cortes activos, leer stock por corte, leer ventas del día. No puede leer `precio_kg`, `costo_total` ni `costo_kg_vendible` de ninguna media res, en ninguna respuesta del backend. |
| FR-305 | El rol `dueno` tiene además: alta/edición de medias reses y despostados, lectura de costos y precios de compra, edición de cortes y PLU, acceso a reportes, invitación de empleados. |
| FR-306 | Cualquiera puede registrarse (nombre, email, contraseña, rol pedido). Toda cuenta nueva nace con `estado = 'pendiente'` y no tiene ningún acceso hasta que un `dueno` aprobado la pasa a `aprobado` (o la rechaza). |
| FR-307 | Anular una venta nunca la borra: siempre es un `UPDATE` de `anulada = true` (Principio I). |
| FR-308 | El `dueno` puede anular cualquier venta sin límite de tiempo. El `empleado` puede anular únicamente una venta cuyo `usuario_id` sea el suyo, y solo si pasaron menos de 5 minutos desde `fecha_hora`. |

### 4.3 Casos borde

- Dos dispositivos del mismo mostrador sincronizan al mismo tiempo tras un corte de luz: ninguno debe duplicar ventas gracias a la restricción única de `id_cliente_local`.
- Un empleado intenta anular una venta ajena: rechazado con un mensaje claro, no un error genérico.
- Reloj del dispositivo desincronizado: la ventana de 5 minutos para anulación se evalúa con la hora del servidor (`fecha_hora`), no con la del navegador del empleado.

---

## 5. Fase 4 — Reportes

### 5.1 Historias de usuario

**US-4.1** — Como dueño, veo el rendimiento por proveedor y por período para decidir a quién comprarle.

~~**US-4.2** — Como dueño, exporto reportes a Excel para mirarlos fuera del sistema.~~ Descartado (decisión del dueño, 2026-10-04, mismo criterio que FR-210 de Fase 2): ningún reporte se exporta a Excel.

### 5.2 Requisitos funcionales

| ID | Requisito |
|---|---|
| FR-401 | El sistema genera un reporte de rendimiento (`rendimiento_%`, `costo_kg_vendible` promedio) agrupado por `medias_reses.proveedor`, para un rango de fechas elegido. |
| FR-402 | El sistema genera un reporte de rendimiento agrupado por período (día, semana o mes, a definir en el plan de esta fase). |
| FR-403 | El sistema genera un reporte de rendimiento agrupado por `medias_reses.categoria` (Novillo, Novillito, Vaquillona, Vaca, Toro, Ternero), para un rango de fechas elegido. Las medias reses sin categoría cargada quedan agrupadas aparte, no se excluyen del reporte. |
| ~~FR-404~~ | ~~Todo reporte se puede exportar a Excel.~~ Descartado (decisión del dueño, 2026-10-04): no se construye. |

### 5.3 Casos borde

- Rango de fechas sin medias reses cerradas: el reporte se muestra vacío con un mensaje, no como error.

> **Resuelto (decisión del dueño, 2026-10-04):** la pregunta abierta sobre el reporte "por categoría de animal" (sección 9 de la especificación original) se resolvió agregando una columna real `categoria` a `medias_reses` (`V17__categoria_animal.sql`, ver `fase1/data-model.md`), opcional, con la clasificación típica del Mercado de Liniers. Ver FR-403 arriba y `fase1/contracts/despostado-api.md` para el campo en la API de carga.

---

## 6. Requisitos no funcionales

Derivados directamente de `constitution.md`; esta spec no agrega ninguno nuevo.

| Área | Requisito |
|---|---|
| Rendimiento | Toda respuesta a un escaneo (éxito o error) se muestra en menos de 1000 ms, incluso sin conexión (Principio IV). |
| Localización | Interfaz en español rioplatense con voseo; formato es-AR (`$ 6.420`, `1.250,5 kg`); campos numéricos aceptan coma o punto; zona horaria `America/Argentina/Buenos_Aires` para toda fecha (Principio V). |
| Accesibilidad | Botones/campos ≥ 44 px (56 px en el escaneo); `<label>` en todo campo; contraste ≥ 4.5:1; ningún estado depende solo del color; uso cómodo en celular y en compu (Principio VI). |
| Seguridad | RLS activo en todas las tablas desde la primera migración; el empleado nunca recibe `precio_kg`/costos; ninguna credencial privilegiada llega al navegador; registro abierto pero sin ningún acceso hasta la aprobación del dueño (Principio II). |
| Integridad de datos | Kilos e importes como `numeric`/`BigDecimal`, nunca flotante; stock siempre calculado, nunca almacenado; ventas se anulan, no se borran; cada escaneo es idempotente por `id_cliente_local` (Principio I). |
| Simplicidad | Ninguna funcionalidad de una fase posterior se construye antes de tiempo; lo que resuelve Postgres (`CHECK`, `UNIQUE`, vistas) no se duplica en la aplicación (Principio VII). |
| Cambios controlados | Todo cambio de esquema vía migración versionada en el repo; toda configuración sensible en variables de entorno (Principio VIII). |

---

## 7. Fuera de alcance

Igual que la especificación original, sección 1: facturación electrónica (ARCA), cobros, cuentas corrientes de clientes, precios de venta por corte. Pueden incorporarse en una fase futura, con su propia spec.

---

## 8. Entidades clave

Ver `especificacion-carniceria.md`, sección 8.1, para el DDL completo (`cortes`, `medias_reses`, `despostado`, `perdidas`, `ventas`, `perfiles`, `config_etiqueta`). Dos aclaraciones que impone la constitución sobre ese modelo:

- El stock **no** es una tabla ni una columna: es una vista (`despostado − ventas no anuladas`, por corte).
- El rol `empleado` nunca debe recibir `precio_kg` de `medias_reses` ni ningún costo derivado: se expone mediante una vista o proyección sin esas columnas, reforzada por las políticas de RLS.

---

## 9. Verificación contra la constitución

Chequeo previo a convertir esta spec en plan, como exige la sección de Gobernanza de `constitution.md`.

| Principio | Cumplimiento en esta spec |
|---|---|
| I. Integridad de los datos | FR-101, FR-102, FR-204, FR-205, FR-301–FR-302, FR-307 fijan `numeric`, stock por vista, anulación en vez de borrado, e idempotencia por `id_cliente_local`. |
| II. Seguridad | FR-304–FR-306 fijan el límite de lo que ve cada rol y que el registro no da ningún acceso sin aprobación del dueño; la arquitectura JWT + RLS queda en `constitution.md` (Restricciones técnicas) y debe respetarse en el plan. |
| III. Cálculos confiables | FR-103 exige funciones puras con test; los ejemplos de la sección 6 y 7 de la especificación son casos de test obligatorios en el plan. |
| IV. El mostrador no se detiene | FR-201, FR-301–FR-303 cubren foco persistente, cola offline y feedback bajo 1 segundo. |
| V. Hecho para Argentina | Sección 6 de esta spec remite directo al Principio V; ningún FR lo contradice. |
| VI. Usabilidad y accesibilidad | FR-208 (umbral 15 %) y la sección 6 de esta spec remiten al Principio VI. |
| VII. Simplicidad | Esta spec separa las 4 fases y marca explícitamente qué no se construye todavía (sección 7). |
| VIII. Cambios controlados | Toda entidad nueva o cambiada en el plan derivado de esta spec debe llegar como migración versionada. |

No se detectaron contradicciones entre esta spec y la constitución.

---

## 10. Preguntas abiertas

- **Marca/modelo de la balanza y formato exacto de su etiqueta EAN-13**: no bloquea esta spec porque FR-202 exige que el formato sea configurable (`config_etiqueta`) desde el día uno.
- **Agrupación por período en FR-402** (día/semana/mes): a definir en el plan de Fase 4.

---

**Siguiente paso:** `fase1/plan.md` por fase (`fase2/plan-fase2.md`, `fase3/plan-fase3.md`, ...), empezando por la Fase 1 (Despostado), verificando contra la sección 9 de este documento antes de generar tareas.
