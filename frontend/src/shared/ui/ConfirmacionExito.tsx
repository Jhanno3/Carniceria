interface ConfirmacionExitoProps {
  mensaje: string
}

/** Tilde verde con un pop chico (nunca el único indicador: siempre va con texto, sección 5). */
export function ConfirmacionExito({ mensaje }: ConfirmacionExitoProps) {
  return (
    <div className="flex flex-col items-center gap-3 py-4">
      <span className="animate-pop-in flex h-14 w-14 items-center justify-center rounded-full bg-exito-fondo">
        <svg
          viewBox="0 0 24 24"
          width="28"
          height="28"
          fill="none"
          stroke="currentColor"
          strokeWidth="2.5"
          strokeLinecap="round"
          strokeLinejoin="round"
          className="text-exito-texto"
          aria-hidden="true"
        >
          <path d="M5 13l5 5L19 7" />
        </svg>
      </span>
      <p className="text-cuerpo font-medium text-exito-texto">{mensaje}</p>
    </div>
  )
}
