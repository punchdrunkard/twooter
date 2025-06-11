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
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.payload.JsonFieldType;

import xyz.twooter.common.infrastructure.pagination.PaginationMetadata;
import xyz.twooter.docs.RestDocsSupport;
import xyz.twooter.media.presentation.dto.response.MediaSimpleResponse;
import xyz.twooter.member.presentation.dto.response.MemberBasic;
import xyz.twooter.member.presentation.dto.response.MemberSummaryResponse;
import xyz.twooter.post.presentation.dto.request.PostCreateRequest;
import xyz.twooter.post.presentation.dto.request.ReplyCreateRequest;
import xyz.twooter.post.presentation.dto.response.MediaEntity;
import xyz.twooter.post.presentation.dto.response.PostCreateResponse;
import xyz.twooter.post.presentation.dto.response.PostDeleteResponse;
import xyz.twooter.post.presentation.dto.response.PostLikeResponse;
import xyz.twooter.post.presentation.dto.response.PostReplyCreateResponse;
import xyz.twooter.post.presentation.dto.response.PostResponse;
import xyz.twooter.post.presentation.dto.response.PostThreadResponse;
import xyz.twooter.post.presentation.dto.response.RepostCreateResponse;
import xyz.twooter.support.security.WithMockCustomUser;

@WithMockCustomUser
class PostControllerDocsTest extends RestDocsSupport {

	final MemberSummaryResponse TEST_MEMBER_SUMMARY = MemberSummaryResponse.builder()
		.id(123L)
		.email("test@test.com")
		.handle("table_cleaner")
		.nickname("테이블 청소 마스터")
		.avatarPath("https://cdn.twooter.xyz/media/avatar")
		.build();
	final String TEST_ACCESS_TOKEN = "Bearer eyJhbGciOiJIUzI1NiJ9.eyJoYW5kbGUiOiJ0d29vdGVyXzEyMyIsInRva2VuVHlwZSI6IkFDQ0VTUyIsImlhdCI6MTcxMjMyMzIzMiwiZXhwIjoxNzEyMzI1MDMyfQ.exampleToken";

	@DisplayName("포스트 생성 API")
	@Test
	void createPost() throws Exception {
		// given
		PostCreateRequest request = givenPostCreateRequest();
		PostCreateResponse response = givenPostCreateResponse();

		given(postService.createPost(any(), any())).willReturn(response);

		// when & then
		mockMvc.perform(
				post("/api/posts")
					.content(objectMapper.writeValueAsString(request))
					.header("Authorization", TEST_ACCESS_TOKEN)
					.contentType(MediaType.APPLICATION_JSON)
			)
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.id").value(response.getId()))
			.andExpect(jsonPath("$.content").value(response.getContent()))
			.andExpect(jsonPath("$.media").isArray())
			.andDo(document("post-create",
				preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				requestHeaders(
					headerWithName("Authorization").description("액세스 토큰 (Bearer 타입)")
				),
				requestFields(
					fieldWithPath("content").type(JsonFieldType.STRING)
						.description("포스트 내용 (미디어가 없는 경우 필수, 최대 500자)"),
					fieldWithPath("media").type(JsonFieldType.ARRAY).optional()
						.description("첨부된 미디어 파일 URL (내용이 비어있는 경우 필수, 최대 4개), 파일 업로드 API를 이용해, 업로드 후 링크를 첨부")
				),
				responseFields(
					fieldWithPath("id").type(JsonFieldType.NUMBER)
						.description("생성된 포스트 ID"),
					fieldWithPath("content").type(JsonFieldType.STRING)
						.description("포스트 내용"),
					fieldWithPath("author").type(JsonFieldType.OBJECT)
						.description("포스트 작성자 정보"),
					fieldWithPath("media").type(JsonFieldType.ARRAY)
						.description("첨부된 미디어 정보 목록"),
					fieldWithPath("createdAt").type(JsonFieldType.STRING)
						.description("포스트 생성 시간")
				)
					.andWithPrefix("author.", authorEntityFieldsWithEmail())
					.andWithPrefix("media[].", mediaFields()),
				responseHeaders(
					headerWithName("Location").description("생성된 리소스의 URI")
				)
			));
	}

