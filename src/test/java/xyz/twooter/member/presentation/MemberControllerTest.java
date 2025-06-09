package xyz.twooter.member.presentation;

import static org.mockito.BDDMockito.*;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import xyz.twooter.member.presentation.dto.request.FollowRequest;
import xyz.twooter.member.presentation.dto.response.FollowResponse;
import xyz.twooter.member.presentation.dto.response.MemberWithRelationResponse;
import xyz.twooter.member.presentation.dto.response.UnFollowResponse;
import xyz.twooter.support.ControllerTestSupport;
import xyz.twooter.support.security.WithMockCustomUser;

@WithMockCustomUser
class MemberControllerTest extends ControllerTestSupport {

	static final LocalDateTime currentTime = LocalDateTime.of(2025, 5, 5, 10, 10);

	@Nested
	@DisplayName("멤버 팔로우 API")
	class FollowMemberTests {

		@DisplayName("성공 - 멤버를 팔로우할 수 있다.")
		@Test
		void shouldFollowMember() throws Exception {
			// given
			FollowRequest request = givenFollowRequest();
			FollowResponse response = givenFollowResponse();

			given(followService.followMember(any(), anyLong())).willReturn(response);

			// when & then
			mockMvc.perform(post("/api/members/follow")
					.content(objectMapper.writeValueAsString(request))
					.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.targetMemberId").value(response.getTargetMemberId()));
		}

		@DisplayName("실패 - target member id는 null이 될 수 없다.")
		@Test
		void shouldThrowErrorWhenTargetMemberIdIsNull() throws Exception {
			// given
			FollowRequest targetMemberIdNullRequest = FollowRequest.builder()
				.targetMemberId(null)
				.build();

			// when & then
			mockMvc.perform(post("/api/members/follow")
					.content(objectMapper.writeValueAsString(targetMemberIdNullRequest))
					.contentType(MediaType.APPLICATION_JSON)
				)
				.andExpect(status().isBadRequest());
		}
	}

	@Nested
	@DisplayName("멤버 언팔로우 API")
	class UnfollowMemberTests {

		@DisplayName("성공 - 멤버를 언팔로우할 수 있다.")
		@Test
		void shouldUnfollowMember() throws Exception {
			// given
			Long targetMemberId = 1L;
			given(followService.unfollowMember(any(), anyLong())).willReturn(UnFollowResponse.of(targetMemberId));

			// when & then
			mockMvc.perform(delete("/api/members/follow/{targetMemberId}", targetMemberId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.targetMemberId").value(targetMemberId));
		}
	}

	@Nested
	@DisplayName("멤버 팔로워 조회 API")
	class GetFollowersTests {

		@DisplayName("성공 - 멤버의 팔로워 목록을 조회할 수 있다.")
		@Test
		void shouldGetFollowers() throws Exception {
			// given
			Long memberId = 1L;
			String cursor = null;
			Integer limit = 20;

			given(followService.getFollowers(any(), any(), any(), any()))
				.willReturn(MemberWithRelationResponse.builder().build());

			// when & then
			mockMvc.perform(get("/api/members/{memberId}/followers", memberId)
					.param("cursor", cursor)
					.param("limit", String.valueOf(limit)))
				.andExpect(status().isOk());
		}
	}

	@Nested
	@DisplayName("멤버 팔로잉 조회 API")
	class GetFollowingTests {

		@DisplayName("성공 - 멤버의 팔로잉 목록을 조회할 수 있다.")
		@Test
		void shouldGetFollowing() throws Exception {
			// given
			Long memberId = 1L;
			String cursor = null;
			Integer limit = 20;

			given(followService.getFollowing(any(), any(), any(), any()))
				.willReturn(MemberWithRelationResponse.builder().build());

			// when & then
			mockMvc.perform(get("/api/members/{memberId}/followings", memberId)
					.param("cursor", cursor)
					.param("limit", String.valueOf(limit)))
				.andExpect(status().isOk());
		}
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
