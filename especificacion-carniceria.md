# Sistema de despostado y control diario para carnicería

Especificación funcional, de diseño y de datos. Versión 0.1 · Octubre 2026.

Boceto visual de referencia: https://claude.ai/artifact/Q7VG6DSTmXU1BCh7uhnCq7

---

## 1. Objetivo

Una aplicación web para una carnicería argentina que permita:

1. **Registrar el despostado de cada media res**: peso de entrada, precio de compra, kilos de cada corte y kilos perdidos (hueso, grasa, merma).
2. **Conocer el rendimiento real**: qué porcentaje de la media res se vende y cuánto cuesta de verdad cada kilo vendible.
3. **Llevar el control diario de ventas y stock** escaneando las etiquetas con código de barras que imprime la balanza.

Fuera de alcance en la versión 1: facturación electrónica (ARCA), cobros, cuentas corrientes de clientes y precios de venta por corte. Se pueden sumar después.

---

## 2. Contexto de negocio

### 2.1 Valores de referencia para una media res de novillo de 100 kg

| Concepto | Kg aprox. | Comentario |
|---|---|---|
| Carne vendible (cortes con y sin hueso) | 75–82 | Asado, cortes del trasero, delantero, picada |
| Hueso sin venta | 8–12 | Espinazo, caracú, puntas |
| Grasa y recortes de descarte | 4–8 | Depende de la terminación del animal |
| Merma (oreo, deshidratación, aserrín) | 1–3 | Sube con los días en cámara |

Si se deshuesa todo, la carne limpia baja a 65–72 kg cada 100 kg. Estos números son orientativos: el sistema existe justamente para medir el rendimiento real de cada media res.

### 2.2 Lista inicial de cortes

Editable por el dueño. Cada corte tiene un PLU que debe coincidir con el configurado en la balanza.

| Corte | Cuarto | Zona en el mapa | Valor de ejemplo (kg) |
|---|---|---|---|
| Cuadril | Trasero | cuadril | 3,0 |
| Lomo | Trasero | lomo | 1,8 |
| Bife angosto | Trasero | bifeAngosto | 4,5 |
| Nalga | Trasero | nalga | 6,0 |
| Cuadrada | Trasero | nalga | 2,5 |
| Tapa de nalga | Trasero | nalga | 1,2 |
| Bola de lomo | Trasero | bola | 3,0 |
| Peceto | Trasero | bola | 1,4 |
| Vacío | Trasero | vacio | 3,3 |
| Matambre | Trasero | matambre | 1,3 |
| Osobuco | Ambos | osobuco | 3,5 |
| Asado | Delantero | asado | 11,0 |
| Tapa de asado | Delantero | asado | 1,5 |
| Falda | Delantero | falda | 4,0 |
| Bife ancho | Delantero | bifeAncho | 4,0 |
| Paleta | Delantero | paleta | 6,5 |
| Carne picada / recortes | Ambos | (sin zona) | 6,5 |

Aguja, Marucha, Pecho y Cogote no están en la lista: en esta carnicería no se despostan como cortes aparte, van incluidos dentro de otros cortes ya existentes (Paleta/Asado).

Pérdidas de ejemplo: hueso 11 kg, grasa 6 kg, merma 2 kg. Estos valores son solo ilustrativos para "Restablecer ejemplo"; no hace falta que la suma cierre exacta contra el peso de entrada.

---

## 3. Usuarios y roles

| Rol | Puede |
|---|---|
| **Dueño** (admin) | Todo: cargar medias reses y despostados, ver costos y precios de compra, editar cortes y PLU, ver reportes, gestionar usuarios. |
| **Empleado** (mostrador) | Escanear ventas, ver stock por corte y ventas del día. **No ve** precios de compra ni costos. |

Login con email y contraseña (Supabase Auth). Sin registro público: el dueño invita a los empleados.

---

## 4. Pantallas

Las dos pantallas comparten una barra superior con el nombre de la sección y una navegación en forma de pastillas: **Despostado** y **Control diario**. La sección activa va con fondo oscuro.

### 4.1 Despostado

Sirve para cargar una media res y ver su rendimiento en vivo.

