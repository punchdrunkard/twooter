package xyz.twooter.post.application;

import static org.assertj.core.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import xyz.twooter.media.domain.Media;
import xyz.twooter.media.domain.repository.MediaRepository;
import xyz.twooter.member.domain.Member;
import xyz.twooter.member.domain.MemberProfile;
import xyz.twooter.member.domain.repository.MemberProfileRepository;
import xyz.twooter.member.domain.repository.MemberRepository;
import xyz.twooter.post.domain.Post;
import xyz.twooter.post.domain.PostMedia;
import xyz.twooter.post.domain.repository.PostMediaRepository;
import xyz.twooter.post.domain.repository.PostRepository;
import xyz.twooter.post.presentation.dto.request.PostCreateRequest;
import xyz.twooter.post.presentation.dto.response.PostCreateResponse;
import xyz.twooter.support.IntegrationTestSupport;
class PostServiceTest extends IntegrationTestSupport {

	@Autowired
	private PostService postService;
	@Autowired
	private MemberRepository memberRepository;
	@Autowired
	private MemberProfileRepository memberProfileRepository;
	@Autowired
	private PostRepository postRepository;
	@Autowired
	private PostMediaRepository postMediaRepository;
	@Autowired
	private MediaRepository mediaRepository;

	@Nested
	class CreatePost {

		@Test
		@DisplayName("미디어 없이 포스트를 생성할 수 있다.")
		void createPost_shouldCreatePostWithoutMedia() {
			// given
			Member member = saveTestMember();
			saveTestProfile(member);

			PostCreateRequest request = PostCreateRequest.builder()
				.content("텍스트 포스트입니다.")
				.media(null)
				.build();

			// when
			PostCreateResponse response = postService.createPost(request, member);

			// then
			assertThat(response.getContent()).isEqualTo("텍스트 포스트입니다.");
			assertThat(response.getMedia()).isEmpty();
			assertThat(response.getAuthor().getHandle()).isEqualTo(member.getHandle());

			List<Post> posts = postRepository.findAll();
			assertThat(posts).hasSize(1);
		}

		@Test
		@DisplayName("미디어를 포함한 포스트를 생성할 수 있다.")
		void createPost_shouldCreatePostWithMedia() {
			// given
			Member member = saveTestMember();
			saveTestProfile(member);

			Media media1 = mediaRepository.save(createMedia("media1.jpg"));
			Media media2 = mediaRepository.save(createMedia("media2.jpg"));

			PostCreateRequest request = PostCreateRequest.builder()
				.content("미디어 포함 포스트입니다.")
				.media(new Long[] {media1.getId(), media2.getId()})
				.build();

			// when
			PostCreateResponse response = postService.createPost(request, member);

			// then
			assertThat(response.getMedia()).hasSize(2);
			assertThat(response.getMedia()[0].getMediaId()).isEqualTo(media1.getId());
			assertThat(response.getMedia()[1].getMediaId()).isEqualTo(media2.getId());

			List<PostMedia> mappings = postMediaRepository.findAll();
			assertThat(mappings).hasSize(2);
		}
	}

	// === 헬퍼 ===

	private Member saveTestMember() {
		Member member = Member.builder()
			.email("test@twooter.com")
			.handle("tester")
			.build();
		return memberRepository.save(member);
	}

	private void saveTestProfile(Member member) {
		MemberProfile profile = MemberProfile.builder()
			.member(member)
			.bio("소개입니다.")
			.nickname("테스터")
			.avatarPath("avatar path")
			.build();
		memberProfileRepository.save(profile);
	}

	private Media createMedia(String path) {
		return Media.builder()
			.path("https://cdn.twooter.xyz/" + path)
			.build();
	}
}
