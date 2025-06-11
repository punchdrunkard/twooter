package xyz.twooter.post.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import xyz.twooter.member.domain.Member;
import xyz.twooter.post.domain.PostLike;
import xyz.twooter.post.domain.exception.PostNotFoundException;
import xyz.twooter.post.domain.repository.PostLikeRepository;
import xyz.twooter.post.domain.repository.PostRepository;
import xyz.twooter.post.presentation.dto.response.PostLikeResponse;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PostLikeService {

	private final PostLikeRepository postLikeRepository;
	private final PostRepository postRepository;

	public long getLikeCount(Long postId) {
		return postLikeRepository.countByPostId(postId);
	}

	public boolean isLikedByMember(Long postId, Long memberId) {
		return postLikeRepository.existsByPostIdAndMemberId(postId, memberId);
	}

	@Transactional
	public PostLikeResponse toggleLikeAndCount(Long postId, Member member) {
		validateTargetPost(postId);

		boolean isNowLiked = postLikeRepository.existsByPostIdAndMemberId(postId, member.getId())
			? deleteLike(postId, member)
			: saveLike(postId, member);

		if (isNowLiked) {
			postLikeRepository.incrementLikeCount(postId);
		} else {
			postLikeRepository.decrementLikeCount(postId);
		}

		return PostLikeResponse.builder()
			.postId(postId)
			.isLiked(isNowLiked)
			.build();
	}

	private boolean deleteLike(Long postId, Member member) {
		postLikeRepository.deleteByPostIdAndMemberId(postId, member.getId());
		return false;
	}

	private boolean saveLike(Long postId, Member member) {
		postLikeRepository.save(
			PostLike.builder()
				.memberId(member.getId())
				.postId(postId)
				.build());
		return true;
	}

	@Transactional
	public void decreaseLikeCount(Long postId) {
		postLikeRepository.decrementLikeCount(postId);
	}

	@Transactional
	public void increaseLikeCount(Long postId) {
		postLikeRepository.incrementLikeCount(postId);
	}

	private void validateTargetPost(Long postId) {
		if (!postRepository.existsById(postId) || postRepository.findIsDeletedById(postId)) {
			throw new PostNotFoundException();
		}
	}
}
