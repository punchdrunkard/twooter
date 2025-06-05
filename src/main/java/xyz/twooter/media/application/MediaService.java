package xyz.twooter.media.application;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import xyz.twooter.common.storage.StorageService;
import xyz.twooter.media.domain.Media;
import xyz.twooter.media.domain.exception.InvalidMediaException;
import xyz.twooter.media.domain.repository.MediaRepository;
import xyz.twooter.media.presentation.dto.request.SignedUrlResponse;
import xyz.twooter.media.presentation.dto.response.MediaSimpleResponse;
import xyz.twooter.post.presentation.dto.response.MediaEntity;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MediaService {

	private final MediaRepository mediaRepository;
	private final StorageService storageService;

	public List<MediaEntity> getMediaByPostId(Long postId) {
		return mediaRepository.findMediaByPostId(postId).stream()
			.map(MediaEntity::fromEntity)
			.toList();
	}

	@Transactional
	public List<Long> saveMedia(List<String> mediaUrls) {
		List<Media> entities = mediaUrls.stream()
			.map(Media::new)
			.toList();

		return mediaRepository.saveAll(entities)
			.stream()
			.map(Media::getId)
			.toList();
	}

	public List<MediaSimpleResponse> getMediaListFromId(List<Long> mediaIds) {
		if (mediaIds.isEmpty()) {
			return List.of();
		}

		List<Media> mediaList = mediaRepository.findAllByIdIn(mediaIds);
		if (mediaList.size() != mediaIds.size()) {
			throw new InvalidMediaException();
		}

		return mediaList.stream()
			.map(MediaSimpleResponse::of)
			.toList();
	}

	public Map<Long, List<MediaEntity>> getMediaByPostIds(List<Long> postIds) {
		if (postIds.isEmpty()) {
			return new HashMap<>();
		}

		return mediaRepository.findMediaByPostIds(postIds).stream()
			.map(MediaEntity::fromEntity)
			.collect(Collectors.groupingBy(MediaEntity::getId));
	}

	public SignedUrlResponse generateUploadUrl(String filename, String contentType) {
		String objectPath = "media/" + UUID.randomUUID() + "_" + filename;
		String signedUrl = storageService.generateUploadUrl(objectPath, contentType);
		return new SignedUrlResponse(signedUrl);
	}
}
