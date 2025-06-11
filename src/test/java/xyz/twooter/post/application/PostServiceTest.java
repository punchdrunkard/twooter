package xyz.twooter.post.application;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import jakarta.persistence.EntityManager;
import xyz.twooter.common.error.BusinessException;
import xyz.twooter.common.error.ErrorCode;
import xyz.twooter.media.domain.Media;
import xyz.twooter.media.domain.repository.MediaRepository;
import xyz.twooter.media.presentation.dto.response.MediaSimpleResponse;
import xyz.twooter.member.domain.Member;
import xyz.twooter.member.domain.repository.MemberRepository;
import xyz.twooter.post.domain.Post;
import xyz.twooter.post.domain.PostLike;
import xyz.twooter.post.domain.PostMedia;
import xyz.twooter.post.domain.exception.DuplicateRepostException;
import xyz.twooter.post.domain.exception.PostNotFoundException;
import xyz.twooter.post.domain.repository.PostLikeRepository;
import xyz.twooter.post.domain.repository.PostMediaRepository;
import xyz.twooter.post.domain.repository.PostRepository;
import xyz.twooter.post.presentation.dto.request.PostCreateRequest;
import xyz.twooter.post.presentation.dto.request.ReplyCreateRequest;
import xyz.twooter.post.presentation.dto.response.PostCreateResponse;
import xyz.twooter.post.presentation.dto.response.PostDeleteResponse;
import xyz.twooter.post.presentation.dto.response.PostReplyCreateResponse;
import xyz.twooter.post.presentation.dto.response.PostResponse;
import xyz.twooter.post.presentation.dto.response.PostThreadResponse;
import xyz.twooter.post.presentation.dto.response.RepostCreateResponse;
import xyz.twooter.support.IntegrationTestSupport;

class PostServiceTest extends IntegrationTestSupport {

	@Autowired
	private EntityManager entityManager;

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
			Member author = saveTestMember();
			Post post = Post.createPost(author.getId(), "this is deleted post");
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
			String postContent = "테스트 포스트 내용입니다.";
			Post post = Post.createPost(author.getId(), postContent);
			postRepository.save(post);

			// when
			PostResponse response = postService.getPost(post.getId(), author);

