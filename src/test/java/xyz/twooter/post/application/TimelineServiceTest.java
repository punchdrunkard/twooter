package xyz.twooter.post.application;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import jakarta.persistence.EntityManager;
import xyz.twooter.common.infrastructure.pagination.InvalidCursorException;
import xyz.twooter.media.domain.Media;
import xyz.twooter.media.domain.repository.MediaRepository;
import xyz.twooter.member.domain.Member;
import xyz.twooter.member.domain.repository.MemberRepository;
import xyz.twooter.post.domain.Post;
import xyz.twooter.post.domain.PostLike;
import xyz.twooter.post.domain.PostMedia;
import xyz.twooter.post.domain.repository.PostLikeRepository;
import xyz.twooter.post.domain.repository.PostMediaRepository;
import xyz.twooter.post.domain.repository.PostRepository;
import xyz.twooter.post.presentation.dto.response.MediaEntity;
import xyz.twooter.post.presentation.dto.response.TimelineItemResponse;
import xyz.twooter.post.presentation.dto.response.TimelineResponse;
import xyz.twooter.support.IntegrationTestSupport;

class TimelineServiceTest extends IntegrationTestSupport {

	@Autowired
	private TimelineService timelineService;

	@Autowired
	private MemberRepository memberRepository;
	@Autowired
	private PostRepository postRepository;
	@Autowired
	private PostMediaRepository postMediaRepository;
	@Autowired
	private MediaRepository mediaRepository;
	@Autowired
	private PostLikeRepository postLikeRepository;
	@Autowired
	private EntityManager entityManager;

	private final LocalDateTime TIME_BASE = LocalDateTime.of(2025, 5, 5, 0, 0);

	@Nested
	class GetTimeline {

		@DisplayName("성공 - 빈 타임라인 조회")
		@Test
		void shouldReturnEmptyTimelineWhenNoPostsExist() {
			// given
			Member viewer = saveTestMember("viewer");
			Member targetUser = saveTestMember("target");
			String cursor = null;
			Integer limit = 10;

			// when - targetUser의 타임라인 조회 (포스트 없음)
			TimelineResponse response = timelineService.getTimeline(cursor, limit, viewer, targetUser.getId());

			// then
			assertThat(response.getTimeline()).isEmpty();
			assertThat(response.getMetadata().isHasNext()).isFalse();
			assertThat(response.getMetadata().getNextCursor()).isNull();
		}

		@Test
		@DisplayName("성공 - 포스트만 포함된 경우")
		void shouldReturnTimelineWithOwnPosts() {
			// given
			Member viewer = saveTestMember("viewer");
			Member targetUser = saveTestMember("target");

			Post post1 = Post.createPost(targetUser.getId(), "첫 번째 포스트");
			Post post2 = Post.createPost(targetUser.getId(), "두 번째 포스트");
			postRepository.saveAll(List.of(post1, post2));

			// when - targetUser의 타임라인 조회
			TimelineResponse response = timelineService.getTimeline(null, 10, viewer, targetUser.getId());

			// then
			assertThat(response.getTimeline()).hasSize(2);

			TimelineItemResponse firstItem = response.getTimeline().get(0);
			assertThat(firstItem.getType()).isEqualTo("post");
			assertThat(firstItem.getPost().getContent()).isEqualTo("두 번째 포스트"); // 최신순
			assertThat(firstItem.getPost().getAuthor().getHandle()).isEqualTo(targetUser.getHandle());
			assertThat(firstItem.getRepostBy()).isNull();

			TimelineItemResponse secondItem = response.getTimeline().get(1);
			assertThat(secondItem.getPost().getContent()).isEqualTo("첫 번째 포스트");
		}

