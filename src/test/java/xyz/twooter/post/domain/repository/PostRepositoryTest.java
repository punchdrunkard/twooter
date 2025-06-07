package xyz.twooter.post.domain.repository;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import jakarta.persistence.EntityManager;
import xyz.twooter.member.domain.Follow;
import xyz.twooter.member.domain.Member;
import xyz.twooter.post.domain.Post;
import xyz.twooter.post.domain.PostLike;
import xyz.twooter.post.presentation.dto.projection.TimelineItemProjection;
import xyz.twooter.support.IntegrationTestSupport;

class PostRepositoryTest extends IntegrationTestSupport {

	@Autowired
	private EntityManager entityManager;

	@Autowired
	private PostRepository postRepository;

	private Member targetUser;
	private Member viewer;
	private Member otherUser;
	private LocalDateTime baseTime;

	@BeforeEach
	void setUp() {
		baseTime = LocalDateTime.of(2025, 5, 5, 10, 10, 0);

		// 테스트 유저들 생성
		targetUser = createMember("target", "타겟유저", "target@test.com");
		viewer = createMember("viewer", "뷰어", "viewer@test.com");
		otherUser = createMember("other", "다른유저", "other@test.com");

		entityManager.flush();
		entityManager.clear();
	}

	@Nested
	@DisplayName("findUserTimelineWithPagination 메서드는")
	class FindUserTimelineWithPagination {

		@Nested
		@DisplayName("기본 조회 시")
		class WhenBasicQuery {

			@DisplayName("로그인 하지 않은 경우에도 조회할 수 있다.")
			@Test
			void shouldGetTimelineWhenViewerDoesNotLogin() {
				// given
				Post post1 = createPost(targetUser, "첫 번째 포스트", baseTime.minusHours(1));
				Post post2 = createPost(targetUser, "두 번째 포스트", baseTime.minusHours(2));
				Post post3 = createPost(otherUser, "다른 유저 포스트", baseTime.minusHours(3));

				entityManager.flush();
				entityManager.clear();

				// when
				List<TimelineItemProjection> result = postRepository.findUserTimelineWithPagination(
					targetUser.getId(), null, null, null, 10);

				// then
				assertThat(result).hasSize(2);
				assertThat(result.get(0).getType()).isEqualTo("post");
				assertThat(result.get(0).getOriginalPostContent()).isEqualTo("첫 번째 포스트");
				assertThat(result.get(1).getOriginalPostContent()).isEqualTo("두 번째 포스트");

				// 시간순 정렬 확인
				assertThat(result.get(0).getFeedCreatedAt()).isAfter(result.get(1).getFeedCreatedAt());
			}

			@Test
			@DisplayName("리포스트가 없을 때 유저 포스트만 반환한다")
			void shouldReturnUserPostsOnlyWhenNoRepostsExist() {
				// Given
				Post post1 = createPost(targetUser, "첫 번째 포스트", baseTime.minusHours(1));
				Post post2 = createPost(targetUser, "두 번째 포스트", baseTime.minusHours(2));
				Post post3 = createPost(otherUser, "다른 유저 포스트", baseTime.minusHours(3));

				entityManager.flush();
				entityManager.clear();

				// When
				List<TimelineItemProjection> result = postRepository.findUserTimelineWithPagination(
					targetUser.getId(), viewer.getId(), null, null, 10);

				// Then
				assertThat(result).hasSize(2);
				assertThat(result.get(0).getType()).isEqualTo("post");
				assertThat(result.get(0).getOriginalPostContent()).isEqualTo("첫 번째 포스트");
				assertThat(result.get(1).getOriginalPostContent()).isEqualTo("두 번째 포스트");

				// 시간순 정렬 확인
				assertThat(result.get(0).getFeedCreatedAt()).isAfter(result.get(1).getFeedCreatedAt());
			}

			@Test
			@DisplayName("포스트와 리포스트가 모두 있을 때 섞어서 반환한다")
			void shouldReturnMixedPostsAndRepostsWhenUserHasBoth() {
				// Given
				Post originalPost = createPost(otherUser, "원본 포스트", baseTime.minusHours(3));
				Post userPost = createPost(targetUser, "유저 포스트", baseTime.minusHours(1));
				Post repost = createRepost(targetUser, originalPost, baseTime.minusHours(2));

				entityManager.flush();
				entityManager.clear();

				// When
				List<TimelineItemProjection> result = postRepository.findUserTimelineWithPagination(
					targetUser.getId(), viewer.getId(), null, null, 10);

				// Then
				assertThat(result).hasSize(2);

				// 첫 번째는 유저 포스트 (가장 최근)
				assertThat(result.get(0).getType()).isEqualTo("post");
				assertThat(result.get(0).getOriginalPostContent()).isEqualTo("유저 포스트");

				// 두 번째는 리포스트
				assertThat(result.get(1).getType()).isEqualTo("repost");
				assertThat(result.get(1).getOriginalPostContent()).isEqualTo("원본 포스트");
				assertThat(result.get(1).getOriginalPostAuthorHandle()).isEqualTo("other");
				assertThat(result.get(1).getRepostAuthorHandle()).isEqualTo("target");
			}

