package xyz.twooter.member.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import xyz.twooter.member.domain.Follow;

public interface FollowRepository extends JpaRepository<Follow, Long> {

	boolean existsByFollowerIdAndFolloweeId(Long followerId, Long followeeId);
}