**Bloque de resumen (4 tarjetas en grilla, se acomodan solas según el ancho):**

1. **Entrada**: campo "Peso media res (kg)" en grande y campo "Precio de compra ($/kg)".
2. **Carne vendible**: kilos totales en rojo oscuro y su porcentaje sobre la media res.
3. **Pérdida**: kilos de hueso + grasa + merma en azul y su porcentaje.
4. **Costo real por kg vendible**: muestra "—" y el texto "Cargá el precio de compra" hasta que haya precio. Debajo compara con el precio pagado.

**Barra de composición:** una barra horizontal apilada con vendible, hueso, grasa, merma y "sin asignar" (gris), con su leyenda debajo (kg y %). A la derecha del título, un mensaje de control:

- Verde: "Cuadra con el peso de entrada" (diferencia de hasta 0,05 kg).
- Naranja: "Faltan asignar X kg".
- Rojo: "Sobran X kg: revisá las pesadas".

**Mapa de cortes:** esquema lateral de la media res (cabeza a la izquierda) dibujado en SVG, con una zona por corte o grupo de cortes. Cada zona se pinta de claro a rojo oscuro según los kilos que tiene respecto de la zona más pesada. El texto de la zona pasa a blanco cuando el fondo es oscuro. Al seleccionar un corte en la tabla, su zona se marca con borde negro grueso y debajo del mapa aparece "Vacío · 3,3 kg · 3,3 % de la media res". Aclaración fija: "Esquema orientativo. Carne picada y recortes no tienen zona en el mapa."

**Tabla de cortes vendibles:** columnas Corte (botón seleccionable), Cuarto, Kilos (campo editable), % y una barrita de peso relativo. La fila seleccionada se resalta en rosado suave. En pantallas angostas la tabla se desplaza horizontalmente dentro de su caja.

**Lo que no se vende:** tres tarjetas en tonos azules (hueso, grasa, merma) con campo de kilos y su porcentaje.

**Acciones:** "Restablecer ejemplo" (secundario) y "Guardar despostado" (principal, rojo oscuro).

### 4.2 Control diario

Es la pantalla del mostrador. Tiene que poder usarse rápido y sin mouse.

**Bloque de escaneo (borde grueso, lo más visible de la pantalla):**

- Campo grande "Escaneá la etiqueta de la balanza", con el foco puesto siempre que la pantalla esté abierta. El lector USB escribe el código y manda Enter.
- Botón "Usar cámara del celular" como alternativa.
- Franja "Último escaneo": código leído, interpretación (producto pesado, PLU, corte, peso) y estado ("Descontado del stock" en verde, o un error en rojo si el PLU no existe o el código es inválido).

**Resumen del día (4 tarjetas):** kilos vendidos hoy, etiquetas escaneadas, stock vendible en cámara, medias reses abiertas.

**Ventas de hoy:** lista con hora, corte y kilos, la más reciente arriba, con enlace a ver todas. Cada venta se puede anular (solo el dueño, o el empleado dentro de los 5 minutos; a definir).

**Stock por corte:** columnas Corte, Entró, Vendido, Queda y una barra de disponible. Si queda menos de un umbral (por defecto 15 % de lo que entró), la fila se marca en naranja con la etiqueta "Queda poco".

**Acciones:** "Cerrar el día". *(No se exporta a Excel — decisión del dueño, 2026-10-04.)*

---

## 5. Sistema visual

### 5.1 Colores

| Uso | Color |
|---|---|
| Fondo | `#FFFFFF` |
| Texto principal | `#1C1917` |
| Texto secundario | `#57534E` (cumple contraste 4.5:1) |
| Bordes | `#E7E5E4`, campos `#D6D3D1` |
| Fondos suaves | `#F5F5F4`, `#FAFAF9` |
| Acento: carne vendible, botón principal | `#8E1B1B` |
| Pérdida: hueso / grasa / merma | `#2F5D8A` / `#6E93BA` / `#A9C1DB` |
| Escala del mapa | De `rgb(246,228,224)` a `rgb(110,20,20)` |
| Éxito | texto `#166534`, fondo `#DCFCE7` |
| Aviso | texto `#9A3412`, fondo `#FFF4E5` |
| Error | `#B91C1C` |