			@Test
			@DisplayName("유저 포스트가 없을 때 빈 리스트를 반환한다")
			void shouldReturnEmptyListWhenUserHasNoPosts() {
				// Given - targetUser has no posts
				createPost(otherUser, "다른 유저 포스트", baseTime.minusHours(1));

				entityManager.flush();
				entityManager.clear();

				// When
				List<TimelineItemProjection> result = postRepository.findUserTimelineWithPagination(
					targetUser.getId(), viewer.getId(), null, null, 10);

				// Then
				assertThat(result).isEmpty();
			}
		}

		@Nested
		@DisplayName("사용자 상호작용 확인 시")
		class WhenCheckingUserInteractions {

			@Test
			@DisplayName("뷰어가 좋아요한 포스트의 경우 올바른 좋아요 상태를 반환한다")
			void shouldReturnCorrectLikeStatusWhenViewerLikedPosts() {
				// Given
				Post post1 = createPost(targetUser, "좋아요한 포스트", baseTime.minusHours(1));
				Post post2 = createPost(targetUser, "좋아요 안한 포스트", baseTime.minusHours(2));
				Post originalPost = createPost(otherUser, "좋아요한 원본 포스트", baseTime.minusHours(4));
				Post repost = createRepost(targetUser, originalPost, baseTime.minusHours(3));

				// 뷰어가 좋아요
				createPostLike(viewer, post1);
				createPostLike(viewer, originalPost);

				entityManager.flush();
				entityManager.clear();

				// When
				List<TimelineItemProjection> result = postRepository.findUserTimelineWithPagination(
					targetUser.getId(), viewer.getId(), null, null, 10);

				// Then
				assertThat(result).hasSize(3);

				// 좋아요한 포스트
				TimelineItemProjection likedPost = result.stream()
					.filter(p -> "좋아요한 포스트".equals(p.getOriginalPostContent()))
					.findFirst().orElseThrow();
				assertThat(likedPost.getIsLiked()).isTrue();

				// 좋아요 안한 포스트
				TimelineItemProjection notLikedPost = result.stream()
					.filter(p -> "좋아요 안한 포스트".equals(p.getOriginalPostContent()))
					.findFirst().orElseThrow();
				assertThat(notLikedPost.getIsLiked()).isFalse();

				// 좋아요한 원본 포스트의 리포스트
				TimelineItemProjection likedRepost = result.stream()
					.filter(p -> "repost".equals(p.getType()))
					.findFirst().orElseThrow();
				assertThat(likedRepost.getIsLiked()).isTrue();
			}

			@Test
			@DisplayName("뷰어가 리포스트한 포스트의 경우 올바른 리포스트 상태를 반환한다")
			void shouldReturnCorrectRepostStatusWhenViewerReposted() {
				// Given
				Post originalPost1 = createPost(otherUser, "리포스트한 포스트", baseTime.minusHours(3));
				Post originalPost2 = createPost(otherUser, "리포스트 안한 포스트", baseTime.minusHours(4));
				Post targetRepost1 = createRepost(targetUser, originalPost1, baseTime.minusHours(1));
				Post targetRepost2 = createRepost(targetUser, originalPost2, baseTime.minusHours(2));

				// 뷰어가 첫 번째 포스트만 리포스트
				createRepost(viewer, originalPost1, baseTime.minusMinutes(30));

				entityManager.flush();
				entityManager.clear();

				// When
				List<TimelineItemProjection> result = postRepository.findUserTimelineWithPagination(
					targetUser.getId(), viewer.getId(), null, null, 10);

				// Then
				assertThat(result).hasSize(2);

				TimelineItemProjection repostedByViewer = result.stream()
					.filter(p -> "리포스트한 포스트".equals(p.getOriginalPostContent()))
					.findFirst().orElseThrow();
				assertThat(repostedByViewer.getIsReposted()).isTrue();

				TimelineItemProjection notRepostedByViewer = result.stream()
					.filter(p -> "리포스트 안한 포스트".equals(p.getOriginalPostContent()))
					.findFirst().orElseThrow();
				assertThat(notRepostedByViewer.getIsReposted()).isFalse();
			}

