package xyz.twooter.auth.presentation;

import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import xyz.twooter.auth.presentation.dto.request.SignInRequest;
import xyz.twooter.auth.presentation.dto.request.SignUpRequest;
import xyz.twooter.auth.presentation.dto.request.TokenReissueRequest;
import xyz.twooter.auth.presentation.dto.response.LogoutResponse;
import xyz.twooter.auth.presentation.dto.response.SignInResponse;
import xyz.twooter.auth.presentation.dto.response.SignUpInfoResponse;
import xyz.twooter.auth.presentation.dto.response.TokenReissueResponse;
import xyz.twooter.common.error.ErrorCode;
import xyz.twooter.member.presentation.dto.response.MemberSummaryResponse;
import xyz.twooter.support.ControllerTestSupport;

class AuthControllerTest extends ControllerTestSupport {

	@DisplayName("회원가입 성공 시, 해당 유저의 정보를 반환한다.")
	@Test
	void signup() throws Exception {
		// given
		SignUpRequest request = SignUpRequest.builder()
			.email("user@example.com")
			.password("StrongP@ssw0rd!")
			.handle("twooter_123")
			.build();

		SignUpInfoResponse response = new SignUpInfoResponse(
			new MemberSummaryResponse(
				"user@example.com",
				"twooter_123",
				"닉네임 기본값",
				"testpath"
			)
		);

		given(authService.signUp(any())).willReturn(response);

		// when & then
		mockMvc.perform(
				post("/api/auth/signup")
					.content(objectMapper.writeValueAsString(request))
					.contentType(MediaType.APPLICATION_JSON)
			)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.member.email").value("user@example.com"))
			.andExpect(jsonPath("$.member.handle").value("twooter_123"));
	}

