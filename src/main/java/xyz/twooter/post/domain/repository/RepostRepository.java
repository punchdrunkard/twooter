package xyz.twooter.post.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import xyz.twooter.post.domain.Repost;

public interface RepostRepository extends JpaRepository<Repost, Long> {

	long countByPostId(Long postId);

	boolean existsByPostIdAndMemberId(Long postId, Long memberId);
}
