package xyz.twooter.auth.application;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;

import xyz.twooter.auth.domain.RefreshToken;
import xyz.twooter.auth.domain.TokenType;
import xyz.twooter.auth.domain.exception.InvalidTokenException;
import xyz.twooter.auth.infrastructure.jwt.JWTUtil;
import xyz.twooter.common.infrastructure.redis.RedisUtil;
import xyz.twooter.support.MockTestSupport;

class TokenServiceTest extends MockTestSupport {
	private final String TEST_HANDLE = "testUser";
	private final String TEST_TOKEN = "refresh.token.test";
	private final String NEW_TOKEN = "new.refresh.token";
	private final String TOKEN_PREFIX = "refresh:token:";
	private final String USER_PREFIX = "refresh:user:";
	private final String BLACKLIST_PREFIX_REFRESH = "refresh:blacklist:";
	private final String BLACKLIST_PREFIX_ACCESS = "access:blacklist:";

	@Mock
	private RedisUtil redisUtil;

	@Mock
	private JWTUtil jwtUtil;

	@InjectMocks
	private TokenService tokenService;

	@BeforeEach
	void setUp() {
		ReflectionTestUtils.setField(tokenService, "refreshTokenValidityMs", 604800000L); // 7일
	}

	@Test
	@DisplayName("createRefreshToken 메서드는 새로운 리프레시 토큰을 생성하고 Redis에 저장해야 한다")
	void shouldCreateAndStoreRefreshToken() {
		// given
		when(jwtUtil.createJwt(eq(TEST_HANDLE), eq(TokenType.REFRESH), anyLong())).thenReturn(TEST_TOKEN);

		// when
		String token = tokenService.createRefreshToken(TEST_HANDLE);

		// then
		assertEquals(TEST_TOKEN, token);
		verify(jwtUtil).createJwt(eq(TEST_HANDLE), eq(TokenType.REFRESH), anyLong());
		verify(redisUtil).set(eq(TOKEN_PREFIX + TEST_TOKEN), any(RefreshToken.class), eq(604800000L),
			eq(TimeUnit.MILLISECONDS));
		verify(redisUtil).set(eq(USER_PREFIX + TEST_HANDLE), any(Set.class), eq(604800000L), eq(TimeUnit.MILLISECONDS));
	}

	@Test
	@DisplayName("validateRefreshToken 메서드는 유효한 토큰에 대해 RefreshToken 객체를 반환해야 한다")
	void shouldReturnRefreshTokenWhenTokenIsValid() {
		// given
		RefreshToken storedToken = RefreshToken.builder()
			.id(TEST_TOKEN)
			.userHandle(TEST_HANDLE)
			.expiryDate(LocalDateTime.now().plusDays(7))
			.build();

		when(redisUtil.hasKey(BLACKLIST_PREFIX_REFRESH + TEST_TOKEN)).thenReturn(false);
		when(redisUtil.hasKey(TOKEN_PREFIX + TEST_TOKEN)).thenReturn(true);
		when(redisUtil.get(TOKEN_PREFIX + TEST_TOKEN, RefreshToken.class)).thenReturn(storedToken);
		when(jwtUtil.isExpired(TEST_TOKEN)).thenReturn(false);

		// when
		RefreshToken result = tokenService.validateRefreshToken(TEST_TOKEN);

		// then
		assertNotNull(result);
		assertEquals(TEST_TOKEN, result.getId());
		assertEquals(TEST_HANDLE, result.getUserHandle());
		assertFalse(result.isRevoke());
	}

	@Test
	@DisplayName("validateRefreshToken 메서드는 블랙리스트에 있는 토큰에 대해 예외를 발생시켜야 한다")
	void shouldThrowExceptionWhenTokenIsBlacklisted() {
		// given
		when(redisUtil.hasKey(BLACKLIST_PREFIX_REFRESH + TEST_TOKEN)).thenReturn(true);
		when(jwtUtil.getHandle(TEST_TOKEN)).thenReturn(TEST_HANDLE);

		// when & then
		InvalidTokenException exception = assertThrows(InvalidTokenException.class, () -> {
			tokenService.validateRefreshToken(TEST_TOKEN);
		});

		assertEquals("Refresh token has been used before, possible token theft detected", exception.getMessage());
		verify(redisUtil).hasKey(BLACKLIST_PREFIX_REFRESH + TEST_TOKEN);
	}

