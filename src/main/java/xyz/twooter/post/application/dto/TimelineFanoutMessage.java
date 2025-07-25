package xyz.twooter.post.application.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import xyz.twooter.post.domain.Post;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
public class TimelineFanoutMessage {

	private EventType type;
	private Long postId;
	private Long authorId;
	private LocalDateTime createdAt;
	private Long followerId;
	private Long followeeId;

	@Builder
	public TimelineFanoutMessage(EventType type, Long postId, Long authorId, LocalDateTime createdAt, Long followerId,
		Long followeeId) {
		this.type = type;
		this.postId = postId;
		this.authorId = authorId;
		this.createdAt = createdAt;
		this.followerId = followerId;
		this.followeeId = followeeId;
	}

	public static TimelineFanoutMessage ofPostCreation(Post post) {
		return TimelineFanoutMessage.builder()
			.type(EventType.POST_CREATED)
			.postId(post.getId())
			.authorId(post.getAuthorId())
			.createdAt(post.getCreatedAt())
			.build();
	}

	public static TimelineFanoutMessage ofPostDeletion(Post post) {
		return TimelineFanoutMessage.builder()
			.type(EventType.POST_DELETED)
			.postId(post.getId())
			.authorId(post.getAuthorId())
			.build();
	}

	public static TimelineFanoutMessage ofFollowCreation(Long followerId, Long followeeId) {
		return TimelineFanoutMessage.builder()
			.type(EventType.FOLLOW_CREATED)
			.followerId(followerId)
			.followeeId(followeeId)
			.build();
	}

	public static TimelineFanoutMessage ofFollowDeletion(Long followerId, Long followeeId) {
		return TimelineFanoutMessage.builder()
			.type(EventType.UNFOLLOW_CREATED)
			.followerId(followerId)
			.followeeId(followeeId)
			.build();
	}
}
