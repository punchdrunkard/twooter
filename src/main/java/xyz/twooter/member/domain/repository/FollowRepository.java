package xyz.twooter.member.domain.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import xyz.twooter.member.domain.Follow;

public interface FollowRepository extends JpaRepository<Follow, Long>, FollowCustomRepository {

	boolean existsByFollowerIdAndFolloweeId(Long followerId, Long followeeId);

	void deleteByFollowerIdAndFolloweeId(Long id, Long targetMemberId);

	List<Follow> findByFolloweeIdIn(List<Long> followeeIds);

	@Query("SELECT f.followerId FROM Follow f WHERE f.followeeId = :followeeId")
	List<Long> findAllFollowerIdsByFolloweeId(@Param("followeeId") Long followeeId);
}
