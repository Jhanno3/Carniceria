# Constitución del proyecto — Sistema de despostado y control diario para carnicería

Este documento rige todo el desarrollo del proyecto. Ninguna especificación, plan ni tarea puede contradecirlo. Se basa en `especificacion-carniceria.md` (v0.1) y en las decisiones de stack ya tomadas.

---

## Principios

### I. Integridad de los datos

- Los kilos y los importes se guardan siempre como `numeric` (Postgres) / `BigDecimal` (Java), nunca como `float` o `double`.
  **Por qué:** un error de redondeo en coma flotante sobre kilos o pesos infla o esconde pérdida real en un despostado o una venta.
  **Cómo se comprueba:** ninguna migración ni entidad usa `float8`/`float`/`double`/`Float`/`Double` para kilos o dinero; se revisa en cada PR que toque esos campos.

- El stock nunca se guarda como columna: siempre se calcula (`despostado − ventas no anuladas`, por corte).
  **Por qué:** una columna de stock se desincroniza en cuanto alguien anula una venta o corrige un despostado.
  **Cómo se comprueba:** no existe ninguna tabla ni columna llamada "stock"; el valor sale siempre de una vista SQL o una consulta equivalente, nunca de un campo escrito.

- Una venta no se borra: se anula (`anulada = true`).
  **Por qué:** borrar destruye el historial necesario para auditar el día y el rendimiento real.
  **Cómo se comprueba:** no hay ningún `DELETE` sobre `ventas` en código ni en migraciones; anular es siempre un `UPDATE`.

- Ningún escaneo puede registrarse dos veces.
  **Por qué:** la cola offline puede reintentar el mismo escaneo al reconectar y duplicaría la venta.
  **Cómo se comprueba:** `id_cliente_local` tiene restricción `UNIQUE` en la base; reenviar un `id_cliente_local` ya existente no crea una segunda venta.

### II. Seguridad

- Row Level Security activado en todas las tablas desde la primera migración que las crea.
  **Por qué:** una tabla sin RLS queda expuesta en cuanto exista cualquier forma de llegar a Postgres sin pasar por la lógica de la aplicación.
  **Cómo se comprueba:** toda migración que crea una tabla incluye `ALTER TABLE ... ENABLE ROW LEVEL SECURITY` en el mismo commit, con sus políticas correspondientes.

- El empleado nunca ve precios de compra ni costos.
  **Por qué:** es información sensible del negocio que no le compete al mostrador.
  **Cómo se comprueba:** ninguna respuesta accesible con rol empleado incluye `precio_kg`, `costo_total` ni `costo_kg_vendible`; hay un test de autorización por rol para cada endpoint que podría exponerlos.

- Ninguna credencial con privilegios de escritura irrestringidos sobre la base (contraseña directa de Postgres, clave `service_role` de Supabase) llega al navegador. El frontend React usa el SDK de Supabase solo para el login (con la clave anónima) y obtiene un JWT; para todo lo demás habla con el backend Spring Boot, que valida ese JWT como resource server y lo propaga a Postgres (`SET LOCAL`) para que las políticas de RLS sigan aplicando por usuario real, no por un rol genérico de aplicación.
  **Por qué:** una credencial privilegiada expuesta en el cliente anula cualquier política de RLS; propagar el JWT en vez de reemplazarlo por un rol de aplicación mantiene a RLS como la autoridad real sobre los datos.
  **Cómo se comprueba:** búsqueda de `service_role` y de cadenas de conexión a Postgres en el código del frontend — no debe aparecer ninguna; test de integración que verifica que una consulta a Postgres sin el JWT propagado no puede leer datos de otro usuario.

- Registro con aprobación previa: cualquiera puede pedir una cuenta, pero nadie tiene ningún acceso hasta que el dueño la aprueba explícitamente. *(Revisado — la especificación original decía "sin registro público: el dueño invita"; el dueño pidió este cambio durante la implementación de la Fase 1. Ver `research.md`, sección del Bloque 7b.)*
  **Por qué:** evita que cualquiera se cree una cuenta con acceso al sistema de ventas y costos, sin obligar al dueño a crear cada cuenta a mano.
  **Cómo se comprueba:** toda cuenta nueva nace con `estado = 'pendiente'` en `perfiles`, sin excepción, sin importar qué rol pida; `is_dueno()` exige `estado = 'aprobado'` además de `rol = 'dueno'`, así que nadie se autoaprueba pidiendo ser dueño; no existe ningún endpoint que deje pasar `estado = 'pendiente'`.

### III. Cálculos confiables

- Toda la lógica de negocio (rendimiento, costo por kg vendible, decodificación de etiquetas EAN-13, stock) vive en funciones puras, con tests escritos antes o junto con el código que las implementa.
  **Por qué:** son los cálculos que determinan si el negocio gana o pierde plata; un bug ahí es invisible hasta que ya costó dinero.
  **Cómo se comprueba:** cada función de cálculo tiene tests en el mismo PR que la introduce o modifica; no hay lógica de cálculo embebida en controladores, componentes de UI o triggers sin un test que la cubra.