		@DisplayName("성공 - 리포스트만 포함된 경우")
		@Test
		void shouldReturnTimelineWithAllPostsAreRepost() {
			// given
			Member viewer = saveTestMember("viewer");
			Member targetUser = saveTestMember("target");
			Member other = saveTestMember("other");

			Post post1 = Post.createPost(other.getId(), "첫 번째 포스트");
			Post post2 = Post.createPost(other.getId(), "두 번째 포스트");
			Post post3 = Post.createPost(other.getId(), "세 번째 포스트");

			// 원본 포스트들 먼저 저장
			postRepository.saveAll(List.of(post1, post2, post3));

			// targetUser가 리포스트
			Post repost1 = Post.createRepost(targetUser.getId(), post1.getId());
			Post repost2 = Post.createRepost(targetUser.getId(), post2.getId());
			Post repost3 = Post.createRepost(targetUser.getId(), post3.getId());

			postRepository.saveAll(List.of(repost1, repost2, repost3));

			// when - targetUser의 타임라인 조회
			TimelineResponse response = timelineService.getTimeline(null, 10, viewer, targetUser.getId());

			// then
			assertThat(response.getTimeline())
				.hasSize(3)
				.allSatisfy(item -> {
					assertThat(item.getType()).isEqualTo("repost");
					assertThat(item.getRepostBy()).isNotNull();
					assertThat(item.getRepostBy().getHandle()).isEqualTo(targetUser.getHandle());
				});
		}

		@DisplayName("성공 - 타임라인은 최신순으로 정렬되어 있다.")
		@Test
		void shouldSortLatestWhenGetTimeline() {
			// given
			Member viewer = saveTestMember("viewer");
			Member targetUser = saveTestMember("target");

			Post olderPost = Post.createPost(targetUser.getId(), "오래된 포스트");
			postRepository.save(olderPost);

			Post latestPost = Post.createPost(targetUser.getId(), "최신 포스트");
			postRepository.save(latestPost);

			updateCreatedAt(latestPost.getId(), TIME_BASE); // 가장 최근
			updateCreatedAt(olderPost.getId(), TIME_BASE.minusHours(3));

			// when - targetUser의 타임라인 조회
			TimelineResponse response = timelineService.getTimeline(null, 10, viewer, targetUser.getId());
			List<TimelineItemResponse> timeline = response.getTimeline();

			// then
			assertThat(timeline)
				.hasSize(2)
				.extracting(item -> item.getPost().getId())
				.containsExactly(latestPost.getId(), olderPost.getId());
		}

		@DisplayName("특정 사용자의 타임라인에는 해당 사용자의 포스트/리포스트만 포함된다.")
		@Test
		void shouldContainOnlyTargetUserContent() {
			// given
			Member viewer = saveTestMember("viewer");
			Member targetUser = saveTestMember("target");
			Member other = saveTestMember("other");

			Post targetPost = Post.createPost(targetUser.getId(), "타겟의 포스트");
			Post otherPost = Post.createPost(other.getId(), "다른 사람의 포스트");
			Post viewerPost = Post.createPost(viewer.getId(), "뷰어의 포스트");
			postRepository.saveAll(List.of(targetPost, otherPost, viewerPost));

			// targetUser가 다른 사람 포스트를 리포스트
			Post targetRepost = Post.createRepost(targetUser.getId(), otherPost.getId());
			// 다른 사람이 targetUser 포스트를 리포스트 (타임라인에 포함되지 않아야 함)
			Post otherRepost = Post.createRepost(other.getId(), targetPost.getId());
			postRepository.saveAll(List.of(targetRepost, otherRepost));

			// when - targetUser의 타임라인 조회
			TimelineResponse response = timelineService.getTimeline(null, 10, viewer, targetUser.getId());
			List<TimelineItemResponse> timeline = response.getTimeline();

			// then - targetUser의 포스트와 리포스트만 포함
			assertThat(timeline).hasSize(2);

			List<String> types = timeline.stream()
				.map(TimelineItemResponse::getType)
				.toList();
			assertThat(types).containsExactlyInAnyOrder("post", "repost");

			// 모든 항목이 targetUser와 관련되어야 함
			timeline.forEach(item -> {
				if ("post".equals(item.getType())) {
					assertThat(item.getPost().getAuthor().getHandle()).isEqualTo(targetUser.getHandle());
				} else if ("repost".equals(item.getType())) {
					assertThat(item.getRepostBy().getHandle()).isEqualTo(targetUser.getHandle());
				}
			});
		}

