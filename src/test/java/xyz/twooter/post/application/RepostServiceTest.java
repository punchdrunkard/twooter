package xyz.twooter.post.application;

import static org.assertj.core.api.AssertionsForClassTypes.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import xyz.twooter.member.domain.Member;
import xyz.twooter.member.domain.repository.MemberRepository;
import xyz.twooter.post.domain.Post;
import xyz.twooter.post.domain.Repost;
import xyz.twooter.post.domain.repository.PostRepository;
import xyz.twooter.post.domain.repository.RepostRepository;
import xyz.twooter.support.IntegrationTestSupport;

class RepostServiceTest extends IntegrationTestSupport {

	@Autowired
	private RepostService repostService;

	@Autowired
	private RepostRepository repostRepository;

	@Autowired
	private PostRepository postRepository;

	@Autowired
	private MemberRepository memberRepository;

	@Nested
	class GetRepostCount {

		@Test
		@DisplayName("성공 - 특정 포스트의 리포스트 개수를 조회한다.")
		void shouldReturnCorrectCount() {
			// given
			Member member1 = saveTestMember("member1", "member1@twooter.com");
			Member member2 = saveTestMember("member2", "member2@twooter.com");
			Member member3 = saveTestMember("member3", "member3@twooter.com");

			Post post = saveTestPost(member1);

			// 리포스트 2개 생성
			saveTestRepost(post.getId(), member2.getId());
			saveTestRepost(post.getId(), member3.getId());

			// when
			long repostCount = repostService.getRepostCount(post.getId());

			// then
			assertThat(repostCount).isEqualTo(2);
		}

		@Test
		@DisplayName("성공 - 리포스트가 없는 포스트는 0을 반환한다.")
		void shouldReturnZeroWhenNoReposts() {
			// given
			Member member = saveTestMember("tester", "test@twooter.com");
			Post post = saveTestPost(member);

			// when
			long repostCount = repostService.getRepostCount(post.getId());

			// then
			assertThat(repostCount).isZero();
		}
	}

	@Nested
	class IsRepostedByMember {

		@Test
		@DisplayName("성공 - 회원이 포스트를 리포스트한 경우 true를 반환한다.")
		void shouldReturnTrueWhenReposted() {
			// given
			Member author = saveTestMember("author", "author@twooter.com");
			Member reposter = saveTestMember("reposter", "reposter@twooter.com");

			Post post = saveTestPost(author);
			saveTestRepost(post.getId(), reposter.getId());

			// when
			boolean isReposted = repostService.isRepostedByMember(post.getId(), reposter.getId());

			// then
			assertThat(isReposted).isTrue();
		}

		@Test
		@DisplayName("성공 - 회원이 포스트를 리포스트하지 않은 경우 false를 반환한다.")
		void shouldReturnFalseWhenNotReposted() {
			// given
			Member author = saveTestMember("author", "author@twooter.com");
			Member nonReposter = saveTestMember("nonreposter", "nonreposter@twooter.com");

			Post post = saveTestPost(author);

			// when
			boolean isReposted = repostService.isRepostedByMember(post.getId(), nonReposter.getId());

			// then
			assertThat(isReposted).isFalse();
		}
	}

	// === 헬퍼 ===

	private Member saveTestMember(String handle, String email) {
		Member member = Member.builder()
			.handle(handle)
			.email(email)
			.build();
		return memberRepository.save(member);
	}

	private Post saveTestPost(Member author) {
		Post post = Post.builder()
			.authorId(author.getId())
			.content("테스트 포스트입니다.")
			.build();
		return postRepository.save(post);
	}

	private Repost saveTestRepost(Long postId, Long memberId) {
		Repost repost = Repost.builder()
			.postId(postId)
			.memberId(memberId)
			.build();
		return repostRepository.save(repost);
	}
}