	@Test
	@DisplayName("validateRefreshToken 메서드는 Redis에 없는 토큰에 대해 예외를 발생시켜야 한다")
	void shouldThrowExceptionWhenTokenNotFoundInRedis() {
		// given
		when(redisUtil.hasKey(BLACKLIST_PREFIX_REFRESH + TEST_TOKEN)).thenReturn(false);
		when(redisUtil.hasKey(TOKEN_PREFIX + TEST_TOKEN)).thenReturn(false);

		// when & then
		InvalidTokenException exception = assertThrows(InvalidTokenException.class, () -> {
			tokenService.validateRefreshToken(TEST_TOKEN);
		});

		assertEquals("Refresh token not found or expired", exception.getMessage());
	}

	@Test
	@DisplayName("validateRefreshToken 메서드는 만료된 토큰에 대해 예외를 발생시켜야 한다")
	void shouldThrowExceptionWhenTokenIsExpired() {
		// given
		RefreshToken storedToken = RefreshToken.builder()
			.id(TEST_TOKEN)
			.userHandle(TEST_HANDLE)
			.expiryDate(LocalDateTime.now().plusDays(7))
			.build();

		when(redisUtil.hasKey(BLACKLIST_PREFIX_REFRESH + TEST_TOKEN)).thenReturn(false);
		when(redisUtil.hasKey(TOKEN_PREFIX + TEST_TOKEN)).thenReturn(true);
		when(redisUtil.get(TOKEN_PREFIX + TEST_TOKEN, RefreshToken.class)).thenReturn(storedToken);
		when(jwtUtil.isExpired(TEST_TOKEN)).thenReturn(true);

		// when & then
		InvalidTokenException exception = assertThrows(InvalidTokenException.class, () -> {
			tokenService.validateRefreshToken(TEST_TOKEN);
		});

		assertEquals("Refresh token has expired", exception.getMessage());
		verify(redisUtil).delete(TOKEN_PREFIX + TEST_TOKEN);
	}

	@Test
	@DisplayName("validateRefreshToken 메서드는 취소된 토큰에 대해 예외를 발생시켜야 한다")
	void shouldThrowExceptionWhenTokenIsRevoked() {
		// given
		RefreshToken storedToken = RefreshToken.builder()
			.id(TEST_TOKEN)
			.userHandle(TEST_HANDLE)
			.expiryDate(LocalDateTime.now().plusDays(7))
			.build();
		storedToken.revoke(); // 토큰 취소

		when(redisUtil.hasKey(BLACKLIST_PREFIX_REFRESH + TEST_TOKEN)).thenReturn(false);
		when(redisUtil.hasKey(TOKEN_PREFIX + TEST_TOKEN)).thenReturn(true);
		when(redisUtil.get(TOKEN_PREFIX + TEST_TOKEN, RefreshToken.class)).thenReturn(storedToken);

		// when & then
		InvalidTokenException exception = assertThrows(InvalidTokenException.class, () -> {
			tokenService.validateRefreshToken(TEST_TOKEN);
		});

		assertEquals("Refresh token was revoked", exception.getMessage());
	}

