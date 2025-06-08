package xyz.twooter.member.domain.repository;

import static org.assertj.core.api.Assertions.*;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import jakarta.persistence.EntityManager;
import xyz.twooter.member.domain.Follow;
import xyz.twooter.member.domain.Member;
import xyz.twooter.member.presentation.dto.response.MemberProfileWithRelation;
import xyz.twooter.support.IntegrationTestSupport;

class FollowRepositoryTest extends IntegrationTestSupport {

	@Autowired
	private EntityManager entityManager;

	@Autowired
	private FollowRepository followRepository;

	@Nested
	@DisplayName("findFollowersWithRelation 메소드")
	class FindFollowersWithRelation {

		@DisplayName("성공 - 대상 유저의 팔로워 목록을 정확하게 조회해야 한다.")
		@Test
		void shouldFindFollowersWithRelation() {
			// given
			Member targetMember = createMember("target");
			Member follower1 = createMember("follower1");
			Member follower2 = createMember("follower2");
			createMember("notFollower");

			createFollow(follower1, targetMember);
			createFollow(follower2, targetMember);

			// when
			List<MemberProfileWithRelation> followers = followRepository.findFollowersWithRelation(
				targetMember.getId(),
				null, // 로그인하지 않은 경우
				null, // 커서가 없는 경우 (첫 페이지)
				null, // 커서 ID가 없는 경우 (첫 페이지)
				10 // limit
			);

			// then
			assertThat(followers).isNotEmpty();
			assertThat(followers).hasSize(2);
			List<Long> followersId = followers.stream().map(MemberProfileWithRelation::getId).toList();
			assertThat(followersId).containsExactlyInAnyOrder(follower1.getId(), follower2.getId());
		}

		@DisplayName("성공 - 로그인하지 않은 경우, 팔로우 관계 필드가 모두 false여야 한다")
		@Test
		void shouldFollowRelationsSetNullWhenViewerDoesNotLogin() {
			// given
			Member targetMember = createMember("target");
			Member follower1 = createMember("follower1");
			Member follower2 = createMember("follower2");
			createFollow(follower1, targetMember);
			createFollow(follower2, targetMember);

			// when
			List<MemberProfileWithRelation> followers = followRepository.findFollowersWithRelation(
				targetMember.getId(),
				null, // 로그인하지 않은 경우
				null, // 커서가 없는 경우 (첫 페이지)
				null, // 커서 ID가 없는 경우 (첫 페이지)
				10 // limit
			);

			// then
			assertThat(followers).hasSize(2);
			assertThat(followers.stream().map(MemberProfileWithRelation::isFollowingByMe).collect(Collectors.toList()))
				.containsOnly(false);
			assertThat(followers.stream().map(MemberProfileWithRelation::isFollowsMe).collect(Collectors.toList()))
				.containsOnly(false);
		}

		@DisplayName("성공 - 로그인한 유저가 조회 시, 각 팔로워에 대한 자신의 팔로우 관계를 정확히 보여줘야 한다")
		@Test
		void shouldShowsViewerFollowRelationsForLoggedInUser() {
			// given
			Member targetMember = createMember("target");
			Member follower1 = createMember("follower1");
			Member follower2 = createMember("follower2");
			Member follower3 = createMember("follower3");

			Member viewer = createMember("viewer");
			createFollow(follower1, targetMember);
			createFollow(follower2, targetMember);
			createFollow(follower3, targetMember);

			createFollow(viewer, follower1); // viewer -> follower1 팔로우
			createFollow(follower2, viewer); // follower2 -> viewer 팔로우
			// viewer follower 3은 맞팔로우
			createFollow(follower3, viewer);
			createFollow(viewer, follower3);

			// when
			List<MemberProfileWithRelation> followers = followRepository.findFollowersWithRelation(
				targetMember.getId(),
				viewer.getId(), // 로그인한 유저
				null, // 커서가 없는 경우 (첫 페이지)
				null, // 커서 ID가 없는 경우 (첫 페이지)
				10 // limit
			);

			// then
			assertThat(followers).hasSize(3);
			assertThat(followers)
				.extracting(MemberProfileWithRelation::getId, MemberProfileWithRelation::isFollowingByMe, MemberProfileWithRelation::isFollowsMe)
				.containsExactlyInAnyOrder( // 순서는 상관없으므로 AnyOrder 사용
					tuple(follower1.getId(), true, false),  // viewer -> follower1 (O), follower1 -> viewer (X)
					tuple(follower2.getId(), false, true),  // viewer -> follower2 (X), follower2 -> viewer (O)
					tuple(follower3.getId(), true, true)   // viewer <-> follower3 (O, O)
				);
		}
	}

