package xyz.twooter.post.application;

import static org.assertj.core.api.AssertionsForClassTypes.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import xyz.twooter.member.domain.Member;
import xyz.twooter.member.domain.repository.MemberRepository;
import xyz.twooter.post.domain.Post;
import xyz.twooter.post.domain.PostLike;
import xyz.twooter.post.domain.repository.PostLikeRepository;
import xyz.twooter.post.domain.repository.PostRepository;
import xyz.twooter.post.presentation.dto.response.PostLikeResponse;
import xyz.twooter.support.IntegrationTestSupport;

class PostLikeServiceTest extends IntegrationTestSupport {

	@Autowired
	private PostLikeService postLikeService;

	@Autowired
	private PostLikeRepository postLikeRepository;

	@Autowired
	private PostRepository postRepository;

	@Autowired
	private MemberRepository memberRepository;

	@Nested
	class GetLikeCount {

		@Test
		@DisplayName("성공 - 특정 포스트의 좋아요 개수를 조회한다.")
		void shouldReturnCorrectCount() {
			// given
			Member member1 = saveTestMember("member1", "member1@twooter.com");
			Member member2 = saveTestMember("member2", "member2@twooter.com");
			Member member3 = saveTestMember("member3", "member3@twooter.com");

			Post post = saveTestPost(member1);

			// 좋아요 2개 생성
			saveTestPostLike(post.getId(), member2.getId());
			saveTestPostLike(post.getId(), member3.getId());

			// when
			long likeCount = postLikeService.getLikeCount(post.getId());

			// then
			assertThat(likeCount).isEqualTo(2);
		}

		@Test
		@DisplayName("성공 - 좋아요가 없는 포스트는 0을 반환한다.")
		void shouldReturnZeroWhenNoLikes() {
			// given
			Member member = saveTestMember("tester", "test@twooter.com");
			Post post = saveTestPost(member);

			// when
			long likeCount = postLikeService.getLikeCount(post.getId());

			// then
			assertThat(likeCount).isZero();
		}
	}

	@Nested
	class IsLikedByMember {

		@Test
		@DisplayName("성공 - 회원이 포스트에 좋아요를 누른 경우 true를 반환한다.")
		void shouldReturnTrueWhenLiked() {
			// given
			Member author = saveTestMember("author", "author@twooter.com");
			Member liker = saveTestMember("liker", "liker@twooter.com");

			Post post = saveTestPost(author);
			saveTestPostLike(post.getId(), liker.getId());

			// when
			boolean isLiked = postLikeService.isLikedByMember(post.getId(), liker.getId());

			// then
			assertThat(isLiked).isTrue();
		}

		@Test
		@DisplayName("성공 - 회원이 포스트에 좋아요를 누르지 않은 경우 false를 반환한다.")
		void shouldReturnFalseWhenNotLiked() {
			// given
			Member author = saveTestMember("author", "author@twooter.com");
			Member nonLiker = saveTestMember("nonliker", "nonliker@twooter.com");

			Post post = saveTestPost(author);

			// when
			boolean isLiked = postLikeService.isLikedByMember(post.getId(), nonLiker.getId());

			// then
			assertThat(isLiked).isFalse();
		}
	}

	@Nested
	class LikePost {
		@DisplayName("성공 - 좋아요가 없을 때, 좋아요를 생성할 수 있다. ")
		@Test
		void shouldLikeWhenThePostIsNotLikedByMember() {
			// given
			Member member = saveTestMember("me");
			Member author = saveTestMember("author");

			Post post = saveTestPost(author);

			// when
			PostLikeResponse response = postLikeService.toggleLikeAndCount(post.getId(), member);

			// then
			assertThat(response.getPostId()).isEqualTo(post.getId());
			assertThat(response.getIsLiked()).isTrue();
		}

		@DisplayName("성공 - 좋아요가 이미 있을 때, 좋아요를 취소한다.")
		@Test
		void shouldRevokeLikeWhenThePostIsAlreadyLikedByMember() {
			// given
			Member member = saveTestMember("me");
			Member author = saveTestMember("author");
			Post post = saveTestPost(author);
			saveTestPostLike(post.getId(), member.getId());

			// when
			PostLikeResponse response = postLikeService.toggleLikeAndCount(post.getId(), member);

			// then
			assertThat(response.getPostId()).isEqualTo(post.getId());
			assertThat(response.getIsLiked()).isFalse();
		}
	}
	// === 헬퍼 ===

	private Member saveTestMember(String handle) {
		return saveTestMember(handle, handle + "@test.com");
	}

	private Member saveTestMember(String handle, String email) {
		Member member = Member.createDefaultMember(email, "password", handle);
		return memberRepository.save(member);
	}

	private Post saveTestPost(Member author) {
		Post post = Post.builder()
			.authorId(author.getId())
			.content("테스트 포스트입니다.")
			.build();
		return postRepository.save(post);
	}

	private PostLike saveTestPostLike(Long postId, Long memberId) {
		PostLike postLike = PostLike.builder()
			.postId(postId)
			.memberId(memberId)
			.build();
		return postLikeRepository.save(postLike);
	}
}
