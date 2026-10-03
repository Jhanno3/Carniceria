package com.carniceria.perfiles.service;

import com.carniceria.cortes.service.CatalogoInicialService;
import com.carniceria.escaneo.service.ConfigEtiquetaInicialService;
import com.carniceria.perfiles.dto.PerfilActualizarRequest;
import com.carniceria.perfiles.dto.PerfilResponse;
import com.carniceria.perfiles.entity.PerfilEntity;
import com.carniceria.perfiles.entity.PerfilEntity.Estado;
import com.carniceria.perfiles.entity.PerfilEntity.Rol;
import com.carniceria.perfiles.repository.PerfilRepository;
import com.carniceria.shared.error.AccesoDenegadoException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PerfilService {

	private final PerfilRepository perfilRepository;
	private final CatalogoInicialService catalogoInicialService;
	private final ConfigEtiquetaInicialService configEtiquetaInicialService;

	public PerfilService(PerfilRepository perfilRepository, CatalogoInicialService catalogoInicialService,
			ConfigEtiquetaInicialService configEtiquetaInicialService) {
		this.perfilRepository = perfilRepository;
		this.catalogoInicialService = catalogoInicialService;
		this.configEtiquetaInicialService = configEtiquetaInicialService;
	}

	/**
	 * Registro con aprobación previa: la cuenta de Supabase Auth ya existe (la creó el
	 * propio frontend con supabase.auth.signUp), pero el perfil recién se crea la
	 * primera vez que esa persona le pega a la API. Un "dueno" (directo, o invitado con
	 * código y auto-aprobado como "empleado" — ver {@code crearDesdeMetadata}) se siembra
	 * acá mismo su catálogo de cortes si todavía no tiene ninguno, bajo su propia sesión
	 * (V10__multi_negocio.sql: sembrar "para otro" desde otra sesión no pasa RLS).
	 */
	@Transactional
	public PerfilResponse obtenerOCrearPropio(Jwt jwt) {
		UUID id = UUID.fromString(jwt.getSubject());
		PerfilEntity perfil = perfilRepository.findById(id)
				.orElseGet(() -> crearDesdeMetadata(id, jwt));

		// "Dueño de sí mismo" (dueno_id == su propio id), no "rol == dueno" a secas: un
		// admin puede también operar su propio negocio de prueba (dueno_id fijado a mano
		// para esta cuenta) y merece el mismo catálogo inicial que cualquier dueño nuevo.
		if (id.equals(perfil.getDuenoId())) {
			catalogoInicialService.sembrarSiHaceFalta(id);
			configEtiquetaInicialService.sembrarSiHaceFalta(id);
		}
		return PerfilResponse.de(perfil);
	}

	@Transactional
	public List<PerfilResponse> listarPorEstado(String estadoTexto) {
		Estado estado = parsearEstado(estadoTexto);
		return perfilRepository.findByEstadoOrderByNombreAsc(estado).stream()
				.map(PerfilResponse::de)
				.toList();
	}

	/** Solo admin (RLS): aprueba/rechaza cuentas de dueño, o cambia el rol de cualquiera. */
	@Transactional
	public PerfilResponse actualizar(UUID id, PerfilActualizarRequest request) {
		Rol rol = parsearRol(request.rol());
		Estado estado = parsearEstado(request.estado());

		// dueno_id según el rol destino (V10__multi_negocio.sql): un "dueno" es dueño de sí
		// mismo; "admin" no opera ningún negocio; "empleado" no se toca acá — el suyo ya
		// quedó fijado al registrarse con el código de invitación.
		int filasActualizadas = switch (rol) {
			case admin -> perfilRepository.actualizarRolEstadoYDuenoId(id, rol, estado, null);
			case dueno -> perfilRepository.actualizarRolEstadoYDuenoId(id, rol, estado, id);
			case empleado -> perfilRepository.actualizarRolYEstado(id, rol, estado);
		};
		if (filasActualizadas == 0) {
			// Existe pero RLS no deja escribirlo (ej. uno mismo, sin ser admin) → 403,
			// no 404. Si ni siquiera existe, sí es 404 (no revela más de lo que ya podía verse).
			if (!perfilRepository.existsById(id)) {
				throw new PerfilNoEncontradoException(id);
			}
			throw new AccesoDenegadoException();
		}
		return perfilRepository.findById(id).map(PerfilResponse::de)
				.orElseThrow(() -> new PerfilNoEncontradoException(id));
	}

	private PerfilEntity crearDesdeMetadata(UUID id, Jwt jwt) {
		Map<String, Object> metadata = jwt.getClaimAsMap("user_metadata");
		String nombre = metadata == null ? null : (String) metadata.get("nombre");
		Object rolSolicitado = metadata == null ? null : metadata.get("rol_solicitado");
		Rol rol = rolSolicitado instanceof String texto ? parsearRolOEmpleado(texto) : Rol.empleado;

		if (rol == Rol.empleado) {
			UUID duenoInvitadorId = extraerUuid(metadata, "dueno_invitador_id");
			if (duenoInvitadorId != null && perfilRepository.esDuenoValido(duenoInvitadorId)) {
				// Invitación válida: el dueño ya lo vouch-eó al invitarlo, entra aprobado
				// directo — ni admin ni el dueño tienen que aprobar un segundo paso.
				PerfilEntity perfil = new PerfilEntity(id, nombre, Rol.empleado, Estado.aprobado);
				perfil.setDuenoId(duenoInvitadorId);
				return perfilRepository.save(perfil);
			}
			// Sin invitación válida: no hay forma de saber a qué negocio pertenece. Caso
			// borde raro (no hay flujo de registro de empleado sin link de invitación en el
			// frontend) — queda pendiente y sin dueno_id, alguien tiene que resolverlo a mano.
		}

		PerfilEntity perfil = new PerfilEntity(id, nombre, rol, Estado.pendiente);
		return perfilRepository.save(perfil);
	}

	/**
	 * `rol_solicitado` lo manda quien se registra (es `user_metadata` de Supabase Auth,
	 * controlado por el cliente) — nunca puede resultar en `admin`, solo en los dos roles
	 * que el formulario de registro ofrece. Cualquier otro valor (incluido "admin" o
	 * basura) cae a `empleado`, el nivel sin privilegios. Promover a alguien a `admin` es
	 * una acción explícita de otro `admin` vía `PUT /perfiles/{id}` (`parsearRol`), nunca
	 * algo que uno mismo pueda pedir.
	 */
	private Rol parsearRolOEmpleado(String valor) {
		return "dueno".equals(valor) ? Rol.dueno : Rol.empleado;
	}

	private UUID extraerUuid(Map<String, Object> metadata, String clave) {
		Object valor = metadata == null ? null : metadata.get(clave);
		try {
			return valor instanceof String texto ? UUID.fromString(texto) : null;
		} catch (IllegalArgumentException e) {
			return null;
		}
	}

	private Rol parsearRol(String valor) {
		try {
			return Rol.valueOf(valor);
		} catch (IllegalArgumentException e) {
			throw new ValorInvalidoException("rol", valor);
		}
	}

	private Estado parsearEstado(String valor) {
		try {
			return Estado.valueOf(valor);
		} catch (IllegalArgumentException e) {
			throw new ValorInvalidoException("estado", valor);
		}
	}
}