		@DisplayName("성공 - 미디어가 포함된 포스트만 있는 경우")
		@Test
		void shouldReturnTimelineWithMediaPosts() {
			// given
			Member viewer = saveTestMember("viewer");
			Member targetUser = saveTestMember("target");

			// 미디어 생성
			Media image1 = createAndSaveMedia("https://example.com/image1.jpg");
			Media image2 = createAndSaveMedia("https://example.com/image2.jpg");
			Media video = createAndSaveMedia("https://example.com/video.mp4");

			// targetUser의 포스트 생성
			Post postWithSingleImage = Post.createPost(targetUser.getId(), "단일 이미지 포스트");
			Post savedPost1 = postRepository.save(postWithSingleImage);

			Post postWithMultipleMedia = Post.createPost(targetUser.getId(), "다중 미디어 포스트");
			Post savedPost2 = postRepository.save(postWithMultipleMedia);

			// 포스트-미디어 관계 설정
			createPostMediaRelation(savedPost1, List.of(image1));
			createPostMediaRelation(savedPost2, List.of(image2, video));

			// when - targetUser의 타임라인 조회
			TimelineResponse response = timelineService.getTimeline(null, 10, viewer, targetUser.getId());

			// then
			assertThat(response.getTimeline())
				.hasSize(2)
				.allSatisfy(item -> {
					assertThat(item.getType()).isEqualTo("post");
					assertThat(item.getPost().getAuthor().getHandle()).isEqualTo(targetUser.getHandle());
					assertThat(item.getPost().getMediaEntities()).isNotEmpty();
				});

			// 미디어 검증
			TimelineItemResponse firstItem = response.getTimeline().get(0);
			assertThat(firstItem.getPost().getMediaEntities())
				.hasSize(2)
				.extracting(MediaEntity::getPath)
				.containsExactlyInAnyOrder(
					"https://example.com/image2.jpg",
					"https://example.com/video.mp4"
				);

			TimelineItemResponse secondItem = response.getTimeline().get(1);
			assertThat(secondItem.getPost().getMediaEntities())
				.hasSize(1)
				.extracting(MediaEntity::getPath)
				.containsExactly("https://example.com/image1.jpg");
		}

		@DisplayName("성공 - 미디어가 포함된 리포스트만 있는 경우")
		@Test
		void shouldReturnTimelineWithMediaReposts() {
			// given
			Member viewer = saveTestMember("viewer");
			Member targetUser = saveTestMember("target");
			Member other = saveTestMember("other");

			// 미디어 생성
			Media image1 = createAndSaveMedia("https://example.com/original1.jpg");
			Media image2 = createAndSaveMedia("https://example.com/original2.jpg");
			Media gif = createAndSaveMedia("https://example.com/funny.gif");

			// other가 미디어 포스트 작성
			Post originalPost1 = createPostWithMedia(other, "첫 번째 미디어 포스트", List.of(image1));
			Post originalPost2 = createPostWithMedia(other, "두 번째 미디어 포스트", List.of(image2, gif));

			// targetUser가 리포스트
			Post repost1 = Post.createRepost(targetUser.getId(), originalPost1.getId());
			Post repost2 = Post.createRepost(targetUser.getId(), originalPost2.getId());
			postRepository.saveAll(List.of(repost1, repost2));

			// when - targetUser의 타임라인 조회
			TimelineResponse response = timelineService.getTimeline(null, 10, viewer, targetUser.getId());

			// then
			assertThat(response.getTimeline())
				.hasSize(2)
				.allSatisfy(item -> {
					assertThat(item.getType()).isEqualTo("repost");
					assertThat(item.getRepostBy().getHandle()).isEqualTo(targetUser.getHandle());
					assertThat(item.getPost().getAuthor().getHandle()).isEqualTo(other.getHandle());
					assertThat(item.getPost().getMediaEntities()).isNotEmpty();
				});

			// 리포스트된 원본 포스트의 미디어 검증
			List<List<String>> allMediaPaths = response.getTimeline().stream()
				.map(item -> item.getPost().getMediaEntities().stream()
					.map(MediaEntity::getPath)
					.sorted()
					.toList())
				.toList();

			assertThat(allMediaPaths).containsExactlyInAnyOrder(
				List.of("https://example.com/original1.jpg"),
				List.of("https://example.com/funny.gif", "https://example.com/original2.jpg")
			);
		}

