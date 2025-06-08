package xyz.twooter.docs.member;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.restdocs.headers.HeaderDocumentation.*;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.*;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.*;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import xyz.twooter.docs.RestDocsSupport;
import xyz.twooter.member.presentation.dto.request.FollowRequest;
import xyz.twooter.member.presentation.dto.response.FollowResponse;
import xyz.twooter.member.presentation.dto.response.UnFollowResponse;

class MemberControllerDocsTest extends RestDocsSupport {

	static final LocalDateTime currentTime = LocalDateTime.of(2025, 5, 5, 10, 10);

	@DisplayName("멤버 팔로우")
	@Test
	void followMember() throws Exception {
		// given
		FollowRequest request = givenFollowRequest();
		FollowResponse response = givenFollowResponse();

		given(memberService.followMember(any(), anyLong())).willReturn(response);

		// when  & then
		mockMvc.perform(post("/api/members/follow")
				.content(objectMapper.writeValueAsString(request))
				.header("Authorization",
					"Bearer eyJhbGciOiJIUzI1NiJ9.eyJoYW5kbGUiOiJ0d29vdGVyXzEyMyIsInRva2VuVHlwZSI6IkFDQ0VTUyIsImlhdCI6MTcxMjMyMzIzMiwiZXhwIjoxNzEyMzI1MDMyfQ.exampleToken")
				.contentType(MediaType.APPLICATION_JSON)
			)
			.andExpect(status().isCreated())
			.andDo(document("follow-member",
				preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				requestHeaders(
					headerWithName("Authorization").description("인증 토큰")
				),
				requestFields(
					fieldWithPath("targetMemberId").description("팔로우할 대상 멤버의 ID")
				),
				responseFields(
					fieldWithPath("targetMemberId").description("팔로우한 대상 멤버의 ID"),
					fieldWithPath("followedAt").description("팔로우한 시간")
				)
			));
	}

	@DisplayName("멤버 언팔로우")
	@Test
	void unfollowMember() throws Exception {
		// given
		Long targetMemberId = 1L;
		UnFollowResponse response = UnFollowResponse.of(targetMemberId);

		given(memberService.unfollowMember(any(), anyLong())).willReturn(response);

		// when  & then
		mockMvc.perform(delete("/api/members/follow/{targetMemberId}", targetMemberId)
				.header("Authorization",
					"Bearer eyJhbGciOiJIUzI1NiJ9.eyJoYW5kbGUiOiJ0d29vdGVyXzEyMyIsInRva2VuVHlwZSI6IkFDQ0VTUyIsImlhdCI6MTcxMjMyMzIzMiwiZXhwIjoxNzEyMzI1MDMyfQ.exampleToken")
			)
			.andExpect(status().isOk())
			.andDo(document("unfollow-member",
				preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				requestHeaders(
					headerWithName("Authorization").description("인증 토큰")
				),
				pathParameters(
					parameterWithName("targetMemberId").description("언팔로우할 대상 멤버의 ID")
				),
				responseFields(
					fieldWithPath("targetMemberId").description("언팔로우한 대상 멤버의 ID"),
					fieldWithPath("unfollowedAt").description("언팔로우한 시간")
				)
			));
	}

	private static FollowResponse givenFollowResponse() {
		return FollowResponse.builder()
			.targetMemberId(1L)
			.followedAt(currentTime)
			.build();
	}

	private static FollowRequest givenFollowRequest() {
		return FollowRequest.builder()
			.targetMemberId(1L)
			.build();
	}
}
