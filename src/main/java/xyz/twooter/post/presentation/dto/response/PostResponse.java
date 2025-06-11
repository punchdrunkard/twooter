package xyz.twooter.post.presentation.dto.response;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import xyz.twooter.member.presentation.dto.response.MemberBasic;

@Getter
@Builder
@AllArgsConstructor
@EqualsAndHashCode
public class PostResponse {
	private Long id;
	private MemberBasic author;
	private String content;
	private long likeCount;
	private boolean isLiked;
	private long repostCount;
	private boolean isReposted;
	private long viewCount;
	@Builder.Default
	private List<MediaEntity> mediaEntities = new ArrayList<>();
	private LocalDateTime createdAt;
	private boolean isDeleted;

	public static PostResponse deletedPost(Long postId, LocalDateTime createdAt) {
		return PostResponse.builder()
			.id(postId)
			.author(null)
			.content(null)
			.mediaEntities(new ArrayList<>())
			.likeCount(0L)
			.isLiked(false)
			.repostCount(0L)
			.isReposted(false)
			.viewCount(0L)
			.createdAt(createdAt)
			.isDeleted(true)
			.build();
	}

	public static PostResponse deletedPost(Long postId) {
		return PostResponse.builder()
			.id(postId)
			.author(null)
			.content(null)
			.mediaEntities(new ArrayList<>())
			.likeCount(0L)
			.isLiked(false)
			.repostCount(0L)
			.isReposted(false)
			.viewCount(0L)
			.createdAt(null)
			.isDeleted(true)
			.build();
	}
}
