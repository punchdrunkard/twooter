package xyz.twooter.media.application;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import xyz.twooter.media.domain.Media;
import xyz.twooter.media.domain.exception.InvalidMediaException;
import xyz.twooter.media.domain.repository.MediaRepository;
import xyz.twooter.media.presentation.dto.response.MediaSimpleResponse;
import xyz.twooter.post.domain.PostMedia;
import xyz.twooter.post.domain.repository.PostMediaRepository;
import xyz.twooter.post.presentation.dto.response.MediaEntity;
import xyz.twooter.support.IntegrationTestSupport;

class MediaServiceTest extends IntegrationTestSupport {

	@Autowired
	private MediaService mediaService;

	@Autowired
	private MediaRepository mediaRepository;

	@Autowired
	private PostMediaRepository postMediaRepository;

	@Nested
	class SaveMedia {

		@Test
		@DisplayName("성공 - 주어진 미디어 경로를 통해 엔티티를 생성하고 ID 목록을 반환한다")
		void saveMedia_shouldReturnMediaIdsWhenMediaUrlsAreGiven() {
			// given
			List<String> mediaUrls = List.of("https://cdn.twooter.xyz/101.png", "https://cdn.twooter.xyz/102.png");

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
				.toList();
			assertThat(savedUrls).containsAll(mediaUrls);

			// 4. ID와 URL 매핑 검증
			savedEntities.forEach(media -> {
				assertThat(mediaUrls).contains(media.getPath());
				assertThat(saveMediaIds).contains(media.getId());
			});
		}

		@Test
		@DisplayName("성공 - 빈 목록이 주어지면 빈 아이디 목록을 반환한다")
		void saveMedia_shouldReturnEmptyListWhenEmptyUrlsAreGiven() {
			// given
			List<String> emptyUrls = List.of();

			// when
			List<Long> result = mediaService.saveMedia(emptyUrls);

			// then
			assertThat(result).isEmpty();
		}
	}

	@Nested
	class GetMediaByPostId {

		@Test
		@DisplayName("성공 - 주어진 포스트 id로 연관된 미디어들을 가져온다")
		void getMediaByPostId_shouldReturnMediaWhenPostIdIsGiven() {
			// given
			final long postId = 1L;
			final String path1 = "https://cdn.twooter.xyz/101.png";
			final String path2 = "https://cdn.twooter.xyz/102.png";

			List<Media> savedMedia = createAndSaveMedias(path1, path2);
			Media media1 = savedMedia.get(0);
			Media media2 = savedMedia.get(1);

			createAndSavePostMediaMapping(postId, media1.getId(), media2.getId());

			// when
			List<MediaEntity> mediaEntities = mediaService.getMediaByPostId(postId);

			// then
			// 1. 반환된 미디어 수 검증
			assertThat(mediaEntities).hasSize(2);

			// 2. 반환된 미디어 경로 검증
			assertThat(mediaEntities).extracting("mediaUrl")
				.containsExactlyInAnyOrder(path1, path2);

			// 3. 반환된 미디어 ID 검증
			assertThat(mediaEntities).extracting("mediaId")
				.containsExactlyInAnyOrder(media1.getId(), media2.getId());
		}

		@Test
		@DisplayName("성공 - 연결된 미디어가 없는 포스트는 빈 목록을 반환한다")
		void getMediaByPostId_shouldReturnEmptyListWhenPostHasNoMedia() {
			// given
			final long postIdWithoutMedia = 999L;

			// when
			List<MediaEntity> mediaEntities = mediaService.getMediaByPostId(postIdWithoutMedia);

			// then
			assertThat(mediaEntities).isEmpty();
		}
	}

	@Nested
	class GetMediaListFromId {

		@Test
		@DisplayName("성공 - 주어진 미디어 ID 목록에 해당하는 미디어 정보를 반환한다")
		void getMediaListFromId_shouldReturnMediaListWhenIdsAreGiven() {
			// given
			final String path1 = "https://cdn.twooter.xyz/101.png";
			final String path2 = "https://cdn.twooter.xyz/102.png";

			List<Media> savedMedia = createAndSaveMedias(path1, path2);
			List<Long> mediaIds = savedMedia.stream()
				.map(Media::getId)
				.toList();

			// when
			List<MediaSimpleResponse> responses = mediaService.getMediaListFromId(mediaIds);

			// then
			assertThat(responses).hasSize(2);
			assertThat(responses).extracting("mediaUrl")
				.containsExactlyInAnyOrder(path1, path2);
		}

		@Test
		@DisplayName("성공 - 빈 ID 목록이 주어지면 빈 목록을 반환한다")
		void getMediaListFromId_shouldReturnEmptyListWhenEmptyIdsAreGiven() {
			// given
			List<Long> emptyIds = List.of();

			// when
			List<MediaSimpleResponse> responses = mediaService.getMediaListFromId(emptyIds);

			// then
			assertThat(responses).isEmpty();
		}

		@Test
		@DisplayName("실패 - 존재하지 않는 미디어 ID가 포함되면 InvalidMediaException을 발생시킨다")
		void getMediaListFromId_shouldThrowExceptionWhenInvalidIdIsGiven() {
			// given
			final String path = "https://cdn.twooter.xyz/test.png";
			List<Media> savedMedia = createAndSaveMedias(path);
			Long validId = savedMedia.get(0).getId();
			Long invalidId = 9999L; // 존재하지 않는 ID

			List<Long> mediaIds = List.of(validId, invalidId);

			// when // then
			assertThrows(InvalidMediaException.class, () -> {
				mediaService.getMediaListFromId(mediaIds);
			});
		}
	}

	// === 헬퍼 ===

	private List<Media> createAndSaveMedias(String... paths) {
		List<Media> medias = Arrays.stream(paths)
			.map(path -> Media.builder().path(path).build())
			.toList();

		return mediaRepository.saveAll(medias);
	}

	private void createAndSavePostMediaMapping(long postId, Long... mediaIds) {
		List<PostMedia> postMedias = Arrays.stream(mediaIds)
			.map(mediaId -> PostMedia.builder()
				.postId(postId)
				.mediaId(mediaId)
				.build())
			.toList();

		postMediaRepository.saveAll(postMedias);
	}
}
