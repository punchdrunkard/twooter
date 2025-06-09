package xyz.twooter.member.domain.repository;

import static xyz.twooter.common.infrastructure.querydsl.QueryDslPaginationUtils.*;
import static xyz.twooter.member.domain.QFollow.*;

import java.time.LocalDateTime;
import java.util.List;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;

import lombok.RequiredArgsConstructor;
import xyz.twooter.member.domain.QFollow;
import xyz.twooter.member.domain.QMember;
import xyz.twooter.member.domain.repository.projection.MemberProfileProjection;
import xyz.twooter.member.presentation.dto.response.MemberProfileWithRelation;

@RequiredArgsConstructor
public class FollowRepositoryImpl implements FollowCustomRepository {

	private final JPAQueryFactory queryFactory;

	private final QFollow viewerFollowing = new QFollow("viewerFollowing");  // viewer가 팔로우하는 관계
	private final QFollow viewerFollower = new QFollow("viewerFollower");    // viewer를 팔로우하는 관계

	private final QMember follower = new QMember("follower");
	private final QMember following = new QMember("following");

	@Override
	public List<MemberProfileProjection> findFollowersWithRelation(
		Long memberId, Long viewerId, LocalDateTime cursorCreatedAt,
		Long cursorId, int limit) {

		// viewerId가 null이 아닌지 체크하는 BooleanExpression 생성
		BooleanExpression viewerLoggedIn = viewerId != null ? Expressions.TRUE : Expressions.FALSE;

		return queryFactory
			.select(Projections.fields(MemberProfileProjection.class,
				follower.id,
				follower.handle,
				follower.nickname,
				follower.avatarPath,
				follower.bio,
				// isFollowingByMe: 내가 이 사람을 팔로우하는지
				// viewerId가 null이면 이 조건은 항상 false
				new CaseBuilder()
					// viewerLoggedIn 이면서 AND viewerFollowing이 조인되면 true
					.when(viewerLoggedIn.and(viewerFollowing.id.isNotNull())).then(true)
					.otherwise(false)
					.as("isFollowingByMe"),
				// followMe: 이 사람이 나를 팔로우하는지
				// viewerId가 null이면 이 조건은 항상 false
				new CaseBuilder()
					// viewerLoggedIn 이면서 AND viewerFollower가 조인되면 true
					.when(viewerLoggedIn.and(viewerFollower.id.isNotNull())).then(true)
					.otherwise(false)
					.as("followsMe")
			))
			.from(follow)
			.join(follower).on(follow.followerId.eq(follower.id))
			// viewer가 이 팔로워를 팔로우하는지 확인
			.leftJoin(viewerFollowing)
			.on(viewerFollowing.followeeId.eq(follower.id)
				.and(viewerId != null ? viewerFollowing.followerId.eq(viewerId) : null))
			// 이 팔로워가 viewer를 팔로우하는지 확인
			.leftJoin(viewerFollower).on(
				viewerFollower.followerId.eq(follower.id)
					.and(viewerId != null ? viewerFollower.followeeId.eq(viewerId) : null)
			)
			.where(
				follow.followeeId.eq(memberId),  // memberId의 팔로워들
				applyPaginationCondition(follow.createdAt, follow.id, cursorCreatedAt, cursorId)
			)
			.orderBy(follow.createdAt.desc(), follow.id.desc())
			.limit(limit + 1)
			.fetch();
	}

	@Override
	public List<MemberProfileProjection> findFolloweesWithRelation( // 메서드 이름 변경
		Long memberId, Long viewerId, LocalDateTime cursorCreatedAt,
		Long cursorId, int limit) {

		// viewerId가 null이 아닌지 체크하는 BooleanExpression 생성
		BooleanExpression viewerLoggedIn = viewerId != null ? Expressions.TRUE : Expressions.FALSE;

		return queryFactory
			.select(Projections.fields(MemberProfileProjection.class,
				following.id, // 별칭 변경에 따라 필드도 변경
				following.handle,
				following.nickname,
				following.avatarPath,
				following.bio,
				// isFollowingByMe: 내가 이 사람을 팔로우하는지
				// viewerId가 null이면 이 조건은 항상 false
				new CaseBuilder()
					// viewerLoggedIn 이면서 AND viewerFollowing이 조인되면 true
					.when(viewerLoggedIn.and(viewerFollowing.id.isNotNull())).then(true)
					.otherwise(false)
					.as("isFollowingByMe"),
				// followMe: 이 사람이 나를 팔로우하는지
				// viewerId가 null이면 이 조건은 항상 false
				new CaseBuilder()
					// viewerLoggedIn 이면서 AND viewerFollower가 조인되면 true
					.when(viewerLoggedIn.and(viewerFollower.id.isNotNull())).then(true)
					.otherwise(false)
					.as("followsMe")
			))
			.from(follow)
			.join(following)
			.on(follow.followeeId.eq(following.id))
			// viewer가 이 'followee'를 팔로우하는지
			.leftJoin(viewerFollowing)
			.on(viewerFollowing.followeeId.eq(following.id)
				.and(viewerId != null ? viewerFollowing.followerId.eq(viewerId) : null))
			// 이 'followee'가 viewer를 팔로우하는지 확인 (followsMe)
			.leftJoin(viewerFollower)
			.on(
				viewerFollower.followerId.eq(following.id)
					.and(viewerId != null ? viewerFollower.followeeId.eq(viewerId) : null)
			)
			.where(
				follow.followerId.eq(memberId),
				applyPaginationCondition(follow.createdAt, follow.id, cursorCreatedAt, cursorId)
			)
			.orderBy(follow.createdAt.desc(), follow.id.desc())
			.limit(limit + 1)
			.fetch();
	}
}
