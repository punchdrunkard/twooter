package xyz.twooter.post.application;

import static xyz.twooter.common.infrastructure.pagination.CursorUtil.*;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import xyz.twooter.common.infrastructure.pagination.CursorUtil;
import xyz.twooter.common.infrastructure.pagination.PaginationMetadata;
import xyz.twooter.common.infrastructure.redis.RedisUtil;
import xyz.twooter.media.application.MediaService;
import xyz.twooter.member.domain.Member;
import xyz.twooter.member.presentation.dto.response.MemberBasic;
import xyz.twooter.post.domain.model.PostType;
import xyz.twooter.post.domain.repository.PostRepository;
import xyz.twooter.post.domain.repository.projection.TimelineItemProjection;
import xyz.twooter.post.presentation.dto.response.MediaEntity;
import xyz.twooter.post.presentation.dto.response.PostResponse;
import xyz.twooter.post.presentation.dto.response.TimelineItemResponse;
import xyz.twooter.post.presentation.dto.response.TimelineResponse;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class TimelineService {

	private final PostRepository postRepository;
	private final MediaService mediaService;
	private final CursorUtil cursorUtil;
	private final RedisUtil redisUtil;
	private static final String TIMELINE_ZSET_PREFIX = "timeline:user:";

	public TimelineResponse getTimelineByUserId(String cursor, Integer limit, Member currentMember,
		Long targetMemberId) {
		Long memberId = currentMember == null ? null : currentMember.getId();
		CursorUtil.Cursor decodedCursor = extractCursor(cursor);
		int fetchLimit = limit + 1;

		List<TimelineItemProjection> timelineItems = postRepository.findUserTimelineWithPagination(
			targetMemberId,
			memberId,
			decodedCursor != null ? decodedCursor.getTimestamp() : null,
			decodedCursor != null ? decodedCursor.getId() : null,
			fetchLimit
		);

		return buildTimelineResponseFromProjections(timelineItems, limit);
	}

	public TimelineResponse getHomeTimeline(String cursor, Integer limit, Member currentMember) {
		Long memberId = currentMember.getId();
		String timelineKey = TIMELINE_ZSET_PREFIX + memberId;

		long start = (cursor != null && !cursor.isBlank()) ? Long.parseLong(cursor) : 0;
		long end = start + limit;

		Set<String> postIdsStr = redisUtil.zReverseRange(timelineKey, start, end);

		if (postIdsStr == null || postIdsStr.isEmpty()) {
			log.warn("Cache miss for user timeline: {}. Falling back to DB.", memberId);
			CursorUtil.Cursor decodedCursor = extractCursor(cursor);
			int fetchLimit = limit + 1;
			List<TimelineItemProjection> timelineItems = postRepository.findHomeTimelineWithPagination(
				memberId,
				decodedCursor != null ? decodedCursor.getTimestamp() : null,
				decodedCursor != null ? decodedCursor.getId() : null,
				fetchLimit
			);
			return buildTimelineResponseFromProjections(timelineItems, limit);
		}

		List<Long> postIds = postIdsStr.stream().map(Long::valueOf).toList();

		boolean hasNext = postIds.size() > limit;
		List<Long> responsePostIds = hasNext ? postIds.subList(0, limit) : postIds;

		List<TimelineItemProjection> projections = postRepository.findTimelineItemsByPostIds(responsePostIds, memberId);

		Map<Long, TimelineItemProjection> projectionMap = projections.stream()
			.collect(Collectors.toMap(TimelineItemProjection::getOriginalPostId, p -> p));

		List<TimelineItemProjection> sortedProjections = responsePostIds.stream()
			.map(projectionMap::get)
			.filter(p -> p != null)
			.toList();

		String nextCursor = hasNext ? String.valueOf(start + limit) : null;

		return buildTimelineResponseFromProjections(sortedProjections, limit, hasNext, nextCursor);
	}

	private TimelineResponse buildTimelineResponseFromProjections(List<TimelineItemProjection> projections,
		int requestedLimit) {
		boolean hasNext = projections.size() > requestedLimit;
		List<TimelineItemProjection> responseItems = hasNext ?
			projections.subList(0, requestedLimit) : projections;

		String nextCursor = null;
		if (hasNext && !responseItems.isEmpty()) {
			TimelineItemProjection lastItem = responseItems.get(responseItems.size() - 1);
			nextCursor = cursorUtil.encode(lastItem.getFeedCreatedAt(), lastItem.getOriginalPostId());
		}

		return buildTimelineResponse(responseItems, hasNext, nextCursor);
	}

	private TimelineResponse buildTimelineResponseFromProjections(List<TimelineItemProjection> projections,
		int requestedLimit, boolean hasNext, String nextCursor) {
		List<TimelineItemProjection> responseItems = projections.size() > requestedLimit ?
			projections.subList(0, requestedLimit) : projections;

		return buildTimelineResponse(responseItems, hasNext, nextCursor);
	}

	private TimelineResponse buildTimelineResponse(List<TimelineItemProjection> responseItems, boolean hasNext,
		String nextCursor) {
		List<Long> postIds = responseItems.stream()
			.map(TimelineItemProjection::getOriginalPostId)
			.distinct()
			.toList();
		Map<Long, List<MediaEntity>> mediaByPostId = mediaService.getMediaByPostIds(postIds);

		List<TimelineItemResponse> timelineItems = responseItems.stream()
			.map(projection -> convertToTimelineItemResponse(projection, mediaByPostId))
			.toList();

		PaginationMetadata metadata = PaginationMetadata.builder()
			.hasNext(hasNext)
			.nextCursor(nextCursor)
			.build();

		return TimelineResponse.builder()
			.timeline(timelineItems)
			.metadata(metadata)
			.build();
	}

	private TimelineItemResponse convertToTimelineItemResponse(
		TimelineItemProjection projection,
		Map<Long, List<MediaEntity>> mediaByPostId
	) {
		List<MediaEntity> mediaEntities = mediaByPostId.getOrDefault(
			projection.getOriginalPostId(),
			List.of()
		);

		PostResponse postResponse = PostResponse.builder()
			.id(projection.getOriginalPostId())
			.author(new MemberBasic(
				projection.getOriginalPostAuthorId(),
				projection.getOriginalPostAuthorHandle(),
				projection.getOriginalPostAuthorNickname(),
				projection.getOriginalPostAuthorAvatarPath()
			))
			.content(projection.getOriginalPostContent())
			.likeCount(projection.getLikeCount())
			.isLiked(projection.getIsLiked())
			.repostCount(projection.getRepostCount())
			.isReposted(projection.getIsReposted())
			.mediaEntities(mediaEntities)
			.createdAt(projection.getOriginalPostCreatedAt())
			.isDeleted(projection.getIsDeleted())
			.build();

		return TimelineItemResponse.builder()
			.type(projection.getType())
			.createdAt(projection.getFeedCreatedAt())
			.post(postResponse)
			.repostBy(PostType.isRepost(projection.getType()) ? createRepostByMember(projection) : null)
			.build();
	}

	private MemberBasic createRepostByMember(TimelineItemProjection projection) {
		if (!PostType.isRepost(projection.getType())) {
			return null;
		}

		return MemberBasic.builder()
			.handle(projection.getRepostAuthorHandle())
			.nickname(projection.getRepostAuthorNickname())
			.avatarPath(projection.getRepostAuthorAvatarPath())
			.build();
	}
}