	@Test
	@DisplayName("revokeRefreshToken은 사용자 토큰 목록에서 마지막 토큰을 제거하면 Redis에서 삭제해야 한다")
	void shouldDeleteUserTokenListWhenLastTokenRemoved() {
		// given
		RefreshToken storedToken = RefreshToken.builder()
			.id(TEST_TOKEN)
			.userHandle(TEST_HANDLE)
			.expiryDate(LocalDateTime.now().plusDays(7))
			.build();

		Set<String> userTokens = new HashSet<>();
		userTokens.add(TEST_TOKEN);

		when(redisUtil.hasKey(TOKEN_PREFIX + TEST_TOKEN)).thenReturn(true);
		when(redisUtil.get(TOKEN_PREFIX + TEST_TOKEN, RefreshToken.class)).thenReturn(storedToken);
		when(redisUtil.hasKey(USER_PREFIX + TEST_HANDLE)).thenReturn(true);
		when(redisUtil.get(USER_PREFIX + TEST_HANDLE, Set.class)).thenReturn(userTokens);

		// when
		tokenService.revokeRefreshToken(TEST_TOKEN);

		// then
		verify(redisUtil).delete(USER_PREFIX + TEST_HANDLE); // 🔥 핵심 확인
		verify(redisUtil, never()).set(eq(USER_PREFIX + TEST_HANDLE), any(), anyLong(), any());
	}
	
	@Test
	@DisplayName("revokeRefreshToken은 사용자 토큰 목록에서 일부 토큰만 제거되면 Redis에 다시 저장해야 한다")
	void shouldUpdateUserTokenListWhenTokensRemain() {
		// given
		RefreshToken storedToken = RefreshToken.builder()
			.id(TEST_TOKEN)
			.userHandle(TEST_HANDLE)
			.expiryDate(LocalDateTime.now().plusDays(7))
			.build();

		Set<String> userTokens = new HashSet<>();
		userTokens.add(TEST_TOKEN);
		userTokens.add("another.token");

		when(redisUtil.hasKey(TOKEN_PREFIX + TEST_TOKEN)).thenReturn(true);
		when(redisUtil.get(TOKEN_PREFIX + TEST_TOKEN, RefreshToken.class)).thenReturn(storedToken);
		when(redisUtil.hasKey(USER_PREFIX + TEST_HANDLE)).thenReturn(true);
		when(redisUtil.get(USER_PREFIX + TEST_HANDLE, Set.class)).thenReturn(userTokens);

		// when
		tokenService.revokeRefreshToken(TEST_TOKEN);

		// then
		verify(redisUtil).set(eq(USER_PREFIX + TEST_HANDLE), argThat(tokens ->
				tokens instanceof Set && ((Set<?>)tokens).contains("another.token") && ((Set<?>)tokens).size() == 1),
			eq(604800000L), eq(TimeUnit.MILLISECONDS)
		);
		verify(redisUtil, never()).delete(USER_PREFIX + TEST_HANDLE);
	}

	@Test
	@DisplayName("revokeAllUserTokens 메서드는 사용자의 모든 토큰을 취소해야 한다")
	void shouldRevokeAllTokensForUser() {
		// given
		Set<String> userTokens = new HashSet<>();
		userTokens.add(TEST_TOKEN);
		userTokens.add("another.token");

		RefreshToken token1 = RefreshToken.builder()
			.id(TEST_TOKEN)
			.userHandle(TEST_HANDLE)
			.expiryDate(LocalDateTime.now().plusDays(7))
			.build();

		RefreshToken token2 = RefreshToken.builder()
			.id("another.token")
			.userHandle(TEST_HANDLE)
			.expiryDate(LocalDateTime.now().plusDays(7))
			.build();

		when(redisUtil.hasKey(USER_PREFIX + TEST_HANDLE)).thenReturn(true);
		when(redisUtil.get(USER_PREFIX + TEST_HANDLE, Set.class)).thenReturn(userTokens);
		when(redisUtil.hasKey(TOKEN_PREFIX + TEST_TOKEN)).thenReturn(true);
		when(redisUtil.hasKey(TOKEN_PREFIX + "another.token")).thenReturn(true);
		when(redisUtil.get(TOKEN_PREFIX + TEST_TOKEN, RefreshToken.class)).thenReturn(token1);
		when(redisUtil.get(TOKEN_PREFIX + "another.token", RefreshToken.class)).thenReturn(token2);

		// when
		tokenService.revokeAllUserTokens(TEST_HANDLE);

		// then
		verify(redisUtil).set(eq(TOKEN_PREFIX + TEST_TOKEN), any(RefreshToken.class), eq(300L), eq(TimeUnit.SECONDS));
		verify(redisUtil).set(eq(TOKEN_PREFIX + "another.token"), any(RefreshToken.class), eq(300L),
			eq(TimeUnit.SECONDS));
		verify(redisUtil).delete(USER_PREFIX + TEST_HANDLE);
	}

