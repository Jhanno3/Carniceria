import { useEffect } from 'react'

interface ModalProps {
  titulo: string
  children: React.ReactNode
  onCerrar: () => void
}

export function Modal({ titulo, children, onCerrar }: ModalProps) {
  useEffect(() => {
    function alEscapar(evento: KeyboardEvent) {
      if (evento.key === 'Escape') onCerrar()
    }
    document.addEventListener('keydown', alEscapar)
    return () => document.removeEventListener('keydown', alEscapar)
  }, [onCerrar])

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-texto/40 p-4"
      onClick={onCerrar}
    >
      <div
        role="dialog"
        aria-modal="true"
        aria-labelledby="modal-titulo"
        onClick={(e) => e.stopPropagation()}
        className="flex w-full max-w-sm flex-col gap-4 rounded-xl border border-borde bg-fondo p-6 shadow-lg"
      >
        <h2 id="modal-titulo" className="text-titulo-seccion font-titulos font-bold text-texto">
          {titulo}
        </h2>
        {children}
      </div>
    </div>
  )
}
