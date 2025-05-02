package xyz.twooter.media.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.regex.Pattern;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import xyz.twooter.common.storage.StorageService;
import xyz.twooter.media.domain.repository.MediaRepository;
import xyz.twooter.media.presentation.dto.request.SignedUrlResponse;
import xyz.twooter.support.MockTestSupport;

class MediaServiceMockTest extends MockTestSupport {

	@Mock
	private MediaRepository mediaRepository;

	@Mock
	private StorageService storageService;

	@InjectMocks
	private MediaService mediaService;

	@Test
	@DisplayName("generateUploadUrl 메소드는 파일명과 콘텐츠 타입으로 업로드 URL을 생성한다")
	void generateUploadUrl_ShouldGenerateSignedUrl() {
		// given
		String filename = "test-image.jpg";
		String contentType = "image/jpeg";
		String expectedUrl = "https://storage-url.com/signed-url";

		// UUID 패턴 매칭을 위한 정규표현식
		Pattern uuidPattern = Pattern.compile(
			"media/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}_" + filename);

		when(storageService.generateUploadUrl(anyString(), eq(contentType))).thenReturn(expectedUrl);

		// when
		SignedUrlResponse response = mediaService.generateUploadUrl(filename, contentType);

		// then
		assertThat(response).isNotNull();
		assertThat(response.getUrl()).isEqualTo(expectedUrl);

		// storageService.generateUploadUrl이 호출되었는지 확인하고,
		// 첫 번째 인자(objectPath)가 정규표현식 패턴과 일치하는지 확인
		verify(storageService).generateUploadUrl(org.mockito.ArgumentMatchers.argThat(
			argument -> uuidPattern.matcher(argument).matches()
		), eq(contentType));
	}
}