- Los ejemplos numéricos de la especificación son casos de test obligatorios.
  **Por qué:** son los únicos números que el dueño validó a mano; si el código no los reproduce, está mal.
  **Cómo se comprueba:** existe un test que reproduce "100 kg a $5.200/kg, 81 kg vendibles → $6.420 por kg vendible" y otro que reproduce "`20 00012 01250 1` → PLU 12, 1,250 kg".

### IV. El mostrador no se detiene

- El escaneo funciona sin conexión: cola local en el navegador (IndexedDB) con `id_cliente_local`, que sincroniza al volver la conexión.
  **Por qué:** un corte de internet en el local no puede frenar las ventas de un negocio físico.
  **Cómo se comprueba:** test que desconecta la red, escanea, reconecta y verifica que la venta llega una sola vez al backend.

- El campo de escaneo mantiene el foco siempre que la pantalla de Control diario esté abierta.
  **Por qué:** el lector USB escribe donde esté el cursor; perder el foco rompe el flujo de venta.
  **Cómo se comprueba:** test de UI que fuerza un blur (click afuera, apertura y cierre de un modal) y verifica que el foco vuelve solo al campo.

- Cada escaneo da una respuesta clara de éxito o error en menos de un segundo.
  **Por qué:** el empleado necesita saber al instante si la venta se descontó, sin mirar dos veces.
  **Cómo se comprueba:** test de tiempo entre el Enter del escaneo y el render del estado; presupuesto máximo 1000 ms, incluyendo el caso offline (se resuelve contra la cola local, no espera a la red).

### V. Hecho para Argentina

- Interfaz en español rioplatense, con voseo.
  **Por qué:** lo usan un dueño y empleados de mostrador no técnicos, en su idioma y registro cotidiano.
  **Cómo se comprueba:** revisión de los textos de UI — imperativos en voseo ("escaneá", "cargá"), sin "tú" ni "usted".

- Formato es-AR: coma decimal, punto de miles, pesos (`$ 6.420`, `1.250,5 kg`).
  **Por qué:** cualquier otro formato numérico es ilegible o ambiguo para el usuario argentino.
  **Cómo se comprueba:** todo número mostrado pasa por un formateador es-AR centralizado; no hay interpolación directa de números sin formatear en componentes.

- Los campos numéricos aceptan coma o punto como separador decimal al tipear.
  **Por qué:** el teclado y el hábito del usuario mezclan ambos; rechazar uno genera fricción y errores de carga.
  **Cómo se comprueba:** test de parseo que acepta `"1250,5"` y `"1250.5"` como el mismo valor.

- Zona horaria `America/Argentina/Buenos_Aires` para toda fecha y hora.
  **Por qué:** "cerrar el día" y "ventas de hoy" dependen de la hora del local, no de la del servidor.
  **Cómo se comprueba:** toda fecha se guarda en `timestamptz`; test que verifica que el corte de "día" ocurre a medianoche hora Argentina, sin importar dónde corra el servidor.

### VI. Usabilidad y accesibilidad

- Se usa en celular y en compu.
  **Por qué:** el dueño carga despostados desde la compu y el mostrador puede escanear desde un celular con cámara.
  **Cómo se comprueba:** cada pantalla se prueba en un viewport angosto (≤375 px) y uno ancho; nada obliga a scroll horizontal de la página completa.

- Botones y campos de al menos 44 px de alto (56 px en el escaneo), `<label>` en todo campo, contraste mínimo 4.5:1, nada que dependa solo del color.
  **Por qué:** lo usa gente parada en el mostrador, apurada, no solo sentada frente a un escritorio.
  **Cómo se comprueba:** checklist de accesibilidad (altura, `<label>`, contraste) antes de cerrar cualquier pantalla nueva; los estados de éxito/error llevan texto o ícono además del color.

- El umbral de "queda poco" es del 15 % de lo que entró por corte.
  **Por qué:** es el valor que propone la especificación para avisar a tiempo antes de quedarse sin stock de un corte.
  **Cómo se comprueba:** test que marca una fila como "Queda poco" cuando el stock cae por debajo del 15 % de lo despostado para ese corte, y no la marca por encima de ese umbral.

- El dueño puede anular cualquier venta sin límite de tiempo; el empleado solo puede anular una venta que él mismo cargó, y solo dentro de los 5 minutos de registrada.
  **Por qué:** permite corregir errores de carga en el momento sin dejar que el mostrador borre ventas antiguas o ajenas sin control del dueño.
  **Cómo se comprueba:** test que verifica que un empleado no puede anular una venta de otro usuario ni una propia con más de 5 minutos de antigüedad, y que el dueño puede anular cualquiera.

- Se respeta el sistema visual de la especificación (sección 5: colores, tipografías, componentes).
  **Por qué:** es el único lenguaje visual acordado; inventar otro genera inconsistencia y retrabajo.
  **Cómo se comprueba:** los componentes nuevos reusan los tokens ya definidos; un color o fuente nueva requiere actualizar la especificación antes de usarse.

### VII. Simplicidad

