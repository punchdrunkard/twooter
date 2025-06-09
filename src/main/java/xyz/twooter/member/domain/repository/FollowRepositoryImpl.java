package xyz.twooter.member.domain.repository;

import static xyz.twooter.common.infrastructure.querydsl.QueryDslPaginationUtils.*;
import static xyz.twooter.member.domain.QFollow.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;

import lombok.RequiredArgsConstructor;
import xyz.twooter.member.domain.QFollow;
import xyz.twooter.member.domain.QMember;
import xyz.twooter.member.domain.repository.projection.MemberProfileProjection;

@RequiredArgsConstructor
public class FollowRepositoryImpl implements FollowCustomRepository {

	private final JPAQueryFactory queryFactory;

	private static final QFollow viewerFollowing = new QFollow("viewerFollowing");
	private static final QFollow viewerFollower = new QFollow("viewerFollower");
	private static final QMember follower = new QMember("follower");
	private static final QMember following = new QMember("following");

	@Override
	public List<MemberProfileProjection> findFollowersWithRelation(
		Long memberId, Long viewerId, LocalDateTime cursorCreatedAt,
		Long cursorId, int limit) {
		return findMembersWithFollowRelation(
			FollowRelationType.FOLLOWERS, memberId, viewerId, cursorCreatedAt, cursorId, limit
		);
	}

	@Override
	public List<MemberProfileProjection> findFolloweesWithRelation(
		Long memberId, Long viewerId, LocalDateTime cursorCreatedAt,
		Long cursorId, int limit) {
		return findMembersWithFollowRelation(
			FollowRelationType.FOLLOWEES, memberId, viewerId, cursorCreatedAt, cursorId, limit
		);
	}

	private List<MemberProfileProjection> findMembersWithFollowRelation(
		FollowRelationType type, Long memberId, Long viewerId,
		LocalDateTime cursorCreatedAt, Long cursorId, int limit) {

		BooleanExpression viewerLoggedIn = Objects.nonNull(viewerId) ? Expressions.TRUE : Expressions.FALSE;

		// 조인 대상 멤버
		QMember targetMember = type.getTargetMember(follower, following);

		return queryFactory
			.select(Projections.fields(MemberProfileProjection.class,
				targetMember.id,
				targetMember.handle,
				targetMember.nickname,
				targetMember.avatarPath,
				targetMember.bio,
				// isFollowingByMe: 내가 이 사람을 팔로우하는지
				new CaseBuilder()
					.when(viewerLoggedIn.and(viewerFollowing.id.isNotNull())).then(true)
					.otherwise(false)
					.as("isFollowingByMe"),
				// followsMe: 이 사람이 나를 팔로우하는지
				new CaseBuilder()
					.when(viewerLoggedIn.and(viewerFollower.id.isNotNull())).then(true)
					.otherwise(false)
					.as("followsMe")
			))
			.from(follow)
			.join(targetMember).on(type.getJoinCondition(follow, targetMember))
			// viewer가 targetMember를 팔로우하는지
			.leftJoin(viewerFollowing)
			.on(viewerFollowing.followeeId.eq(targetMember.id)
				.and(Objects.nonNull(viewerId) ? viewerFollowing.followerId.eq(viewerId) : Expressions.FALSE))
			// targetMember가 viewer를 팔로우하는지
			.leftJoin(viewerFollower)
			.on(viewerFollower.followerId.eq(targetMember.id)
				.and(Objects.nonNull(viewerId) ? viewerFollower.followeeId.eq(viewerId) : Expressions.FALSE))
			.where(
				type.getWhereCondition(follow, memberId),
				applyPaginationCondition(follow.createdAt, follow.id, cursorCreatedAt, cursorId)
			)
			.orderBy(follow.createdAt.desc(), follow.id.desc())
			.limit(limit + 1)
			.fetch();
	}
}