			@Test
			@DisplayName("뷰어가 null일 때 모든 상호작용 상태를 false로 반환한다")
			void shouldReturnFalseForInteractionsWhenViewerIsNull() {
				// Given
				Post post = createPost(targetUser, "테스트 포스트", baseTime.minusHours(1));

				entityManager.flush();
				entityManager.clear();

				// When
				List<TimelineItemProjection> result = postRepository.findUserTimelineWithPagination(
					targetUser.getId(), null, null, null, 10);

				// Then
				assertThat(result).hasSize(1);
				assertThat(result.get(0).getIsLiked()).isFalse();
				assertThat(result.get(0).getIsReposted()).isFalse();
			}
		}

		@Nested
		@DisplayName("페이지네이션 적용 시")
		class WhenApplyingPagination {

			@Test
			@DisplayName("커서가 제공되면 올바르게 페이지네이션을 적용한다")
			void shouldApplyPaginationCorrectlyWhenCursorProvided() {
				// Given
				LocalDateTime time1 = baseTime.minusHours(1);
				LocalDateTime time2 = baseTime.minusHours(2);
				LocalDateTime time3 = baseTime.minusHours(3);

				Post post1 = createPost(targetUser, "첫 번째 포스트", time1);
				Post post2 = createPost(targetUser, "두 번째 포스트", time2);
				Post post3 = createPost(targetUser, "세 번째 포스트", time3);

				entityManager.flush();
				entityManager.clear();

				// When - 첫 번째 포스트 이후부터 조회
				List<TimelineItemProjection> result = postRepository.findUserTimelineWithPagination(
					targetUser.getId(), viewer.getId(), time1, post1.getId(), 10);

				// Then
				assertThat(result).hasSize(2);
				assertThat(result.get(0).getOriginalPostContent()).isEqualTo("두 번째 포스트");
				assertThat(result.get(1).getOriginalPostContent()).isEqualTo("세 번째 포스트");
			}

			@Test
			@DisplayName("더 많은 포스트가 있을 때 limit을 준수한다")
			void shouldRespectLimitWhenMorePostsExist() {
				// Given
				for (int i = 1; i <= 5; i++) {
					createPost(targetUser, "포스트 " + i, baseTime.minusHours(i));
				}

				entityManager.flush();
				entityManager.clear();

				// When
				List<TimelineItemProjection> result = postRepository.findUserTimelineWithPagination(
					targetUser.getId(), viewer.getId(), null, null, 3);

				// Then
				assertThat(result).hasSize(4); // limit + 1 for hasNext check
			}

			@Test
			@DisplayName("동일한 시간의 포스트들에 대해 ID 기준으로 올바르게 정렬한다")
			void shouldHandleSameTimestampCorrectlyWhenPaginationApplied() {
				// Given - 같은 시간에 생성된 포스트들
				LocalDateTime sameTime = baseTime.minusHours(1);
				Post post1 = createPostWithSpecificTime(targetUser, "포스트 1", sameTime);
				Post post2 = createPostWithSpecificTime(targetUser, "포스트 2", sameTime);
				Post post3 = createPostWithSpecificTime(targetUser, "포스트 3", sameTime);

				entityManager.flush();
				entityManager.clear();

				// When - 첫 번째 포스트 이후부터 조회 (ID로 구분)
				List<TimelineItemProjection> result = postRepository.findUserTimelineWithPagination(
					targetUser.getId(), viewer.getId(), sameTime,
					Math.max(post1.getId(), Math.max(post2.getId(), post3.getId())), 10);

				// Then - ID가 더 작은 포스트들만 조회되어야 함
				assertThat(result).hasSizeLessThan(3);
			}
		}

		@Nested
		@DisplayName("필터링 적용 시")
		class WhenApplyingFiltering {

			@Test
			@DisplayName("삭제된 포스트를 제외한다")
			void shouldExcludeDeletedPostsWhenFiltering() {
				// Given
				Post activePost = createPost(targetUser, "활성 포스트", baseTime.minusHours(1));
				Post deletedPost = createPost(targetUser, "삭제된 포스트", baseTime.minusHours(2));
				deletedPost.softDelete(); // 소프트 삭제

				entityManager.flush();
				entityManager.clear();

				// When
				List<TimelineItemProjection> result = postRepository.findUserTimelineWithPagination(
					targetUser.getId(), viewer.getId(), null, null, 10);

				// Then
				assertThat(result).hasSize(1);
				assertThat(result.get(0).getOriginalPostContent()).isEqualTo("활성 포스트");
			}

