package xyz.twooter.auth.presentation;

import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import xyz.twooter.auth.presentation.dto.request.SignUpRequest;
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
}
