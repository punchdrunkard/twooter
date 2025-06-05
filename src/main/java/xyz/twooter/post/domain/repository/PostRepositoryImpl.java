package xyz.twooter.post.domain.repository;

import static xyz.twooter.post.domain.QPost.*;
import static xyz.twooter.post.domain.QPostLike.*;

import java.time.LocalDateTime;
import java.util.List;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.DateTimePath;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;

import lombok.RequiredArgsConstructor;
import xyz.twooter.member.domain.QMember;
import xyz.twooter.post.domain.QPost;
import xyz.twooter.post.presentation.dto.projection.TimelineItemProjection;

@RequiredArgsConstructor
public class PostRepositoryImpl implements PostCustomRepository {

	private final JPAQueryFactory queryFactory;

	private final QPost originalPost = new QPost("originalPost");
	private final QPost repost = new QPost("repost");
	private final QMember author = new QMember("author");

	@Override
	public List<TimelineItemProjection> findUserTimelineWithPagination(
		Long targetUserId,
		Long viewerId,
		LocalDateTime cursorCreatedAt,
		Long cursorId,
		int limit) {

		QMember originalAuthor = new QMember("originalAuthor");

		return queryFactory
			.select(Projections.constructor(TimelineItemProjection.class,
				// type: 리포스트 여부 판단
				new CaseBuilder()
					.when(post.repostOfId.isNotNull()).then("repost")
					.otherwise("post"),

				// createdAt: 타임라인 정렬 기준 (포스트 작성 시간 or 리포스트 시간)
				post.createdAt,

				// postId: 실제 표시할 포스트 ID (원본 포스트 ID 우선)
				post.repostOfId.coalesce(post.id),

				// content: 실제 표시할 컨텐츠 (원본 컨텐츠 우선)
				originalPost.content.coalesce(post.content),

				// author: 실제 포스트 작성자 (원본 작성자 우선)
				originalAuthor.handle.coalesce(author.handle),
				originalAuthor.nickname.coalesce(author.nickname),
				originalAuthor.avatarPath.coalesce(author.avatarPath),

				// 통계: 원본 포스트 기준
				originalPost.likeCount.coalesce(post.likeCount),
				originalPost.repostCount.coalesce(post.repostCount),
				originalPost.viewCount.coalesce(post.viewCount),

				// 뷰어 기준 상태: 원본 포스트 기준으로 판단
				isLikedByViewer(post.repostOfId.coalesce(post.id), viewerId),
				isRepostedByViewer(post.repostOfId.coalesce(post.id), viewerId),

				// 삭제 여부: 원본 포스트 기준
				originalPost.isDeleted.coalesce(post.isDeleted),

				// 원본 포스트 작성 시간
				originalPost.createdAt.coalesce(post.createdAt),

				// 리포스터 정보 (리포스트인 경우만)
				new CaseBuilder()
					.when(post.repostOfId.isNotNull()).then(author.handle)
					.otherwise((String)null),
				new CaseBuilder()
					.when(post.repostOfId.isNotNull()).then(author.nickname)
					.otherwise((String)null),
				new CaseBuilder()
					.when(post.repostOfId.isNotNull()).then(author.avatarPath)
					.otherwise((String)null)
			))
			.from(post)
			.join(author).on(post.authorId.eq(author.id))
			.leftJoin(originalPost).on(post.repostOfId.eq(originalPost.id))
			.leftJoin(originalAuthor).on(originalPost.authorId.eq(originalAuthor.id))
			.where(
				post.authorId.eq(targetUserId),
				post.isDeleted.isFalse(),
				// 리포스트인 경우 원본이 삭제되지 않았는지 확인
				originalPost.isDeleted.isFalse().or(originalPost.id.isNull()),
				applyPaginationCondition(post.createdAt, post.id, cursorCreatedAt, cursorId)
			)
			.orderBy(
				post.createdAt.desc(),
				post.id.desc()
			)
			.limit(limit + 1)
			.fetch();
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

	private BooleanExpression isLikedByViewer(NumberExpression<Long> postId, Long viewerId) {
		if (viewerId == null) {
			return Expressions.asBoolean(false);
		}

		return JPAExpressions
			.selectOne()
			.from(postLike)
			.where(
				postLike.postId.eq(postId),
				postLike.memberId.eq(viewerId)
			)
			.exists();
	}

	private BooleanExpression isRepostedByViewer(NumberExpression<Long> postId, Long viewerId) {
		if (viewerId == null) {
			return Expressions.asBoolean(false);
		}

		return JPAExpressions
			.selectOne()
			.from(repost)
			.where(
				repost.repostOfId.eq(postId),
				repost.authorId.eq(viewerId),
				repost.isDeleted.isFalse()
			)
			.exists();
	}
}
