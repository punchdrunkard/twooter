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
import xyz.twooter.auth.presentation.dto.response.SignInResponse;
import xyz.twooter.auth.presentation.dto.response.SignUpInfoResponse;
import xyz.twooter.member.presentation.dto.MemberSummary;
import xyz.twooter.support.ControllerTestSupport;

class AuthControllerTest extends ControllerTestSupport {

	@DisplayName("회원가입 성공 시, 해당 유저의 정보를 반환한다.")
	@Test
	void singup() throws Exception {
		// given
		SignUpRequest request = SignUpRequest.builder()
			.email("user@example.com")
			.password("StrongP@ssw0rd!")
			.handle("twooter_123")
			.build();

		SignUpInfoResponse response = new SignUpInfoResponse(
			new MemberSummary(
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

		MemberSummary memberSummary = MemberSummary.builder()
			.email("user@example.com")
			.handle("twooter_123")
			.nickname("닉네임 기본값")
			.avatarPath("testpath")
			.build();

		SignInResponse response = new SignInResponse(
			"test.access.token",
			"test.refresh.token",
			memberSummary
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
}
