package xyz.twooter.post.application;

import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import xyz.twooter.media.application.MediaService;
import xyz.twooter.media.presentation.dto.response.MediaSimpleResponse;
import xyz.twooter.member.application.MemberService;
import xyz.twooter.member.domain.Member;
import xyz.twooter.member.presentation.dto.response.MemberBasic;
import xyz.twooter.member.presentation.dto.response.MemberSummaryResponse;
import xyz.twooter.post.domain.Post;
import xyz.twooter.post.domain.PostMedia;
import xyz.twooter.post.domain.exception.PostNotFoundException;
import xyz.twooter.post.domain.repository.PostMediaRepository;
import xyz.twooter.post.domain.repository.PostRepository;
import xyz.twooter.post.presentation.dto.projection.PostDetailProjection;
import xyz.twooter.post.presentation.dto.request.PostCreateRequest;
import xyz.twooter.post.presentation.dto.response.MediaEntity;
import xyz.twooter.post.presentation.dto.response.PostCreateResponse;
import xyz.twooter.post.presentation.dto.response.PostResponse;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PostService {

	private final PostRepository postRepository;
	private final PostMediaRepository postMediaRepository;

	private final MemberService memberService;
	private final MediaService mediaService;

	@Transactional
	public PostCreateResponse createPost(PostCreateRequest request, Member member) {
		Post post = createAndSavePost(request, member);
		MemberSummaryResponse authorSummary = memberService.createMemberSummary(member);

		String[] mediaArray = request.getMedia();
		List<String> mediaUrls = (mediaArray != null) ?
			Arrays.asList(mediaArray) : List.of();

		List<Long> mediaIds = mediaService.saveMedia(mediaUrls);
		savePostMediaMappings(post, mediaIds);

		List<MediaSimpleResponse> mediaResponses = mediaService.getMediaListFromId(mediaIds);
		return PostCreateResponse.of(post, authorSummary, mediaResponses);
	}

	public PostResponse getPost(Long postId, Member member) {

		Long memberId = member == null ? null : member.getId();
		PostDetailProjection projection = postRepository.findPostDetailById(postId, memberId)
			.orElseThrow(PostNotFoundException::new);

		if (projection.getIsDeleted()) {
			return PostResponse.deletedPost(postId);
		}

		List<MediaEntity> mediaEntities = mediaService.getMediaByPostId(postId);

		return PostResponse.builder()
			.id(projection.getId())
			.author(new MemberBasic(projection.getHandle(), projection.getNickname(),
				projection.getAvatarPath()))
			.content(projection.getContent())
			.likeCount(projection.getLikeCount())
			.isLiked(projection.getIsLiked())
			.repostCount(projection.getRepostCount())
			.isReposted(projection.getIsReposted())
			.viewCount(projection.getViewCount())
			.mediaEntities(mediaEntities)
			.createdAt(projection.getCreatedAt())
			.isDeleted(false)
			.build();
	}

	@Transactional
	public void incrementViewCount(Long postId) {
		postRepository.incrementViewCount(postId);
	}

	private Post createAndSavePost(PostCreateRequest request, Member member) {
		Post post = Post.builder()
			.authorId(member.getId())
			.content(request.getContent())
			.build();
		return postRepository.save(post);
	}

	private void savePostMediaMappings(Post post, List<Long> mediaIds) {
		if (mediaIds.isEmpty()) {
			return;
		}

		List<PostMedia> mappings = mediaIds.stream()
			.map(mediaId -> new PostMedia(post.getId(), mediaId))
			.toList();
		postMediaRepository.saveAll(mappings);
	}
}
