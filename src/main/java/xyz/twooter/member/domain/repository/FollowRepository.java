package xyz.twooter.member.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import xyz.twooter.member.domain.Follow;

public interface FollowRepository extends JpaRepository<Follow, Long>, FollowCustomRepository {

	boolean existsByFollowerIdAndFolloweeId(Long followerId, Long followeeId);

	void deleteByFollowerIdAndFolloweeId(Long id, Long targetMemberId);
}
