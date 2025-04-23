package xyz.twooter.auth.infrastructure.jwt;

import java.io.IOException;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import xyz.twooter.auth.domain.exception.InvalidTokenException;
import xyz.twooter.common.error.ErrorCode;
import xyz.twooter.common.error.ErrorResponse;

@Component
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response,
		AuthenticationException authException) throws IOException {

		// 원래 예외 가져오기 (필터에서 설정한 경우)
		Throwable exception = (Throwable)request.getAttribute("exception");
		ErrorCode errorCode = ErrorCode.AUTHENTICATION_FAILED;

		// 적절한 에러 코드 결정
		if (exception instanceof InvalidTokenException) {
			errorCode = ErrorCode.INVALID_TOKEN;
		} else if (exception instanceof ExpiredJwtException) {
			errorCode = ErrorCode.TOKEN_EXPIRED;
		}

		// 에러 응답 생성
		ErrorResponse errorResponse = ErrorResponse.of(errorCode);

		// JSON 응답 설정
		response.setContentType("application/json;charset=UTF-8");
		response.setStatus(errorCode.getStatus());
		objectMapper.writeValue(response.getOutputStream(), errorResponse);
	}
}
