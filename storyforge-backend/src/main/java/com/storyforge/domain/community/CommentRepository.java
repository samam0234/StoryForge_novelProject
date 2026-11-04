package com.storyforge.domain.community;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    @EntityGraph(attributePaths = "author")
    List<Comment> findByWorkIdOrderByCreatedAt(Long workId);

    @EntityGraph(attributePaths = "author")
    List<Comment> findByPostIdOrderByCreatedAt(Long postId);

    @EntityGraph(attributePaths = "author")
    @Query("select c from Comment c where c.id = :id")
    Optional<Comment> findWithAuthor(@Param("id") Long id);

    int countByWorkIdAndHiddenAtIsNull(Long workId);

    int countByPostIdAndHiddenAtIsNull(Long postId);

    long countByAuthorId(Long authorId);
}
