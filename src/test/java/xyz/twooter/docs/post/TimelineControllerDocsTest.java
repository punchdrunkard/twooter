package xyz.twooter.docs.post;

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

import xyz.twooter.common.infrastructure.pagination.PaginationMetadata;
import xyz.twooter.docs.RestDocsSupport;
import xyz.twooter.member.presentation.dto.response.MemberBasic;
import xyz.twooter.post.presentation.dto.response.MediaEntity;
import xyz.twooter.post.presentation.dto.response.PostResponse;
import xyz.twooter.post.presentation.dto.response.TimelineItemResponse;
import xyz.twooter.post.presentation.dto.response.TimelineResponse;
import xyz.twooter.support.security.WithMockCustomUser;

@WithMockCustomUser
class TimelineControllerDocsTest extends RestDocsSupport {

	@DisplayName("나의 타임라인 조회 API")
	@Test
	void getMyTimeline() throws Exception {
		// given
		String cursor = "dXNlcjpVMDYxTkZUVDI=";
		Integer limit = 10;
		TimelineResponse response = givenTimelineResponse();

		given(timelineService.getTimeline(any(), any(), any(), any())).willReturn(response);

		// when & then
		mockMvc.perform(
				get("/api/timeline/me")
					.param("cursor", cursor)
					.param("limit", String.valueOf(limit))
					.header("Authorization",
						"Bearer eyJhbGciOiJIUzI1NiJ9.eyJoYW5kbGUiOiJ0d29vdGVyXzEyMyIsInRva2VuVHlwZSI6IkFDQ0VTUyIsImlhdCI6MTcxMjMyMzIzMiwiZXhwIjoxNzEyMzI1MDMyfQ.exampleToken")
					.contentType(MediaType.APPLICATION_JSON)
			)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.timeline").isArray())
			.andExpect(jsonPath("$.metadata").exists())
			.andDo(document("timeline-get",
				preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				requestHeaders(
					headerWithName("Authorization").description("액세스 토큰 (Bearer 타입)")
				),
				queryParameters(
					parameterWithName("cursor").optional()
						.description("이전 요청의 response 메타 데이터가 반환한 next_cursor 의 속성 (아무 값이 없을 경우 컬렉션이 첫 번째 페이지를 가져옴)"),
					parameterWithName("limit").optional().description("페이지당 반환될 아이템 수 (기본값: 20)")
				),
				responseFields(
					fieldWithPath("timeline").type(JsonFieldType.ARRAY).description("타임라인 아이템 목록"),
					fieldWithPath("metadata").type(JsonFieldType.OBJECT).description("페이지네이션 메타데이터")
				)
					.andWithPrefix("timeline[].", timelineItemFields())
					.andWithPrefix("timeline[].post.", postResponseFields())
					.andWithPrefix("timeline[].post.author.", memberBasicFields())
					.andWithPrefix("timeline[].post.mediaEntities[].", mediaEntityFields())
					.andWithPrefix("timeline[].repostBy.", memberBasicFields()) // repostBy는 repost 타입일 때만 존재
					.andWithPrefix("metadata.", paginationMetadataFields())
			));
	}

	@DisplayName("특정 유저의 타임라인 조회 API")
	@Test
	void getUserTimeline() throws Exception {
		// given
		String cursor = "dXNlcjpVMDYxTkZUVDI=";
		Integer limit = 10;
		TimelineResponse response = givenTimelineResponse();

		given(timelineService.getTimelineByHandle(any(), any(), any(), any())).willReturn(response);

		// when & then
		mockMvc.perform(
				get("/api/timeline/user/{userHandle}", "table_cleaner")
					.param("cursor", cursor)
					.param("limit", String.valueOf(limit))
					.contentType(MediaType.APPLICATION_JSON)
			)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.timeline").isArray())
			.andExpect(jsonPath("$.metadata").exists())
			.andDo(document("timeline-user-get",
				preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				pathParameters(
					parameterWithName("userHandle").description("조회 대상 유저의 handle")
				),
				queryParameters(
					parameterWithName("cursor").optional()
						.description("이전 요청의 response 메타 데이터가 반환한 next_cursor 의 속성 (아무 값이 없을 경우 컬렉션이 첫 번째 페이지를 가져옴)"),
					parameterWithName("limit").optional().description("페이지당 반환될 아이템 수 (기본값: 20)")
				),
				responseFields(
					fieldWithPath("timeline").type(JsonFieldType.ARRAY).description("타임라인 아이템 목록"),
					fieldWithPath("metadata").type(JsonFieldType.OBJECT).description("페이지네이션 메타데이터")
				)
					.andWithPrefix("timeline[].", timelineItemFields())
					.andWithPrefix("timeline[].post.", postResponseFields())
					.andWithPrefix("timeline[].post.author.", memberBasicFields())
					.andWithPrefix("timeline[].post.mediaEntities[].", mediaEntityFields())
					.andWithPrefix("timeline[].repostBy.", memberBasicFields()) // repostBy는 repost 타입일 때만 존재
					.andWithPrefix("metadata.", paginationMetadataFields())
			));
	}

