package com.carniceria.cortes.service;

import com.carniceria.cortes.dto.CorteRequest;
import com.carniceria.cortes.dto.CorteResponse;
import com.carniceria.cortes.entity.CorteEntity;
import com.carniceria.cortes.entity.CorteEntity.Cuarto;
import com.carniceria.cortes.entity.CorteEntity.TipoProducto;
import com.carniceria.cortes.repository.CorteRepository;
import com.carniceria.shared.BigDecimals;
import com.carniceria.shared.error.AccesoDenegadoException;
import java.math.BigDecimal;
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
		TipoProducto tipoProducto = parsearTipoProducto(request.tipoProducto());
		Cuarto cuarto = parsearCuarto(request.cuarto(), tipoProducto);
		// RLS ya scopea findByPlu al propio negocio: dos dueños distintos pueden usar el
		// mismo PLU sin pisarse (V10__multi_negocio.sql, unique(dueno_id, plu)).
		corteRepository.findByPlu(request.plu()).ifPresent(existente -> {
			throw new PluDuplicadoException(request.plu());
		});

		BigDecimal precioVenta = BigDecimals.parse(request.precioVenta());
		CorteEntity corte = new CorteEntity(
				request.nombre(), request.plu(), cuarto, tipoProducto, request.zonaMapa(), true, precioVenta,
				duenoId);
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
		TipoProducto tipoProducto = parsearTipoProducto(request.tipoProducto());
		Cuarto cuarto = parsearCuarto(request.cuarto(), tipoProducto);
		boolean activo = request.activo() == null || request.activo();
		BigDecimal precioVenta = BigDecimals.parse(request.precioVenta());

		corteRepository.findByPlu(request.plu())
				.filter(otro -> !otro.getId().equals(id))
				.ifPresent(otro -> {
					throw new PluDuplicadoException(request.plu());
				});

		// UPDATE explícito (no mutar la entidad): si no, un empleado que solo puede
		// LEER un corte activo recibiría un 200 "éxito" sin que RLS haya cambiado nada
		// de verdad en la base — ver el comentario en CorteRepository.actualizar(...).
		int filasActualizadas = corteRepository.actualizar(
				id, request.nombre(), request.plu(), cuarto, tipoProducto, request.zonaMapa(), activo, precioVenta);
		if (filasActualizadas == 0) {
			if (!corteRepository.existsById(id)) {
				throw new CorteNoEncontradoException(id);
			}
			throw new AccesoDenegadoException();
		}
		return corteRepository.findById(id).map(CorteResponse::de)
				.orElseThrow(() -> new CorteNoEncontradoException(id));
	}

	private TipoProducto parsearTipoProducto(String valor) {
		if (valor == null || valor.isBlank()) {
			return TipoProducto.Vacuno;
		}
		try {
			return TipoProducto.valueOf(valor);
		} catch (IllegalArgumentException e) {
			throw new TipoProductoInvalidoException(valor);
		}
	}

	/** {@code cuarto} es obligatorio solo para "Vacuno" (clasificación anatómica de la media
	 * res); para los demás tipos no aplica, y mandarlo igual es un error (Fase 7). */
	private Cuarto parsearCuarto(String valor, TipoProducto tipoProducto) {
		boolean vacio = valor == null || valor.isBlank();
		if (tipoProducto != TipoProducto.Vacuno) {
			if (!vacio) {
				throw new CuartoNoAplicaException(tipoProducto);
			}
			return null;
		}
		if (vacio) {
			// Enum.valueOf(null) tira NullPointerException, no IllegalArgumentException —
			// hay que cortar acá antes de llegar al valueOf de abajo.
			throw new CuartoInvalidoException(valor);
		}
		try {
			return Cuarto.valueOf(valor);
		} catch (IllegalArgumentException e) {
			throw new CuartoInvalidoException(valor);
		}
	}
}
