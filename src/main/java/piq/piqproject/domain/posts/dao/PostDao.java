package piq.piqproject.domain.posts.dao;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import piq.piqproject.domain.posts.entity.PostEntity;
import piq.piqproject.domain.posts.repository.PostRepository;

@Repository
@RequiredArgsConstructor
public class PostDao {

    private final PostRepository postRepository;

    public void savePost(PostEntity post) {
        postRepository.save(post);
    }
}
