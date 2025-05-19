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
	private final PostLikeService postLikeService;
	private final RepostService repostService;

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
		Post post = postRepository.findById(postId).orElseThrow(PostNotFoundException::new);

		if (post.isDeleted()) {
			return PostResponse.deletedPost(postId);
		}

		MemberBasic memberBasicInfo = memberService.getMemberBasic(post.getAuthorId());
		List<MediaEntity> mediaEntities = mediaService.getMediaByPostId(postId);

		return PostResponse.builder()
			.id(post.getId())
			.author(memberBasicInfo)
			.content(post.getContent())
			.likeCount(postLikeService.getLikeCount(postId))
			.isLiked(postLikeService.isLikedByMember(postId, member.getId()))
			.repostCount(repostService.getRepostCount(postId))
			.isReposted(repostService.isRepostedByMember(postId, member.getId()))
			.mediaEntities(mediaEntities)
			.createdAt(post.getCreatedAt())
			.isDeleted(false)
			.build();
	}

	private Post createAndSavePost(PostCreateRequest request, Member member) {
		Post post = Post.builder()
			.authorId(member.getId())
			.content(request.getContent())
			.build();
		return postRepository.save(post);
	}

	private void savePostMediaMappings(Post post, List<Long> mediaIds) {
		if (mediaIds.isEmpty())
			return;

		// 수정이 필요한 부분
		List<PostMedia> mappings = mediaIds.stream()
			.map(mediaId -> new PostMedia(post.getId(), mediaId))
			.toList();
		postMediaRepository.saveAll(mappings);
	}

}
