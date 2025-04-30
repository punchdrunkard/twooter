package xyz.twooter.post.presentation.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Builder;
import lombok.Getter;
import xyz.twooter.media.domain.Media;
import xyz.twooter.media.presentation.response.MediaSimpleResponse;
import xyz.twooter.member.presentation.dto.MemberSummaryResponse;
import xyz.twooter.post.domain.Post;

@Builder
@Getter
public class PostCreateResponse {
	private Long id;
	private String content;
	private MemberSummaryResponse author;
	private MediaSimpleResponse[] media;
	private LocalDateTime createdAt;

	public static PostCreateResponse of(Post post, MemberSummaryResponse memberSummaryResponse, List<Media> mediaList) {
		MediaSimpleResponse[] mediaResponses = mediaList.stream()
			.map(media -> MediaSimpleResponse.builder()
				.mediaId(media.getId())
				.mediaUrl(media.getPath())  // 또는 .getUrl() 등 실제 경로
				.build())
			.toArray(MediaSimpleResponse[]::new);

		return PostCreateResponse.builder()
			.id(post.getId())
			.content(post.getContent())
			.author(memberSummaryResponse)
			.media(mediaResponses)
			.createdAt(post.getCreatedTime())
			.build();
	}
}