			// then
			assertThat(response.getId()).isEqualTo(post.getId());
			assertThat(response.getContent()).isEqualTo(postContent);
			assertThat(response.getAuthor().getHandle()).isEqualTo(author.getHandle());
			assertThat(response.isDeleted()).isFalse();
			assertThat(response.getCreatedAt()).isNotNull();
		}

		@DisplayName("성공 - 미디어가 있는 포스트를 조회하면 미디어 정보도 함께 반환한다.")
		@Test
		void shouldReturnPostWithMediaWhenPostHasMedia() {
			// given
			Member author = saveTestMember();
			Post post = Post.createPost(author.getId(), "미디어 포함 포스트입니다.");
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
			assertThat(response.getMediaEntities()).extracting("mediaUrl")
				.contains(
					"https://cdn.twooter.xyz/test1.jpg",
					"https://cdn.twooter.xyz/test2.jpg"
				);
		}

		@DisplayName("성공 - 유저가 로그인하지 않은 경우, 본인의 리트윗/좋아요 정보를 제외한다.")
		@Test
		void shouldFindPostDetailWhenUserDoesNotLogin() {
			// given
			Member author = saveTestMember();
			Post post = Post.createPost(author.getId(), "테스트 포스트 내용입니다.");
			postRepository.save(post);

			// when
			PostResponse response = postService.getPost(post.getId(), null);

			// then
			assertFalse(response.isLiked());
			assertFalse(response.isDeleted());

		}

		@DisplayName("성공 - 유저가 로그인한 경우, 본인의 리포스트/좋아요 정보를 포함한다.")
		@Test
		void shouldFindPostDetailWhenUserLogin() {
			// given
			Member author = saveTestMember();
			Member currentMember = saveTestMember("currentMember");

			Post post = Post.createPost(author.getId(), "테스트 포스트 내용입니다.");
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

	@Nested
	class Repost {

		@DisplayName("성공")
		@Test
		void shouldReturnOriginalPostIdAndRepostIdWhenRepost() {
			// given
			Member author = saveTestMember();
			Post originalPost = Post.createPost(author.getId(), "원본 포스트입니다.");
			postRepository.save(originalPost);
			Member currentMember = saveTestMember("currentMember");

			// when
			RepostCreateResponse response = postService.repost(originalPost.getId(), currentMember);

			// then
			assertThat(response.getOriginalPostId()).isEqualTo(originalPost.getId());
			assertThat(response.getRepostId()).isNotNull();
		}

		@DisplayName("실패 - 존재하지 않는 포스트 ID로 리포스트 시도")
		@Test
		void shouldThrowErrorWhenRepostNotExistPost() {
			// given
			Long invalidPostId = -1L;
			Member currentMember = saveTestMember("currentMember");

			// when & then
			assertThrows(PostNotFoundException.class, () -> {
				postService.repost(invalidPostId, currentMember);
			});
		}

		@DisplayName("실패 - 삭제된 포스트 ID로 리포스트 시도")
		@Test
		void shouldThrowErrorTargetPostIsDeleted() {
			// given
			Member currentMember = saveTestMember("currentMember");
			Member author = saveTestMember("author");
			Post originalPost = Post.createPost(author.getId(), "원본 포스트입니다.");
			postRepository.save(originalPost);
			originalPost.softDelete();

			// when & then
			assertThrows(PostNotFoundException.class, () -> {
				postService.repost(originalPost.getId(), currentMember);
			});
		}

		@DisplayName("실패 - 중복 리포스트 시도")
		@Test
		void shouldThrowErrorWhenTryDuplicateRepost() {
			// given (이미 리포스트가 존재할 때)
			Member author = saveTestMember("author");
			Post originalPost = Post.createPost(author.getId(), "원본 포스트입니다.");
			postRepository.save(originalPost);

			Member currentMember = saveTestMember("currentMember");
			Post repost = Post.createRepost(currentMember.getId(), originalPost.getId());
			postRepository.save(repost);

			// when & then
			assertThrows(DuplicateRepostException.class, () -> {
				postService.repost(originalPost.getId(), currentMember);
			});
		}
	}

	@Nested
	class DeletePost {

		@DisplayName("성공 - 본인이 작성한 포스트를 삭제할 수 있다.")
		@Test
		void shouldDeletePostWhenAuthorDeletes() {
			// given
			Member author = saveTestMember();
			Post post = Post.createPost(author.getId(), "삭제할 포스트입니다.");
			postRepository.save(post);

			// when
			PostDeleteResponse response = postService.deletePost(post.getId(), author);

			// then
			assertThat(response.getPostId()).isEqualTo(post.getId());

			// DB 상태 검증
			Optional<Post> deletedPost = postRepository.findById(post.getId());
			assertThat(deletedPost).isPresent();
			assertThat(deletedPost.get().isDeleted()).isTrue();
		}

		@DisplayName("실패 - 다른 사람이 작성한 포스트를 삭제하려고 할 때 에러가 발생한다.")
		@Test
		void shouldThrowErrorWhenNonAuthorTriesToDelete() {
			// given
			Member author = saveTestMember("author");
			Post post = Post.createPost(author.getId(), "다른 사람이 작성한 포스트입니다.");
			postRepository.save(post);

			Member nonAuthor = saveTestMember("nonAuthor");

			// when & then
			assertThatThrownBy(() -> postService.deletePost(post.getId(), nonAuthor))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.ACCESS_DENIED);

			Optional<Post> unchangedPost = postRepository.findById(post.getId());
			assertThat(unchangedPost).isPresent();
			assertThat(unchangedPost.get().isDeleted()).isFalse();
		}

		@DisplayName("실패 - 포스트가 존재하지 않을 때 에러가 발생한다.")
		@Test
		void shouldThrowErrorWhenThePostDoesNotExist() {
			// given
			Long invalidPostId = -1L;
			Member author = saveTestMember();

			// when &  then
			assertThrows(PostNotFoundException.class, () -> {
				postService.deletePost(invalidPostId, author);
			});
		}

		@DisplayName("성공 - 리포스트인 경우, 원본 포스트의 리포스트 수를 감소시킨다.")
		@Test
		void shouldDecreaseOriginalRepostCountWhenRepostIsDeleted() {
			// given
			Member author = saveTestMember("author");
			Post originalPost = Post.createPost(author.getId(), "원본 포스트입니다.");
			postRepository.save(originalPost);

			Post repost = Post.createRepost(author.getId(), originalPost.getId());
			postRepository.save(repost);

			postRepository.incrementRepostCount(originalPost.getId());
			entityManager.flush();
			entityManager.clear();

			Post afterIncrement = postRepository.findById(originalPost.getId()).orElseThrow();

			// when
			PostDeleteResponse response = postService.deletePost(repost.getId(), author);
			entityManager.flush();
			entityManager.clear();

			// then
			Post afterDelete = postRepository.findById(originalPost.getId()).orElseThrow();
			assertThat(response.getPostId()).isEqualTo(repost.getId());
			assertThat(afterDelete.getRepostCount()).isEqualTo(afterIncrement.getRepostCount() - 1);
		}

		@DisplayName("실패 - 삭제된 포스트인 경우, 에러가 발생한다.")
		@Test
		void shouldThrowErrorWhenThePostIsAlreadyDeleted() {
			// given
			Member member = saveTestMember();
			Post post = Post.createPost(member.getId(), "삭제된 포스트입니다.");
			postRepository.save(post);
			post.softDelete();

			// when & then
			assertThrows(PostNotFoundException.class, () -> {
				postService.deletePost(post.getId(), member);
			});
		}
	}

	@Nested
	class CreateReply {
		@DisplayName("성공")
		@Test
		void shouldCreateReplyWhenTheRequestIsValid() {
			// given
			Member author = saveTestMember("author");
			Post parentPost = Post.createPost(author.getId(), "부모 포스트입니다.");
			postRepository.save(parentPost);
			Member currentMember = saveTestMember("currentMember");
			String replyContent = "답글 내용입니다.";
			String[] mediaUrls = {"reply1.jpg", "reply2.jpg"};
			ReplyCreateRequest request = ReplyCreateRequest.builder()
				.content(replyContent)
				.media(mediaUrls)
				.parentId(parentPost.getId())
				.build();

			// when
			PostReplyCreateResponse response = postService.createReply(request, currentMember);

			// then
			assertThat(response.getContent()).isEqualTo(replyContent);
			assertThat(response.getParentId()).isEqualTo(parentPost.getId());
			assertThat(response.getAuthor().getBasicInfo().getHandle()).isEqualTo(currentMember.getHandle());
			assertThat(response.getMedia()).hasSize(2);
		}

		@DisplayName("실패 - 부모의 ID가 존재하지 않는 경우")
		@Test
		void shouldThrowErrorWhenTheParentPostDoesNotExists() {
			// given
			Member currentMember = saveTestMember("currentMember");
			Long nonExistentParentId = -1L;
			String replyContent = "답글 내용입니다.";
			ReplyCreateRequest request = ReplyCreateRequest.builder()
				.content(replyContent)
				.media(null)
				.parentId(nonExistentParentId)
				.build();

			// when & then
			assertThrows(PostNotFoundException.class, () -> {
				postService.createReply(request, currentMember);
			});
		}

		@DisplayName("실패 - 삭제된 부모 포스트에 답글을 작성하려는 경우")
		@Test
		void shouldThrowErrorWhenTheParentPostIsDeleted() {
			// given
			Member author = saveTestMember("author");
			Post parentPost = Post.createPost(author.getId(), "부모 포스트입니다.");
			postRepository.save(parentPost);
			parentPost.softDelete();

			Member currentMember = saveTestMember("currentMember");
			String replyContent = "답글 내용입니다.";
			ReplyCreateRequest request = ReplyCreateRequest.builder()
				.content(replyContent)
				.media(null)
				.parentId(parentPost.getId())
				.build();

			// when & then
			assertThrows(PostNotFoundException.class, () -> {
				postService.createReply(request, currentMember);
			});
		}
	}

	@Nested
	class GetReplies {

		@DisplayName("성공 - 부모 포스트에 대한 답글을 조회할 수 있다.")
		@Test
		void shouldReturnRepliesForParentPost() {
			// given
			Member author = saveTestMember("author");
			Post parentPost = Post.createPost(author.getId(), "부모 포스���입니다.");
			postRepository.save(parentPost);

			Member currentMember = saveTestMember("currentMember");
			Post reply1 = Post.createReply(currentMember.getId(), "답글 내용입니다.", parentPost.getId());
			Post reply2 = Post.createReply(author.getId(), "두 번째 답글입니다.", parentPost.getId());

			postRepository.save(reply1);
			postRepository.save(reply2);

			// when
			PostThreadResponse response = postService.getReplies(parentPost.getId(), currentMember, null, 10);
			List<PostResponse> replies = response.getPosts();

			// then
			assertThat(replies).hasSize(2);
			assertThat(replies.get(0).getId()).isEqualTo(reply1.getId());
			assertThat(replies.get(0).getAuthor().getId()).isEqualTo(currentMember.getId());
			assertThat(replies.get(1).getId()).isEqualTo(reply2.getId());
			assertThat(replies.get(1).getAuthor().getId()).isEqualTo(author.getId());
		}

		@DisplayName("성공 - 미디어가 있는 답글들을 올바르게 조회한다.")
		@Test
		void shouldGetReplyWithMediaWHeeRepliesHaveMedia() {
			// given
			Member author = saveTestMember("author");
			Post parentPost = Post.createPost(author.getId(), "부모 포스트입니다.");
			postRepository.save(parentPost);

			Member currentMember = saveTestMember("currentMember");
			Post reply1 = Post.createReply(currentMember.getId(), "답글 내용입니다.", parentPost.getId());
			Post reply2 = Post.createReply(author.getId(), "두 번째 답글입니다.", parentPost.getId());
			postRepository.save(reply1);
			postRepository.save(reply2);

			// 미디어 생성 및 연결
			Media media1 = mediaRepository.save(createMedia("reply1.jpg"));
			Media media2 = mediaRepository.save(createMedia("reply2.jpg"));
			Media media22 = mediaRepository.save(createMedia("reply2.jpg"));

			postMediaRepository.save(new PostMedia(reply1.getId(), media1.getId()));
			postMediaRepository.save(new PostMedia(reply2.getId(), media2.getId()));
			postMediaRepository.save(new PostMedia(reply2.getId(), media22.getId()));

			// when
			PostThreadResponse response = postService.getReplies(parentPost.getId(), currentMember, null, 10);
			List<PostResponse> replies = response.getPosts();

			// then
			assertThat(replies).hasSize(2);
			assertThat(replies.get(0).getMediaEntities().get(0).getMediaId())
				.isEqualTo(media1.getId());
			assertThat(replies.get(1).getMediaEntities()).hasSize(2);
		}

		@DisplayName("성공 - 답글 중 삭제된 답글이 있는 경우, 삭제 응답을 반환한다.")
		@Test
		void shouldGetDeletedContentWithRepliesHasDeletedPost() {
			// given
			Member author = saveTestMember("author");
			Post parentPost = Post.createPost(author.getId(), "부모 포스트입니다.");
			postRepository.save(parentPost);

			Member currentMember = saveTestMember("currentMember");
			Post reply1 = Post.createReply(currentMember.getId(), "답글 내용입니다.", parentPost.getId());
			Post reply2 = Post.createReply(author.getId(), "두 번째 답글입니다.", parentPost.getId());
			postRepository.save(reply1);
			postRepository.save(reply2);
			reply2.softDelete(); // 두 번째 답글 삭제

			// when
			PostThreadResponse response = postService.getReplies(parentPost.getId(), currentMember, null, 10);
			List<PostResponse> replies = response.getPosts();

			// then
			assertThat(replies).hasSize(2);
			assertThat(replies.get(0).getId()).isEqualTo(reply1.getId());
			assertThat(replies.get(1)).isEqualTo(PostResponse.deletedPost(reply2.getId(), reply2.getCreatedAt()));
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
