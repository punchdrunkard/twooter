package xyz.twooter.post.presentation;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import xyz.twooter.common.infrastructure.pagination.PaginationMetadata;
import xyz.twooter.member.presentation.dto.response.MemberBasic;
import xyz.twooter.post.presentation.dto.response.MediaEntity;
import xyz.twooter.post.presentation.dto.response.PostResponse;
import xyz.twooter.post.presentation.dto.response.TimelineItemResponse;
import xyz.twooter.post.presentation.dto.response.TimelineResponse;
import xyz.twooter.support.ControllerTestSupport;
import xyz.twooter.support.security.WithMockCustomUser;

@WithMockCustomUser
class TimelineControllerTest extends ControllerTestSupport {

	private static final LocalDateTime TEST_DATE = LocalDateTime.of(2025, 5, 5, 0, 0);
	private static final String VALID_CURSOR = "dXNlcjpVMDYxTkZUVDI=";

	@Nested
	@DisplayName("내 타임라인 조회 API")
	class GetMyTimelineTests {

		@Test
		@DisplayName("성공 - 첫 페이지 조회 (cursor 없음)")
		void shouldGetMyTimelineFirstPage() throws Exception {
			TimelineResponse response = createTimelineResponseWithNextCursor();

			given(timelineService.getTimeline(isNull(), anyInt(), any(), any())).willReturn(response);

			mockMvc.perform(
					get("/api/timeline/me")
						.param("limit", "20")
				)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.timeline").isArray())
				.andExpect(jsonPath("$.timeline").isNotEmpty())
				.andExpect(jsonPath("$.metadata.hasNext").value(response.getMetadata().isHasNext()))
				.andExpect(jsonPath("$.metadata.nextCursor").value(response.getMetadata().getNextCursor()));
		}

		@Test
		@DisplayName("성공 - 다음 페이지 조회 (cursor 사용)")
		void shouldGetMyTimelineWithCursor() throws Exception {
			TimelineResponse response = createTimelineResponseLastPage();

			given(timelineService.getTimeline(eq(VALID_CURSOR), anyInt(), any(), any())).willReturn(response);

			mockMvc.perform(
					get("/api/timeline/me")
						.param("cursor", VALID_CURSOR)
						.param("limit", "20")
				)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.timeline").isArray())
				.andExpect(jsonPath("$.metadata.hasNext").value(false))
				.andExpect(jsonPath("$.metadata.nextCursor").doesNotExist());
		}

