// Dígito verificador EAN-13, algoritmo GS1 estándar — espejo de
// backend/.../escaneo/modelo/Ean13.java (plan-fase3.md, 3.1). Sobre los primeros 12
// dígitos, los de posición impar (1ª, 3ª, ...) se suman tal cual y los de posición par
// ×3; el dígito verificador es lo que le falta a esa suma para llegar al siguiente
// múltiplo de 10.

const SOLO_DIGITOS = /^\d+$/

export function validarDigitoVerificador(codigo: string | null | undefined): boolean {
  if (codigo == null || codigo.length !== 13 || !SOLO_DIGITOS.test(codigo)) {
    return false
  }
  let suma = 0
  for (let i = 0; i < 12; i++) {
    const digito = codigo.charCodeAt(i) - 48
    suma += i % 2 === 0 ? digito : digito * 3
  }
  const esperado = (10 - (suma % 10)) % 10
  const real = codigo.charCodeAt(12) - 48
  return esperado === real
}
