package xyz.twooter.post.domain.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import xyz.twooter.post.domain.Post;
import xyz.twooter.post.presentation.dto.projection.PostDetailProjection;

public interface PostRepository extends JpaRepository<Post, Long> {

	@Query("""
		SELECT new xyz.twooter.post.presentation.dto.projection.PostDetailProjection(
		    p.id, p.content, m.handle, m.nickname, m.avatarPath, p.createdAt, p.viewCount,
		    COUNT(DISTINCT pl.id),
		    COUNT(DISTINCT r.id),
		    CASE WHEN :memberId IS NULL THEN false 
		         ELSE EXISTS (SELECT 1 FROM PostLike userLike WHERE userLike.postId = p.id AND userLike.memberId = :memberId) 
		    END,
		    CASE WHEN :memberId IS NULL THEN false 
		         ELSE EXISTS (SELECT 1 FROM Repost userRepost WHERE userRepost.postId = p.id AND userRepost.memberId = :memberId) 
		    END,
		    p.isDeleted
		)
		FROM Post p
		JOIN Member m ON p.authorId = m.id
		LEFT JOIN PostLike pl ON pl.postId = p.id
		LEFT JOIN Repost r ON r.postId = p.id
		WHERE p.id = :postId
		GROUP BY p.id, p.content, m.handle, m.nickname, m.avatarPath, p.createdAt, p.viewCount, p.isDeleted
		""")
	Optional<PostDetailProjection> findPostDetailById(@Param("postId") Long postId, @Param("memberId") Long memberId);

	@Modifying
	@Query("UPDATE Post p SET p.viewCount = p.viewCount + 1 WHERE p.id = :postId")
	void incrementViewCount(@Param("postId") Long postId);
}
