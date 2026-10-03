package com.carniceria.perfiles.service;

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

	public PerfilService(PerfilRepository perfilRepository) {
		this.perfilRepository = perfilRepository;
	}

	/**
	 * Registro con aprobación previa: la cuenta de Supabase Auth ya existe (la creó el
	 * propio frontend con supabase.auth.signUp), pero el perfil recién se crea la
	 * primera vez que esa persona le pega a la API — siempre en estado 'pendiente',
	 * sin que ella pueda elegir otra cosa.
	 */
	@Transactional
	public PerfilResponse obtenerOCrearPropio(Jwt jwt) {
		UUID id = UUID.fromString(jwt.getSubject());
		return perfilRepository.findById(id)
				.map(PerfilResponse::de)
				.orElseGet(() -> PerfilResponse.de(crearDesdeMetadata(id, jwt)));
	}

	@Transactional
	public List<PerfilResponse> listarPorEstado(String estadoTexto) {
		Estado estado = parsearEstado(estadoTexto);
		return perfilRepository.findByEstadoOrderByNombreAsc(estado).stream()
				.map(PerfilResponse::de)
				.toList();
	}

	@Transactional
	public PerfilResponse actualizar(UUID id, PerfilActualizarRequest request) {
		Rol rol = parsearRol(request.rol());
		Estado estado = parsearEstado(request.estado());

		int filasActualizadas = perfilRepository.actualizarRolYEstado(id, rol, estado);
		if (filasActualizadas == 0) {
			// Existe pero RLS no deja escribirlo (ej. uno mismo, sin ser dueño) → 403,
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

		PerfilEntity perfil = new PerfilEntity(id, nombre, rol, Estado.pendiente);
		return perfilRepository.save(perfil);
	}

	private Rol parsearRolOEmpleado(String valor) {
		try {
			return Rol.valueOf(valor);
		} catch (IllegalArgumentException e) {
			return Rol.empleado;
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
