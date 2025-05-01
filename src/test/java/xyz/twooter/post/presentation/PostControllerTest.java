package xyz.twooter.post.presentation;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import xyz.twooter.media.presentation.dto.response.MediaSimpleResponse;
import xyz.twooter.post.presentation.dto.request.PostCreateRequest;
import xyz.twooter.post.presentation.dto.response.PostCreateResponse;
import xyz.twooter.support.ControllerTestSupport;
import xyz.twooter.support.security.WithMockCustomUser;

@WithMockCustomUser
class PostControllerTest extends ControllerTestSupport {

	private static final String POST_CONTENT = "포스트 내용";
	private static final LocalDateTime TEST_DATE = LocalDateTime.of(2025, 5, 5, 0, 0);

	@DisplayName("미디어 없는 (내용만 존재하는) 포스트 생성 성공 - media 타입이 null 일 때")
	@Test
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

	@DisplayName("미디어가 함께 존재하는 포스트 작성 성공")
	@Test
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

	@DisplayName("미디어와 포스트 내용 둘 다 없는 경우, 에러를 발생시킨다.")
	@Test
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

	@DisplayName("포스트 내용이 500자를 초과하면 에러가 발생한다.")
	@Test
	void shouldFailWhenContentExceedsLimit() throws Exception {
		// given
		String longContent = "a".repeat(501); // 501자
		PostCreateRequest request = PostCreateRequest.builder()
			.content(longContent)
			.media(null)
			.build();

		// when // then
		mockMvc.perform(
				post("/api/posts")
					.content(objectMapper.writeValueAsString(request))
					.contentType(MediaType.APPLICATION_JSON)
			)
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").exists()); // 에러 메시지 존재 여부
	}

	@DisplayName("미디어 ID가 4개를 초과하면 에러가 발생한다.")
	@Test
	void shouldFailWhenTooManyMediaIdsProvided() throws Exception {
		// given
		Long[] tooManyMediaIds = {1L, 2L, 3L, 4L, 5L}; // 5개
		PostCreateRequest request = PostCreateRequest.builder()
			.content("정상적인 내용입니다.")
			.media(tooManyMediaIds)
			.build();

		// when // then
		mockMvc.perform(
				post("/api/posts")
					.content(objectMapper.writeValueAsString(request))
					.contentType(MediaType.APPLICATION_JSON)
			)
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").exists());
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
			.media(new Long[] {101L, 102L})
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
			.content(POST_CONTENT)
			.media(mediaResponses)
			.createdAt(TEST_DATE)
			.build();
	}
}
