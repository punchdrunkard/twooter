package xyz.twooter.post.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import xyz.twooter.media.application.MediaService;
import xyz.twooter.member.application.MemberService;
import xyz.twooter.member.domain.Member;
import xyz.twooter.member.presentation.dto.response.MemberBasic;
import xyz.twooter.post.domain.Post;
import xyz.twooter.post.domain.repository.PostRepository;
import xyz.twooter.post.presentation.dto.response.MediaEntity;
import xyz.twooter.post.presentation.dto.response.PostResponse;
import xyz.twooter.support.MockTestSupport;

;

class PostServiceMockTest extends MockTestSupport {

	@Mock
	private PostRepository postRepository;

	@Mock
	private MemberService memberService;

	@Mock
	private MediaService mediaService;

	@Mock
	private PostLikeService postLikeService;

	@Mock
	private RepostService repostService;

	@InjectMocks
	private PostService postService;

	@Nested
	class GetPost {
		@Test
		@DisplayName("성공 - 좋아요와 리포스트 정보를 올바르게 포함한다")
		void shouldIncludeLikeAndRepostInfo() {
			// given
			Long postId = 1L;
			Long authorId = 10L;
			Long userId = 20L;

			Member mockMember = mock(Member.class);
			when(mockMember.getId()).thenReturn(userId);

			Post post = mock(Post.class);
			when(post.getId()).thenReturn(postId);
			when(post.getAuthorId()).thenReturn(authorId);
			when(post.isDeleted()).thenReturn(false);

			MemberBasic memberBasic = mock(MemberBasic.class);
			List<MediaEntity> mediaEntities = Collections.emptyList();

			// 좋아요와 리포스트 정보 모킹
			when(postLikeService.getLikeCount(postId)).thenReturn(15L);
			when(postLikeService.isLikedByMember(postId, userId)).thenReturn(true);
			when(repostService.getRepostCount(postId)).thenReturn(7L);
			when(repostService.isRepostedByMember(postId, userId)).thenReturn(false);

			when(postRepository.findById(postId)).thenReturn(Optional.of(post));
			when(memberService.getMemberBasic(authorId)).thenReturn(memberBasic);
			when(mediaService.getMediaByPostId(postId)).thenReturn(mediaEntities);

			// when
			PostResponse response = postService.getPost(postId, mockMember);

			// then
			assertThat(response.getLikeCount()).isEqualTo(15L);
			assertThat(response.isLiked()).isTrue();
			assertThat(response.getRepostCount()).isEqualTo(7L);
			assertThat(response.isReposted()).isFalse();

			// verify
			verify(postLikeService).getLikeCount(postId);
			verify(postLikeService).isLikedByMember(postId, userId);
			verify(repostService).getRepostCount(postId);
			verify(repostService).isRepostedByMember(postId, userId);
		}
	}
}
