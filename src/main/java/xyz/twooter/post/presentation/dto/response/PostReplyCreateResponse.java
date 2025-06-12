package xyz.twooter.post.presentation.dto.response;

import java.util.List;

import lombok.Getter;
import lombok.experimental.SuperBuilder;
import xyz.twooter.media.presentation.dto.response.MediaSimpleResponse;
import xyz.twooter.member.presentation.dto.response.MemberSummaryResponse;
import xyz.twooter.post.domain.Post;

@Getter
@SuperBuilder
public class PostReplyCreateResponse extends PostCreateResponse {
	private Long parentId;

	public static PostReplyCreateResponse of(Post post, MemberSummaryResponse memberSummaryResponse,
		List<MediaSimpleResponse> mediaList, Long parentId) {
		MediaSimpleResponse[] mediaResponses =
			(mediaList == null ? List.<MediaSimpleResponse>of() : mediaList)
				.stream()
				.map(media -> MediaSimpleResponse.builder()
					.mediaId(media.getMediaId())
					.mediaUrl(media.getMediaUrl())
					.build())
				.toArray(MediaSimpleResponse[]::new);

		return PostReplyCreateResponse.builder()
			.id(post.getId())
			.content(post.getContent())
			.author(memberSummaryResponse)
			.media(mediaResponses)
			.createdAt(post.getCreatedAt())
			.parentId(parentId)
			.build();
	}
}
