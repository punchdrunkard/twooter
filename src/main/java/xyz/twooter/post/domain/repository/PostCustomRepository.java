package xyz.twooter.post.domain.repository;

import java.time.LocalDateTime;
import java.util.List;

import xyz.twooter.post.presentation.dto.projection.TimelineItemProjection;

public interface PostCustomRepository {

	List<TimelineItemProjection> findUserTimelineWithPagination(
		Long targetMemberId,
		Long viewerId, // 현재 로그인한 유저 (null 가능)
		LocalDateTime cursorCreatedAt, // null이 될 수 있음 (첫 페이지)
		Long cursorId, // null이 될 수 있음 (첫 페이지)
		int limit
	);

	List<TimelineItemProjection> findHomeTimelineWithPagination(
		Long memberId,
		LocalDateTime cursorCreatedAt, // null이 될 수 있음 (첫 페이지)
		Long cursorId, // null이 될 수 있음 (첫 페이지)
		int limit
	);
}