	@DisplayName("현재 유저의 홈 타임라인 조회 API")
	@Test
	void getHomeTimeline() throws Exception {
	  // given
		String cursor = "dXNlcjpVMDYxTkZUVDI=";
		Integer limit = 10;
		TimelineResponse response = givenTimelineResponse();

		given(timelineService.getHomeTimeline(any(), any(), any())).willReturn(response);

		// when & then
		mockMvc.perform(
				get("/api/timeline/home")
					.param("cursor", cursor)
					.param("limit", String.valueOf(limit))
					.header("Authorization",
						"Bearer eyJhbGciOiJIUzI1NiJ9.eyJoYW5kbGUiOiJ0d29vdGVyXzEyMyIsInRva2VuVHlwZSI6IkFDQ0VTUyIsImlhdCI6MTcxMjMyMzIzMiwiZXhwIjoxNzEyMzI1MDMyfQ.exampleToken")
					.contentType(MediaType.APPLICATION_JSON)
			)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.timeline").isArray())
			.andExpect(jsonPath("$.metadata").exists())
			.andDo(document("timeline-get-home",
				preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				requestHeaders(
					headerWithName("Authorization").description("액세스 토큰 (Bearer 타입)")
				),
				queryParameters(
					parameterWithName("cursor").optional()
						.description("이전 요청의 response 메타 데이터가 반환한 next_cursor 의 속성 (아무 값이 없을 경우 컬렉션이 첫 번째 페이지를 가져옴)"),
					parameterWithName("limit").optional().description("페이지당 반환될 아이템 수 (기본값: 20)")
				),
				responseFields(
					fieldWithPath("timeline").type(JsonFieldType.ARRAY).description("타임라인 아이템 목록"),
					fieldWithPath("metadata").type(JsonFieldType.OBJECT).description("페이지네이션 메타데이터")
				)
					.andWithPrefix("timeline[].", timelineItemFields())
					.andWithPrefix("timeline[].post.", postResponseFields())
					.andWithPrefix("timeline[].post.author.", memberBasicFields())
					.andWithPrefix("timeline[].post.mediaEntities[].", mediaEntityFields())
					.andWithPrefix("timeline[].repostBy.", memberBasicFields()) // repostBy는 repost 타입일 때만 존재
					.andWithPrefix("metadata.", paginationMetadataFields())
			));
	}

	// === 필드 문서화 메서드 ===
	private List<FieldDescriptor> timelineItemFields() {
		return List.of(
			fieldWithPath("type").type(JsonFieldType.STRING).description("타임라인 아이템 타입 (post 또는 repost)"),
			fieldWithPath("createdAt").type(JsonFieldType.STRING).description("타임라인 아이템 생성 시간 (리포스트의 경우 리포스트한 시간)"),
			fieldWithPath("post").type(JsonFieldType.OBJECT).description("포스트 정보 (원본 포스트 또는 리포스트된 포스트)"),
			fieldWithPath("repostBy").type(JsonFieldType.OBJECT)
				.optional()
				.description("리포스트한 사용자 정보 (type이 'repost'일 때만 존재)")
		);
	}

	private List<FieldDescriptor> postResponseFields() {
		return List.of(
			fieldWithPath("id").type(JsonFieldType.NUMBER).description("포스트 Id"),
			fieldWithPath("author").type(JsonFieldType.OBJECT).description("포스트 작성자 정보"),
			fieldWithPath("content").type(JsonFieldType.STRING).description("포스트 내용"),
			fieldWithPath("likeCount").type(JsonFieldType.NUMBER).description("좋아요 수"),
			fieldWithPath("repostCount").type(JsonFieldType.NUMBER).description("리포스트 수"),
			fieldWithPath("viewCount").type(JsonFieldType.NUMBER).description("조회수"),
			fieldWithPath("mediaEntities").type(JsonFieldType.ARRAY).description("첨부된 미디어 정보 목록"),
			fieldWithPath("createdAt").type(JsonFieldType.STRING).description("포스트 생성 시간"),
			fieldWithPath("liked").type(JsonFieldType.BOOLEAN).description("현재 사용자의 좋아요 여부"),
			fieldWithPath("reposted").type(JsonFieldType.BOOLEAN).description("현재 사용자의 리포스트 여부"),
			fieldWithPath("deleted").type(JsonFieldType.BOOLEAN).description("삭제 여부")
		);
	}

