package xyz.twooter.media.domain.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import xyz.twooter.media.domain.Media;

public interface MediaRepository extends JpaRepository<Media, Long> {
	List<Media> findAllByIdIn(List<Long> ids);
}
