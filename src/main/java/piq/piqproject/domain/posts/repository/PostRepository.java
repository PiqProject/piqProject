package piq.piqproject.domain.posts.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import piq.piqproject.domain.posts.entity.PostEntity;

public interface PostRepository extends JpaRepository<PostEntity, Long> {
}
