package xyz.twooter.post.presentation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import xyz.twooter.media.domain.Media;

@Getter
@Builder
@AllArgsConstructor
public class MediaEntity {
	private Long id;
	private String path;

	public static MediaEntity fromEntity(Media media) {
		return MediaEntity.builder()
			.id(media.getId())
			.path(media.getPath())
			.build();
	}
}