		@DisplayName("성공 - 미디어가 포함된 포스트와 리포스트가 혼재된 경우")
		@Test
		void shouldReturnTimelineWithMixedMediaContent() {
			// given
			Member viewer = saveTestMember("viewer");
			Member targetUser = saveTestMember("target");
			Member other = saveTestMember("other");

			// 다양한 미디어 생성
			Media targetImage = createAndSaveMedia("https://example.com/target-photo.jpg");
			Media otherVideo = createAndSaveMedia("https://example.com/other-video.mp4");
			Media sharedGif = createAndSaveMedia("https://example.com/shared.gif");

			// targetUser의 포스트 (미디어 포함)
			Post targetPost = createPostWithMedia(targetUser, "타겟의 미디어 포스트", List.of(targetImage, sharedGif));

			// other의 포스트 (미디어 포함)
			Post otherPost = createPostWithMedia(other, "다른 사람 미디어 포스트", List.of(otherVideo));

			// targetUser가 other의 포스트를 리포스트
			Post targetRepost = Post.createRepost(targetUser.getId(), otherPost.getId());
			postRepository.save(targetRepost);

			// when - targetUser의 타임라인 조회
			TimelineResponse response = timelineService.getTimeline(null, 10, viewer, targetUser.getId());

			// then
			assertThat(response.getTimeline()).hasSize(2);

			// 타입별 분류
			List<TimelineItemResponse> posts = response.getTimeline().stream()
				.filter(item -> "post".equals(item.getType()))
				.toList();
			List<TimelineItemResponse> reposts = response.getTimeline().stream()
				.filter(item -> "repost".equals(item.getType()))
				.toList();

			// targetUser의 포스트 검증
			assertThat(posts)
				.hasSize(1)
				.first()
				.satisfies(item -> {
					assertThat(item.getPost().getAuthor().getHandle()).isEqualTo(targetUser.getHandle());
					assertThat(item.getPost().getContent()).isEqualTo("타겟의 미디어 포스트");
					assertThat(item.getPost().getMediaEntities())
						.hasSize(2)
						.extracting(MediaEntity::getPath)
						.containsExactlyInAnyOrder(
							"https://example.com/target-photo.jpg",
							"https://example.com/shared.gif"
						);
				});

			// targetUser의 리포스트 검증
			assertThat(reposts)
				.hasSize(1)
				.first()
				.satisfies(item -> {
					assertThat(item.getRepostBy().getHandle()).isEqualTo(targetUser.getHandle());
					assertThat(item.getPost().getAuthor().getHandle()).isEqualTo(other.getHandle());
					assertThat(item.getPost().getContent()).isEqualTo("다른 사람 미디어 포스트");
					assertThat(item.getPost().getMediaEntities())
						.hasSize(1)
						.extracting(MediaEntity::getPath)
						.containsExactly("https://example.com/other-video.mp4");
				});
		}

		@DisplayName("성공 - 뷰어의 좋아요/리포스트 상태가 올바르게 반영된다")
		@Test
		void shouldReturnCorrectViewerStatusForTargetUserTimeline() {
			// given
			Member viewer = saveTestMember("viewer");
			Member targetUser = saveTestMember("target");

			Post targetPost = Post.createPost(targetUser.getId(), "타겟의 포스트");
			postRepository.save(targetPost);

			// viewer가 targetPost에 좋아요
			PostLike like = PostLike.builder()
				.postId(targetPost.getId())
				.memberId(viewer.getId())
				.build();
			postLikeRepository.save(like);

			// when - viewer가 targetUser의 타임라인 조회
			TimelineResponse response = timelineService.getTimeline(null, 10, viewer, targetUser.getId());

			// then - viewer의 상태가 반영되어야 함
			assertThat(response.getTimeline())
				.hasSize(1)
				.first()
				.satisfies(item -> {
					assertThat(item.getPost().isLiked()).isTrue(); // viewer가 좋아요 했음
					assertThat(item.getPost().getAuthor().getHandle()).isEqualTo(targetUser.getHandle());
				});
		}