	@DisplayName("포스트 조회 API")
	@Test
	void getPost() throws Exception {
		// given
		Long postId = 1L;
		PostResponse response = givenPostResponse(postId);

		given(postService.getPost(anyLong(), any())).willReturn(response);

		// when & then
		mockMvc.perform(
				get("/api/posts/{postId}", postId))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content").value(response.getContent()))
			.andExpect(jsonPath("$.likeCount").value(response.getLikeCount()))
			.andExpect(jsonPath("$.mediaEntities").isArray())
			.andDo(document("post-get",
				preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				pathParameters(
					parameterWithName("postId").description("조회할 포스트 ID")
				),
				responseFields(
					fieldWithPath("id").type(JsonFieldType.NUMBER)
						.description("포스트 Id"),
					fieldWithPath("content").type(JsonFieldType.STRING)
						.description("포스트 내용"),
					fieldWithPath("author").type(JsonFieldType.OBJECT)
						.description("포스트 작성자 정보"),
					fieldWithPath("likeCount").type(JsonFieldType.NUMBER)
						.description("좋아요 수"),
					fieldWithPath("liked").type(JsonFieldType.BOOLEAN)
						.description("현재 사용자의 좋아요 여부"),
					fieldWithPath("repostCount").type(JsonFieldType.NUMBER)
						.description("리포스트 수"),
					fieldWithPath("reposted").type(JsonFieldType.BOOLEAN)
						.description("현재 사용자의 리포스트 여부"),
					fieldWithPath("viewCount").type(JsonFieldType.NUMBER)
						.description("조회수"),
					fieldWithPath("mediaEntities").type(JsonFieldType.ARRAY)
						.description("첨부된 미디어 정보 목록"),
					fieldWithPath("createdAt").type(JsonFieldType.STRING)
						.description("포스트 생성 시간"),
					fieldWithPath("deleted").type(JsonFieldType.BOOLEAN)
						.description("삭제 여부")
				)
					.andWithPrefix("author.", authorEntityFields())
					.andWithPrefix("mediaEntities[].", mediaFields())
			));
	}

	@DisplayName("포스트 좋아요 API")
	@Test
	void likePost() throws Exception {
		// given
		Long postId = 1L;
		PostLikeResponse response = PostLikeResponse.builder()
			.postId(postId)
			.isLiked(true)
			.build();

		// when
		given(postLikeService.toggleLikeAndCount(anyLong(), any())).willReturn(response);

		// then
		mockMvc.perform(
				patch("/api/posts/{postId}/like", postId)
					.header("Authorization", TEST_ACCESS_TOKEN)
			)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.postId").value(postId))
			.andExpect(jsonPath("$.isLiked").value(true))
			.andDo(document("post-like",
				preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				requestHeaders(
					headerWithName("Authorization").description("액세스 토큰 (Bearer 타입)")
				),
				pathParameters(
					parameterWithName("postId").description("좋아요/좋아요 취소할 포스트 ID")
				),
				responseFields(
					fieldWithPath("postId").type(JsonFieldType.NUMBER)
						.description("포스트 ID"),
					fieldWithPath("isLiked").type(JsonFieldType.BOOLEAN)
						.description("현재 사용자의 좋아요 여부")
				)
			));
	}

	@DisplayName("포스트 리포스트 API")
	@Test
	void repost() throws Exception {
		// given
		Long repostId = 1L;
		Long originalPostId = 2L;

		RepostCreateResponse response = RepostCreateResponse.builder()
			.repostId(repostId)
			.originalPostId(originalPostId)
			.repostedAt(LocalDateTime.of(2025, 5, 5, 0, 0))
			.build();

		// when
		given(postService.repostAndIncreaseCount(anyLong(), any())).willReturn(response);

		// then
		mockMvc.perform(
				post("/api/posts/{postId}/repost", repostId)
					.header("Authorization", TEST_ACCESS_TOKEN)
			)
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.repostId").value(repostId))
			.andExpect(jsonPath("$.originalPostId").value(originalPostId))
			.andExpect(jsonPath("$.repostedAt").value("2025-05-05T00:00:00"))
			.andDo(document("post-repost",
				preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				requestHeaders(
					headerWithName("Authorization").description("액세스 토큰 (Bearer 타입)")
				),
				pathParameters(
					parameterWithName("postId").description("리포스트할 대상 포스트 ID")
				),
				responseFields(
					fieldWithPath("repostId").type(JsonFieldType.NUMBER)
						.description("생성된 리포스트 ID"),
					fieldWithPath("originalPostId").type(JsonFieldType.NUMBER)
						.description("원본 포스트 ID"),
					fieldWithPath("repostedAt").type(JsonFieldType.STRING)
						.description("리포스트 생성 시간")
				)
			));
	}

