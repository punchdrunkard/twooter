package xyz.twooter.post.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import xyz.twooter.post.domain.Post;

public interface PostRepository extends JpaRepository<Post, Long> {


}