		@DisplayName("성공 - 자신의 타임라인도 조회 가능하다")
		@Test
		void shouldReturnOwnTimelineWhenTargetIsViewer() {
			// given
			Member member = saveTestMember("member");

			Post myPost = Post.createPost(member.getId(), "내 포스트");
			postRepository.save(myPost);

			// when - 자신의 타임라인 조회 (viewer == targetUser)
			TimelineResponse response = timelineService.getTimeline(null, 10, member, member.getId());

			// then
			assertThat(response.getTimeline())
				.hasSize(1)
				.first()
				.satisfies(item -> {
					assertThat(item.getPost().getAuthor().getHandle()).isEqualTo(member.getHandle());
					assertThat(item.getPost().getContent()).isEqualTo("내 포스트");
				});
		}
	}

	@Nested
	@DisplayName("커서 기반 페이지네이션 검증")
	class CursorPaginationTests {

		@DisplayName("성공 - 첫 페이지 조회 시 nextCursor 생성")
		@Test
		void shouldGenerateNextCursorOnFirstPage() {
			// given
			Member viewer = saveTestMember("viewer");
			Member targetUser = saveTestMember("target");

			// 5개의 포스트 생성 (시간 차이로 순서 보장)
			List<Post> posts = createPostsWithTimeGap(targetUser, 5);

			// when - 첫 페이지 조회 (limit: 3)
			TimelineResponse response = timelineService.getTimeline(null, 3, viewer, targetUser.getId());

			// then
			assertThat(response.getTimeline()).hasSize(3);
			assertThat(response.getMetadata().isHasNext()).isTrue();
			assertThat(response.getMetadata().getNextCursor()).isNotNull();

			// 첫 페이지는 최신 3개 포스트
			List<Long> expectedPostIds = posts.stream()
				.sorted((p1, p2) -> p2.getId().compareTo(p1.getId())) // ID 역순 (최신순)
				.limit(3)
				.map(Post::getId)
				.toList();

			List<Long> actualPostIds = response.getTimeline().stream()
				.map(item -> item.getPost().getId())
				.toList();

			assertThat(actualPostIds).containsExactlyElementsOf(expectedPostIds);
		}

		@DisplayName("성공 - nextCursor로 다음 페이지 조회")
		@Test
		void shouldRetrieveNextPageWithCursor() {
			// given
			Member viewer = saveTestMember("viewer");
			Member targetUser = saveTestMember("target");

			// 시간 간격을 두고 5개 포스트 생성
			List<Post> posts = createPostsWithTimeGap(targetUser, 5);

			// 첫 페이지 조회
			TimelineResponse firstPage = timelineService.getTimeline(null, 2, viewer, targetUser.getId());
			String nextCursor = firstPage.getMetadata().getNextCursor();

			// when - 커서로 다음 페이지 조회
			TimelineResponse secondPage = timelineService.getTimeline(nextCursor, 2, viewer, targetUser.getId());

			// then
			assertThat(secondPage.getTimeline()).hasSize(2);
			assertThat(secondPage.getMetadata().isHasNext()).isTrue();
			assertThat(secondPage.getMetadata().getNextCursor()).isNotNull();

			// 첫 페이지와 두 번째 페이지의 포스트가 겹치지 않아야 함
			List<Long> firstPagePostIds = firstPage.getTimeline().stream()
				.map(item -> item.getPost().getId())
				.toList();
			List<Long> secondPagePostIds = secondPage.getTimeline().stream()
				.map(item -> item.getPost().getId())
				.toList();

			assertThat(firstPagePostIds).doesNotContainAnyElementsOf(secondPagePostIds);

			// 연속성 확인: 두 번째 페이지의 포스트들이 첫 페이지보다 오래되어야 함
			Long lastPostIdFromFirstPage = firstPagePostIds.get(firstPagePostIds.size() - 1);
			Long firstPostIdFromSecondPage = secondPagePostIds.get(0);
			assertThat(firstPostIdFromSecondPage).isLessThan(lastPostIdFromFirstPage);
		}

