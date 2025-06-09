package xyz.twooter.member.application;

import static org.assertj.core.api.Assertions.*;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import xyz.twooter.auth.domain.exception.IllegalMemberIdException;
import xyz.twooter.member.domain.Follow;
import xyz.twooter.member.domain.Member;
import xyz.twooter.member.domain.exception.AlreadyFollowingException;
import xyz.twooter.member.domain.exception.SelfFollowException;
import xyz.twooter.member.domain.repository.FollowRepository;
import xyz.twooter.member.domain.repository.MemberRepository;
import xyz.twooter.member.presentation.dto.response.FollowResponse;
import xyz.twooter.member.presentation.dto.response.MemberProfileWithRelation;
import xyz.twooter.member.presentation.dto.response.MemberWithRelationResponse;
import xyz.twooter.member.presentation.dto.response.UnFollowResponse;
import xyz.twooter.support.IntegrationTestSupport;

class FollowServiceTest extends IntegrationTestSupport {

	@Autowired
	private FollowService followService;
	@Autowired
	private FollowRepository followRepository;
	@Autowired
	private MemberRepository memberRepository;

	@Nested
	@DisplayName("Follow Member")
	class FollowMemberTests {

		@DisplayName("성공 - 멤버를 팔로우할 수 있다.")
		@Test
		void shouldFollowMemberWhenValidRequest() {
			// given
			Member member = saveTestMember("follower");
			Member targetMember = saveTestMember("followee");

			// when
			FollowResponse followResponse = followService.followMember(member, targetMember.getId());

			// then
			assertThat(followResponse).isNotNull();
			assertThat(followResponse.getTargetMemberId()).isEqualTo(targetMember.getId());
			assertThat(followResponse.getFollowedAt()).isNotNull();
		}

		@DisplayName("실패 - 존재하지 않는 멤버를 팔로우할 수 없다.")
		@Test
		void shouldThrowExceptionWhenTargetMemberIdDoesNotExist() {
			// given
			Member member = saveTestMember("follower");
			Long nonExistentMemberId = -1L; // Assuming this ID does not exist

			// when & then
			assertThatThrownBy(() -> followService.followMember(member, nonExistentMemberId))
				.isInstanceOf(IllegalMemberIdException.class);
		}

		@DisplayName("실패 - 이미 팔로우하고 있는 경우 예외가 발생한다.")
		@Test
		void shouldThrowExceptionWhenFollowingAlreadyFollowedMember() {
			// given
			Member member = saveTestMember("follower");
			Member targetMember = saveTestMember("followee");
			saveFollow(member, targetMember);

			// when & then
			assertThatThrownBy(() -> followService.followMember(member, targetMember.getId()))
				.isInstanceOf(AlreadyFollowingException.class);
		}

		@DisplayName("실패 - 자기 자신을 팔로우할 수 없다.")
		@Test
		void shouldThrowExceptionWhenFollowingSelf() {
			// given
			Member member = saveTestMember();

			// when & then
			assertThatThrownBy(() -> followService.followMember(member, member.getId()))
				.isInstanceOf(SelfFollowException.class);
		}
	}

	@Nested
	@DisplayName("Unfollow Member")
	class UnfollowMemberTests {

		@DisplayName("성공 - 멤버를 언팔로우할 수 있다.")
		@Test
		void shouldUnfollowMemberWhenValidRequest() {
			// given
			Member member = saveTestMember("follower");
			Member targetMember = saveTestMember("followee");
			saveFollow(member, targetMember);

			// when
			UnFollowResponse response = followService.unfollowMember(member, targetMember.getId());

			// then
			assertThat(followRepository.existsByFollowerIdAndFolloweeId(member.getId(), targetMember.getId()))
				.isFalse();
			assertThat(response).isNotNull();
			assertThat(response.getTargetMemberId()).isEqualTo(targetMember.getId());
		}

		@DisplayName("실패 - 존재하지 않는 멤버를 언팔로우할 수 없다.")
		@Test
		void shouldThrowExceptionWhenUnfollowingNonExistentMember() {
			// given
			Member member = saveTestMember("follower");
			Long nonExistentMemberId = -1L; // Assuming this ID does not exist

			// when & then
			assertThatThrownBy(() -> followService.unfollowMember(member, nonExistentMemberId))
				.isInstanceOf(IllegalMemberIdException.class);
		}

