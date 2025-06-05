package xyz.twooter.docs.media;

import static org.mockito.BDDMockito.*;
import static org.springframework.restdocs.headers.HeaderDocumentation.*;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.*;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import xyz.twooter.docs.RestDocsSupport;
import xyz.twooter.media.presentation.dto.request.SignedUrlResponse;

class MediaControllerDocsTest extends RestDocsSupport {

	@DisplayName("파일 업로드용 Signed URL을 발급한다.")
	@Test
	void getUploadUrl() throws Exception {
		// given
		String filename = "test.jpg";
		String contentType = "image/jpeg";

		SignedUrlResponse response = SignedUrlResponse.builder()
			.url("https://storage.googleapis.com/your-bucket/media/abc123.jpg?signature=...")
			.build();

		given(mediaService.generateUploadUrl(any(), any()))
			.willReturn(response);

		// when & then
		mockMvc.perform(get("/api/media/upload-url")
				.param("filename", filename)
				.param("contentType", contentType)
				.header("Authorization",
					"Bearer eyJhbGciOiJIUzI1NiJ9.eyJoYW5kbGUiOiJ0d29vdGVyXzEyMyIsInRva2VuVHlwZSI6IkFDQ0VTUyIsImlhdCI6MTcxMjMyMzIzMiwiZXhwIjoxNzEyMzI1MDMyfQ.exampleToken")
			)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.url").value(response.getUrl()))
			.andDo(document("media-get-upload-url",
				preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				requestHeaders(
					headerWithName("Authorization").description("액세스 토큰 (Bearer 타입)")
				),
				queryParameters(  // Correct method for documenting query parameters
					parameterWithName("filename").description("업로드할 파일명 (예: test.jpg)"),
					parameterWithName("contentType").description("파일의 MIME 타입 (예: image/jpeg)")
				),
				responseFields(
					fieldWithPath("url").description("GCS에 직접 업로드 가능한 signed URL")
				)
			));
	}
}
