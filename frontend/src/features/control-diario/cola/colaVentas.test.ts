// jsdom no implementa IndexedDB — este polyfill se importa solo acá, no en el setup
// global de Vitest (plan-fase3.md, Bloque 6), para no afectar al resto de los tests.
import 'fake-indexeddb/auto'
import { beforeEach, describe, expect, it } from 'vitest'
import { agregar, contar, eliminar, listar } from './colaVentas'
import type { EscaneoPendiente } from './colaVentas'

function escaneo(idClienteLocal: string, creadoEn: string): EscaneoPendiente {
  return {
    idClienteLocal,
    codigo: '2000012012501',
    decodificado: { corteId: 'corte-1', corteNombre: 'Vacío', kg: 1.25 },
    creadoEn,
  }
}

// Sin esto, fake-indexeddb conserva la base entre tests del mismo archivo (vive en el
// mismo contexto de JS mientras dure el archivo).
beforeEach(() => {
  return new Promise<void>((resolve, reject) => {
    const pedido = indexedDB.deleteDatabase('carniceria-cola-ventas')
    pedido.onsuccess = () => resolve()
    pedido.onerror = () => reject(pedido.error)
    pedido.onblocked = () => resolve()
  })
})

describe('colaVentas', () => {
  it('agrega un escaneo pendiente y lo puede listar', async () => {
    await agregar(escaneo('a', '2026-10-05T10:00:00.000Z'))

    const pendientes = await listar()

    expect(pendientes).toHaveLength(1)
    expect(pendientes[0].idClienteLocal).toBe('a')
    expect(pendientes[0].decodificado.corteNombre).toBe('Vacío')
  })

  it('lista en orden de creación', async () => {
    await agregar(escaneo('segundo', '2026-10-05T10:01:00.000Z'))
    await agregar(escaneo('primero', '2026-10-05T10:00:00.000Z'))

    const pendientes = await listar()

    expect(pendientes.map((p) => p.idClienteLocal)).toEqual(['primero', 'segundo'])
  })

  it('elimina por idClienteLocal', async () => {
    await agregar(escaneo('a', '2026-10-05T10:00:00.000Z'))
    await agregar(escaneo('b', '2026-10-05T10:01:00.000Z'))

    await eliminar('a')
    const pendientes = await listar()

    expect(pendientes.map((p) => p.idClienteLocal)).toEqual(['b'])
  })

  it('cuenta refleja cuántos quedan', async () => {
    expect(await contar()).toBe(0)

    await agregar(escaneo('a', '2026-10-05T10:00:00.000Z'))
    expect(await contar()).toBe(1)

    await agregar(escaneo('b', '2026-10-05T10:01:00.000Z'))
    expect(await contar()).toBe(2)

    await eliminar('a')
    expect(await contar()).toBe(1)
  })

  it('agregar dos veces el mismo idClienteLocal no duplica', async () => {
    await agregar(escaneo('a', '2026-10-05T10:00:00.000Z'))
    await agregar(escaneo('a', '2026-10-05T10:00:00.000Z'))

    expect(await contar()).toBe(1)
  })
})
