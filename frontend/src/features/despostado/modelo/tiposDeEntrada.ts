// Fase 6, FR-601/FR-602: qué parte de la media res llegó como entrada. El mapeo a los
// cortes del catálogo que cada uno habilita vive solo acá (frontend) — el backend no lo
// valida, es nada más una restricción de interfaz (ver fase6/tasks-fase6.md).

// AchurasEmbutidos/Cerdo/Carne (Fase 7) no van en TIPOS_DE_ENTRADA ni en CORTES_POR_TIPO:
// no se ofrecen en este selector — los pone el modal "Añadir stock" de Control diario por
// su cuenta (tasks-fase7.md). Están en el type union solo para que ese modal pueda tipar
// el tipoEntrada que manda en el POST reusando este mismo tipo.
export type TipoEntrada =
  | 'MediaRes'
  | 'Delantero'
  | 'Pecho'
  | 'Parrillero'
  | 'AsadoCompleto'
  | 'Mocho'
  | 'Rueda'
  | 'AchurasEmbutidos'
  | 'Cerdo'
  | 'Carne'

export const TIPOS_DE_ENTRADA: { valor: TipoEntrada; etiqueta: string }[] = [
  { valor: 'MediaRes', etiqueta: 'Media res' },
  { valor: 'Delantero', etiqueta: 'Delantero' },
  { valor: 'Pecho', etiqueta: 'Pecho' },
  { valor: 'Parrillero', etiqueta: 'Parrillero' },
  { valor: 'AsadoCompleto', etiqueta: 'Asado completo' },
  { valor: 'Mocho', etiqueta: 'Mocho' },
  { valor: 'Rueda', etiqueta: 'Rueda' },
]

// Nombres tal como están en el catálogo (CatalogoInicialService). "Delantero" (tipo de
// entrada comercial) no coincide con cortes.cuarto = 'Delantero' a propósito — son
// clasificaciones independientes, ver la nota en tasks-fase6.md.
const MOCHO = ['Nalga', 'Cuadrada', 'Peceto', 'Bola de lomo', 'Cuadril', 'Tortuga', 'Osobuco']

const CORTES_POR_TIPO: Partial<Record<TipoEntrada, string[]>> = {
  Delantero: ['Paleta', 'Roast beef', 'Matambre', 'Asado', 'Vacío', 'Falda', 'Cogote'],
  Pecho: ['Paleta', 'Roast beef', 'Cogote', 'Falda'],
  Parrillero: ['Asado', 'Falda', 'Vacío', 'Bife ancho', 'Bife angosto', 'Lomo'],
  AsadoCompleto: ['Asado', 'Vacío', 'Matambre'],
  Mocho: MOCHO,
  Rueda: MOCHO.filter((corte) => corte !== 'Cuadril'),
}

/** `null` = sin restricción (Media res). Cualquier otro tipo devuelve el set de nombres habilitados. */
export function cortesHabilitados(tipo: TipoEntrada): Set<string> | null {
  const cortes = CORTES_POR_TIPO[tipo]
  return cortes ? new Set(cortes) : null
}
