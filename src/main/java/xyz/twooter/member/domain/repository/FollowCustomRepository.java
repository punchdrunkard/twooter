package xyz.twooter.member.domain.repository;

import java.time.LocalDateTime;
import java.util.List;

import xyz.twooter.member.domain.repository.projection.MemberProfileProjection;

public interface FollowCustomRepository {

	List<MemberProfileProjection> findFollowersWithRelation(
		Long memberId,
		Long viewerId, // 현재 로그인한 유저 (null 가능)
		LocalDateTime cursorCreatedAt, // null이 될 수 있음 (첫 페이지)
		Long cursorId, // null이 될 수 있음 (첫 페이지)
		int limit
	);

	List<MemberProfileProjection> findFolloweesWithRelation(
		Long memberId,
		Long viewerId, // 현재 로그인한 유저 (null 가능)
		LocalDateTime cursorCreatedAt, // null이 될 수 있음 (첫 페이지)
		Long cursorId, // null이 될 수 있음 (첫 페이지)
		int limit
	);
}
