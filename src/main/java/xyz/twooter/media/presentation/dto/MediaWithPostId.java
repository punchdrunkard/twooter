package xyz.twooter.media.presentation.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import xyz.twooter.post.presentation.dto.response.MediaEntity;

@Getter
@AllArgsConstructor
public class MediaWithPostId {
	private Long mediaId;
	private String path;
	private Long postId;

	public MediaEntity toMediaEntity() {
		return MediaEntity.builder()
			.mediaId(mediaId)
			.mediaUrl(path)
			.build();
	}
}
