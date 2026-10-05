import { FormularioConfigEtiqueta } from './components/FormularioConfigEtiqueta'

export function AjustesPage() {
  return (
    <main className="mx-auto max-w-2xl p-4">
      <h1 className="mb-4 text-titulo-seccion font-titulos font-bold text-texto">Ajustes</h1>
      <p className="mb-4 text-cuerpo text-texto-secundario">
        Configuración de la etiqueta de la balanza (FR-209): cómo decodificar el código de barras que imprime tu
        balanza.
      </p>
      <FormularioConfigEtiqueta />
    </main>
  )
}