Rojo (vendible) y azul (pérdida) difieren también en luminosidad, así se distinguen aunque alguien no vea bien los colores.

### 5.2 Tipografía

- **Títulos y números grandes:** Archivo (500, 700, 800).
- **Texto e interfaz:** Public Sans (400 a 700).
- Números con `font-variant-numeric: tabular-nums` para que las columnas queden alineadas.
- Tamaños: títulos de sección 18 px, cifras de tarjetas 34 px, texto 15 px, etiquetas 13 px en mayúsculas con espaciado.

### 5.3 Componentes

- Tarjetas: borde 1 px, radio 12 px, padding 16 px.
- Botones y campos: alto mínimo 44 px (56 px en el escaneo).
- Íconos: SVG de trazo simple, sin emojis.

### 5.4 Formato y accesibilidad

- Idioma y formato **es-AR**: coma decimal, punto de miles ("1.250,5 kg", "$ 6.420"). Los campos aceptan coma o punto.
- Kilos con 1 decimal en pantalla y 3 decimales al guardar (la balanza pesa en gramos).
- Todo campo tiene su `<label>`. Todo lo clickeable es un `<button>` o `<a>` real. Foco visible.
- Responsive: las tarjetas y columnas se apilan en el celular; las tablas anchas se desplazan dentro de su caja, nunca la página entera.

### 5.5 Modo oscuro

El usuario elige manualmente entre modo claro y oscuro (botón sol/luna, visible en el login y en la barra de navegación); arranca según `prefers-color-scheme` del sistema si todavía no eligió nada, y la elección queda guardada en el navegador. Mismos componentes, mismos tamaños — solo cambian los colores de superficie y acento:

| Uso | Color (claro) | Color (oscuro) |
|---|---|---|
| Fondo | `#FFFFFF` | `#1C1917` |
| Fondos suaves | `#F5F5F4`, `#FAFAF9` | `#44403C`, `#292524` |
| Texto principal | `#1C1917` | `#F5F5F4` |
| Texto secundario | `#57534E` | `#A8A29E` |
| Bordes | `#E7E5E4`, campos `#D6D3D1` | `#57534E`, campos `#78716C` |
| Acento: carne vendible, botón principal | `#8E1B1B` | `#EF4444` |
| Pérdida: hueso / grasa / merma | `#2F5D8A` / `#6E93BA` / `#A9C1DB` | `#60A5FA` / `#93C5FD` / `#BFDBFE` |
| Éxito | texto `#166534`, fondo `#DCFCE7` | texto `#86EFAC`, fondo `#14532D` |
| Aviso | texto `#9A3412`, fondo `#FFF4E5` | texto `#FDBA74`, fondo `#7C2D12` |
| Error | texto `#B91C1C`, fondo `#FEF2F2` | texto `#F87171`, fondo `#7F1D1D` |
| Fila seleccionada (rosado) | `#FCE7F3` | `#500724` |

La escala del mapa de cortes (`rgb(246,228,224)` → `rgb(110,20,20)`, sección 5.1) **no cambia** con el tema: es una escala de datos ("más oscuro = más kilos"), no un color de superficie, y cambiarla rompería su lectura entre los dos modos.

---

## 6. Cálculos

```
vendible_kg      = suma de kilos de todos los cortes
perdida_kg       = hueso + grasa + merma
sin_asignar_kg   = peso_media_res − vendible_kg − perdida_kg
rendimiento_%    = vendible_kg / peso_media_res × 100
costo_total      = peso_media_res × precio_compra_kg
costo_kg_vendible = costo_total / vendible_kg
stock_corte      = kilos despostados del corte − kilos vendidos del corte
```

Ejemplo: 100 kg a $ 5.200/kg = $ 520.000. Con 81 kg vendibles, el costo real es $ 6.420 por kg vendible.

Toda la lógica de cálculo debe estar en funciones puras con tests.

---

## 7. Etiquetas de la balanza (EAN-13)

Las balanzas etiquetadoras imprimen un EAN-13 de "producto de peso variable" que empieza con 2. El formato más común es:

