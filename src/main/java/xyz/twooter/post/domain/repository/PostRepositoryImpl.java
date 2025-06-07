package xyz.twooter.post.domain.repository;

import static xyz.twooter.member.domain.QFollow.*;
import static xyz.twooter.post.domain.QPost.*;

import java.time.LocalDateTime;
import java.util.List;

import com.querydsl.core.types.ConstructorExpression;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.DateTimePath;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.jpa.impl.JPAQueryFactory;

import lombok.RequiredArgsConstructor;
import xyz.twooter.member.domain.QMember;
import xyz.twooter.post.domain.QPost;
import xyz.twooter.post.domain.QPostLike;
import xyz.twooter.post.presentation.dto.projection.TimelineItemProjection;

@RequiredArgsConstructor
public class PostRepositoryImpl implements PostCustomRepository {

	private final JPAQueryFactory queryFactory;

	private static final QPost originalPost = new QPost("originalPost");
	private static final QPost viewerRepost = new QPost("viewerRepost");
	private static final QMember author = new QMember("author");
	private static final QMember originalAuthor = new QMember("originalAuthor");
	private static final QPostLike viewerLike = new QPostLike("viewerLike");

	@Override
	public List<TimelineItemProjection> findUserTimelineWithPagination(
		Long targetUserId,
		Long viewerId,
		LocalDateTime cursorCreatedAt,
		Long cursorId,
		int limit) {

		return queryFactory
			.select(createTimelineProjection(originalAuthor, viewerLike, viewerRepost))
			.from(post)
			.join(author).on(post.authorId.eq(author.id))
			.leftJoin(originalPost).on(post.repostOfId.eq(originalPost.id))
			.leftJoin(originalAuthor).on(originalPost.authorId.eq(originalAuthor.id))
			.leftJoin(viewerLike)
			.on(viewerLike.postId.eq(post.repostOfId.coalesce(post.id))
				.and(viewerId != null ? viewerLike.memberId.eq(viewerId) : null))
			.leftJoin(viewerRepost)
			.on(viewerRepost.repostOfId.eq(post.repostOfId.coalesce(post.id))
				.and(viewerId != null ? viewerRepost.authorId.eq(viewerId) : null)
				.and(viewerRepost.isDeleted.isFalse()))
			.where(
				post.authorId.eq(targetUserId),
				post.isDeleted.isFalse(),
				originalPost.isDeleted.isFalse().or(originalPost.id.isNull()),
				applyPaginationCondition(post.createdAt, post.id, cursorCreatedAt, cursorId)
			)
			.orderBy(post.createdAt.desc(), post.id.desc())
			.limit(limit + 1)
			.fetch();
	}

	@Override
	public List<TimelineItemProjection> findHomeTimelineWithPagination(
		Long memberId,
		LocalDateTime cursorCreatedAt,
		Long cursorId,
		int limit) {

		return queryFactory
			.select(createTimelineProjection(originalAuthor, viewerLike, viewerRepost))
			.from(post)
			.join(author).on(post.authorId.eq(author.id))
			.leftJoin(originalPost).on(post.repostOfId.eq(originalPost.id))
			.leftJoin(originalAuthor).on(originalPost.authorId.eq(originalAuthor.id))
			// 현재 유저의 좋아요 여부
			.leftJoin(viewerLike)
			.on(viewerLike.postId.eq(post.repostOfId.coalesce(post.id))
				.and(memberId != null ? viewerLike.memberId.eq(memberId) : null))
			// 현재 유저의 리포스트 여부
			.leftJoin(viewerRepost)
			.on(viewerRepost.repostOfId.eq(post.repostOfId.coalesce(post.id))
				.and(memberId != null ? viewerRepost.authorId.eq(memberId) : null)
				.and(viewerRepost.isDeleted.isFalse()))
			// 팔로잉 조건
			.leftJoin(follow)
			.on(follow.followerId.eq(memberId)
				.and(follow.followeeId.eq(post.authorId)))
			.where(
				// 자신의 포스트이거나 팔로우한 사용자의 포스트
				post.authorId.eq(memberId).or(follow.followerId.isNotNull()),
				post.isDeleted.isFalse(),
				originalPost.isDeleted.isFalse().or(originalPost.id.isNull()),
				applyPaginationCondition(post.createdAt, post.id, cursorCreatedAt, cursorId)
			)
			.orderBy(post.createdAt.desc(), post.id.desc())
			.limit(limit + 1)
			.fetch();
	}

	private static ConstructorExpression<TimelineItemProjection> createTimelineProjection(QMember originalAuthor,
		QPostLike viewerLike, QPost viewerRepost) {
		return Projections.constructor(TimelineItemProjection.class,
			// type: 리포스트 여부 판단
			new CaseBuilder()
				.when(post.repostOfId.isNotNull()).then("repost")
				.otherwise("post"),

			// createdAt: 타임라인 정렬 기준
			post.createdAt,

			// postId: 실제 표시할 포스트 ID
			post.repostOfId.coalesce(post.id),

			// content: 실제 표시할 컨텐츠
			originalPost.content.coalesce(post.content),

			// author: 실제 포스트 작성자
			originalAuthor.id.coalesce(author.id),
			originalAuthor.handle.coalesce(author.handle),
			originalAuthor.nickname.coalesce(author.nickname),
			originalAuthor.avatarPath.coalesce(author.avatarPath),

			// 통계: 원본 포스트 기준
			originalPost.likeCount.coalesce(post.likeCount),
			originalPost.repostCount.coalesce(post.repostCount),
			originalPost.viewCount.coalesce(post.viewCount),

			viewerLike.id.isNotNull(),
			viewerRepost.id.isNotNull(),

			// 삭제 여부
			originalPost.isDeleted.coalesce(post.isDeleted),

			// 원본 포스트 작성 시간
			originalPost.createdAt.coalesce(post.createdAt),

			// 리포스터 정보
			new CaseBuilder()
				.when(post.repostOfId.isNotNull()).then(author.id)
				.otherwise((Long)null),
			new CaseBuilder()
				.when(post.repostOfId.isNotNull()).then(author.handle)
				.otherwise((String)null),
			new CaseBuilder()
				.when(post.repostOfId.isNotNull()).then(author.nickname)
				.otherwise((String)null),
			new CaseBuilder()
				.when(post.repostOfId.isNotNull()).then(author.avatarPath)
				.otherwise((String)null)
		);
	}

	private BooleanExpression applyPaginationCondition(
		DateTimePath<LocalDateTime> createdAt,
		NumberPath<Long> id,
		LocalDateTime beforeTimestamp,
		Long beforeId) {

		if (beforeTimestamp == null || beforeId == null) {
			return null;
		}

		return createdAt.lt(beforeTimestamp)
			.or(createdAt.eq(beforeTimestamp).and(id.lt(beforeId)));
	}
}