			@Test
			@DisplayName("삭제된 원본 포스트의 리포스트를 제외한다")
			void shouldExcludeRepostsOfDeletedOriginalPostsWhenFiltering() {
				// Given
				Post activeOriginal = createPost(otherUser, "활성 원본", baseTime.minusHours(3));
				Post deletedOriginal = createPost(otherUser, "삭제된 원본", baseTime.minusHours(4));
				deletedOriginal.softDelete();

				Post activeRepost = createRepost(targetUser, activeOriginal, baseTime.minusHours(1));
				Post deletedRepost = createRepost(targetUser, deletedOriginal, baseTime.minusHours(2));

				entityManager.flush();
				entityManager.clear();

				// When
				List<TimelineItemProjection> result = postRepository.findUserTimelineWithPagination(
					targetUser.getId(), viewer.getId(), null, null, 10);

				// Then
				assertThat(result).hasSize(1);
				assertThat(result.get(0).getType()).isEqualTo("repost");
				assertThat(result.get(0).getOriginalPostContent()).isEqualTo("활성 원본");
			}
		}
	}

	@Nested
	@DisplayName("findHomeTimelineWithPagination 메서드는")
	class FindHomeTimeline {
		private Member followedUser1;
		private Member followedUser2;
		private Member notFollowedUser;

		@BeforeEach
		void setUpHomeTimeline() {
			// 팔로우할 유저들 생성
			followedUser1 = createMember("followed1", "팔로우유저1", "followed1@test.com");
			followedUser2 = createMember("followed2", "팔로우유저2", "followed2@test.com");
			notFollowedUser = createMember("notfollowed", "언팔로우유저", "notfollowed@test.com");

			// viewer가 followedUser1, followedUser2를 팔로우
			createFollow(viewer, followedUser1);
			createFollow(viewer, followedUser2);

			entityManager.flush();
			entityManager.clear();
		}

		@DisplayName("자신의 포스트와 팔로우 한 사용자의 포스트를 모두 조회할 수 있다.")
		@Test
		void shouldReturnBothFollowersAndMyPosts() {
			// given
			Post myPost = createPost(viewer, "내 포스트", baseTime.minusHours(1));
			Post followedPost1 = createPost(followedUser1, "팔로우한 유저1의 포스트", baseTime.minusHours(2));
			Post followedPost2 = createPost(followedUser2, "팔로우한 유저2의 포스트", baseTime.minusHours(3));
			Post notFollowedPost = createPost(notFollowedUser, "팔로우하지 않은 유저의 포스트", baseTime.minusHours(4));
			Post followsRepost1 = createRepost(followedUser1, myPost, baseTime.minusHours(5));

			// when
			List<TimelineItemProjection> result = postRepository.findHomeTimelineWithPagination(
				viewer.getId(), null, null, 10);

			// then
			assertThat(result).hasSize(4); // 내 포스트 + 팔로우한 유저의 포스트 2개 + 리포스트

			assertThat(
				result.stream().map(TimelineItemProjection::getOriginalPostId).collect(Collectors.toList()))
				.containsExactly(
					myPost.getId(),
					followedPost1.getId(),
					followedPost2.getId(),
					followsRepost1.getRepostOfId()
				);
		}
	}

	// Helper Methods
	private Member createMember(String handle, String nickname, String email) {
		Member member = Member.builder()
			.handle(handle)
			.nickname(nickname)
			.email(email)
			.build();

		entityManager.persist(member);
		return member;
	}

	private Post createPost(Member author, String content, LocalDateTime createdAt) {
		Post post = Post.builder()
			.authorId(author.getId())
			.content(content)
			.build();

		entityManager.persist(post);
		entityManager.flush(); // ID 생성을 위해 flush
		setCreatedAt(post.getId(), createdAt);
		return post;
	}

	private Post createPostWithSpecificTime(Member author, String content, LocalDateTime createdAt) {
		return createPost(author, content, createdAt);
	}

	private Post createRepost(Member author, Post originalPost, LocalDateTime createdAt) {
		Post repost = Post.builder()
			.authorId(author.getId())
			.repostOfId(originalPost.getId())
			.build();

		entityManager.persist(repost);
		entityManager.flush(); // ID 생성을 위해 flush
		setCreatedAt(repost.getId(), createdAt);
		return repost;
	}

	private PostLike createPostLike(Member member, Post post) {
		PostLike postLike = PostLike.builder()
			.memberId(member.getId())
			.postId(post.getId())
			.build();

		entityManager.persist(postLike);
		return postLike;
	}

	private void setCreatedAt(Long postId, LocalDateTime dateTime) {
		entityManager.createNativeQuery(
				"UPDATE post SET created_at = :dateTime WHERE id = :id"
			)
			.setParameter("dateTime", dateTime)
			.setParameter("id", postId)
			.executeUpdate();
	}

	private Follow createFollow(Member follower, Member followee) {
		Follow follow = Follow.builder()
			.followerId(follower.getId())
			.followeeId(followee.getId())
			.build();

		entityManager.persist(follow);
		return follow;
	}
}
