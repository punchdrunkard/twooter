package xyz.twooter.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import lombok.RequiredArgsConstructor;
import xyz.twooter.auth.application.TokenService;
import xyz.twooter.auth.infrastructure.jwt.CustomAuthenticationEntryPoint;
import xyz.twooter.auth.infrastructure.jwt.JWTFilter;
import xyz.twooter.auth.infrastructure.jwt.JWTUtil;
import xyz.twooter.auth.infrastructure.usersdetails.CustomUserDetailsService;
import xyz.twooter.common.filter.ExceptionTranslationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfiguration {

	private final JWTUtil jwtUtil;
	private final CustomUserDetailsService userDetailsService;
	private final CustomAuthenticationEntryPoint authenticationEntryPoint;
	private final TokenService tokenService;

	@Bean
	public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
		return configuration.getAuthenticationManager();
	}

	@Bean
	public BCryptPasswordEncoder bCryptPasswordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		return http
			// CSRF 비활성화
			.csrf(AbstractHttpConfigurer::disable)
			// Form 로그인 비활성화
			.formLogin(AbstractHttpConfigurer::disable)
			// HTTP Basic 인증 비활성화
			.httpBasic(AbstractHttpConfigurer::disable)
			// 경로별 인가 설정
			.authorizeHttpRequests(auth -> auth
				.requestMatchers("/h2-console/**", "/api/auth/**", "/docs/**").permitAll()
				.anyRequest().authenticated()
			)
			.headers(headers -> headers
				.frameOptions(HeadersConfigurer.FrameOptionsConfig::disable)
			)
			// 세션 설정 - STATELESS
			.sessionManagement(session -> session
				.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
			)
			.exceptionHandling(e -> e.authenticationEntryPoint(authenticationEntryPoint))
			// JWT 필터 추가
			.addFilterBefore(new JWTFilter(jwtUtil, userDetailsService, tokenService),
				UsernamePasswordAuthenticationFilter.class)
			.addFilterBefore(new ExceptionTranslationFilter(), JWTFilter.class)
			.build();
	}
}
