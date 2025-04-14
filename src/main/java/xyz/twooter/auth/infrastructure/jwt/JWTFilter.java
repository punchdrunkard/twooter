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

@RequiredArgsConstructor
public class JWTFilter extends OncePerRequestFilter {

	public static final String AUTHORIZATION_HEADER = "Authorization";
	public static final String TOKEN_PREFIX = "Bearer ";

	private final JWTUtil jwtUtil;
	private final UserDetailsService userDetailsService;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
		throws ServletException, IOException {
		// 요청 헤더에서 JWT 토큰 추출
		String jwt = resolveToken(request);

		try {
			// 토큰이 존재하고 만료되지 않았는지 확인
			if (StringUtils.hasText(jwt) && !jwtUtil.isExpired(jwt)) {
				// 토큰에서 사용자 handle 추출
				String handle = jwtUtil.getHandle(jwt);

				// JWT 토큰이 유효한 경우 Spring Security 인증 정보를 설정
				UserDetails userDetails = userDetailsService.loadUserByUsername(handle);

				// 인증 객체 생성 및 SecurityContext에 저장
				Authentication authentication = new UsernamePasswordAuthenticationToken(
					userDetails, null, userDetails.getAuthorities());
				SecurityContextHolder.getContext().setAuthentication(authentication);
			}
		} catch (Exception e) {
			logger.error("JWT 인증 처리 중 오류 발생: {}", e);
			// 인증 오류가 발생해도 필터 체인은 계속 진행됨
			// 인증되지 않은 요청은 Spring Security에 의해 처리됨
		}

		// 다음 필터로 요청 전달
		filterChain.doFilter(request, response);
	}

	private String resolveToken(HttpServletRequest request) {
		String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
		if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(TOKEN_PREFIX)) {
			return bearerToken.substring(TOKEN_PREFIX.length());
		}
		return null;
	}
}