		@Test
		@DisplayName("성공 - limit 파라미터 없이 조회 (기본값 적용)")
		void shouldGetMyTimelineWithDefaultLimit() throws Exception {
			TimelineResponse response = createTimelineResponseWithNextCursor();

			given(timelineService.getTimeline(isNull(), eq(20), any(), any())).willReturn(response);

			mockMvc.perform(get("/api/timeline/me"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.timeline").isArray())
				.andExpect(jsonPath("$.metadata").exists());
		}

		@Test
		@DisplayName("성공 - 빈 타임라인 조회")
		void shouldReturnEmptyTimeline() throws Exception {
			TimelineResponse emptyResponse = createEmptyTimelineResponse();

			given(timelineService.getTimeline(isNull(), anyInt(), any(), any())).willReturn(emptyResponse);

			mockMvc.perform(get("/api/timeline/me"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.timeline").isArray())
				.andExpect(jsonPath("$.timeline").isEmpty())
				.andExpect(jsonPath("$.metadata.hasNext").value(false));
		}

		@Test
		@DisplayName("실패 - limit이 1 미만일 때")
		void shouldFailWhenLimitIsZeroOrNegative() throws Exception {
			mockMvc.perform(
					get("/api/timeline/me")
						.param("limit", "0")
				)
				.andExpect(status().isBadRequest());
		}

		@Test
		@DisplayName("실패 - limit이 숫자가 아닐 때")
		void shouldFailWhenLimitIsNotNumber() throws Exception {
			mockMvc.perform(
					get("/api/timeline/me")
						.param("limit", "not-a-number")
				)
				.andExpect(status().isBadRequest());
		}

		@Test
		@DisplayName("성공 - 다양한 타입의 타임라인 항목들 조회")
		void shouldGetTimelineWithMixedItemTypes() throws Exception {
			TimelineResponse response = createMixedTimelineResponse();

			given(timelineService.getTimeline(isNull(), anyInt(), any(), any())).willReturn(response);

			mockMvc.perform(get("/api/timeline/me"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.timeline").isArray())
				.andExpect(jsonPath("$.timeline[0].type").value("post"))
				.andExpect(jsonPath("$.timeline[1].type").value("repost"))
				.andExpect(jsonPath("$.timeline[1].repostBy").exists());
		}
	}

	@Nested
	@DisplayName("특정 유저 타임라인 조회 API")
	class GetUserTimelineTests {
		@Test
		@DisplayName("성공 - 첫 페이지 조회 (cursor 없음)")
		void shouldGetMyTimelineFirstPage() throws Exception {
			TimelineResponse response = createTimelineResponseWithNextCursor();

			given(timelineService.getTimelineByHandle(isNull(), anyInt(), any(), any())).willReturn(response);

			mockMvc.perform(
					get("/api/timeline/user/{userHandle}", "table_cleaner")
						.param("limit", "20")
				)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.timeline").isArray())
				.andExpect(jsonPath("$.timeline").isNotEmpty())
				.andExpect(jsonPath("$.metadata.hasNext").value(response.getMetadata().isHasNext()))
				.andExpect(jsonPath("$.metadata.nextCursor").value(response.getMetadata().getNextCursor()));
		}
	}

	// === 헬퍼 메서드 ===

	private TimelineResponse createTimelineResponseWithNextCursor() {
		MemberBasic author = createMemberBasic("table_cleaner", "테이블 청소 마스터");
		PostResponse post = createPostResponse(1L, author, "첫 번째 포스트입니다.");

		List<TimelineItemResponse> timeline = List.of(
			createTimelineItem("post", post, null)
		);

		PaginationMetadata metadata = PaginationMetadata.builder()
			.nextCursor("dGVhbTpDMDYxRkE1UEI=")
			.hasNext(true)
			.build();

		return TimelineResponse.builder()
			.timeline(timeline)
			.metadata(metadata)
			.build();
	}

	private TimelineResponse createTimelineResponseLastPage() {
		MemberBasic author = createMemberBasic("late_night_coder", "야간 코더");
		PostResponse post = createPostResponse(3L, author, "마지막 포스트입니다.");

		List<TimelineItemResponse> timeline = List.of(
			createTimelineItem("post", post, null)
		);

		PaginationMetadata metadata = PaginationMetadata.builder()
			.nextCursor(null)
			.hasNext(false)
			.build();

		return TimelineResponse.builder()
			.timeline(timeline)
			.metadata(metadata)
			.build();
	}

	private TimelineResponse createEmptyTimelineResponse() {
		PaginationMetadata metadata = PaginationMetadata.builder()
			.nextCursor(null)
			.hasNext(false)
			.build();

		return TimelineResponse.builder()
			.timeline(List.of())
			.metadata(metadata)
			.build();
	}

	private TimelineResponse createMixedTimelineResponse() {
		MemberBasic author1 = createMemberBasic("original_poster", "원본 작성자");
		MemberBasic author2 = createMemberBasic("reposter", "리포스터");

		PostResponse originalPost = createPostResponse(1L, author1, "원본 포스트 내용입니다.");

		List<TimelineItemResponse> timeline = List.of(
			// 일반 포스트
			createTimelineItem("post", originalPost, null),
			// 리포스트
			createTimelineItem("repost", originalPost, author2)
		);

		PaginationMetadata metadata = PaginationMetadata.builder()
			.nextCursor("dGVhbTpDMDYxRkE1UEI=")
			.hasNext(true)
			.build();

		return TimelineResponse.builder()
			.timeline(timeline)
			.metadata(metadata)
			.build();
	}

	private TimelineItemResponse createTimelineItem(String type, PostResponse post, MemberBasic repostBy) {
		return TimelineItemResponse.builder()
			.type(type)
			.createdAt(TEST_DATE)
			.post(post)
			.repostBy(repostBy)
			.build();
	}

	private PostResponse createPostResponse(Long id, MemberBasic author, String content) {
		List<MediaEntity> mediaList = List.of(
			MediaEntity.builder().id(101L).path("https://cdn.twooter.xyz/media/101.jpg").build(),
			MediaEntity.builder().id(102L).path("https://cdn.twooter.xyz/media/102.jpg").build()
		);

		return PostResponse.builder()
			.id(id)
			.author(author)
			.content(content)
			.likeCount(15L)
			.isLiked(true)
			.repostCount(3L)
			.isReposted(false)
			.viewCount(42L)
			.mediaEntities(mediaList)
			.createdAt(TEST_DATE)
			.isDeleted(false)
			.build();
	}

	private MemberBasic createMemberBasic(String handle, String nickname) {
		return MemberBasic.builder()
			.handle(handle)
			.nickname(nickname)
			.avatarPath("https://cdn.twooter.xyz/media/avatar")
			.build();
	}
}
