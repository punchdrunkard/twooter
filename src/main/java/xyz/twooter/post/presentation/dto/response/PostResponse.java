package xyz.twooter.post.presentation.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class PostResponse {
	AuthorEntity author;
	String content;
	Long likeCount;
	boolean isLiked;
	Long repostCount;
	boolean isReposted;
	boolean isBookmarked;
	Long viewCount;
	List<MediaEntity> media;
	LocalDateTime createdAt;
}
