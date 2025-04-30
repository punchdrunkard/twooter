package xyz.twooter.post.presentation.dto.response;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;
import xyz.twooter.media.presentation.response.MediaSimpleResponse;
import xyz.twooter.member.presentation.dto.MemberSummaryResponse;

@Builder
@Getter
public class PostCreateResponse {
	private Long id;
	private String content;
	private MemberSummaryResponse author;
	private MediaSimpleResponse[] media;
	private LocalDateTime createdAt;
}