		@DisplayName("성공 - 마지막 페이지에서는 nextCursor가 null")
		@Test
		void shouldReturnNullCursorOnLastPage() {
			// given
			Member viewer = saveTestMember("viewer");
			Member targetUser = saveTestMember("target");

			// 3개 포스트만 생성
			createPostsWithTimeGap(targetUser, 3);

			// when - limit을 크게 설정하여 모든 포스트 조회
			TimelineResponse response = timelineService.getTimeline(null, 10, viewer, targetUser.getId());

			// then - 마지막 페이지이므로 nextCursor가 null
			assertThat(response.getTimeline()).hasSize(3);
			assertThat(response.getMetadata().isHasNext()).isFalse();
			assertThat(response.getMetadata().getNextCursor()).isNull();
		}

		@DisplayName("성공 - 정확히 limit만큼 있는 경우")
		@Test
		void shouldHandleExactLimitCase() {
			// given
			Member viewer = saveTestMember("viewer");
			Member targetUser = saveTestMember("target");

			// 정확히 3개 포스트 생성
			createPostsWithTimeGap(targetUser, 3);

			// when - limit도 3으로 설정
			TimelineResponse response = timelineService.getTimeline(null, 3, viewer, targetUser.getId());

			// then - 정확히 limit만큼 있으므로 hasNext는 false
			assertThat(response.getTimeline()).hasSize(3);
			assertThat(response.getMetadata().isHasNext()).isFalse();
			assertThat(response.getMetadata().getNextCursor()).isNull();
		}

		@DisplayName("성공 - 리포스트와 포스트가 혼재된 경우의 페이지네이션")
		@Test
		void shouldPaginateWithMixedPostTypes() {
			// given
			Member viewer = saveTestMember("viewer");
			Member targetUser = saveTestMember("target");
			Member other = saveTestMember("other");

			// 시간 간격을 두고 다양한 타입의 포스트 생성
			List<Post> createdPosts = new ArrayList<>();

			// 원본 포스트들 먼저 생성
			Post otherPost1 = Post.createPost(other.getId(), "원본 포스트 1");
			Post otherPost2 = Post.createPost(other.getId(), "원본 포스트 2");
			postRepository.saveAll(List.of(otherPost1, otherPost2));

			// 시간 간격을 두고 targetUser의 활동 생성
			Post targetPost1 = Post.createPost(targetUser.getId(), "타겟 포스트 1");
			postRepository.save(targetPost1);
			updateCreatedAt(targetPost1.getId(), TIME_BASE.minusHours(4));
			createdPosts.add(targetPost1);

			Post targetRepost1 = Post.createRepost(targetUser.getId(), otherPost1.getId());
			postRepository.save(targetRepost1);
			updateCreatedAt(targetRepost1.getId(), TIME_BASE.minusHours(3));
			createdPosts.add(targetRepost1);

			Post targetPost2 = Post.createPost(targetUser.getId(), "타겟 포스트 2");
			postRepository.save(targetPost2);
			updateCreatedAt(targetPost2.getId(), TIME_BASE.minusHours(2));
			createdPosts.add(targetPost2);

			Post targetRepost2 = Post.createRepost(targetUser.getId(), otherPost2.getId());
			postRepository.save(targetRepost2);
			updateCreatedAt(targetRepost2.getId(), TIME_BASE.minusHours(1));
			createdPosts.add(targetRepost2);

			// when - 첫 페이지 조회 (limit: 2)
			TimelineResponse firstPage = timelineService.getTimeline(null, 2, viewer, targetUser.getId());

			// then - 첫 페이지 검증
			assertThat(firstPage.getTimeline()).hasSize(2);
			assertThat(firstPage.getMetadata().isHasNext()).isTrue();
			assertThat(firstPage.getMetadata().getNextCursor()).isNotNull();

			// 최신 2개 (리포스트2, 포스트2)
			List<String> firstPageTypes = firstPage.getTimeline().stream()
				.map(TimelineItemResponse::getType)
				.toList();
			assertThat(firstPageTypes).containsExactly("repost", "post");

			// when - 두 번째 페이지 조회
			TimelineResponse secondPage = timelineService.getTimeline(
				firstPage.getMetadata().getNextCursor(), 2, viewer, targetUser.getId());

			// then - 두 번째 페이지 검증
			assertThat(secondPage.getTimeline()).hasSize(2);
			assertThat(secondPage.getMetadata().isHasNext()).isFalse();
			assertThat(secondPage.getMetadata().getNextCursor()).isNull();

			// 나머지 2개 (리포스트1, 포스트1)
			List<String> secondPageTypes = secondPage.getTimeline().stream()
				.map(TimelineItemResponse::getType)
				.toList();
			assertThat(secondPageTypes).containsExactly("repost", "post");
		}

