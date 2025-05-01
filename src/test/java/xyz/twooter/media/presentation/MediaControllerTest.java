package xyz.twooter.media.presentation;

import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import xyz.twooter.media.presentation.dto.request.SignedUrlResponse;
import xyz.twooter.support.ControllerTestSupport;

class MediaControllerTest extends ControllerTestSupport {

	@DisplayName("파일 업로드 URL을 요청한다.")
	@Test
	void getSignedUrl() throws Exception {
		// given
		String fileName = "test.jpg";
		String contentType = "image/jpeg";

		SignedUrlResponse response = SignedUrlResponse.builder()
			.url("example signed url")
			.build();

		given(mediaService.generateUploadUrl(any(), any()))
			.willReturn(response);

		// when & then
		mockMvc.perform(get("/api/media/upload-url")
				.param("filename", fileName)
				.param("contentType", contentType)
			)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.url").value(response.getUrl()));
	}
}
