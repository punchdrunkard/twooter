package xyz.twooter.post.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import xyz.twooter.post.domain.PostLike;

public interface PostLikeRepository extends JpaRepository<PostLike, Long> {

	long countByPostId(Long postId);

	boolean existsByPostIdAndMemberId(Long postId, Long memberId);
}
