package com.opstrack.common.exception;

import com.opstrack.auth.exception.EmailAlreadyExistsException;
import com.opstrack.task.exception.TaskNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.security.core.AuthenticationException;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(TaskNotFoundException.class)
	public ResponseEntity<ApiErrorResponse> handleTaskNotFound(
			TaskNotFoundException exception,
			HttpServletRequest request
	) {
		return buildError(
				HttpStatus.NOT_FOUND,
				exception.getMessage(),
				request.getRequestURI(),
				Map.of()
		);
	}

	@ExceptionHandler(EmailAlreadyExistsException.class)
	public ResponseEntity<ApiErrorResponse> handleEmailAlreadyExists(
			EmailAlreadyExistsException exception,
			HttpServletRequest request
	) {
		return buildError(
				HttpStatus.CONFLICT,
				exception.getMessage(),
				request.getRequestURI(),
				Map.of()
		);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiErrorResponse> handleValidation(
			MethodArgumentNotValidException exception,
			HttpServletRequest request
	) {
		Map<String, String> validationErrors = new LinkedHashMap<>();

		exception.getBindingResult()
				.getFieldErrors()
				.forEach(error -> validationErrors.put(
						error.getField(),
						error.getDefaultMessage()
				));

		return buildError(
				HttpStatus.BAD_REQUEST,
				"Request validation failed",
				request.getRequestURI(),
				validationErrors
		);
	}

	@ExceptionHandler(AuthenticationException.class)
	public ResponseEntity<ApiErrorResponse> handleAuthentication(
			AuthenticationException exception,
			HttpServletRequest request
	) {
		return buildError(
				HttpStatus.UNAUTHORIZED,
				"Invalid email or password",
				request.getRequestURI(),
				Map.of()
		);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiErrorResponse> handleUnexpected(
			Exception exception,
			HttpServletRequest request
	) {
		log.error(
				"Unexpected error while processing request: {}",
				request.getRequestURI(),
				exception
		);

		return buildError(
				HttpStatus.INTERNAL_SERVER_ERROR,
				"Unexpected server error",
				request.getRequestURI(),
				Map.of()
		);
	}

	private ResponseEntity<ApiErrorResponse> buildError(
			HttpStatus status,
			String message,
			String path,
			Map<String, String> validationErrors
	) {
		ApiErrorResponse body = new ApiErrorResponse(
				OffsetDateTime.now(),
				status.value(),
				status.getReasonPhrase(),
				message,
				path,
				validationErrors
		);

		return ResponseEntity
				.status(status)
				.body(body);
	}
}