	private List<FieldDescriptor> memberBasicFields() {
		return List.of(
			fieldWithPath("handle").type(JsonFieldType.STRING).description("사용자 핸들"),
			fieldWithPath("nickname").type(JsonFieldType.STRING).description("사용자 닉네임"),
			fieldWithPath("avatarPath").type(JsonFieldType.STRING).description("사용자 아바타 URL")
		);
	}

	private List<FieldDescriptor> mediaEntityFields() {
		return List.of(
			fieldWithPath("id").type(JsonFieldType.NUMBER).description("미디어 ID"),
			fieldWithPath("path").type(JsonFieldType.STRING).description("미디어 경로 URL")
		);
	}

	private List<FieldDescriptor> paginationMetadataFields() {
		return List.of(
			fieldWithPath("nextCursor").type(JsonFieldType.STRING).optional().description("다음 페이지를 가져오는 데 사용될 커서"),
			fieldWithPath("hasNext").type(JsonFieldType.BOOLEAN).description("다음 페이지가 존재하는지 여부")
		);
	}

	// === 응답 객체 생성 메서드 ===
	private TimelineResponse givenTimelineResponse() {
		MemberBasic author1 = MemberBasic.builder()
			.handle("table_cleaner")
			.nickname("테이블 청소 마스터")
			.avatarPath("https://cdn.twooter.xyz/media/avatar1")
			.build();

		MemberBasic author2 = MemberBasic.builder()
			.handle("table_specialist")
			.nickname("친절한 책상 정리 전문가")
			.avatarPath("https://cdn.twooter.xyz/media/avatar2")
			.build();

		MediaEntity media1 = MediaEntity.builder().id(101L).path("https://cdn.twooter.xyz/media/101.jpg").build();
		MediaEntity media2 = MediaEntity.builder().id(102L).path("https://cdn.twooter.xyz/media/102.jpg").build();
		MediaEntity media3 = MediaEntity.builder().id(103L).path("https://cdn.twooter.xyz/media/101.jpg").build();
		MediaEntity media4 = MediaEntity.builder().id(104L).path("https://cdn.twooter.xyz/media/102.jpg").build();

		PostResponse post1 = PostResponse.builder()
			.id(1L)
			.author(author1)
			.content("새 책상을 정리하다가 유용해 보이는 오래된 자료를 발견해서 이메일로 보냅니다.")
			.likeCount(15L)
			.repostCount(3L)
			.viewCount(42L)
			.mediaEntities(List.of(media1, media2))
			.createdAt(LocalDateTime.of(2025, 5, 5, 0, 0))
			.isLiked(true)
			.isReposted(true)
			.isDeleted(false)
			.build();

		TimelineItemResponse timelineItem1 = TimelineItemResponse.builder()
			.type("post")
			.createdAt(LocalDateTime.of(2025, 5, 5, 0, 0))
			.post(post1)
			.build();

		PostResponse post2 = PostResponse.builder()
			.id(2L)
			.author(author2)
			.content("비고: 누가 남긴 자료인지 모르겠네요.")
			.likeCount(15L)
			.repostCount(3L)
			.viewCount(42L)
			.mediaEntities(List.of(media3, media4))
			.createdAt(LocalDateTime.of(2025, 5, 5, 10, 10))
			.isLiked(true)
			.isReposted(false)
			.isDeleted(false)
			.build();

		TimelineItemResponse timelineItem2 = TimelineItemResponse.builder()
			.type("repost")
			.createdAt(LocalDateTime.of(2025, 5, 5, 5, 5))
			.post(post2)
			.repostBy(author1) // 리포스트한 사용자 정보
			.build();

		PaginationMetadata metadata = new PaginationMetadata("dGVhbTpDMDYxRkE1UEI=", true);

		return new TimelineResponse(List.of(timelineItem1, timelineItem2), metadata);
	}
}
