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
import org.springframework.http.MediaType;

import xyz.twooter.common.infrastructure.pagination.PaginationMetadata;
import xyz.twooter.media.presentation.dto.response.MediaSimpleResponse;
import xyz.twooter.member.presentation.dto.response.MemberBasic;
import xyz.twooter.member.presentation.dto.response.MemberSummaryResponse;
import xyz.twooter.post.domain.exception.PostNotFoundException;
import xyz.twooter.post.presentation.dto.request.PostCreateRequest;
import xyz.twooter.post.presentation.dto.request.ReplyCreateRequest;
import xyz.twooter.post.presentation.dto.response.MediaEntity;
import xyz.twooter.post.presentation.dto.response.PostCreateResponse;
import xyz.twooter.post.presentation.dto.response.PostLikeResponse;
import xyz.twooter.post.presentation.dto.response.PostReplyCreateResponse;
import xyz.twooter.post.presentation.dto.response.PostResponse;
import xyz.twooter.post.presentation.dto.response.PostThreadResponse;
import xyz.twooter.support.ControllerTestSupport;
import xyz.twooter.support.security.WithMockCustomUser;

@WithMockCustomUser
class PostControllerTest extends ControllerTestSupport {

	private static final String POST_CONTENT = "포스트 내용";
	private static final LocalDateTime TEST_DATE = LocalDateTime.of(2025, 5, 5, 0, 0);

	@Nested
	@DisplayName("포스트 생성 API")
	class CreatePostTests {

		@Test
		@DisplayName("성공 - 미디어 없는 (내용만 존재하는) 포스트 - media 타입이 null일 때")
		void shouldCreatePostWithoutMedia() throws Exception {
			PostCreateRequest request = createPostRequestWithContentOnly();
			PostCreateResponse response = createPostResponseWithoutMedia();

			given(postService.createPost(any(), any())).willReturn(response);

			mockMvc.perform(
					post("/api/posts")
						.content(objectMapper.writeValueAsString(request))
						.contentType(MediaType.APPLICATION_JSON)
				)
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(response.getId()))
				.andExpect(jsonPath("$.media").isArray());
		}

