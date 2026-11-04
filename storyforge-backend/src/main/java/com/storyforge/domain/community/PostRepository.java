package com.storyforge.domain.community;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Long> {

    @EntityGraph(attributePaths = "author")
    @Query("""
            select p from Post p
            where p.category in :categories
              and p.hiddenAt is null
              and p.author.id not in (select b.blockedId from com.storyforge.domain.moderation.UserBlock b where b.blockerId = :viewer)
              and (:like = '' or lower(p.title) like :like escape '\\' or lower(p.content) like :like escape '\\')
            """)
    Page<Post> search(@Param("categories") Collection<Post.Category> categories, @Param("like") String like,
                      @Param("viewer") Long viewer, Pageable pageable);

    @EntityGraph(attributePaths = "author")
    @Query("select p from Post p where p.id = :id")
    Optional<Post> findWithAuthor(@Param("id") Long id);

    long countByAuthorId(Long authorId);

    @Modifying
    @Query("update Post p set p.likeCount = p.likeCount + :delta where p.id = :id")
    void addLikes(@Param("id") Long id, @Param("delta") int delta);

    @Modifying
    @Query("update Post p set p.viewCount = p.viewCount + 1 where p.id = :id")
    void addView(@Param("id") Long id);

    @Modifying
    @Query("update Post p set p.commentCount = :count where p.id = :id")
    void setCommentCount(@Param("id") Long id, @Param("count") int count);

    @Query("select p.likeCount from Post p where p.id = :id")
    int likeCount(@Param("id") Long id);
}
