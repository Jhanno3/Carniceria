import { openDB, type DBSchema } from 'idb'

// Cola local de escaneos sin conexión (plan-fase3.md, 3.2). Vive enteramente en el
// navegador del dispositivo que escanea — Postgres nunca la ve directamente, cumple el
// mismo rol que una tabla de "staging" pendiente de subir (ver data-model-fase2.md,
// "Agregado de Fase 3"). Funciones puras async, sin React: los hooks del Bloque 7 solo
// las invocan.
export interface EscaneoPendiente {
  idClienteLocal: string
  codigo: string
  // Resultado ya resuelto del decodificador local (modelo/decodificadorEtiqueta.ts), para
  // no tener que volver a decodificar al dibujar la lista de pendientes.
  decodificado: { corteId: string; corteNombre: string; kg: number }
  // ISO, reloj del dispositivo — solo para ordenar la sincronización (Bloque 7). Nunca
  // viaja al backend ni reemplaza a fecha_hora del servidor.
  creadoEn: string
}

const NOMBRE_DB = 'carniceria-cola-ventas'
const NOMBRE_STORE = 'escaneos-pendientes'

interface ColaVentasDB extends DBSchema {
  'escaneos-pendientes': {
    key: string
    value: EscaneoPendiente
  }
}

function abrir() {
  return openDB<ColaVentasDB>(NOMBRE_DB, 1, {
    upgrade(db) {
      db.createObjectStore(NOMBRE_STORE, { keyPath: 'idClienteLocal' })
    },
  })
}

// Cada función abre su propia conexión y la cierra al terminar (en vez de cachear una
// sola para todo el módulo): la cola se usa con poca frecuencia, así que no vale la pena
// la complejidad de mantener una conexión viva — y dejarla abierta bloquea sin avisar un
// `indexedDB.deleteDatabase(...)` posterior (p. ej. en los tests).
async function conLaBaseAbierta<T>(trabajo: (db: Awaited<ReturnType<typeof abrir>>) => Promise<T>): Promise<T> {
  const db = await abrir()
  try {
    return await trabajo(db)
  } finally {
    db.close()
  }
}

// `put`, no `add`: agregar el mismo idClienteLocal dos veces no duplica ni tira error,
// simplemente lo deja como estaba (mismo criterio de idempotencia que la unicidad de
// id_cliente_local en el backend, V13__ventas.sql).
export function agregar(escaneo: EscaneoPendiente): Promise<string> {
  return conLaBaseAbierta((db) => db.put(NOMBRE_STORE, escaneo))
}

export function listar(): Promise<EscaneoPendiente[]> {
  return conLaBaseAbierta(async (db) => {
    const pendientes = await db.getAll(NOMBRE_STORE)
    return pendientes.sort((a, b) => a.creadoEn.localeCompare(b.creadoEn))
  })
}

export function eliminar(idClienteLocal: string): Promise<void> {
  return conLaBaseAbierta((db) => db.delete(NOMBRE_STORE, idClienteLocal))
}

export function contar(): Promise<number> {
  return conLaBaseAbierta((db) => db.count(NOMBRE_STORE))
}
