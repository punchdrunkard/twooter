package xyz.twooter.post.presentation.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Getter;
import lombok.experimental.SuperBuilder;
import xyz.twooter.media.presentation.dto.response.MediaSimpleResponse;
import xyz.twooter.member.presentation.dto.response.MemberSummaryResponse;
import xyz.twooter.post.domain.Post;

@Getter
@SuperBuilder
public class PostCreateResponse {
	private Long id;
	private String content;
	private MemberSummaryResponse author;
	private MediaSimpleResponse[] media;
	private LocalDateTime createdAt;

	public static PostCreateResponse of(Post post, MemberSummaryResponse memberSummaryResponse,
		List<MediaSimpleResponse> mediaList) {
		MediaSimpleResponse[] mediaResponses = mediaList.stream()
			.map(media -> MediaSimpleResponse.builder()
				.mediaId(media.getMediaId())
				.mediaUrl(media.getMediaUrl())
				.build())
			.toArray(MediaSimpleResponse[]::new);

		return PostCreateResponse.builder()
			.id(post.getId())
			.content(post.getContent())
			.author(memberSummaryResponse)
			.media(mediaResponses)
			.createdAt(post.getCreatedAt())
			.build();
	}
}