	@DisplayName("포스트 삭제 API")
	@Test
	void deletePost() throws Exception {
		// given
		Long postId = 1L;
		given(postService.deletePost(anyLong(), any())).willReturn(
			PostDeleteResponse.builder()
				.postId(postId)
				.build()
		);

		// when & then
		mockMvc.perform(
				delete("/api/posts/{postId}", postId)
					.header("Authorization", TEST_ACCESS_TOKEN))
			.andExpect(status().isOk())
			.andDo(document("post-delete",
				preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				requestHeaders(
					headerWithName("Authorization").description("액세스 토큰 (Bearer 타입)")
				),
				pathParameters(
					parameterWithName("postId").description("삭제할 포스트 ID")
				),
				responseFields(
					fieldWithPath("postId").type(JsonFieldType.NUMBER)
						.description("삭제된 포스트 ID")
				)
			));
	}

	@DisplayName("답글 생성 API")
	@Test
	void reply() throws Exception {
		// given
		Long parentId = 1L;
		ReplyCreateRequest request = ReplyCreateRequest.builder()
			.content("답글 내용입니다.")
			.parentId(parentId)
			.media(new String[] {"https://cdn.twooter.xyz/media/201.jpg"})
			.build();

		PostReplyCreateResponse response = PostReplyCreateResponse.builder()
			.id(2L)
			.content("답글 내용입니다.")
			.author(TEST_MEMBER_SUMMARY)
			.media(new MediaSimpleResponse[] {
				MediaSimpleResponse.builder()
					.mediaId(201L)
					.mediaUrl("https://cdn.twooter.xyz/media/201.jpg")
					.build()
			})
			.parentId(parentId)
			.createdAt(LocalDateTime.of(2025, 5, 5, 0, 0))
			.build();

		given(postService.createReply(any(), any())).willReturn(response);

		// when & then
		mockMvc.perform(
				post("/api/posts/replies")
					.content(objectMapper.writeValueAsString(request))
					.header("Authorization", TEST_ACCESS_TOKEN)
					.contentType(MediaType.APPLICATION_JSON)
			)
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.id").value(response.getId()))
			.andExpect(jsonPath("$.content").value(response.getContent()))
			.andExpect(jsonPath("$.media").isArray())
			.andDo(document("post-reply-create",
				preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				requestHeaders(
					headerWithName("Authorization").description("액세스 토큰 (Bearer 타입)")
				),
				requestFields(
					fieldWithPath("parentId").type(JsonFieldType.NUMBER)
						.description("답글의 부모 포스트 ID (필수)"),
					fieldWithPath("content").type(JsonFieldType.STRING)
						.description("포스트 내용 (미디어가 없는 경우 필수, 최대 500자)"),
					fieldWithPath("media").type(JsonFieldType.ARRAY).optional()
						.description("첨부된 미디어 파일 URL (내용이 비어있는 경우 필수, 최대 4개), 파일 업로드 API를 이용해, 업로드 후 링크를 첨부")
				),
				responseFields(
					fieldWithPath("id").type(JsonFieldType.NUMBER)
						.description("생성된 포스트 ID"),
					fieldWithPath("content").type(JsonFieldType.STRING)
						.description("포스트 내용"),
					fieldWithPath("author").type(JsonFieldType.OBJECT)
						.description("포스트 작성자 정보"),
					fieldWithPath("media").type(JsonFieldType.ARRAY)
						.description("첨부된 미디어 정보 목록"),
					fieldWithPath("createdAt").type(JsonFieldType.STRING)
						.description("포스트 생성 시간"),
					fieldWithPath("parentId").type(JsonFieldType.NUMBER)
						.description("생성된 답글의 부모 ID")
				)
					.andWithPrefix("author.", authorEntityFieldsWithEmail())
					.andWithPrefix("media[].", mediaFields()),
				responseHeaders(
					headerWithName("Location").description("생성된 리소스의 URI")
				)
			));
	}