	@Test
	@DisplayName("rotateRefreshToken 메서드는 기존 토큰을 무효화하고 새 토큰을 발급해야 한다")
	void shouldRevokeOldTokenAndCreateNewOne() {
		// given
		RefreshToken oldToken = RefreshToken.builder()
			.id(TEST_TOKEN)
			.userHandle(TEST_HANDLE)
			.expiryDate(LocalDateTime.now().plusDays(7))
			.build();

		when(redisUtil.hasKey(BLACKLIST_PREFIX_REFRESH + TEST_TOKEN)).thenReturn(false);
		when(redisUtil.hasKey(TOKEN_PREFIX + TEST_TOKEN)).thenReturn(true);
		when(redisUtil.get(TOKEN_PREFIX + TEST_TOKEN, RefreshToken.class)).thenReturn(oldToken);
		when(jwtUtil.isExpired(TEST_TOKEN)).thenReturn(false);
		when(jwtUtil.createJwt(eq(TEST_HANDLE), eq(TokenType.REFRESH), anyLong())).thenReturn(NEW_TOKEN);

		// when
		String newToken = tokenService.rotateRefreshToken(TEST_TOKEN);

		// then
		assertEquals(NEW_TOKEN, newToken);
		verify(redisUtil).set(eq(TOKEN_PREFIX + TEST_TOKEN), any(RefreshToken.class), eq(300L), eq(TimeUnit.SECONDS));
		verify(redisUtil).set(eq(BLACKLIST_PREFIX_REFRESH + TEST_TOKEN), anyString(), anyLong(),
			eq(TimeUnit.MILLISECONDS));
		verify(jwtUtil).createJwt(eq(TEST_HANDLE), eq(TokenType.REFRESH), anyLong());
	}

	@Test
	@DisplayName("blacklistToken 메서드는 액세스 토큰을 블랙리스트에 추가해야 한다")
	void shouldAddAccessTokenToBlacklist() {
		// given
		when(jwtUtil.isExpired("access.token")).thenReturn(false);
		when(jwtUtil.getRemainingTimeInMillis("access.token")).thenReturn(300000L);

		// when
		tokenService.blacklistToken("access.token", TokenType.ACCESS);

		// then
		verify(redisUtil).set(eq("access:blacklist:access.token"), eq("true"), eq(300000L), eq(TimeUnit.MILLISECONDS));
	}

	@Test
	@DisplayName("blacklistToken 메서드는 리프레시 토큰을 블랙리스트에 추가해야 한다")
	void shouldAddRefreshTokenToBlacklist() {
		// when
		tokenService.blacklistToken(TEST_TOKEN, TokenType.REFRESH);

		// then
		verify(redisUtil).set(eq(BLACKLIST_PREFIX_REFRESH + TEST_TOKEN), anyString(), eq(604800000L),
			eq(TimeUnit.MILLISECONDS));
	}

	@Test
	@DisplayName("isTokenBlacklisted 메서드는 블랙리스트에 있는 토큰을 확인해야 한다")
	void shouldCheckIfTokenIsBlacklisted() {
		// given
		when(redisUtil.hasKey(BLACKLIST_PREFIX_ACCESS + "access.token")).thenReturn(true);
		when(redisUtil.hasKey(BLACKLIST_PREFIX_REFRESH + TEST_TOKEN)).thenReturn(false);

		// when
		boolean accessResult = tokenService.isTokenBlacklisted("access.token", TokenType.ACCESS);
		boolean refreshResult = tokenService.isTokenBlacklisted(TEST_TOKEN, TokenType.REFRESH);

		// then
		assertTrue(accessResult);
		assertFalse(refreshResult);
	}
}