	@Nested
	@DisplayName("findFolloweesWithRelation 메소드")
	class FindFolloweesWithRelation {

		@DisplayName("성공 - 대상 유저의 팔로잉 목록을 정확하게 조회해야 한다.")
		@Test
		void shouldFindFolloweesWithRelation() {
			// given
			Member targetMember = createMember("target");
			Member followee1 = createMember("followee1");
			Member followee2 = createMember("followee2");
			createMember("notFollowee");

			createFollow(targetMember, followee1);
			createFollow(targetMember, followee2);

			// when
			List<MemberProfileWithRelation> followees = followRepository.findFolloweesWithRelation(
				targetMember.getId(),
				null, // 로그인하지 않은 경우
				null, // 커서가 없는 경우 (첫 페이지)
				null, // 커서 ID가 없는 경우 (첫 페이지)
				10 // limit
			);

			// then
			assertThat(followees).isNotEmpty();
			assertThat(followees).hasSize(2);
			List<Long> followeesId = followees.stream().map(MemberProfileWithRelation::getId).toList();
			assertThat(followeesId).containsExactlyInAnyOrder(followee1.getId(), followee2.getId());
		}

		@DisplayName("성공 - 로그인하지 않은 경우, 팔로우 관계 필드가 모두 false여야 한다")
		@Test
		void shouldFollowRelationsSetNullWhenViewerDoesNotLogin() {
			// given
			Member targetMember = createMember("target");
			Member followee1 = createMember("followee1");
			Member followee2 = createMember("followee2");
			createFollow(targetMember, followee1);
			createFollow(targetMember, followee2);

			// when
			List<MemberProfileWithRelation> followees = followRepository.findFolloweesWithRelation(
				targetMember.getId(),
				null, // 로그인하지 않은 경우
				null, // 커서가 없는 경우 (첫 페이지)
				null, // 커서 ID가 없는 경우 (첫 페이지)
				10 // limit
			);

			// then
			assertThat(followees).hasSize(2);
			assertThat(followees.stream().map(MemberProfileWithRelation::isFollowingByMe).collect(Collectors.toList()))
				.containsOnly(false);
		}

		@DisplayName("성공 - 로그인한 유저가 조회 시, 각 팔로잉에 대한 자신의 팔로우 관계를 정확히 보여줘야 한다")
		@Test
		void shouldShowsViewerFollowRelationsForLoggedInUser() {
			// given
			Member targetMember = createMember("target");
			Member followee1 = createMember("followee1");
			Member followee2 = createMember("followee2");
			Member followee3 = createMember("followee3");

			Member viewer = createMember("viewer");
			createFollow(targetMember, followee1);
			createFollow(targetMember, followee2);
			createFollow(targetMember, followee3);

			createFollow(viewer, followee1); // viewer -> followee1 팔로우
			createFollow(followee2, viewer); // followee2 -> viewer 팔로우
			// viewer followee 3은 맞팔로우
			createFollow(followee3, viewer);
			createFollow(viewer, followee3);

			// when
			List<MemberProfileWithRelation> followees = followRepository.findFolloweesWithRelation(
				targetMember.getId(),
				viewer.getId(), // 로그인한 유저
				null, // 커서가 없는 경우 (첫 페이지)
				null, // 커서 ID가 없는 경우 (첫 페이지)
				10 // limit
			);

			// then
			assertThat(followees).hasSize(3);
			assertThat(followees)
				.extracting(MemberProfileWithRelation::getId, MemberProfileWithRelation::isFollowingByMe, MemberProfileWithRelation::isFollowsMe)
				.containsExactlyInAnyOrder( // 순서는 상관없으므로 AnyOrder 사용
					tuple(followee1.getId(), true, false),  // viewer -> followee1 (O), followee1 -> viewer (X)
					tuple(followee2.getId(), false, true),  // viewer -> followee2 (X), followee2 -> viewer (O)
					tuple(followee3.getId(), true, true)   // viewer <-> followee3 (O, O)
				);
		}
	}

	// ==== 헬퍼 =====
	private Follow createFollow(Member follower, Member followee) {
		Follow follow = Follow.builder()
			.followerId(follower.getId())
			.followeeId(followee.getId())
			.build();

		entityManager.persist(follow);
		return follow;
	}

	private Member createMember(String handle) {
		Member member = Member.builder()
			.handle(handle)
			.nickname("nickname_" + handle)
			.email(handle + "@test.test")
			.build();

		entityManager.persist(member);
		return member;
	}
}
