package xyz.twooter.post.application;

import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import xyz.twooter.common.error.BusinessException;
import xyz.twooter.common.error.ErrorCode;
import xyz.twooter.media.application.MediaService;
import xyz.twooter.media.presentation.dto.response.MediaSimpleResponse;
import xyz.twooter.member.application.MemberService;
import xyz.twooter.member.domain.Member;
import xyz.twooter.member.presentation.dto.response.MemberBasic;
import xyz.twooter.member.presentation.dto.response.MemberSummaryResponse;
import xyz.twooter.post.domain.Post;
import xyz.twooter.post.domain.PostMedia;
import xyz.twooter.post.domain.exception.DuplicateRepostException;
import xyz.twooter.post.domain.exception.PostNotFoundException;
import xyz.twooter.post.domain.repository.PostMediaRepository;
import xyz.twooter.post.domain.repository.PostRepository;
import xyz.twooter.post.domain.repository.projection.PostDetailProjection;
import xyz.twooter.post.presentation.dto.request.PostCreateRequest;
import xyz.twooter.post.presentation.dto.request.ReplyCreateRequest;
import xyz.twooter.post.presentation.dto.response.MediaEntity;
import xyz.twooter.post.presentation.dto.response.PostCreateResponse;
import xyz.twooter.post.presentation.dto.response.PostDeleteResponse;
import xyz.twooter.post.presentation.dto.response.PostReplyCreateResponse;
import xyz.twooter.post.presentation.dto.response.PostResponse;
import xyz.twooter.post.presentation.dto.response.RepostCreateResponse;

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
		List<MediaSimpleResponse> mediaResponses = processAndAttachMedia(request, post);
		return PostCreateResponse.of(post, authorSummary, mediaResponses);
	}

	@Transactional
	public PostReplyCreateResponse createReply(ReplyCreateRequest request, Member member) {
		Post parentPost = postRepository.findById(request.getParentId())
			.filter(p -> !p.isDeleted())
			.orElseThrow(PostNotFoundException::new);

		Post reply = Post.createReply(
			member.getId(),
			request.getContent(),
			parentPost.getId()
		);

		postRepository.save(reply);

		MemberSummaryResponse authorSummary = memberService.createMemberSummary(member);
		List<MediaSimpleResponse> mediaResponses = processAndAttachMedia(request, reply);

		return PostReplyCreateResponse.of(reply, authorSummary, mediaResponses, parentPost.getId());
	}

	@Transactional
	public RepostCreateResponse repostAndIncreaseCount(Long postId, Member member) {
		RepostCreateResponse response = repost(postId, member);
		increaseRepostCount(postId);
		return response;
	}

	public PostResponse getPost(Long postId, Member member) {

		Long memberId = member == null ? null : member.getId();
		PostDetailProjection projection = postRepository.findPostDetailById(postId, memberId)
			.orElseThrow(PostNotFoundException::new);

		if (Boolean.TRUE.equals(projection.getIsDeleted())) {
			return PostResponse.deletedPost(postId);
		}

		List<MediaEntity> mediaEntities = mediaService.getMediaByPostId(postId);

		return PostResponse.builder()
			.id(projection.getPostId())
			.author(
				MemberBasic.builder()
					.id(projection.getAuthorId())
					.handle(projection.getAuthorHandle())
					.nickname(projection.getAuthorNickname())
					.build()
			)
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

	public RepostCreateResponse repost(Long postId, Member member) {
		validateTargetPost(postId);
		checkDuplicateRepost(postId, member);

		Post originalPost = postRepository.findById(postId)
			.orElseThrow(PostNotFoundException::new);

		Post post = Post.createRepost(member.getId(), originalPost.getId());
		postRepository.save(post);

		return RepostCreateResponse.builder()
			.repostId(post.getId())
			.originalPostId(originalPost.getId())
			.repostedAt(post.getCreatedAt())
			.build();
	}

	@Transactional
	public PostDeleteResponse deletePost(Long postId, Member member) {
		Post post = postRepository.findById(postId)
			.filter(p -> !p.isDeleted())
			.orElseThrow(PostNotFoundException::new);

		if (!post.isAuthor(member)) {
			throw new BusinessException(ErrorCode.ACCESS_DENIED);
		}

		// repost 인 경우
		if (post.getRepostOfId() != null) {
			postRepository.decrementRepostCount(post.getRepostOfId());
		}

		post.softDelete();
		return PostDeleteResponse.builder()
			.postId(postId)
			.build();
	}

	private void increaseRepostCount(Long postId) {
		postRepository.incrementRepostCount(postId);
	}

	private List<MediaSimpleResponse> processAndAttachMedia(PostCreateRequest request, Post post) {
		String[] mediaArray = request.getMedia();
		List<String> mediaUrls = (mediaArray != null) ? Arrays.asList(mediaArray) : List.of();
		List<Long> mediaIds = mediaService.saveMedia(mediaUrls);
		savePostMediaMappings(post, mediaIds);
		return mediaService.getMediaListFromId(mediaIds);
	}

	private void checkDuplicateRepost(Long postId, Member member) {
		if (postRepository.existsByAuthorIdAndRepostOfId(member.getId(), postId)) {
			throw new DuplicateRepostException();
		}
	}

	private void validateTargetPost(Long postId) {
		if (!postRepository.existsById(postId) || postRepository.findIsDeletedById(postId)) {
			throw new PostNotFoundException();
		}
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
