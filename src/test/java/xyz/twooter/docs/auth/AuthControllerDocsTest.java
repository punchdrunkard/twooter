package xyz.twooter.docs.auth;

import static org.mockito.BDDMockito.*;
import static org.springframework.restdocs.headers.HeaderDocumentation.*;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.*;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.*;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.checkerframework.checker.units.qual.A;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;

import xyz.twooter.auth.presentation.dto.request.SignInRequest;
import xyz.twooter.auth.presentation.dto.request.SignUpRequest;
import xyz.twooter.auth.presentation.dto.request.TokenReissueRequest;
import xyz.twooter.auth.presentation.dto.response.LogoutResponse;
import xyz.twooter.auth.presentation.dto.response.SignInResponse;
import xyz.twooter.auth.presentation.dto.response.SignUpInfoResponse;
import xyz.twooter.auth.presentation.dto.response.TokenReissueResponse;
import xyz.twooter.docs.RestDocsSupport;
import xyz.twooter.member.presentation.dto.response.MemberSummaryResponse;

class AuthControllerDocsTest extends RestDocsSupport {

	static final String TEST_ACCESS_TOKEN = "Bearer <ACCESS_TOKEN>";
	public static final String REFRESH_TOKEN = "<REFRESH_TOKEN>";
	public static final String ACCESS_TOKEN = "<ACCESS_TOKEN>";

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
			MemberSummaryResponse.builder()
				.id(123L)
				.avatarPath("testpath")
				.handle("twooter_123")
				.email("user@example.com")
				.nickname("테이블 청소 마스터")
				.build()
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
					fieldWithPath("password").type(JsonFieldType.STRING)
						.description("사용자 비밀번호 (영문 대/소문자, 숫자, 특수문자 포함 8자 이상)"),
					fieldWithPath("handle").type(JsonFieldType.STRING)
						.description("사용자 핸들 (영문, 숫자, 밑줄(_) 사용 가능, 4~50자)")
				),
				responseFields(
					fieldWithPath("member").type(JsonFieldType.OBJECT).description("회원 정보"),
					fieldWithPath("member.id").type(JsonFieldType.NUMBER).description("회원 고유 ID"),
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

		MemberSummaryResponse memberSummaryResponse = MemberSummaryResponse.builder()
			.id(123L)
			.email("user@example.com")
			.handle("twooter_123")
			.nickname("twooter_123")
			.avatarPath("testpath")
			.build();

		SignInResponse response =
			new SignInResponse(
				ACCESS_TOKEN,
				REFRESH_TOKEN,
				memberSummaryResponse
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
					fieldWithPath("handle").type(JsonFieldType.STRING)
						.description("사용자 핸들 (영문, 숫자, 밑줄(_) 사용 가능, 4~50자)"),
					fieldWithPath("password").type(JsonFieldType.STRING).description("사용자 비밀번호")
				),
				responseFields(
					fieldWithPath("accessToken").type(JsonFieldType.STRING).description("액세스 토큰 (JWT)"),
					fieldWithPath("refreshToken").type(JsonFieldType.STRING).description("리프레시 토큰 (JWT)"),
					fieldWithPath("member").type(JsonFieldType.OBJECT).description("회원 정보"),
					fieldWithPath("member.id").type(JsonFieldType.NUMBER).description("회원 고유 ID"),
					fieldWithPath("member.email").type(JsonFieldType.STRING).description("회원 이메일"),
					fieldWithPath("member.handle").type(JsonFieldType.STRING).description("회원 핸들"),
					fieldWithPath("member.nickname").type(JsonFieldType.STRING).description("회원 닉네임"),
					fieldWithPath("member.avatarPath").type(JsonFieldType.STRING).description("회원 프로필 이미지 경로")
				)
			));
	}

	@DisplayName("토큰 재발급 API")
	@Test
	void reissueToken() throws Exception {
		// given
		TokenReissueRequest request = TokenReissueRequest.builder()
			.refreshToken(REFRESH_TOKEN)
			.build();

		TokenReissueResponse response = new TokenReissueResponse("<NEW_ACCESS_TOKEN>", "<NEW_REFRESH_TOKEN>");

		given(authService.reissueToken(any())).willReturn(response);

		// when & then
		mockMvc.perform(
				post("/api/auth/reissue")
					.content(objectMapper.writeValueAsString(request))
					.contentType(MediaType.APPLICATION_JSON)
			)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.accessToken").exists())
			.andExpect(jsonPath("$.refreshToken").exists())
			.andDo(document("auth-reissue",
				preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				requestFields(
					fieldWithPath("refreshToken").type(JsonFieldType.STRING).description("유효한 리프레시 토큰 (JWT)")
				),
				responseFields(
					fieldWithPath("accessToken").type(JsonFieldType.STRING).description("새로 발급된 액세스 토큰 (JWT)"),
					fieldWithPath("refreshToken").type(JsonFieldType.STRING).description("새로 발급된 리프레시 토큰 (JWT)")
				)
			));
	}

	@DisplayName("로그아웃 API")
	@Test
	void logout() throws Exception {
		// given
		LogoutResponse response = new LogoutResponse("user_handle");

		given(authService.logout(ACCESS_TOKEN)).willReturn(response);

		// when & then
		mockMvc.perform(
				post("/api/auth/logout")
					.header("Authorization", "Bearer " + ACCESS_TOKEN)
					.contentType(MediaType.APPLICATION_JSON)
			)
			.andExpect(status().isOk())
			.andDo(document("auth-logout",
				preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				requestHeaders(
					headerWithName("Authorization").description("Bearer 인증 토큰")
				),
				responseFields(
					fieldWithPath("userHandle").type(JsonFieldType.STRING)
						.description("로그아웃된 유저의 핸들 (토큰이 유효하지 않거나 처리 중 오류가 발생한 경우 'unknown' 반환)")
				)
			));
	}
}
