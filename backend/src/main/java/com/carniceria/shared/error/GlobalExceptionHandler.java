package com.carniceria.shared.error;

import java.sql.SQLException;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

	// RLS (Postgres) deniega un INSERT/UPDATE con SQLSTATE 42501. Es la autoridad real
	// sobre los datos (constitution.md, Principio II): si llega hasta acá sin que
	// ninguna validación de negocio lo haya anticipado, el pedido no tenía permiso.
	private static final String SQLSTATE_RLS_DENEGADO = "42501";

	@ExceptionHandler(ApiException.class)
	public ResponseEntity<ErrorResponse> handleApiException(ApiException ex) {
		return ResponseEntity.status(ex.getStatus()).body(new ErrorResponse(ex.getCodigo(), ex.getMessage()));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> handleValidacion(MethodArgumentNotValidException ex) {
		String mensaje = ex.getBindingResult().getFieldErrors().stream()
				.findFirst()
				.map(error -> error.getField() + ": " + error.getDefaultMessage())
				.orElse("Los datos enviados no son válidos.");
		return ResponseEntity.badRequest().body(new ErrorResponse("DATOS_INVALIDOS", mensaje));
	}

	@ExceptionHandler(DataAccessException.class)
	public ResponseEntity<ErrorResponse> handleDataAccessException(DataAccessException ex) {
		Throwable raiz = ex.getRootCause();
		if (raiz instanceof SQLException sqlException && SQLSTATE_RLS_DENEGADO.equals(sqlException.getSQLState())) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN)
					.body(new ErrorResponse("ACCESO_DENEGADO", "No tenés permiso para hacer esto."));
		}
		return handleUnexpected(ex);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(new ErrorResponse("ERROR_INTERNO", "Ocurrió un error inesperado. Intentá de nuevo."));
	}
}
