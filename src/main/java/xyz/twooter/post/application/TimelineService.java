package xyz.twooter.post.application;

import static xyz.twooter.common.infrastructure.pagination.CursorUtil.*;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import xyz.twooter.common.infrastructure.pagination.CursorUtil;
import xyz.twooter.common.infrastructure.pagination.PaginationMetadata;
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
public class TimelineService {

	private final PostRepository postRepository;

	private final MediaService mediaService;

	private final CursorUtil cursorUtil;

	public TimelineResponse getTimelineByUserId(String cursor, Integer limit, Member currentMember,
		Long targetMemberId) {

		Long memberId = currentMember == null ? null : currentMember.getId();

		// 커서 디코딩 (null 가능)
		CursorUtil.Cursor decodedCursor = extractCursor(cursor);

		// 실제 조회할 개수 (다음 페이지 존재 여부 확인을 위해 +1)
		int fetchLimit = limit + 1;

		List<TimelineItemProjection> timelineItems = postRepository.findUserTimelineWithPagination(
			targetMemberId,
			memberId,
			isCursorNotNull(decodedCursor) ? decodedCursor.getTimestamp() : null,
			isCursorNotNull(decodedCursor) ? decodedCursor.getId() : null,
			fetchLimit
		);

		return buildTimelineResponse(timelineItems, limit);
	}

	public TimelineResponse getHomeTimeline(String cursor, Integer limit, Member currentMember) {

		Long memberId = currentMember.getId();

		// 커서 디코딩 (null 가능)
		CursorUtil.Cursor decodedCursor = extractCursor(cursor);

		// 실제 조회할 개수 (다음 페이지 존재 여부 확인을 위해 +1)
		int fetchLimit = limit + 1;

		List<TimelineItemProjection> timelineItems = postRepository.findHomeTimelineWithPagination(
			memberId,
			isCursorNotNull(decodedCursor) ? decodedCursor.getTimestamp() : null,
			isCursorNotNull(decodedCursor) ? decodedCursor.getId() : null,
			fetchLimit
		);

		return buildTimelineResponse(timelineItems, limit);
	}

	private boolean isCursorNotNull(CursorUtil.Cursor decodedCursor) {
		return decodedCursor != null;
	}

	private TimelineResponse buildTimelineResponse(List<TimelineItemProjection> projections, int requestedLimit) {
		boolean hasNext = projections.size() > requestedLimit;

		// 실제 반환할 아이템들
		List<TimelineItemProjection> responseItems = hasNext ?
			projections.subList(0, requestedLimit) : projections;

		// 모든 포스트 ID 수집
		List<Long> postIds = responseItems.stream()
			.map(TimelineItemProjection::getOriginalPostId)
			.distinct()
			.toList();

		// 배치로 미디어 조회
		Map<Long, List<MediaEntity>> mediaByPostId = mediaService.getMediaByPostIds(postIds);

		// TimelineItemResponse 변환
		List<TimelineItemResponse> timelineItems = responseItems.stream()
			.map(projection -> convertToTimelineItemResponse(projection, mediaByPostId))
			.toList();

		// 페이지네이션 메타데이터 생성
		PaginationMetadata metadata = buildPaginationMetadata(responseItems, hasNext);

		return TimelineResponse.builder()
			.timeline(timelineItems)
			.metadata(metadata)
			.build();
	}

	private TimelineItemResponse convertToTimelineItemResponse(
		TimelineItemProjection projection,
		Map<Long, List<MediaEntity>> mediaByPostId
	) {
		// 해당 포스트의 미디어 조회
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
			.viewCount(projection.getViewCount())
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

	private PaginationMetadata buildPaginationMetadata(List<TimelineItemProjection> items, boolean hasNext) {
		String nextCursor = null;

		if (hasNext && !items.isEmpty()) {
			TimelineItemProjection lastItem = items.get(items.size() - 1);
			nextCursor = cursorUtil.encode(lastItem.getFeedCreatedAt(), lastItem.getOriginalPostId());
		}

		return PaginationMetadata.builder()
			.hasNext(hasNext)
			.nextCursor(nextCursor)
			.build();
	}

}