	@DisplayName("답글 조회 API")
	@Test
	void getReplies() throws Exception {
		// given
		Long parentPostId = 1L;
		PaginationMetadata metadata = PaginationMetadata.builder()
			.nextCursor("dGVhbTpDMDYxRkE1UEI=")
			.hasNext(true)
			.build();

		PostThreadResponse response = PostThreadResponse.builder()
			.posts(List.of(
				givenReplyPostResponse(2L, parentPostId),
				givenReplyPostResponse(3L, parentPostId),
				givenReplyPostResponse(4L, parentPostId)
			))
			.metadata(metadata)
			.build();

		given(postService.getReplies(anyLong(), any())).willReturn(response);

		// when & then
		mockMvc.perform(
				get("/api/posts/{postId}/replies", parentPostId)
			)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.posts").isArray())
			.andExpect(jsonPath("$.metadata.hasNext").value(true))
			.andExpect(jsonPath("$.metadata.nextCursor").value("dGVhbTpDMDYxRkE1UEI="))
			.andDo(document("post-replies-get",
				preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				pathParameters(
					parameterWithName("postId").description("답글을 조회할 부모 포스트 ID")
				),
				responseFields(
					fieldWithPath("posts").type(JsonFieldType.ARRAY)
						.description("답글 포스트 목록"),
					fieldWithPath("metadata").type(JsonFieldType.OBJECT)
						.description("페이지네이션 메타데이터")
				)
					.andWithPrefix("posts[].", postResponseFields())
					.andWithPrefix("posts[].author.", authorEntityFields())
					.andWithPrefix("posts[].mediaEntities[].", mediaFields())
					.andWithPrefix("metadata.", paginationMetadataFields())
			));
	}

	// === 추가 헬퍼 메서드 ===

	/**
	 * 답글 포스트 응답 객체 생성
	 */
	private PostResponse givenReplyPostResponse(Long replyId, Long parentId) {
		MemberBasic author = MemberBasic.builder()
			.id(456L)
			.nickname("답글 작성자")
			.handle("reply_author_" + replyId)
			.avatarPath("https://cdn.twooter.xyz/media/avatar_reply.jpg")
			.build();

		MediaEntity media = MediaEntity.builder()
			.mediaId(200L + replyId)
			.mediaUrl("https://cdn.twooter.xyz/media/reply_" + replyId + ".jpg")
			.build();

		return PostResponse.builder()
			.id(replyId)
			.author(author)
			.content("이것은 포스트 " + parentId + "에 대한 답글입니다. 답글 ID: " + replyId)
			.likeCount(5L + replyId)
			.isLiked(replyId % 2 == 0) // 짝수 ID는 좋아요 상태
			.repostCount(2L)
			.isReposted(false)
			.viewCount(20L + replyId)
			.mediaEntities(List.of(media))
			.createdAt(LocalDateTime.of(2025, 5, 5, 0, (int)replyId.longValue()))
			.isDeleted(false)
			.build();
	}

	/**
	 * PostResponse 필드 문서화
	 */
	private List<FieldDescriptor> postResponseFields() {
		return List.of(
			fieldWithPath("id").type(JsonFieldType.NUMBER)
				.description("답글 포스트 ID"),
			fieldWithPath("author").type(JsonFieldType.OBJECT)
				.description("답글 작성자 정보"),
			fieldWithPath("content").type(JsonFieldType.STRING)
				.description("답글 내용"),
			fieldWithPath("likeCount").type(JsonFieldType.NUMBER)
				.description("좋아요 수"),
			fieldWithPath("liked").type(JsonFieldType.BOOLEAN)
				.description("현재 사용자의 좋아요 여부"),
			fieldWithPath("repostCount").type(JsonFieldType.NUMBER)
				.description("리포스트 수"),
			fieldWithPath("reposted").type(JsonFieldType.BOOLEAN)
				.description("현재 사용자의 리포스트 여부"),
			fieldWithPath("viewCount").type(JsonFieldType.NUMBER)
				.description("조회수"),
			fieldWithPath("mediaEntities").type(JsonFieldType.ARRAY)
				.description("첨부된 미디어 정보 목록"),
			fieldWithPath("createdAt").type(JsonFieldType.STRING)
				.description("답글 생성 시간"),
			fieldWithPath("deleted").type(JsonFieldType.BOOLEAN)
				.description("삭제 여부")
		);
	}

