package xyz.twooter.auth.application;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

import xyz.twooter.auth.domain.TokenType;
import xyz.twooter.auth.infrastructure.jwt.JWTUtil;
import xyz.twooter.auth.presentation.dto.request.SignInRequest;
import xyz.twooter.auth.presentation.dto.request.TokenReissueRequest;
import xyz.twooter.auth.presentation.dto.response.LogoutResponse;
import xyz.twooter.auth.presentation.dto.response.SignInResponse;
import xyz.twooter.auth.presentation.dto.response.TokenReissueResponse;
import xyz.twooter.member.application.MemberService;
import xyz.twooter.member.presentation.dto.response.MemberSummaryResponse;
import xyz.twooter.support.MockTestSupport;

class AuthServiceTest extends MockTestSupport {
	private final String TEST_HANDLE = "testUser";
	private final String TEST_PASSWORD = "password123";
	private final String TEST_EMAIL = "test@email.com";
	private final String TEST_ACCESS_TOKEN = "access.token.test";
	private final String TEST_REFRESH_TOKEN = "refresh.token.test";
	private final String NEW_ACCESS_TOKEN = "new.access.token";
	private final String NEW_REFRESH_TOKEN = "new.refresh.token";
	@Mock
	private MemberService memberService;
	@Mock
	private AuthenticationManagerBuilder authenticationManagerBuilder;
	@Mock
	private JWTUtil jwtUtil;
	@Mock
	private TokenService tokenService;
	@Mock
	private Authentication authentication;
	@Mock
	private AuthenticationManager authenticationManager;
	@InjectMocks
	private AuthService authService;

	@BeforeEach
	void setUp() {
		ReflectionTestUtils.setField(authService, "accessTokenValidity", 1800000L); // 30분
		ReflectionTestUtils.setField(authService, "refreshTokenValidity", 604800000L); // 7일
	}

	@Test
	@DisplayName("유효한 인증 정보로 로그인 시 토큰이 정상적으로 발급되어야 한다")
	void shouldIssueTokensWhenValidCredentialsProvided() {
		// given
		SignInRequest request = new SignInRequest(TEST_HANDLE, TEST_PASSWORD);
		MemberSummaryResponse memberSummaryResponse = new MemberSummaryResponse(TEST_EMAIL, TEST_HANDLE, "테스트 사용자", "test@example.com");

		when(authenticationManagerBuilder.getObject()).thenReturn(authenticationManager);
		when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
			.thenReturn(authentication);
		when(
			jwtUtil.createJwt(eq(TEST_HANDLE), eq(TokenType.ACCESS), anyLong()))
			.thenReturn(TEST_ACCESS_TOKEN);
		when(tokenService.createRefreshToken(TEST_HANDLE)).thenReturn(TEST_REFRESH_TOKEN);
		when(memberService.createMemberSummary(TEST_HANDLE)).thenReturn(memberSummaryResponse);

		// when
		SignInResponse response = authService.signIn(request);

		// then
		assertNotNull(response);
		assertEquals(TEST_ACCESS_TOKEN, response.accessToken());
		assertEquals(TEST_REFRESH_TOKEN, response.refreshToken());
		assertEquals(TEST_HANDLE, response.member().getHandle());
		verify(tokenService).createRefreshToken(TEST_HANDLE);
	}

	@Test
	@DisplayName("토큰 재발급 요청 시 새로운 액세스 토큰과 리프레시 토큰이 발급되어야 한다")
	void shouldReissueTokensWhenRefreshTokenIsValid() {
		// given
		TokenReissueRequest request = new TokenReissueRequest(TEST_REFRESH_TOKEN);

		when(tokenService.rotateRefreshToken(TEST_REFRESH_TOKEN)).thenReturn(NEW_REFRESH_TOKEN);
		when(jwtUtil.getHandle(TEST_REFRESH_TOKEN)).thenReturn(TEST_HANDLE);
		when(jwtUtil.createJwt(eq(TEST_HANDLE), eq(TokenType.ACCESS), anyLong()))
			.thenReturn(NEW_ACCESS_TOKEN);

		// when
		TokenReissueResponse response = authService.reissueToken(request);

		// then
		assertNotNull(response);
		assertEquals(NEW_ACCESS_TOKEN, response.accessToken());
		assertEquals(NEW_REFRESH_TOKEN, response.refreshToken());
		verify(tokenService).rotateRefreshToken(TEST_REFRESH_TOKEN);
	}

	@Test
	@DisplayName("로그아웃 시 사용자의 모든 토큰이 무효화되고 액세스 토큰이 블랙리스트에 추가되어야 한다")
	void shouldRevokeAllTokensAndBlacklistAccessTokenWhenUserLogout() {
		// given
		when(jwtUtil.isValid(TEST_ACCESS_TOKEN)).thenReturn(true);
		when(jwtUtil.getHandle(TEST_ACCESS_TOKEN)).thenReturn(TEST_HANDLE);
		doNothing().when(tokenService).blacklistToken(TEST_ACCESS_TOKEN, TokenType.ACCESS);
		doNothing().when(tokenService).revokeAllUserTokens(TEST_HANDLE);

		// when
		LogoutResponse response = authService.logout(TEST_ACCESS_TOKEN);

		// then
		assertNotNull(response);
		assertEquals(TEST_HANDLE, response.userHandle());
		verify(tokenService).blacklistToken(TEST_ACCESS_TOKEN, TokenType.ACCESS);
		verify(tokenService).revokeAllUserTokens(TEST_HANDLE);
	}

	@Test
	@DisplayName("유효하지 않은 액세스 토큰으로 로그아웃 시 정상적으로 처리되어야 한다")
	void shouldHandleLogoutGracefullyWithInvalidToken() {
		// given
		when(jwtUtil.isValid(TEST_ACCESS_TOKEN)).thenReturn(false);

		// when
		LogoutResponse response = authService.logout(TEST_ACCESS_TOKEN);

		// then
		assertNotNull(response);
		assertEquals("unknown", response.userHandle());
		verify(tokenService, never()).revokeAllUserTokens(any());
	}
}
