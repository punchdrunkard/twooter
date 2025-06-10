package xyz.twooter.post.domain.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import xyz.twooter.post.domain.Post;
import xyz.twooter.post.domain.repository.projection.PostDetailProjection;

public interface PostRepository extends JpaRepository<Post, Long>, PostCustomRepository {

	@Query("""
		SELECT new xyz.twooter.post.domain.repository.projection.PostDetailProjection(
		    p.id, p.content,
		    m.id, m.handle, m.nickname, m.avatarPath,
		    p.createdAt, p.viewCount, p.likeCount, p.repostCount,
		    CASE WHEN :memberId IS NULL THEN false
		         ELSE EXISTS (SELECT 1 FROM PostLike pl WHERE pl.postId = p.id AND pl.memberId = :memberId) 
		    END,
		    CASE WHEN :memberId IS NULL THEN false 
		         ELSE EXISTS (SELECT 1 FROM Post rp WHERE rp.authorId = :memberId AND rp.repostOfId = p.id AND rp.isDeleted = false) 
		    END,
		    p.isDeleted, p.parentPostId, p.quotedPostId,p.repostOfId
		)
		FROM Post p
		JOIN Member m ON p.authorId = m.id
		WHERE p.id = :postId
		""")
	Optional<PostDetailProjection> findPostDetailById(@Param("postId") Long postId, @Param("memberId") Long memberId);

	@Modifying
	@Query("UPDATE Post p SET p.viewCount = p.viewCount + 1 WHERE p.id = :postId")
	void incrementViewCount(@Param("postId") Long postId);

	@Query("SELECT p.isDeleted FROM  Post p WHERE p.id = :postId")
	boolean findIsDeletedById(Long postId);

	boolean existsByAuthorIdAndRepostOfId(Long authorId, Long repostOfId);

	@Modifying
	@Query("UPDATE Post p SET p.repostCount = p.repostCount + 1 WHERE p.id = :postId")
	void incrementRepostCount(@Param("postId") Long postId);

	@Modifying
	@Query("UPDATE Post p SET p.repostCount = p.repostCount - 1 WHERE p.id = :postId")
	void decrementRepostCount(Long postId);

	Long findRepostOfIdById(Long postId);
}

