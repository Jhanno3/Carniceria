-- Registro con aprobación previa: cualquiera puede crear una cuenta de Supabase Auth,
-- pero no tiene ningún acceso hasta que el dueño la aprueba.
alter table perfiles
  add column estado text not null default 'pendiente'
    check (estado in ('pendiente', 'aprobado', 'rechazado'));

-- El/los dueño(s) ya sembrados a mano (Bloque 1) no pueden quedar bloqueados por esto.
update perfiles set estado = 'aprobado' where rol = 'dueno';

-- is_dueno() ahora exige estado = 'aprobado', no solo rol = 'dueno' — si no, alguien
-- podría autoaprobarse pidiendo rol "dueno" en el registro.
create or replace function is_dueno()
returns boolean
language sql
security definer
set search_path = public
stable
as $$
  select exists (
    select 1 from perfiles where id = auth.uid() and rol = 'dueno' and estado = 'aprobado'
  );
$$;

-- Cualquier usuario autenticado puede crear SU PROPIA fila (nunca la de otro) la
-- primera vez que entra — siempre nace en estado 'pendiente' (lo fuerza el backend,
-- no esta política; ver PerfilService).
create policy "perfiles_insert_propio" on perfiles
  for insert
  with check (id = auth.uid());

-- El dueño ve y administra todas las cuentas (para aprobar/rechazar).
create policy "perfiles_dueno_todo" on perfiles
  for all
  using (is_dueno())
  with check (is_dueno());
