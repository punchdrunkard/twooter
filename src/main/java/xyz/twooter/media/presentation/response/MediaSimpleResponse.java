package xyz.twooter.media.presentation.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class MediaSimpleResponse {

	private Long mediaId;
	private String mediaUrl;
}
