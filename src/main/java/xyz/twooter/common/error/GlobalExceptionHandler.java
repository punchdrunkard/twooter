package xyz.twooter.common.error;

import java.nio.file.AccessDeniedException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import lombok.extern.slf4j.Slf4j;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

	@ExceptionHandler(MethodArgumentNotValidException.class)
	protected ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
		return buildErrorResponse(e, ErrorCode.INVALID_INPUT_VALUE, e.getBindingResult(), HttpStatus.BAD_REQUEST);
	}

	@ExceptionHandler(BindException.class)
	protected ResponseEntity<ErrorResponse> handleBindException(BindException e) {
		return buildErrorResponse(e, ErrorCode.INVALID_INPUT_VALUE, e.getBindingResult(), HttpStatus.BAD_REQUEST);
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	protected ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatchException(
		MethodArgumentTypeMismatchException e) {
		return buildErrorResponse(e, ErrorCode.INVALID_TYPE_VALUE, e, HttpStatus.BAD_REQUEST);
	}

	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	protected ResponseEntity<ErrorResponse> handleHttpRequestMethodNotSupportedException(
		HttpRequestMethodNotSupportedException e) {
		return buildErrorResponse(e, ErrorCode.METHOD_NOT_ALLOWED, HttpStatus.METHOD_NOT_ALLOWED);
	}

	@ExceptionHandler(AccessDeniedException.class)
	protected ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException e) {
		return buildErrorResponse(e, ErrorCode.HANDLE_ACCESS_DENIED,
			HttpStatus.valueOf(ErrorCode.HANDLE_ACCESS_DENIED.getStatus()));
	}

	@ExceptionHandler(BusinessException.class)
	protected ResponseEntity<ErrorResponse> handleBusinessException(BusinessException e) {
		return buildErrorResponse(e, e.getErrorCode(), HttpStatus.valueOf(e.getErrorCode().getStatus()));
	}

	@ExceptionHandler(Exception.class)
	protected ResponseEntity<ErrorResponse> handleException(Exception e) {
		return buildErrorResponse(e, ErrorCode.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
	}

	// Build ErrorResponse
	private ResponseEntity<ErrorResponse> buildErrorResponse(Exception e, ErrorCode errorCode, HttpStatus status) {
		log.error("Exception: ", e);
		final ErrorResponse response = ErrorResponse.of(errorCode);
		return new ResponseEntity<>(response, status);
	}

	private ResponseEntity<ErrorResponse> buildErrorResponse(Exception e, ErrorCode errorCode,
		BindingResult bindingResult, HttpStatus status) {
		log.error("Exception: ", e);
		final ErrorResponse response = ErrorResponse.of(errorCode, bindingResult);
		return new ResponseEntity<>(response, status);
	}

	private ResponseEntity<ErrorResponse> buildErrorResponse(Exception e, ErrorCode errorCode,
		MethodArgumentTypeMismatchException ex, HttpStatus status) {
		log.error("Exception: ", e);
		final ErrorResponse response = ErrorResponse.of(ex);
		return new ResponseEntity<>(response, status);
	}
}
