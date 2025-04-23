package xyz.twooter.auth.infrastructure.jwt;

import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import xyz.twooter.auth.application.TokenService;
import xyz.twooter.auth.domain.TokenType;
import xyz.twooter.auth.domain.exception.InvalidTokenException;

@Slf4j
@RequiredArgsConstructor
public class JWTFilter extends OncePerRequestFilter {

	public static final String AUTHORIZATION_HEADER = "Authorization";
	public static final String TOKEN_PREFIX = "Bearer ";

	private final JWTUtil jwtUtil;
	private final UserDetailsService userDetailsService;
	private final TokenService tokenService;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
		throws ServletException, IOException {

		try {
			// 요청 헤더에서 JWT 토큰 추출
			String jwt = resolveToken(request);

			if (StringUtils.hasText(jwt)) {
				validateAndProcessToken(jwt);
			}

			filterChain.doFilter(request, response);
		} catch (InvalidTokenException e) {
			// 예외 정보를 요청 속성에 저장하고 SecurityContext 초기화
			handleInvalidToken(request, e);
			// EntryPoint가 처리할 수 있도록 예외 전파
			throw e;
		}
	}

	private void validateAndProcessToken(String jwt) {
		validateToken(jwt);
		processValidToken(jwt);
	}

	private void validateToken(String jwt) {
		// 토큰이 만료되었는지 확인
		if (jwtUtil.isExpired(jwt) || isBlacklisted(jwt)) {
			log.warn("Expired token attempted to be used");
			throw new InvalidTokenException();
		}
	}

	private boolean isBlacklisted(String jwt) {
		return tokenService.isTokenBlacklisted(jwt, TokenType.ACCESS);
	}

	private void processValidToken(String jwt) {
		// 토큰에서 사용자 handle 추출
		String handle = jwtUtil.getHandle(jwt);
		log.debug("Processing token for user: {}", handle);

		// JWT 토큰이 유효한 경우 Spring Security 인증 정보를 설정
		UserDetails userDetails = userDetailsService.loadUserByUsername(handle);
		Authentication authentication = createAuthentication(userDetails);
		// 인증 객체 생성 및 SecurityContext에 저장
		SecurityContextHolder.getContext().setAuthentication(authentication);

		log.debug("Authentication set for user: {}", handle);
	}

	private Authentication createAuthentication(UserDetails userDetails) {
		return new UsernamePasswordAuthenticationToken(
			userDetails, null, userDetails.getAuthorities());
	}

	private void handleInvalidToken(HttpServletRequest request, InvalidTokenException e) {
		// 예외 정보를 요청 속성에 저장
		request.setAttribute("exception", e);
		// SecurityContext 초기화
		SecurityContextHolder.clearContext();
		log.debug("Invalid token handling completed: {}", e.getMessage());
	}

	private String resolveToken(HttpServletRequest request) {
		String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
		if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(TOKEN_PREFIX)) {
			return bearerToken.substring(TOKEN_PREFIX.length());
		}
		return null;
	}
}