		@DisplayName("실패 - 잘못된 커서인 경우 400을 반환한다.")
		@Test
		void shouldIgnoreInvalidCursor() {
			// given
			Member viewer = saveTestMember("viewer");
			Member targetUser = saveTestMember("target");

			createPostsWithTimeGap(targetUser, 3);

			String invalidCursor = "invalid-cursor-string";

			// when & then
			assertThrows(
				InvalidCursorException.class,
				() -> timelineService.getTimeline(invalidCursor, 10, viewer, targetUser.getId()));
		}

		@DisplayName("성공 - 빈 커서 처리")
		@Test
		void shouldHandleEmptyCursor() {
			// given
			Member viewer = saveTestMember("viewer");
			Member targetUser = saveTestMember("target");

			createPostsWithTimeGap(targetUser, 2);

			// when & then - 다양한 빈 커서 케이스
			TimelineResponse responseWithNull = timelineService.getTimeline(null, 10, viewer, targetUser.getId());
			assertThat(responseWithNull.getTimeline()).hasSize(2);

			TimelineResponse responseWithEmpty = timelineService.getTimeline("", 10, viewer, targetUser.getId());
			assertThat(responseWithEmpty.getTimeline()).hasSize(2);

			TimelineResponse responseWithWhitespace = timelineService.getTimeline("   ", 10, viewer,
				targetUser.getId());
			assertThat(responseWithWhitespace.getTimeline()).hasSize(2);
		}

		@DisplayName("성공 - 미디어가 포함된 포스트의 페이지네이션")
		@Test
		void shouldPaginateWithMediaPosts() {
			// given
			Member viewer = saveTestMember("viewer");
			Member targetUser = saveTestMember("target");

			// 미디어가 포함된 포스트들 생성
			Media image1 = createAndSaveMedia("https://example.com/image1.jpg");
			Media image2 = createAndSaveMedia("https://example.com/image2.jpg");

			Post post1 = createPostWithMedia(targetUser, "첫 번째 미디어 포스트", List.of(image1));
			Post post2 = createPostWithMedia(targetUser, "두 번째 미디어 포스트", List.of(image2));
			Post post3 = Post.createPost(targetUser.getId(), "텍스트 포스트");
			postRepository.save(post3);

			// 시간 순서 설정
			updateCreatedAt(post1.getId(), TIME_BASE.minusHours(2));
			updateCreatedAt(post2.getId(), TIME_BASE.minusHours(1));
			updateCreatedAt(post3.getId(), TIME_BASE);

			// when - 첫 페이지 조회 (limit: 2)
			TimelineResponse firstPage = timelineService.getTimeline(null, 2, viewer, targetUser.getId());

			// then
			assertThat(firstPage.getTimeline()).hasSize(2);
			assertThat(firstPage.getMetadata().isHasNext()).isTrue();

			// 최신 2개: 텍스트 포스트, 두 번째 미디어 포스트
			assertThat(firstPage.getTimeline().get(0).getPost().getContent()).isEqualTo("텍스트 포스트");
			assertThat(firstPage.getTimeline().get(0).getPost().getMediaEntities()).isEmpty();

			assertThat(firstPage.getTimeline().get(1).getPost().getContent()).isEqualTo("두 번째 미디어 포스트");
			assertThat(firstPage.getTimeline().get(1).getPost().getMediaEntities()).hasSize(1);

			// when - 두 번째 페이지 조회
			TimelineResponse secondPage = timelineService.getTimeline(
				firstPage.getMetadata().getNextCursor(), 2, viewer, targetUser.getId());

			// then
			assertThat(secondPage.getTimeline()).hasSize(1);
			assertThat(secondPage.getMetadata().isHasNext()).isFalse();
			assertThat(secondPage.getMetadata().getNextCursor()).isNull();

			assertThat(secondPage.getTimeline().get(0).getPost().getContent()).isEqualTo("첫 번째 미디어 포스트");
			assertThat(secondPage.getTimeline().get(0).getPost().getMediaEntities()).hasSize(1);
		}