		@Test
		@DisplayName("성공 - 미디어가 함께 존재하는 포스트 작성")
		void shouldCreatePostWithMedia() throws Exception {
			PostCreateRequest request = createPostRequestWithMedia();
			PostCreateResponse response = createPostCreateResponseWithMedia();

			given(postService.createPost(any(), any())).willReturn(response);

			mockMvc.perform(
					post("/api/posts")
						.content(objectMapper.writeValueAsString(request))
						.contentType(MediaType.APPLICATION_JSON)
				)
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(response.getId()))
				.andExpect(jsonPath("$.media").isArray());
		}

		@Test
		@DisplayName("실패 - 미디어와 포스트 내용 둘 다 없는 경우")
		void shouldFailWhenMediaAndContentEmpty() throws Exception {
			PostCreateRequest request = PostCreateRequest.builder()
				.content("")
				.media(null)
				.build();

			mockMvc.perform(
					post("/api/posts")
						.content(objectMapper.writeValueAsString(request))
						.contentType(MediaType.APPLICATION_JSON)
				)
				.andExpect(status().isBadRequest());
		}

		@Test
		@DisplayName("실패 - 포스트 내용이 500자를 초과하면 에러가 발생한다.")
		void shouldFailWhenContentExceedsLimit() throws Exception {
			String longContent = "a".repeat(501);
			PostCreateRequest request = PostCreateRequest.builder()
				.content(longContent)
				.media(null)
				.build();

			mockMvc.perform(
					post("/api/posts")
						.content(objectMapper.writeValueAsString(request))
						.contentType(MediaType.APPLICATION_JSON)
				)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").exists());
		}

		@Test
		@DisplayName("실패 - 미디어 URL이 4개를 초과하면 에러가 발생한다.")
		void shouldFailWhenTooManyMediaIdsProvided() throws Exception {
			String[] tooManyUrls = {"url1", "url2", "url3", "url4", "url5"};

			PostCreateRequest request = PostCreateRequest.builder()
				.content("정상적인 내용입니다.")
				.media(tooManyUrls)
				.build();

			mockMvc.perform(
					post("/api/posts")
						.content(objectMapper.writeValueAsString(request))
						.contentType(MediaType.APPLICATION_JSON)
				)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").exists());
		}
	}

	@Nested
	@DisplayName("포스트 단건 조회 API")
	class GetPostTests {

		@Test
		@DisplayName("성공 - 포스트 ID로 조회")
		void shouldGetPostById() throws Exception {
			Long postId = 1L;
			PostResponse response = createPostResponse();

			given(postService.getPost(anyLong(), any())).willReturn(response);

			mockMvc.perform(
					get("/api/posts/{postId}", postId)
				)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content").value(response.getContent()))
				.andExpect(jsonPath("$.likeCount").value(response.getLikeCount()))
				.andExpect(jsonPath("$.mediaEntities").isArray());
		}

		@Test
		@DisplayName("실패 - postId 파라미터가 숫자가 아니면 400 응답")
		void shouldReturn400WhenPostIdNotNumber() throws Exception {
			mockMvc.perform(get("/api/posts/{postId}", "non-numeric"))
				.andExpect(status().isBadRequest());
		}
	}

	@Nested
	@DisplayName("포스트 좋아요 API")
	class LikePostTests {

		@Test
		@DisplayName("성공 - 포스트 좋아요")
		void shouldLikePost() throws Exception {
			Long postId = 1L;
			PostLikeResponse response = PostLikeResponse.builder()
				.postId(postId)
				.isLiked(true)
				.build();

			given(postLikeService.toggleLikeAndCount(anyLong(), any())).willReturn(response);

			mockMvc.perform(
					patch("/api/posts/{postId}/like", postId)
				)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.postId").value(postId))
				.andExpect(jsonPath("$.isLiked").value(true));
		}

		@DisplayName("성공 - 포스트 좋아요 취소")
		@Test
		void shouldRevokeLike() throws Exception {
			// given
			Long postId = 1L;
			PostLikeResponse response = PostLikeResponse.builder()
				.postId(postId)
				.isLiked(false)
				.build();

			// when
			given(postLikeService.toggleLikeAndCount(anyLong(), any())).willReturn(response);

			// then
			mockMvc.perform(
					patch("/api/posts/{postId}/like", postId)
				)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.postId").value(postId))
				.andExpect(jsonPath("$.isLiked").value(false));
		}
	}

	@Nested
	@DisplayName("답글 생성 API")
	class CreateReplyTests {

		@Test
		@DisplayName("성공 - 미디어 없는 답글 생성")
		void shouldCreateReplyWithoutMedia() throws Exception {
			// given
			ReplyCreateRequest request = createReplyRequestWithContentOnly();
			PostReplyCreateResponse response = createReplyResponseWithoutMedia();

			given(postService.createReply(any(), any())).willReturn(response);

			// when & then
			mockMvc.perform(
					post("/api/posts/replies")
						.content(objectMapper.writeValueAsString(request))
						.contentType(MediaType.APPLICATION_JSON)
				)
				.andExpect(status().isCreated())
				.andExpect(header().exists("Location"))
				.andExpect(jsonPath("$.id").value(response.getId()))
				.andExpect(jsonPath("$.content").value(response.getContent()))
				.andExpect(jsonPath("$.parentId").value(response.getParentId()))
				.andExpect(jsonPath("$.media").isArray())
				.andExpect(jsonPath("$.media").isEmpty())
				.andExpect(jsonPath("$.createdAt").exists());
		}

		@Test
		@DisplayName("성공 - 미디어가 포함된 답글 생성")
		void shouldCreateReplyWithMedia() throws Exception {
			// given
			ReplyCreateRequest request = createReplyRequestWithMedia();
			PostReplyCreateResponse response = createReplyResponseWithMedia();

			given(postService.createReply(any(), any())).willReturn(response);

			// when & then
			mockMvc.perform(
					post("/api/posts/replies")
						.content(objectMapper.writeValueAsString(request))
						.contentType(MediaType.APPLICATION_JSON)
				)
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(response.getId()))
				.andExpect(jsonPath("$.content").value(response.getContent()))
				.andExpect(jsonPath("$.parentId").value(response.getParentId()))
				.andExpect(jsonPath("$.media").isArray())
				.andExpect(jsonPath("$.media.length()").value(2));
		}

		@Test
		@DisplayName("실패 - parentId가 null인 경우")
		void shouldFailWhenParentIdIsNull() throws Exception {
			// given
			ReplyCreateRequest request = ReplyCreateRequest.builder()
				.content("답글 내용")
				.media(null)
				.parentId(null)
				.build();

			// when & then
			mockMvc.perform(
					post("/api/posts/replies")
						.content(objectMapper.writeValueAsString(request))
						.contentType(MediaType.APPLICATION_JSON)
				)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").exists());
		}

		@Test
		@DisplayName("실패 - 답글 내용과 미디어가 모두 없는 경우")
		void shouldFailWhenReplyContentAndMediaEmpty() throws Exception {
			// given
			ReplyCreateRequest request = ReplyCreateRequest.builder()
				.content("")
				.media(null)
				.parentId(1L)
				.build();

			// when & then
			mockMvc.perform(
					post("/api/posts/replies")
						.content(objectMapper.writeValueAsString(request))
						.contentType(MediaType.APPLICATION_JSON)
				)
				.andExpect(status().isBadRequest());
		}

		@Test
		@DisplayName("실패 - 답글 내용이 500자를 초과하는 경우")
		void shouldFailWhenReplyContentExceedsLimit() throws Exception {
			// given
			String longContent = "a".repeat(501);
			ReplyCreateRequest request = ReplyCreateRequest.builder()
				.content(longContent)
				.media(null)
				.parentId(1L)
				.build();

			// when & then
			mockMvc.perform(
					post("/api/posts/replies")
						.content(objectMapper.writeValueAsString(request))
						.contentType(MediaType.APPLICATION_JSON)
				)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").exists());
		}

		@Test
		@DisplayName("실패 - 답글 미디어가 4개를 초과하는 경우")
		void shouldFailWhenTooManyMediaInReply() throws Exception {
			// given
			String[] tooManyUrls = {"url1", "url2", "url3", "url4", "url5"};
			ReplyCreateRequest request = ReplyCreateRequest.builder()
				.content("정상적인 답글 내용입니다.")
				.media(tooManyUrls)
				.parentId(1L)
				.build();

			// when & then
			mockMvc.perform(
					post("/api/posts/replies")
						.content(objectMapper.writeValueAsString(request))
						.contentType(MediaType.APPLICATION_JSON)
				)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").exists());
		}

		@Test
		@DisplayName("실패 - 존재하지 않는 부모 포스트에 답글 작성")
		void shouldFailWhenParentPostNotExists() throws Exception {
			// given
			ReplyCreateRequest request = createReplyRequestWithContentOnly();

			given(postService.createReply(any(), any()))
				.willThrow(new PostNotFoundException());

			// when & then
			mockMvc.perform(
					post("/api/posts/replies")
						.content(objectMapper.writeValueAsString(request))
						.contentType(MediaType.APPLICATION_JSON)
				)
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").exists());
		}
	}

	@Nested
	@DisplayName("답글 조회 API")
	class GetRepliesTests {
		@DisplayName("성공 - 부모 parentId로 답글 조회")
		@Test
		void shouldReturnRepliesWhenRequestIsValid() throws Exception {
			// given
			Long parentPostId = 1L;

			PostThreadResponse response = PostThreadResponse.builder()
				.posts(List.of(createPostResponse(), createPostResponse(), createPostResponse()))
				.metadata(createPaginationMetadata())
				.build();

			given(postService.getReplies(anyLong(), any())).willReturn(response);

			// when & then
			mockMvc.perform(
					get("/api/posts/{postId}/replies", parentPostId)
				)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.posts").isArray())
				.andExpect(jsonPath("$.posts.length()").value(3));
		}

		@DisplayName("성공 - 답글이 없는 경우 빈 리스트 반환")
		@Test
		void shouldReturnEmptyListWhenThePostDoesNotHaveAnyReply() throws Exception {
			// given
			Long parentPostId = 1L;

			PostThreadResponse response = PostThreadResponse.builder()
				.posts(List.of())
				.metadata(null)
				.build();

			given(postService.getReplies(anyLong(), any())).willReturn(response);

			// when & then
			mockMvc.perform(
					get("/api/posts/{postId}/replies", parentPostId)
				)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.posts").isArray())
				.andExpect(jsonPath("$.posts.length()").value(0));
		}
	}

	// === 헬퍼 메서드 ===

	private PostCreateRequest createPostRequestWithContentOnly() {
		return PostCreateRequest.builder()
			.content(POST_CONTENT)
			.media(null)
			.build();
	}

	private PostCreateRequest createPostRequestWithMedia() {
		return PostCreateRequest.builder()
			.content(POST_CONTENT)
			.media(new String[] {"url1", "url2"})
			.build();
	}

	private PostCreateResponse createPostResponseWithoutMedia() {
		return PostCreateResponse.builder()
			.id(1L)
			.content(POST_CONTENT)
			.media(new MediaSimpleResponse[] {})
			.createdAt(TEST_DATE)
			.build();
	}

	private PostCreateResponse createPostCreateResponseWithMedia() {
		MediaSimpleResponse[] mediaResponses = {
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
			.content(POST_CONTENT)
			.media(mediaResponses)
			.createdAt(TEST_DATE)
			.build();
	}

	private PostResponse createPostResponse() {
		MemberBasic author = MemberBasic.builder()
			.nickname("테이블 청소 마스터")
			.handle("table_cleaner")
			.avatarPath("https://cdn.twooter.xyz/media/avatar")
			.build();

		List<MediaEntity> mediaList = List.of(
			MediaEntity.builder().mediaId(101L).mediaUrl("https://cdn.twooter.xyz/media/101.jpg").build(),
			MediaEntity.builder().mediaId(102L).mediaUrl("https://cdn.twooter.xyz/media/102.jpg").build()
		);

		return PostResponse.builder()
			.author(author)
			.content(POST_CONTENT)
			.likeCount(15L)
			.isLiked(true)
			.repostCount(3L)
			.isReposted(false)
			.viewCount(42L)
			.mediaEntities(mediaList)
			.createdAt(TEST_DATE)
			.build();
	}

	private MemberSummaryResponse createMemberSummaryResponse() {
		return MemberSummaryResponse.builder()
			.id(1L)
			.nickname("답글 작성자")
			.handle("reply_author")
			.avatarPath("https://cdn.twooter.xyz/media/avatar.jpg")
			.build();
	}

	private ReplyCreateRequest createReplyRequestWithContentOnly() {
		return ReplyCreateRequest.builder()
			.content("답글 내용입니다.")
			.media(null)
			.parentId(1L)
			.build();
	}

	private ReplyCreateRequest createReplyRequestWithMedia() {
		return ReplyCreateRequest.builder()
			.content("미디어가 포함된 답글입니다.")
			.media(new String[] {"reply_url1", "reply_url2"})
			.parentId(1L)
			.build();
	}

	private PostReplyCreateResponse createReplyResponseWithoutMedia() {
		return PostReplyCreateResponse.builder()
			.id(2L)
			.content("답글 내용입니다.")
			.author(createMemberSummaryResponse())
			.media(new MediaSimpleResponse[] {})
			.createdAt(TEST_DATE)
			.parentId(1L)
			.build();
	}

	private PostReplyCreateResponse createReplyResponseWithMedia() {
		MediaSimpleResponse[] mediaResponses = {
			MediaSimpleResponse.builder()
				.mediaId(201L)
				.mediaUrl("https://cdn.twooter.xyz/media/201.jpg")
				.build(),
			MediaSimpleResponse.builder()
				.mediaId(202L)
				.mediaUrl("https://cdn.twooter.xyz/media/202.jpg")
				.build()
		};

		return PostReplyCreateResponse.builder()
			.id(3L)
			.content("미디어가 포함된 답글입니다.")
			.author(createMemberSummaryResponse())
			.media(mediaResponses)
			.build();
	}

	private PaginationMetadata createPaginationMetadata() {
		return PaginationMetadata.builder()
			.nextCursor("dGVhbTpDMDYxRkE1UEI=")
			.hasNext(true)
			.build();
	}
}