	@DisplayName("이메일 형식이 잘못되면 400을 반환한다.")
	@Test
	void shouldReturn400WhenEmailInvalid() throws Exception {
		SignUpRequest invalidRequest = SignUpRequest.builder()
			.email("invalid-email")
			.password("StrongP@ssw0rd!")
			.handle("twooter_123")
			.build();

		mockMvc.perform(post("/api/auth/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(invalidRequest)))
			.andExpect(status().isBadRequest());
	}

	@DisplayName("비밀번호가 조건을 만족하지 않으면 400을 반환한다.")
	@Test
	void shouldReturn400WhenPasswordInvalid() throws Exception {
		SignUpRequest invalidRequest = SignUpRequest.builder()
			.email("user@example.com")
			.password("weakpw")
			.handle("twooter_123")
			.build();

		mockMvc.perform(post("/api/auth/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(invalidRequest)))
			.andExpect(status().isBadRequest());
	}

	@DisplayName("핸들이 조건을 만족하지 않으면 400을 반환한다.")
	@Test
	void shouldReturn400WhenHandleInvalid() throws Exception {
		SignUpRequest invalidRequest = SignUpRequest.builder()
			.email("user@example.com")
			.password("StrongP@ssw0rd!")
			.handle("!!wrong@@")
			.build();

		mockMvc.perform(post("/api/auth/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(invalidRequest)))
			.andExpect(status().isBadRequest());
	}

	@DisplayName("로그인 성공 시 토큰과 유저 정보를 반환한다.")
	@Test
	void signin() throws Exception {
		// given
		SignInRequest request = SignInRequest.builder()
			.handle("twooter_123")
			.password("StrongP@ssw0rd!")
			.build();

		MemberSummaryResponse memberSummaryResponse = MemberSummaryResponse.builder()
			.email("user@example.com")
			.handle("twooter_123")
			.nickname("닉네임 기본값")
			.avatarPath("testpath")
			.build();

		SignInResponse response = new SignInResponse(
			"test.access.token",
			"test.refresh.token",
			memberSummaryResponse
		);

		given(authService.signIn(any(SignInRequest.class))).willReturn(response);

		// when & then
		mockMvc.perform(
				post("/api/auth/signin")
					.content(objectMapper.writeValueAsString(request))
					.contentType(MediaType.APPLICATION_JSON)
			)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.accessToken").value("test.access.token"))
			.andExpect(jsonPath("$.refreshToken").value("test.refresh.token"))
			.andExpect(jsonPath("$.member.email").value("user@example.com"))
			.andExpect(jsonPath("$.member.handle").value("twooter_123"))
			.andExpect(jsonPath("$.member.nickname").value("닉네임 기본값"))
			.andExpect(jsonPath("$.member.avatarPath").value("testpath"));
	}

	@DisplayName("로그인 실패 시 인증 예외가 발생하면 401을 반환한다.")
	@Test
	void shouldReturn401WhenAuthenticationFails() throws Exception {
		// given
		SignInRequest request = SignInRequest.builder()
			.handle("twooter_123")
			.password("StrongP@ssw0rd!")
			.build();

		given(authService.signIn(any(SignInRequest.class)))
			.willThrow(new BadCredentialsException("잘못된 인증 정보입니다."));

		// when & then
		mockMvc.perform(
				post("/api/auth/signin")
					.content(objectMapper.writeValueAsString(request))
					.contentType(MediaType.APPLICATION_JSON)
			)
			.andExpect(status().isUnauthorized());
	}

	@DisplayName("없는 사용자로 로그인 시도하면 401을 반환한다.")
	@Test
	void shouldReturn401WhenUserNotFound() throws Exception {
		// given
		SignInRequest request = SignInRequest.builder()
			.handle("nonexistent_user")
			.password("StrongP@ssw0rd!")
			.build();

		given(authService.signIn(any(SignInRequest.class)))
			.willThrow(new UsernameNotFoundException("사용자를 찾을 수 없습니다."));

		// when & then
		mockMvc.perform(
				post("/api/auth/signin")
					.content(objectMapper.writeValueAsString(request))
					.contentType(MediaType.APPLICATION_JSON)
			)
			.andExpect(status().isUnauthorized());
	}

	@DisplayName("토큰 재발급 성공 시 새로운 액세스 토큰과 리프레시 토큰을 반환한다.")
	@Test
	void reissueToken() throws Exception {
		// given
		TokenReissueRequest request = TokenReissueRequest.builder()
			.refreshToken("old.refresh.token")
			.build();

		TokenReissueResponse response = new TokenReissueResponse(
			"new.access.token",
			"new.refresh.token"
		);

		given(authService.reissueToken(any(TokenReissueRequest.class))).willReturn(response);

		// when & then
		mockMvc.perform(
				post("/api/auth/reissue")
					.content(objectMapper.writeValueAsString(request))
					.contentType(MediaType.APPLICATION_JSON)
			)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.accessToken").value("new.access.token"))
			.andExpect(jsonPath("$.refreshToken").value("new.refresh.token"));
	}

	@DisplayName("유효하지 않은 리프레시 토큰으로 재발급 요청 시 401을 반환한다.")
	@Test
	void shouldReturn401WhenRefreshTokenInvalid() throws Exception {
		// given
		TokenReissueRequest request = TokenReissueRequest.builder()
			.refreshToken("invalid.refresh.token")
			.build();

		given(authService.reissueToken(any(TokenReissueRequest.class)))
			.willThrow(new BadCredentialsException("유효하지 않은 리프레시 토큰입니다."));

		// when & then
		mockMvc.perform(
				post("/api/auth/reissue")
					.content(objectMapper.writeValueAsString(request))
					.contentType(MediaType.APPLICATION_JSON)
			)
			.andExpect(status().isUnauthorized());
	}

	@DisplayName("리프레시 토큰이 null이면 400을 반환한다.")
	@Test
	void shouldReturn400WhenRefreshTokenNull() throws Exception {
		// given
		TokenReissueRequest request = TokenReissueRequest.builder()
			.refreshToken(null)
			.build();

		// when & then
		mockMvc.perform(
				post("/api/auth/reissue")
					.content(objectMapper.writeValueAsString(request))
					.contentType(MediaType.APPLICATION_JSON)
			)
			.andExpect(status().isBadRequest());
	}

	@DisplayName("로그아웃 성공 시 사용자 핸들을 포함한 응답을 반환한다.")
	@Test
	void logout() throws Exception {
		// given
		String accessToken = "test.access.token";
		LogoutResponse response = new LogoutResponse("twooter_123");

		given(authService.logout(accessToken)).willReturn(response);

		// when & then
		mockMvc.perform(
				post("/api/auth/logout")
					.header("Authorization", "Bearer " + accessToken)
			)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.userHandle").value("twooter_123"));
	}

	@DisplayName("Authorization 헤더 없이 로그아웃 요청 시 에러가 발생한다.")
	@Test
	void logoutWithoutAuthHeader() throws Exception {
		// given & when & then
		mockMvc.perform(
				post("/api/auth/logout")
			)
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value(ErrorCode.MISSING_AUTHORIZATION_HEADER.getCode()));
	}

	@DisplayName("토큰이 Bearer 로 시작하지 않으면 에러를 반환한다.")
	@Test
	void logoutWithoutBearerPrefix() throws Exception {
		// given & when & then
		mockMvc.perform(
				post("/api/auth/logout")
					.header("Authorization", "accessToken")
			)
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value(ErrorCode.INVALID_TOKEN_FORMAT.getCode()));
	}

	@DisplayName("유효하지 않은 액세스 토큰으로 로그아웃 요청 시 401을 반환한다.")
	@Test
	void shouldReturn401WhenAccessTokenInvalidForLogout() throws Exception {
		// given
		String invalidAccessToken = "invalid.access.token";

		given(authService.logout(invalidAccessToken))
			.willThrow(new BadCredentialsException("유효하지 않은 액세스 토큰입니다."));

		// when & then
		mockMvc.perform(
				post("/api/auth/logout")
					.header("Authorization", "Bearer " + invalidAccessToken)
			)
			.andExpect(status().isUnauthorized());
	}
}