```
2X  PPPPP  VVVVV  C
│   │      │      └ dígito verificador
│   │      └ valor: peso en gramos (o importe, según configuración)
│   └ código de producto (PLU)
└ prefijo (20 a 29)
```

Ejemplo: `20 00012 01250 1` → PLU 12 (Vacío), 1,250 kg. (El boceto muestra un código acortado; el real tiene 13 dígitos.)

Como cada marca y cada configuración puede variar (largo del PLU, si el valor es peso o importe), **el formato debe ser configurable** desde una pantalla de ajustes: prefijo, posición y largo del PLU, posición y largo del valor, tipo de valor y decimales.

Validaciones al escanear: dígito verificador correcto, prefijo de peso variable, PLU existente, peso mayor a cero. Si algo falla, no se registra la venta y se muestra el motivo.

---

## 8. Datos (Supabase, región São Paulo)

### 8.1 Tablas

```sql
cortes
  id uuid pk
  nombre text not null
  plu integer unique not null
  cuarto text check (cuarto in ('Delantero','Trasero','Ambos'))
  zona_mapa text            -- null para picada/recortes
  activo boolean default true

medias_reses
  id uuid pk
  fecha date not null
  proveedor text
  peso_kg numeric(8,3) not null
  precio_kg numeric(12,2)   -- solo visible para el dueño
  categoria text check (categoria in ('Novillo','Novillito','Vaquillona','Vaca','Toro','Ternero'))  -- opcional, clasificación Mercado de Liniers, para reporte de Fase 4
  estado text check (estado in ('abierta','cerrada')) default 'abierta'
  creado_por uuid references auth.users

despostado
  id uuid pk
  media_res_id uuid references medias_reses
  corte_id uuid references cortes
  kg numeric(8,3) not null

perdidas
  id uuid pk
  media_res_id uuid references medias_reses
  tipo text check (tipo in ('hueso','grasa','merma'))
  kg numeric(8,3) not null

ventas
  id uuid pk
  fecha_hora timestamptz default now()
  corte_id uuid references cortes
  kg numeric(8,3) not null
  codigo_leido text not null
  id_cliente_local uuid unique   -- evita duplicados al sincronizar offline
  anulada boolean default false
  usuario_id uuid references auth.users

perfiles
  id uuid pk references auth.users
  nombre text
  rol text check (rol in ('dueno','empleado'))

config_etiqueta
  (prefijo, inicio_plu, largo_plu, inicio_valor, largo_valor, tipo_valor, decimales)
```

### 8.2 Reglas

- Los kilos se guardan como `numeric`, nunca como número de coma flotante.
- **El stock no se guarda**: se calcula con una vista (`despostado − ventas no anuladas`, por corte).
- Row Level Security activado en todas las tablas desde el día uno. El empleado puede insertar ventas y leer cortes y stock; no puede leer `precio_kg` ni costos (exponer a empleados una vista sin esas columnas).
- Cambios de estructura siempre con migraciones versionadas.
- Plan pago de Supabase para evitar pausas por inactividad y tener backups diarios.

### 8.3 Sin conexión

Si se corta internet en el mostrador, los escaneos se guardan en el navegador (IndexedDB) con un `id_cliente_local` y se suben cuando vuelve la conexión. La pantalla muestra cuántas ventas están pendientes de subir.

---

## 9. Fases

1. **Despostado**: cortes, medias reses, carga de despostado y pérdidas, cálculos y mapa.
2. **Escaneo y stock**: lectura de etiquetas, ventas, stock por corte, resumen del día.
3. **Sin conexión y roles**: cola offline, permisos de empleado, anulación de ventas.
4. **Reportes**: rendimiento por proveedor, por categoría de animal y por período. *(Sin exportación a Excel — decisión del dueño, 2026-10-04.)*

---

## 10. Preguntas abiertas

- Marca y modelo de la balanza, y formato exacto de su etiqueta.
- ¿La balanza imprime peso o importe en el código?
- Stack del frontend (por ejemplo React con Vite o Next.js) y dónde se aloja.
- ¿Se despostan varias medias reses juntas o una por una?
- Umbral de "queda poco" por corte.
- Quién puede anular ventas y hasta cuándo.
