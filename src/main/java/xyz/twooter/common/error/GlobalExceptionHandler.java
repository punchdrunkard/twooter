package xyz.twooter.common.error;

import java.nio.file.AccessDeniedException;
import java.security.SignatureException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
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

	/**
	 * Spring Security 관련 예외 처리
	 */

	// 1. 인증 관련 예외 처리
	@ExceptionHandler(AuthenticationException.class)
	protected ResponseEntity<ErrorResponse> handleAuthenticationException(AuthenticationException e) {
		log.error("Authentication Exception: ", e);
		ErrorCode errorCode = ErrorCode.AUTHENTICATION_FAILED;

		// 구체적인 예외 타입에 따라 다른 에러 코드 반환
		if (e instanceof BadCredentialsException) {
			errorCode = ErrorCode.INVALID_CREDENTIALS;
		} else if (e instanceof UsernameNotFoundException) {
			errorCode = ErrorCode.USER_NOT_FOUND;
		} else if (e instanceof DisabledException) {
			errorCode = ErrorCode.ACCOUNT_DISABLED;
		} else if (e instanceof LockedException) {
			errorCode = ErrorCode.ACCOUNT_LOCKED;
		}

		return buildErrorResponse(e, errorCode, HttpStatus.UNAUTHORIZED);
	}

	// 2. 접근 권한 예외 처리 (Spring Security의 AccessDeniedException)
	@ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
	protected ResponseEntity<ErrorResponse> handleSecurityAccessDeniedException(
		org.springframework.security.access.AccessDeniedException e) {
		log.error("Security Access Denied: ", e);
		return buildErrorResponse(e, ErrorCode.ACCESS_DENIED, HttpStatus.FORBIDDEN);
	}

	// 3. JWT 관련 예외 처리
	@ExceptionHandler(ExpiredJwtException.class)
	protected ResponseEntity<ErrorResponse> handleExpiredJwtException(ExpiredJwtException e) {
		log.error("JWT Token Expired: ", e);
		return buildErrorResponse(e, ErrorCode.TOKEN_EXPIRED, HttpStatus.UNAUTHORIZED);
	}

	@ExceptionHandler({MalformedJwtException.class, SignatureException.class, UnsupportedJwtException.class})
	protected ResponseEntity<ErrorResponse> handleInvalidJwtException(Exception e) {
		log.error("Invalid JWT Token: ", e);
		return buildErrorResponse(e, ErrorCode.INVALID_TOKEN, HttpStatus.UNAUTHORIZED);
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
