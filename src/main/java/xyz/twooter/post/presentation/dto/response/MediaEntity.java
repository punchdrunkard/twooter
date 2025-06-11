package xyz.twooter.post.presentation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import xyz.twooter.media.domain.Media;

@Getter
@Builder
@AllArgsConstructor
public class MediaEntity {
	private Long mediaId;
	private String mediaUrl;

	public static MediaEntity fromEntity(Media media) {
		return MediaEntity.builder()
			.mediaId(media.getId())
			.mediaUrl(media.getPath())
			.build();
	}
}