- La menor cantidad de dependencias y capas posible.
  **Por qué:** cada dependencia es superficie de bugs y mantenimiento para un equipo chico.
  **Cómo se comprueba:** toda dependencia nueva se justifica en el PR que la agrega; se prefiere resolver con lo que ya está (Postgres, Spring, React) antes de sumar una librería.

- Nada de funcionalidad fuera de la fase en curso (sección 9 de la especificación).
  **Por qué:** construir reportes o roles avanzados antes de tener el despostado funcionando desperdicia esfuerzo en algo que puede cambiar.
  **Cómo se comprueba:** todo plan y tarea cita a qué fase pertenece; se rechaza una tarea que adelanta trabajo de una fase futura sin decisión explícita del dueño.

- Si algo se puede resolver con Postgres (vistas, restricciones, políticas), se resuelve ahí.
  **Por qué:** una restricción o vista en la base no se puede esquivar desde ningún cliente; una validación solo en el backend sí.
  **Cómo se comprueba:** reglas como "kg > 0", unicidad de `id_cliente_local` o el cálculo de stock están implementadas como `CHECK`, `UNIQUE` o vistas SQL, no solo como validación en Java.

### VIII. Cambios controlados

- Toda modificación de la base de datos con migraciones versionadas en el repositorio.
  **Por qué:** sin migraciones versionadas nadie puede reproducir el estado de la base ni revertir un cambio.
  **Cómo se comprueba:** no hay cambios de esquema hechos a mano en el panel de Supabase; cada cambio tiene un archivo de migración numerado en el repo.

- Configuración sensible en variables de entorno.
  **Por qué:** una credencial en el código termina en el historial de git y tarde o temprano se filtra.
  **Cómo se comprueba:** no hay credenciales hardcodeadas en el repo; `.env` está en `.gitignore`.

---

## Restricciones técnicas

### Decidido

- **Base de datos:** Supabase (Postgres, Auth, Realtime), región **Canada Central** (`ca-central-1`). Plan pago, sin pausa por inactividad, con backups diarios. *(La especificación original pedía São Paulo por latencia; el proyecto real de Supabase se creó en Canada Central y el dueño decidió seguir ahí en vez de recrearlo — más latencia para el mostrador en Argentina, aceptada a cambio de no rehacer el proyecto.)*
- **Backend:** Java 21 (LTS) + Spring Boot 4.1.x. Es la única capa con credenciales privilegiadas hacia Supabase/Postgres; el frontend nunca accede a la base directamente.
- **Frontend:** React + Vite + Tailwind CSS.
- **Autenticación de usuarios:** email y contraseña vía Supabase Auth (especificación, sección 3), sin registro público. React usa el SDK de Supabase solo para el login (clave anónima) y obtiene un JWT; Spring Boot lo valida como resource server y lo propaga a Postgres (`SET LOCAL`) para que las políticas de RLS se apliquen por usuario real.
- **Realtime:** se consume directo desde React con la clave anónima de Supabase (no pasa por Spring Boot).
- **Testing backend:** JUnit 5 + Mockito.
- **Testing frontend:** Vitest + React Testing Library.
- **Hosting:** frontend en Vercel o Netlify; backend Spring Boot en Railway.
- **Repositorio:** monorepo, con `/backend` (Spring Boot) y `/frontend` (React) en el mismo repositorio.
- **Acceso a datos:** Spring Data JPA + Hibernate, con un interceptor/aspecto que propaga el JWT del usuario a cada transacción (`SET LOCAL`) para que las políticas de RLS se apliquen por usuario real.
- **Migraciones:** Flyway.
- **Estado del servidor en el frontend:** TanStack Query (React Query).
- **Umbral de "queda poco":** 15 % de lo que entró, por corte.
- **Anulación de ventas:** el dueño puede anular cualquier venta sin límite de tiempo; el empleado solo puede anular una venta propia, dentro de los 5 minutos de registrada.
- **Kilos:** `numeric(8,3)` en base, `BigDecimal` en Java; 1 decimal en pantalla, 3 decimales al guardar.
- **Importes:** `numeric(12,2)` en base, `BigDecimal` en Java.
- **Migraciones de base:** versionadas en el repositorio.

### [A DEFINIR]

- Marca y modelo de la balanza, y formato exacto de su etiqueta EAN-13 (especificación, sección 10). No bloquea el desarrollo: el sistema de etiquetas se construye configurable (especificación, sección 7) desde el día uno, sin asumir un formato fijo.

---

## Gobernanza

- Esta constitución prevalece sobre cualquier especificación, plan, tarea o decisión de implementación del proyecto. Ante un conflicto, se ajusta el documento de menor jerarquía — nunca esta constitución por la vía de los hechos.
- Se modifica solo con una decisión explícita del dueño del proyecto, incrementando la versión (MAJOR si se elimina o redefine un principio, MINOR si se agrega uno nuevo, PATCH si se aclara redacción sin cambiar el sentido) y registrando la fecha del cambio.
- Todo plan de implementación debe verificar, antes de ejecutarse, que no contradice ninguno de estos principios. Si lo hace, se ajusta el plan o se enmienda primero la constitución — nunca se implementa en contradicción silenciosa.

---

**Versión:** 1.0.0
**Fecha:** 2 de octubre de 2026
