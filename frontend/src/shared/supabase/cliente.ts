import { createClient } from '@supabase/supabase-js'

// Única credencial de Supabase permitida en el cliente (constitution.md, Principio II).
// Se usa solo para login/sesión — todo lo demás pasa por la API propia (shared/api/apiFetch.ts).
export const supabase = createClient(
  import.meta.env.VITE_SUPABASE_URL,
  import.meta.env.VITE_SUPABASE_ANON_KEY,
)
