package xyz.twooter.media.presentation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import xyz.twooter.media.domain.Media;

@Getter
@Builder
@AllArgsConstructor
public class MediaSimpleResponse {

	private Long mediaId;
	private String mediaUrl;

	public static MediaSimpleResponse of(Media media) {
		return MediaSimpleResponse.builder()
			.mediaId(media.getId())
			.mediaUrl(media.getPath())
			.build();
	}
}
