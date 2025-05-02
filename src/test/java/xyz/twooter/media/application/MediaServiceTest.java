package xyz.twooter.media.application;

import static org.assertj.core.api.Assertions.*;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import xyz.twooter.media.domain.Media;
import xyz.twooter.media.domain.repository.MediaRepository;
import xyz.twooter.support.IntegrationTestSupport;

class MediaServiceTest extends IntegrationTestSupport {

	@Autowired
	private MediaService mediaService;

	@Autowired
	private MediaRepository mediaRepository;

	@DisplayName("주어진 미디어 경로를 통해, 엔티티를 생성하고 생성된 아이디의 목록을 반환한다.")
	@Test
	void shouldReturnMedaIdsWhenMediaUrlsAreGiven() {
		// given
		List<String> mediaUrls = List.of("url1", "url2");

		// when
		List<Long> saveMediaIds = mediaService.saveMedia(mediaUrls);

		// then
		// 1. 반환된 ID 목록 검증
		assertThat(saveMediaIds)
			.hasSize(2)
			.doesNotContainNull();

		// 2. 저장된 엔티티 검증
		List<Media> savedEntities = mediaRepository.findAllById(saveMediaIds);
		assertThat(savedEntities).hasSize(2);

		// 3. 저장된 URL 검증
		List<String> savedUrls = savedEntities.stream()
			.map(Media::getPath)
			.collect(Collectors.toList());
		assertThat(savedUrls).containsAll(mediaUrls);

		// 4. ID와 URL 매핑 검증
		savedEntities.forEach(media -> {
			assertThat(mediaUrls).contains(media.getPath());
			assertThat(saveMediaIds).contains(media.getId());
		});
	}
}