		@DisplayName("성공 - 팔로우 관계가 없는 멤버를 언팔로우할 때 예외가 발생하지 않는다.(idempotent)")
		@Test
		void shouldSucceedUnfollowWhenNotFollowingMember() {
			// given
			Member member = saveTestMember("follower");
			Member targetMember = saveTestMember("followee");

			// when
			UnFollowResponse response = followService.unfollowMember(member, targetMember.getId());

			// then
			assertThat(followRepository.existsByFollowerIdAndFolloweeId(member.getId(), targetMember.getId()))
				.isFalse();
			assertThat(response).isNotNull();
			assertThat(response.getTargetMemberId()).isEqualTo(targetMember.getId());

		}
	}

	@Nested
	@DisplayName("유저의 팔로워 조회")
	class GetFollowersTests {

		@DisplayName("성공 - 유저의 팔로워를 조회할 수 있다.")
		@Test
		void shouldGetFollowersWhenValidMember() {
			// given
			Member member = saveTestMember("member");
			Member follower1 = saveTestMember("follower1");
			Member follower2 = saveTestMember("follower2");
			saveFollow(follower1, member);
			saveFollow(follower2, member);

			// when
			MemberWithRelationResponse response = followService.getFollowers(null, 10, null, member.getId());
			List<MemberProfileWithRelation> followers = response.getMembers();

			// then
			assertThat(followers).hasSize(2);
			assertThat(followers.stream().map(MemberProfileWithRelation::getId).collect(Collectors.toList()))
				.containsExactlyInAnyOrder(
					follower1.getId(),
					follower2.getId()
				);
		}

		@DisplayName("실패 - 존재하지 않는 멤버의 팔로워를 조회할 수 없다.")
		@Test
		void shouldThrowExceptionWhenGettingFollowersOfNonExistentMember() {
			// given
			Long nonExistentMemberId = -1L;

			// when & then
			assertThatThrownBy(() -> followService.getFollowers(null, 10, null, nonExistentMemberId))
				.isInstanceOf(IllegalMemberIdException.class);
		}
	}

	@Nested
	@DisplayName("유저의 팔로잉 목록 조회")
	class GetFollowingTests {

		@DisplayName("성공 - 유저의 팔로잉 목록을 조회할 수 있다.")
		@Test
		void shouldGetFollowingWhenValidMember() {
			// given
			Member member = saveTestMember("member");
			Member followee1 = saveTestMember("followee1");
			Member followee2 = saveTestMember("followee2");
			saveFollow(member, followee1);
			saveFollow(member, followee2);

			// when
			MemberWithRelationResponse response = followService.getFollowing(null, 10, null, member.getId());
			List<MemberProfileWithRelation> followings = response.getMembers();

			// then
			assertThat(followings).hasSize(2);
			assertThat(followings.stream().map(MemberProfileWithRelation::getId).collect(Collectors.toList()))
				.containsExactlyInAnyOrder(
					followee1.getId(),
					followee2.getId()
				);
		}

		@DisplayName("실패 - 존재하지 않는 멤버의 팔로잉 목록을 조회할 수 없다.")
		@Test
		void shouldThrowExceptionWhenGettingFolloweesOfNonExistentMember() {
			// given
			Long nonExistentMemberId = -1L;

			// when & then
			assertThatThrownBy(() -> followService.getFollowing(null, 10, null, nonExistentMemberId))
				.isInstanceOf(IllegalMemberIdException.class);
		}
	}

	// ========= 헬퍼 메서드 =========
	private void saveFollow(Member member, Member targetMember) {
		Follow follow = Follow.builder()
			.followerId(member.getId())
			.followeeId(targetMember.getId())
			.build();
		followRepository.save(follow);
	}

	private Member saveTestMember(String handle) {
		String email = handle + "@test.test";

		Member member = Member.createDefaultMember(email, "password", handle);
		return memberRepository.save(member);
	}

	private Member saveTestMember() {
		Member member = Member.createDefaultMember("test@test.test", "password", "tester");
		return memberRepository.save(member);
	}
}

