package com.carniceria.cortes.service;

import com.carniceria.cortes.dto.CorteRequest;
import com.carniceria.cortes.dto.CorteResponse;
import com.carniceria.cortes.entity.CorteEntity;
import com.carniceria.cortes.entity.CorteEntity.Cuarto;
import com.carniceria.cortes.repository.CorteRepository;
import com.carniceria.shared.error.AccesoDenegadoException;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CorteService {

	private final CorteRepository corteRepository;

	public CorteService(CorteRepository corteRepository) {
		this.corteRepository = corteRepository;
	}

	@Transactional
	public List<CorteResponse> listar(boolean incluirInactivos) {
		List<CorteEntity> cortes = incluirInactivos
				? corteRepository.findAllByOrderByNombreAsc()
				: corteRepository.findByActivoTrueOrderByNombreAsc();
		return cortes.stream().map(CorteResponse::de).toList();
	}

	@Transactional
	public CorteResponse crear(CorteRequest request, UUID duenoId) {
		Cuarto cuarto = parsearCuarto(request.cuarto());
		// RLS ya scopea findByPlu al propio negocio: dos dueños distintos pueden usar el
		// mismo PLU sin pisarse (V10__multi_negocio.sql, unique(dueno_id, plu)).
		corteRepository.findByPlu(request.plu()).ifPresent(existente -> {
			throw new PluDuplicadoException(request.plu());
		});

		CorteEntity corte = new CorteEntity(request.nombre(), request.plu(), cuarto, request.zonaMapa(), true, duenoId);
		corteRepository.save(corte);
		// Flush explícito: el id se genera en memoria (GenerationType.UUID), así que
		// Hibernate puede diferir el INSERT real hasta el próximo flush — sin esto, un
		// INSERT que RLS debería rechazar (ej. alguien sin permiso de escritura) recién
		// fallaría en otro momento, no acá donde se lo puede traducir a un error claro.
		corteRepository.flush();
		return CorteResponse.de(corte);
	}

	@Transactional
	public CorteResponse actualizar(UUID id, CorteRequest request) {
		Cuarto cuarto = parsearCuarto(request.cuarto());
		boolean activo = request.activo() == null || request.activo();

		corteRepository.findByPlu(request.plu())
				.filter(otro -> !otro.getId().equals(id))
				.ifPresent(otro -> {
					throw new PluDuplicadoException(request.plu());
				});

		// UPDATE explícito (no mutar la entidad): si no, un empleado que solo puede
		// LEER un corte activo recibiría un 200 "éxito" sin que RLS haya cambiado nada
		// de verdad en la base — ver el comentario en CorteRepository.actualizar(...).
		int filasActualizadas = corteRepository.actualizar(
				id, request.nombre(), request.plu(), cuarto, request.zonaMapa(), activo);
		if (filasActualizadas == 0) {
			if (!corteRepository.existsById(id)) {
				throw new CorteNoEncontradoException(id);
			}
			throw new AccesoDenegadoException();
		}
		return corteRepository.findById(id).map(CorteResponse::de)
				.orElseThrow(() -> new CorteNoEncontradoException(id));
	}

	private Cuarto parsearCuarto(String valor) {
		try {
			return Cuarto.valueOf(valor);
		} catch (IllegalArgumentException e) {
			throw new CuartoInvalidoException(valor);
		}
	}
}
