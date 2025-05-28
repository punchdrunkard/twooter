package xyz.twooter.post.application;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import xyz.twooter.media.domain.Media;
import xyz.twooter.media.domain.repository.MediaRepository;
import xyz.twooter.media.presentation.dto.response.MediaSimpleResponse;
import xyz.twooter.member.domain.Member;
import xyz.twooter.member.domain.repository.MemberRepository;
import xyz.twooter.post.domain.Post;
import xyz.twooter.post.domain.PostLike;
import xyz.twooter.post.domain.PostMedia;
import xyz.twooter.post.domain.Repost;
import xyz.twooter.post.domain.exception.PostNotFoundException;
import xyz.twooter.post.domain.repository.PostLikeRepository;
import xyz.twooter.post.domain.repository.PostMediaRepository;
import xyz.twooter.post.domain.repository.PostRepository;
import xyz.twooter.post.domain.repository.RepostRepository;
import xyz.twooter.post.presentation.dto.request.PostCreateRequest;
import xyz.twooter.post.presentation.dto.response.PostCreateResponse;
import xyz.twooter.post.presentation.dto.response.PostResponse;
import xyz.twooter.support.IntegrationTestSupport;

class PostServiceTest extends IntegrationTestSupport {

	@Autowired
	private PostService postService;
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
	private RepostRepository repostRepository;

	@BeforeEach
	void setUp() {
		// 테스트 전에 관련 데이터 모두 삭제
		postMediaRepository.deleteAll();
		mediaRepository.deleteAll();
		postRepository.deleteAll();
		memberRepository.deleteAll();
		postLikeRepository.deleteAll();
		repostRepository.deleteAll();
	}

	@Nested
	class CreatePost {

		@Test
		@DisplayName("성공 - 미디어 없이 포스트를 생성할 수 있다.")
		void shouldCreatePostWithoutMedia() {
			// given
			Member member = saveTestMember();

			PostCreateRequest request = PostCreateRequest.builder()
				.content("텍스트 포스트입니다.")
				.media(null)
				.build();

			// when
			PostCreateResponse response = postService.createPost(request, member);

			// then
			assertThat(response.getContent()).isEqualTo("텍스트 포스트입니다.");
			assertThat(response.getMedia()).isEmpty();
			assertThat(response.getAuthor().getBasicInfo().getHandle()).isEqualTo(member.getHandle());

			List<Post> posts = postRepository.findAll();
			assertThat(posts).hasSize(1);
		}

		@Test
		@DisplayName("성공 - 미디어를 포함한 포스트를 생성할 수 있다.")
		void shouldCreatePostWithMedia() {
			// given
			Member member = saveTestMember();
			String[] urls = {"media1.jpg", "media2.jpg"};

			PostCreateRequest request = PostCreateRequest.builder()
				.content("미디어 포함 포스트입니다.")
				.media(urls)
				.build();

			// when
			PostCreateResponse response = postService.createPost(request, member);

			// then
			List<String> savedUrls = Arrays.stream(response.getMedia()).map(MediaSimpleResponse::getMediaUrl).toList();
			assertThat(savedUrls).containsAll(Arrays.asList(urls));
			assertThat(response.getMedia()).hasSize(2);
			List<PostMedia> mappings = postMediaRepository.findAll();
			assertThat(mappings).hasSize(2);
		}
	}

	@Nested
	class GetPost {
		@DisplayName("실패 - 존재하지 않는 postId 로 조회한 경우 에러가 발생한다.")
		@Test
		void shouldFailWhenPostIdIsInvalid() {
			// given
			Long invalidId = -1L;

			// when // then
			assertThrows(PostNotFoundException.class, () -> {
				postService.getPost(invalidId, null);
			});
		}

		@DisplayName("성공 - 삭제된 포스트인 경우 삭제 응답을 반환한다.")
		@Test
		void shouldReturnDeletedResponseWhenPostIsDeleted() {
			// given
			Post post = Post.builder()
				.authorId(saveTestMember().getId())
				.content("this is deleted post")
				.parentPost(null)
				.quotedPost(null)
				.build();

			postRepository.save(post);
			post.softDelete();

			// when
			PostResponse deletedPost = postService.getPost(post.getId(), null);

			// then
			assertThat(deletedPost).isEqualTo(PostResponse.deletedPost(post.getId()));
		}

