package xyz.twooter.post.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import xyz.twooter.post.domain.PostMedia;

public interface PostMediaRepository extends JpaRepository<PostMedia, Long> {
}
