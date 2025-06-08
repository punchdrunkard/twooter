package xyz.twooter.member.domain.repository;

import java.time.LocalDateTime;
import java.util.List;

import xyz.twooter.member.presentation.dto.response.FollowerProfile;

public interface FollowCustomRepository {

	List<FollowerProfile> findFollowersWithRelation(
		Long memberId,
		Long viewerId, // 현재 로그인한 유저 (null 가능)
		LocalDateTime cursorCreatedAt, // null이 될 수 있음 (첫 페이지)
		Long cursorId, // null이 될 수 있음 (첫 페이지)
		int limit
	);
}