	// === 필드 문서화 메서드 ===
	private List<FieldDescriptor> authorEntityFieldsWithEmail() {
		return Stream.concat(
			authorEntityFields().stream(),
			Stream.of(fieldWithPath("email").type(JsonFieldType.STRING).description("작성자 이메일"))
		).toList();
	}

	private List<FieldDescriptor> mediaFields() {
		return List.of(
			fieldWithPath("mediaId").type(JsonFieldType.NUMBER).description("미디어 ID"),
			fieldWithPath("mediaUrl").type(JsonFieldType.STRING).description("미디어 접근 URL")
		);
	}

	private List<FieldDescriptor> authorEntityFields() {
		return List.of(
			fieldWithPath("id").type(JsonFieldType.NUMBER).description("작성자 고유 ID"),
			fieldWithPath("nickname").type(JsonFieldType.STRING).description("작성자 닉네임"),
			fieldWithPath("avatarPath").type(JsonFieldType.STRING).description("작성자 아바타 URL"),
			fieldWithPath("handle").type(JsonFieldType.STRING).description("작성자 핸들")
		);
	}

	// === 요청 / 응답 객체 생성 메서드 ===

	private PostCreateRequest givenPostCreateRequest() {
		return PostCreateRequest.builder()
			.content("트우터에 올릴 새로운 포스트 내용입니다. #첫글 #환영")
			.media(new String[] {"https://cdn.twooter.xyz/media/101.jpg", "https://cdn.twooter.xyz/media/102.jpg"})
			.build();
	}

	private PostCreateResponse givenPostCreateResponse() {
		MediaSimpleResponse[] mediaResponses = new MediaSimpleResponse[] {
			MediaSimpleResponse.builder()
				.mediaId(101L)
				.mediaUrl("https://cdn.twooter.xyz/media/101.jpg")
				.build(),
			MediaSimpleResponse.builder()
				.mediaId(102L)
				.mediaUrl("https://cdn.twooter.xyz/media/102.jpg")
				.build()
		};

		return PostCreateResponse.builder()
			.id(1L)
			.content("트우터에 올릴 새로운 포스트 내용입니다. #첫글 #환영")
			.author(TEST_MEMBER_SUMMARY)
			.media(mediaResponses)
			.createdAt(LocalDateTime.of(2025, 5, 5, 0, 0))
			.build();
	}

	private PostResponse givenPostResponse(Long postId) {
		MemberBasic author = MemberBasic.builder()
			.id(123L)
			.nickname("테이블 청소 마스터")
			.handle("table_cleaner")
			.avatarPath(
				"https://cdn.twooter.xyz/media/avatar")
			.build();

		MediaEntity media1 = MediaEntity.builder()
			.mediaId(101L)
			.mediaUrl("https://cdn.twooter.xyz/media/101.jpg")
			.build();

		MediaEntity media2 = MediaEntity.builder()
			.mediaId(101L)
			.mediaUrl("https://cdn.twooter.xyz/media/102.jpg")
			.build();

		List<MediaEntity> mediaList = List.of(media1, media2);

		return PostResponse.builder()
			.id(postId)
			.author(author)
			.content("새 책상을 정리하다가 유용해 보이는 오래된 자료를 발견해서 이메일로 보냅니다.")
			.likeCount(15L)
			.isLiked(true)
			.repostCount(3L)
			.isReposted(false)
			.viewCount(42L)
			.mediaEntities(mediaList)
			.createdAt(LocalDateTime.of(2025, 5, 5, 0, 0))
			.build();
	}

	private List<FieldDescriptor> paginationMetadataFields() {
		return List.of(
			fieldWithPath("nextCursor").type(JsonFieldType.STRING).optional().description("다음 페이지를 가져오는 데 사용될 커서"),
			fieldWithPath("hasNext").type(JsonFieldType.BOOLEAN).description("다음 페이지가 존재하는지 여부")
		);
	}
}
