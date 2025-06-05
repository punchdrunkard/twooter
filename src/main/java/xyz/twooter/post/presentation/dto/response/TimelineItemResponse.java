package xyz.twooter.post.presentation.dto.response;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import xyz.twooter.member.presentation.dto.response.MemberBasic;

@Getter
@Builder
@AllArgsConstructor
@ToString
public class TimelineItemResponse {
	private String type;
	private LocalDateTime createdAt;
	private PostResponse post;

	@JsonInclude(JsonInclude.Include.NON_NULL)
	private MemberBasic repostBy; // Only for "repost" type
}