		@DisplayName("성공 - 대량 데이터에서의 커서 동작")
		@Test
		void shouldHandleLargeDataSetPagination() {
			// given
			Member viewer = saveTestMember("viewer");
			Member targetUser = saveTestMember("target");

			// 50개의 포스트 생성
			List<Post> posts = createPostsWithTimeGap(targetUser, 50);

			List<Long> allPostIds = posts.stream()
				.sorted((p1, p2) -> p2.getId().compareTo(p1.getId())) // 최신순
				.map(Post::getId)
				.toList();

			// when - 여러 페이지에 걸쳐 조회
			List<Long> retrievedPostIds = new ArrayList<>();
			String cursor = null;
			int pageCount = 0;
			int limit = 10;

			do {
				TimelineResponse page = timelineService.getTimeline(cursor, limit, viewer, targetUser.getId());

				page.getTimeline().forEach(item ->
					retrievedPostIds.add(item.getPost().getId()));

				cursor = page.getMetadata().getNextCursor();
				pageCount++;

				// 무한 루프 방지
				assertThat(pageCount).isLessThanOrEqualTo(10);

			} while (cursor != null);

			// then
			assertThat(retrievedPostIds).hasSize(50);
			assertThat(retrievedPostIds).containsExactlyElementsOf(allPostIds);
			assertThat(pageCount).isEqualTo(5); // 50개를 10개씩 5페이지
		}

		// 🛠️ 헬퍼 메소드
		private List<Post> createPostsWithTimeGap(Member author, int count) {
			List<Post> posts = new ArrayList<>();

			for (int i = 0; i < count; i++) {
				Post post = Post.createPost(author.getId(), "포스트 " + (i + 1));
				Post savedPost = postRepository.save(post);

				// 시간 간격을 두어 순서 보장 (최신부터 역순으로 시간 설정)
				LocalDateTime createdAt = TIME_BASE.minusHours(count - i);
				updateCreatedAt(savedPost.getId(), createdAt);

				posts.add(savedPost);
			}

			return posts;
		}
	}


	// ===== 헬퍼 메서드 =====
	private Member saveTestMember(String handle) {
		String email = handle + "@test.test";

		Member member = Member.createDefaultMember(email, "password", handle);
		return memberRepository.save(member);
	}

	private Member saveTestMember() {
		Member member = Member.createDefaultMember("test@test.test", "password", "tester");
		return memberRepository.save(member);
	}

	private Media createMedia(String path) {
		return Media.builder()
			.path("https://cdn.twooter.xyz/" + path)
			.build();
	}

	private void updateCreatedAt(Long postId, LocalDateTime createdAt) {
		entityManager
			.createNativeQuery("UPDATE post SET created_at = :createdAt WHERE id = :postId")
			.setParameter("createdAt", createdAt)
			.setParameter("postId", postId)
			.executeUpdate();
	}

	private Media createAndSaveMedia(String path) {
		Media media = Media.builder()
			.path(path)
			.build();
		return mediaRepository.save(media);
	}

	private Post createPostWithMedia(Member author, String content, List<Media> mediaList) {
		// 포스트 생성 및 저장
		Post post = Post.createPost(author.getId(), content);
		Post savedPost = postRepository.save(post);

		// 포스트-미디어 관계 설정
		createPostMediaRelation(savedPost, mediaList);

		return savedPost;
	}

	private void createPostMediaRelation(Post post, List<Media> mediaList) {
		List<PostMedia> postMediaList = mediaList.stream()
			.map(media -> PostMedia.builder()
				.postId(post.getId())
				.mediaId(media.getId())
				.build())
			.toList();

		postMediaRepository.saveAll(postMediaList);
	}

	private void createPostLike(Post post, Member member) {
		PostLike like = PostLike.builder()
			.postId(post.getId())
			.memberId(member.getId())
			.build();
		postLikeRepository.save(like);
	}
}
