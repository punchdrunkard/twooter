package xyz.twooter.common.filter;

import java.io.IOException;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import xyz.twooter.auth.domain.exception.InvalidTokenException;
import xyz.twooter.common.error.ErrorCode;
import xyz.twooter.common.error.ErrorResponse;

@Component
public class ExceptionTranslationFilter extends OncePerRequestFilter {

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
		throws ServletException, IOException {
		try {
			filterChain.doFilter(request, response);
		} catch (InvalidTokenException e) {
			// 예외 처리 로직
			request.setAttribute("exception", e);
			ErrorCode errorCode = e.getErrorCode();

			response.setStatus(errorCode.getStatus());
			response.setContentType("application/json;charset=UTF-8");

			ErrorResponse errorResponse = ErrorResponse.of(errorCode);
			ObjectMapper objectMapper = new ObjectMapper();
			objectMapper.writeValue(response.getOutputStream(), errorResponse);
		}
	}
}