		@DisplayName("성공 - 정상적인 포스트를 조회하면 올바른 정보를 반환한다.")
		@Test
		void shouldReturnCorrectPostInfoWhenValidPostIdIsGiven() {
			// given
			Member author = saveTestMember();

			Post post = Post.builder()
				.authorId(author.getId())
				.content("테스트 포스트 내용입니다.")
				.build();

			postRepository.save(post);

			// when
			PostResponse response = postService.getPost(post.getId(), author);

			// then
			assertThat(response.getId()).isEqualTo(post.getId());
			assertThat(response.getContent()).isEqualTo("테스트 포스트 내용입니다.");
			assertThat(response.getAuthor().getHandle()).isEqualTo(author.getHandle());
			assertThat(response.isDeleted()).isFalse();
			assertThat(response.getCreatedAt()).isNotNull();
		}

		@DisplayName("성공 - 미디어가 있는 포스트를 조회하면 미디어 정보도 함께 반환한다.")
		@Test
		void shouldReturnPostWithMediaWhenPostHasMedia() {
			// given
			Member author = saveTestMember();

			Post post = Post.builder()
				.authorId(author.getId())
				.content("미디어 포함 포스트입니다.")
				.build();

			postRepository.save(post);

			// 미디어 생성 및 연결
			Media media1 = mediaRepository.save(createMedia("test1.jpg"));
			Media media2 = mediaRepository.save(createMedia("test2.jpg"));

			postMediaRepository.save(new PostMedia(post.getId(), media1.getId()));
			postMediaRepository.save(new PostMedia(post.getId(), media2.getId()));

			// when
			PostResponse response = postService.getPost(post.getId(), author);

			// then
			assertThat(response.getMediaEntities()).hasSize(2);
			assertThat(response.getMediaEntities()).extracting("path")
				.contains(
					"https://cdn.twooter.xyz/test1.jpg",
					"https://cdn.twooter.xyz/test2.jpg"
				);
		}

		@DisplayName("성공 - 좋아요와 리포스트 갯수를 조회할 수 있다.")
		@Test
		void shouldGetLikedCountAndRepostCount() {
			// given
			Member author = saveTestMember();

			Member member1 = saveTestMember("member1");
			Member member2 = saveTestMember("member2");
			Member member3 = saveTestMember("member3");

			Post post = Post.builder()
				.authorId(author.getId())
				.content("테스트 포스트 내용입니다.")
				.build();
			postRepository.save(post);

			// 리트윗 및 좋아요 갯수
			PostLike like1 = PostLike.builder()
				.postId(post.getId())
				.memberId(member1.getId())
				.build();

			PostLike like2 = PostLike.builder()
				.postId(post.getId())
				.memberId(member2.getId())
				.build();

			PostLike like3 = PostLike.builder()
				.postId(post.getId())
				.memberId(member3.getId())
				.build();

			postLikeRepository.saveAll(List.of(like1, like2, like3));

			Repost repost1 = Repost.builder()
				.postId(post.getId())
				.memberId(member1.getId())
				.build();

			Repost repost2 = Repost.builder()
				.postId(post.getId())
				.memberId(member2.getId())
				.build();

			Repost repost3 = Repost.builder()
				.postId(post.getId())
				.memberId(member3.getId())
				.build();

			repostRepository.saveAll(List.of(repost1, repost2, repost3));

			// when
			PostResponse response = postService.getPost(post.getId(), null);

			// then
			assertThat(response.getLikeCount()).isEqualTo(3);
			assertThat(response.getRepostCount()).isEqualTo(3);
		}

		@DisplayName("성공 - 유저가 로그인하지 않은 경우, 본인의 리트윗/좋아요 정보를 제외한다.")
		@Test
		void shouldFindPostDetailWhenUserDoesNotLogin() {
			// given
			Member author = saveTestMember();
			Post post = Post.builder()
				.authorId(author.getId())
				.content("테스트 포스트 내용입니다.")
				.build();
			postRepository.save(post);

			// when
			PostResponse response = postService.getPost(post.getId(), null);

			// then
			assertFalse(response.isLiked());
			assertFalse(response.isDeleted());

		}

		@DisplayName("성공 - 유저가 로그인한 경우, 본인의 리트윗/좋아요 정보를 포함한다.")
		@Test
		void shouldFindPostDetailWhenUserLogin() {
			// given
			Member author = saveTestMember();
			Member currentMember = saveTestMember("currentMember");

			Post post = Post.builder()
				.authorId(author.getId())
				.content("테스트 포스트 내용입니다.")
				.build();
			postRepository.save(post);

			PostLike like = PostLike.builder()
				.postId(post.getId())
				.memberId(currentMember.getId())
				.build();

			postLikeRepository.save(like);

			// when
			PostResponse response = postService.getPost(post.getId(), currentMember);

			// then
			assertTrue(response.isLiked());
			assertFalse(response.isReposted());
		}
	}

	// === 헬퍼 ===
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
}
