package xyz.twooter.docs.auth;

import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.*;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;

import xyz.twooter.auth.application.AuthService;
import xyz.twooter.auth.presentation.AuthController;
import xyz.twooter.auth.presentation.dto.request.SignInRequest;
import xyz.twooter.auth.presentation.dto.request.SignUpRequest;
import xyz.twooter.auth.presentation.dto.response.SignInResponse;
import xyz.twooter.auth.presentation.dto.response.SignUpInfoResponse;
import xyz.twooter.docs.RestDocsSupport;
import xyz.twooter.member.presentation.dto.MemberSummary;

public class AuthControllerDocsTest extends RestDocsSupport {

	private final AuthService authService = mock(AuthService.class);

	@Override
	protected Object initController() {
		return new AuthController(authService);
	}

	@DisplayName("회원가입 API")
	@Test
	void signup() throws Exception {
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
				"twooter_123",
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
			.andExpect(jsonPath("$.member.handle").value("twooter_123"))
			.andDo(document("auth-signup",
				preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				requestFields(
					fieldWithPath("email").type(JsonFieldType.STRING).description("사용자 이메일"),
					fieldWithPath("password").type(JsonFieldType.STRING).description("사용자 비밀번호 (영문 대/소문자, 숫자, 특수문자 포함 8자 이상)"),
					fieldWithPath("handle").type(JsonFieldType.STRING).description("사용자 핸들 (영문, 숫자, 밑줄(_) 사용 가능, 4~50자)")
				),
				responseFields(
					fieldWithPath("member").type(JsonFieldType.OBJECT).description("회원 정보"),
					fieldWithPath("member.email").type(JsonFieldType.STRING).description("회원 이메일"),
					fieldWithPath("member.handle").type(JsonFieldType.STRING).description("회원 핸들"),
					fieldWithPath("member.nickname").type(JsonFieldType.STRING).description("회원 닉네임"),
					fieldWithPath("member.avatarPath").type(JsonFieldType.STRING).description("회원 프로필 이미지 경로")
				)
			));
	}

	@DisplayName("로그인 API")
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
			.nickname("twooter_123")
			.avatarPath("testpath")
			.build();

		SignInResponse response = new SignInResponse(
			"eyJhbGciOiJIUzI1NiJ9.eyJoYW5kbGUiOiJ0d29vdGVyXzEyMyIsInRva2VuVHlwZSI6IkFDQ0VTUyIsImlhdCI6MTcxMjMyMzIzMiwiZXhwIjoxNzEyMzI1MDMyfQ.exampleToken",
			"eyJhbGciOiJIUzI1NiJ9.eyJoYW5kbGUiOiJ0d29vdGVyXzEyMyIsInRva2VuVHlwZSI6IlJFRlJFU0giLCJpYXQiOjE3MTIzMjMyMzIsImV4cCI6MTcxMjkyODAzMn0.refreshExampleToken",
			memberSummary
		);

		given(authService.signIn(any())).willReturn(response);

		// when & then
		mockMvc.perform(
				post("/api/auth/signin")
					.content(objectMapper.writeValueAsString(request))
					.contentType(MediaType.APPLICATION_JSON)
			)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.accessToken").exists())
			.andExpect(jsonPath("$.refreshToken").exists())
			.andExpect(jsonPath("$.member.handle").value("twooter_123"))
			.andDo(document("auth-signin",
				preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				requestFields(
					fieldWithPath("handle").type(JsonFieldType.STRING).description("사용자 핸들 (영문, 숫자, 밑줄(_) 사용 가능, 4~50자)"),
					fieldWithPath("password").type(JsonFieldType.STRING).description("사용자 비밀번호")
				),
				responseFields(
					fieldWithPath("accessToken").type(JsonFieldType.STRING).description("액세스 토큰 (JWT)"),
					fieldWithPath("refreshToken").type(JsonFieldType.STRING).description("리프레시 토큰 (JWT)"),
					fieldWithPath("member").type(JsonFieldType.OBJECT).description("회원 정보"),
					fieldWithPath("member.email").type(JsonFieldType.STRING).description("회원 이메일"),
					fieldWithPath("member.handle").type(JsonFieldType.STRING).description("회원 핸들"),
					fieldWithPath("member.nickname").type(JsonFieldType.STRING).description("회원 닉네임"),
					fieldWithPath("member.avatarPath").type(JsonFieldType.STRING).description("회원 프로필 이미지 경로")
				)
			));
	}
}
