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
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.payload.JsonFieldType;

import xyz.twooter.docs.RestDocsSupport;
import xyz.twooter.member.presentation.dto.request.FollowRequest;
import xyz.twooter.member.presentation.dto.response.FollowResponse;
import xyz.twooter.member.presentation.dto.response.FollowerProfile;
import xyz.twooter.member.presentation.dto.response.FollowerResponse;
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

	@DisplayName("해당 멤버의 팔로워 목록 조회")
	@Test
	void getFollowers() throws Exception {
		// given
		FollowerResponse response = givenFollowersResponse();

		String cursor = "dXNlcjpVMDYxTkZUVDI=";
		Integer limit = 10;

		given(memberService.getFollowers(any(), any(), any(), any()))
			.willReturn(response); // 실제 FollowerProfile 객체로 대체 필요

		// when & then
		mockMvc.perform(
				get("/api/members/{memberId}/followers", 1L)
					.param("cursor", cursor)
					.param("limit", limit.toString())
					.contentType(MediaType.APPLICATION_JSON)
			)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.followers").isArray())
			.andExpect(jsonPath("$.metadata").exists())
			.andDo(document("get-followers",
				preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				pathParameters(
					parameterWithName("memberId").description("팔로워 목록을 조회할 멤버의 ID")
				),
				queryParameters(
					parameterWithName("cursor").optional()
						.description("이전 요청의 response 메타 데이터가 반환한 next_cursor 의 속성 (아무 값이 없을 경우 컬렉션이 첫 번째 페이지를 가져옴)"),
					parameterWithName("limit").optional().description("조회할 최대 팔로워 수 (기본값: 20, 최소값: 1)")
				),
				responseFields(
					fieldWithPath("followers").description("팔로워 목록"),
					fieldWithPath("metadata").type(JsonFieldType.OBJECT).description("페이지네이션 메타데이터")
				)
					.andWithPrefix("followers[].", followersItemFields())
					.andWithPrefix("metadata.", paginationMetadataFields())
			));
	}

	// ======= 필드 문서화 메서드 ======
	private List<FieldDescriptor> followersItemFields() {
		return List.of(
			fieldWithPath("id").type(JsonFieldType.NUMBER).description("팔로워의 ID"),
			fieldWithPath("handle").type(JsonFieldType.STRING).description("팔로워의 핸들"),
			fieldWithPath("nickname").type(JsonFieldType.STRING).description("팔로워의 닉네임"),
			fieldWithPath("avatarPath").type(JsonFieldType.STRING).description("팔로워의 아바타 이미지 경로"),
			fieldWithPath("bio").type(JsonFieldType.STRING).description("팔로워의 바이오"),
			fieldWithPath("followingByMe").type(JsonFieldType.BOOLEAN).description("내가 이 팔로워를 팔로우하고 있는지 여부"),
			fieldWithPath("followsMe").type(JsonFieldType.BOOLEAN).description("이 팔로워가 나를 팔로우하고 있는지 여부")
		);
	}

	// ==== 응답 객체 생성 메서드 ====
	private static FollowerResponse givenFollowersResponse() {
		FollowerProfile followerProfile1 = FollowerProfile.builder()
			.id(1L)
			.handle("cameraman")
			.nickname("카메라맨")
			.avatarPath("test_avatar_path.jpg")
			.bio("카메라맨의 바이오")
			.isFollowingByMe(false)
			.followsMe(false)
			.build();
		FollowerProfile followerProfile2 = FollowerProfile.builder()
			.id(2L)
			.handle("movie_journalist")
			.nickname("영화 상영 기자")
			.avatarPath("test_avatar_path.jpg")
			.bio("영화 상영 기자의 바이오")
			.isFollowingByMe(false)
			.followsMe(false)
			.build();
		FollowerProfile followerProfile3 = FollowerProfile.builder()
			.id(3L)
			.handle("movie_critic")
			.nickname("영화 평론가")
			.avatarPath("test_avatar_path.jpg")
			.bio("영화 평론가의 바이오")
			.isFollowingByMe(true)
			.followsMe(false)
			.build();
		FollowerResponse response = FollowerResponse.of(List.of(followerProfile1, followerProfile2, followerProfile3),
			10);
		return response;
	}

	private FollowResponse givenFollowResponse() {
		return FollowResponse.builder()
			.targetMemberId(1L)
			.followedAt(currentTime)
			.build();
	}

	private FollowRequest givenFollowRequest() {
		return FollowRequest.builder()
			.targetMemberId(1L)
			.build();
	}

	private List<FieldDescriptor> paginationMetadataFields() {
		return List.of(
			fieldWithPath("nextCursor").type(JsonFieldType.STRING).optional().description("다음 페이지를 가져오는 데 사용될 커서"),
			fieldWithPath("hasNext").type(JsonFieldType.BOOLEAN).description("다음 페이지가 존재하는지 여부")
		);
	}
}
