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

import xyz.twooter.media.presentation.dto.response.MediaSimpleResponse;
import xyz.twooter.member.presentation.dto.response.MemberBasic;
import xyz.twooter.post.presentation.dto.request.PostCreateRequest;
import xyz.twooter.post.presentation.dto.response.MediaEntity;
import xyz.twooter.post.presentation.dto.response.PostCreateResponse;
import xyz.twooter.post.presentation.dto.response.PostResponse;
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
			PostCreateResponse response = createPostResponseWithMedia();

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
					get("/api/posts")
						.param("postId", String.valueOf(postId))
				)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content").value(response.getContent()))
				.andExpect(jsonPath("$.likeCount").value(response.getLikeCount()))
				.andExpect(jsonPath("$.mediaEntities").isArray());
		}

		@Test
		@DisplayName("실패 - postId 파라미터가 없으면 400 응답")
		void shouldReturn400WhenPostIdMissing() throws Exception {
			mockMvc.perform(get("/api/posts"))
				.andExpect(status().isBadRequest());
		}

		@Test
		@DisplayName("실패 - postId 파라미터가 숫자가 아니면 400 응답")
		void shouldReturn400WhenPostIdNotNumber() throws Exception {
			mockMvc.perform(get("/api/posts").param("postId", "not-a-number"))
				.andExpect(status().isBadRequest());
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

	private PostCreateResponse createPostResponseWithMedia() {
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
			MediaEntity.builder().id(101L).path("https://cdn.twooter.xyz/media/101.jpg").build(),
			MediaEntity.builder().id(102L).path("https://cdn.twooter.xyz/media/102.jpg").build()
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
}
