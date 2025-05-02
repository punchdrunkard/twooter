package xyz.twooter.media.application;

import java.util.List;
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

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MediaService {

	private final MediaRepository mediaRepository;
	private final StorageService storageService;

	@Transactional
	public List<Long> saveMedia(List<String> mediaUrls) {
		List<Media> entities = mediaUrls.stream()
			.map(Media::new)
			.collect(Collectors.toList());

		return mediaRepository.saveAll(entities)
			.stream()
			.map(Media::getId)
			.collect(Collectors.toList());
	}

	public List<MediaSimpleResponse> getMediaListFromId(List<Long> mediaIds) {
		if (mediaIds.isEmpty())
			return List.of();

		List<Media> mediaList = mediaRepository.findAllByIdIn(mediaIds);
		if (mediaList.size() != mediaIds.size()) {
			throw new InvalidMediaException();
		}

		return mediaList.stream()
			.map(MediaSimpleResponse::of)
			.toList();
	}

	public SignedUrlResponse generateUploadUrl(String filename, String contentType) {
		String objectPath = "media/" + UUID.randomUUID() + "_" + filename;
		String signedUrl = storageService.generateUploadUrl(objectPath, contentType);
		return new SignedUrlResponse(signedUrl);
	}
}
