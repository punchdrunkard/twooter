package xyz.twooter.post.domain.repository;

import java.time.LocalDateTime;
import java.util.List;
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
		    p.createdAt, p.likeCount, p.repostCount,
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

	@Query("""
		SELECT new xyz.twooter.post.domain.repository.projection.PostDetailProjection(
		    p.id, p.content,
		    m.id, m.handle, m.nickname, m.avatarPath,
		    p.createdAt, p.likeCount, p.repostCount,
		    CASE WHEN :memberId IS NULL THEN false
		         ELSE EXISTS (SELECT 1 FROM PostLike pl WHERE pl.postId = p.id AND pl.memberId = :memberId)
		    END,
		    CASE WHEN :memberId IS NULL THEN false 
		         ELSE EXISTS (SELECT 1 FROM Post rp WHERE rp.authorId = :memberId AND rp.repostOfId = p.id AND rp.isDeleted = false) 
		    END,
		    p.isDeleted, p.parentPostId, p.quotedPostId, p.repostOfId
		)
		FROM Post p
		JOIN Member m ON p.authorId = m.id
		WHERE p.parentPostId = :postId
		  AND (
		      (:cursorCreatedAt IS NULL AND :cursorId IS NULL) OR
		      (p.createdAt > :cursorCreatedAt) OR 
		      (p.createdAt = :cursorCreatedAt AND p.id > :cursorId)
		  )
		ORDER BY p.createdAt ASC, p.id ASC
		LIMIT :limit
		""")
	List<PostDetailProjection> findRepliesByIdWithPagination(
		@Param("postId") Long postId,
		@Param("memberId") Long memberId,
		@Param("cursorCreatedAt") LocalDateTime cursorCreatedAt,
		@Param("cursorId") Long cursorId,
		@Param("limit") int limit);

	@Query("SELECT p.isDeleted FROM  Post p WHERE p.id = :postId")
	boolean findIsDeletedById(Long postId);

	boolean existsByAuthorIdAndRepostOfId(Long authorId, Long repostOfId);

	@Modifying
	@Query("UPDATE Post p SET p.repostCount = p.repostCount + 1 WHERE p.id = :postId")
	void incrementRepostCount(@Param("postId") Long postId);

	@Modifying
	@Query("UPDATE Post p SET p.repostCount = p.repostCount - 1 WHERE p.id = :postId")
	void decrementRepostCount(@Param("postId") Long postId);

	Long findRepostOfIdById(Long postId);

	List<Post> findTop50ByAuthorIdAndIsDeletedFalseAndRepostOfIdIsNullOrderByIdDesc(Long authorId);

	@Query("SELECT p.id FROM Post p WHERE p.authorId = :authorId AND p.isDeleted = false")
	List<Long> findAllIdsByAuthorId(@Param("authorId") Long authorId);
}

