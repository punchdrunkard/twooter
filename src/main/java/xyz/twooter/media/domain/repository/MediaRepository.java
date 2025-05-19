package xyz.twooter.media.domain.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import xyz.twooter.media.domain.Media;

public interface MediaRepository extends JpaRepository<Media, Long> {
	List<Media> findAllByIdIn(List<Long> ids);

	@Query("SELECT m FROM Media m JOIN PostMedia pm ON m.id = pm.mediaId WHERE pm.postId = :postId")
	List<Media> findMediaByPostId(Long postId);
}
