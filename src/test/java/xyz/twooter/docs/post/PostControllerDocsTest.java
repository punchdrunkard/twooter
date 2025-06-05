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

import xyz.twooter.docs.RestDocsSupport;
import xyz.twooter.media.presentation.dto.response.MediaSimpleResponse;
import xyz.twooter.member.presentation.dto.response.MemberBasic;
import xyz.twooter.member.presentation.dto.response.MemberSummaryResponse;
import xyz.twooter.post.presentation.dto.request.PostCreateRequest;
import xyz.twooter.post.presentation.dto.response.MediaEntity;
import xyz.twooter.post.presentation.dto.response.PostCreateResponse;
import xyz.twooter.post.presentation.dto.response.PostResponse;
import xyz.twooter.support.security.WithMockCustomUser;

@WithMockCustomUser
class PostControllerDocsTest extends RestDocsSupport {

	final MemberSummaryResponse TEST_MEMBER_SUMMARY = MemberSummaryResponse.builder()
		.email("test@test.com")
		.handle("table_cleaner")
		.nickname("테이블 청소 마스터")
		.avatarPath("https://cdn.twooter.xyz/media/avatar")
		.build();

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
					.header("Authorization",
						"Bearer eyJhbGciOiJIUzI1NiJ9.eyJoYW5kbGUiOiJ0d29vdGVyXzEyMyIsInRva2VuVHlwZSI6IkFDQ0VTUyIsImlhdCI6MTcxMjMyMzIzMiwiZXhwIjoxNzEyMzI1MDMyfQ.exampleToken")
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
					.andWithPrefix("author.", authorFields())
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
				queryParameters(
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
					.andWithPrefix("mediaEntities[].", mediaEntityFields())
			));
	}

	// === 필드 문서화 메서드 ===
	private List<FieldDescriptor> authorFields() {
		return List.of(
			fieldWithPath("email").type(JsonFieldType.STRING).description("작성자 이메일"),
			fieldWithPath("handle").type(JsonFieldType.STRING).description("작성자 핸들"),
			fieldWithPath("nickname").type(JsonFieldType.STRING).description("작성자 닉네임"),
			fieldWithPath("avatarPath").type(JsonFieldType.STRING).description("작성자 아바타 URL")
		);
	}

	private List<FieldDescriptor> mediaFields() {
		return List.of(
			fieldWithPath("mediaId").type(JsonFieldType.NUMBER).description("미디어 ID"),
			fieldWithPath("mediaUrl").type(JsonFieldType.STRING).description("미디어 접근 URL")
		);
	}

	private List<FieldDescriptor> authorEntityFields() {
		return List.of(
			fieldWithPath("nickname").type(JsonFieldType.STRING).description("작성자 닉네임"),
			fieldWithPath("avatarPath").type(JsonFieldType.STRING).description("작성자 아바타 URL"),
			fieldWithPath("handle").type(JsonFieldType.STRING).description("작성자 핸들")
		);
	}

	private List<FieldDescriptor> mediaEntityFields() {
		return List.of(
			fieldWithPath("id").type(JsonFieldType.NUMBER).description("미디어 ID"),
			fieldWithPath("path").type(JsonFieldType.STRING).description("미디어 경로 URL")
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
			.nickname("테이블 청소 마스터")
			.handle("table_cleaner")
			.avatarPath(
				"https://cdn.twooter.xyz/media/avatar")
			.build();

		MediaEntity media1 = MediaEntity.builder()
			.id(101L)
			.path("https://cdn.twooter.xyz/media/101.jpg")
			.build();

		MediaEntity media2 = MediaEntity.builder()
			.id(101L)
			.path("https://cdn.twooter.xyz/media/102.jpg")
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
}
