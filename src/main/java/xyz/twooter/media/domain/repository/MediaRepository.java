package xyz.twooter.media.domain.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import xyz.twooter.media.domain.Media;
import xyz.twooter.media.presentation.dto.MediaWithPostId;

public interface MediaRepository extends JpaRepository<Media, Long> {
	List<Media> findAllByIdIn(List<Long> ids);

	@Query("SELECT m FROM Media m JOIN PostMedia pm ON m.id = pm.mediaId WHERE pm.postId = :postId")
	List<Media> findMediaByPostId(Long postId);

    @Query("SELECT new xyz.twooter.media.presentation.dto.MediaWithPostId(m.id, m.path, pm.postId) " +
		"FROM Media m JOIN PostMedia pm ON m.id = pm.mediaId " +
		"WHERE pm.postId IN :postIds " +
		"ORDER BY pm.postId, m.id")
    List<MediaWithPostId> findMediaWithPostIdsByPostIds(@Param("postIds") List<Long> postIds);
}
