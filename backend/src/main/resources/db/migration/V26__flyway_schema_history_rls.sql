-- Supabase linter: "rls_disabled_in_public" — flyway_schema_history (creada por Flyway
-- mismo, no por nosotros) quedaba expuesta vía la API REST con la clave anónima, sin RLS.
-- Sin políticas a propósito: RLS habilitada + cero políticas deniega todo a anon/
-- authenticated (mismo comportamiento que ya usan el resto de las tablas para roles sin
-- política), mientras que Flyway sigue escribiendo acá sin problema porque el usuario de
-- migraciones es el dueño de la tabla y los dueños no quedan sujetos a RLS por defecto.
alter table flyway_schema_history enable row level security;
