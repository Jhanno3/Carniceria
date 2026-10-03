-- Hasta acá "dueno" era el único nivel con acceso total. Se divide en dos: "admin"
-- (gestiona cuentas, además de todo lo que tenía "dueno") y "dueno" pasa a ser un nivel
-- intermedio para socios/ayudantes con acceso operativo (Despostado, Inicio) pero sin
-- poder tocar usuarios — a pedido del dueño real del negocio, que quiere que "Usuarios"
-- se vea únicamente en su propia cuenta.
alter table perfiles drop constraint perfiles_rol_check;
alter table perfiles add constraint perfiles_rol_check check (rol in ('admin', 'dueno', 'empleado'));

-- Los dueños aprobados existentes eran, hasta ahora, los únicos con acceso total: pasan
-- al nivel nuevo más alto para no perder nada de lo que ya podían hacer. De acá en más,
-- "dueno" es un rol más acotado (ver PerfilService: nunca se auto-asigna "admin" al
-- registrarse, eso lo asigna otro admin a mano).
update perfiles set rol = 'admin' where rol = 'dueno' and estado = 'aprobado';

-- is_dueno() ahora es "tiene acceso operativo completo" (admin incluye todo lo de dueno).
create or replace function is_dueno()
returns boolean
language sql
security definer
set search_path = public
stable
as $$
  select exists (
    select 1 from perfiles where id = auth.uid() and rol in ('dueno', 'admin') and estado = 'aprobado'
  );
$$;

-- is_admin() es el nivel nuevo, exclusivo: gestión de cuentas (ver más abajo).
create or replace function is_admin()
returns boolean
language sql
security definer
set search_path = public
stable
as $$
  select exists (
    select 1 from perfiles where id = auth.uid() and rol = 'admin' and estado = 'aprobado'
  );
$$;

-- Gestionar perfiles (aprobar/rechazar/cambiar rol) pasa a ser exclusivo de "admin": un
-- "dueno" ya no entra por este policy aunque is_dueno() le siga dando acceso al resto
-- de las tablas (cortes, medias_reses, despostado, perdidas).
drop policy "perfiles_dueno_todo" on perfiles;
create policy "perfiles_admin_todo" on perfiles
  for all
  using (is_admin())
  with check (is_admin());
