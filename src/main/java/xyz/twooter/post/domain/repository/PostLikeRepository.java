package xyz.twooter.post.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import xyz.twooter.post.domain.PostLike;

public interface PostLikeRepository extends JpaRepository<PostLike, Long> {

	long countByPostId(Long postId);

	boolean existsByPostIdAndMemberId(Long postId, Long memberId);

	void deleteByPostIdAndMemberId(Long postId, Long memberId);

	@Modifying
	@Query("UPDATE Post p SET p.likeCount = p.likeCount + 1 WHERE p.id = :postId")
	void incrementLikeCount(@Param("postId") Long postId);

	@Modifying
	@Query("UPDATE Post p SET p.likeCount = p.likeCount - 1 WHERE p.id = :postId AND p.likeCount > 0")
	void decrementLikeCount(@Param("postId") Long postId);
}
