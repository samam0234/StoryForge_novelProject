package com.storyforge.domain.community;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface WorkRepository extends JpaRepository<Work, Long> {

    @Query(value = """
            select w from Work w join fetch w.author
            where w.visibility = com.storyforge.domain.community.Work$Visibility.PUBLIC
              and w.kind = :kind
              and (:genre = '' or w.genre = :genre)
              and (:like = '' or lower(w.title) like :like escape '\\'
                   or lower(coalesce(w.synopsis, '')) like :like escape '\\'
                   or lower(coalesce(w.tags, '')) like :like escape '\\')
            """,
            countQuery = """
            select count(w) from Work w
            where w.visibility = com.storyforge.domain.community.Work$Visibility.PUBLIC
              and w.kind = :kind
              and (:genre = '' or w.genre = :genre)
              and (:like = '' or lower(w.title) like :like escape '\\'
                   or lower(coalesce(w.synopsis, '')) like :like escape '\\'
                   or lower(coalesce(w.tags, '')) like :like escape '\\')
            """)
    Page<Work> board(@Param("kind") Work.Kind kind, @Param("genre") String genre, @Param("like") String like,
                     Pageable pageable);

    @EntityGraph(attributePaths = "author")
    List<Work> findByAuthorIdOrderByUpdatedAtDesc(Long authorId);

    @EntityGraph(attributePaths = "author")
    List<Work> findByAuthorIdAndVisibilityOrderByPublishedAtDesc(Long authorId, Work.Visibility visibility);

    @EntityGraph(attributePaths = "author")
    @Query("select w from Work w where w.id = :id")
    Optional<Work> findWithAuthor(@Param("id") Long id);

    @EntityGraph(attributePaths = "author")
    @Query("select w from Work w where w.id in :ids")
    List<Work> findAllWithAuthor(@Param("ids") Collection<Long> ids);

    @EntityGraph(attributePaths = "author")
    List<Work> findByParentWorkIdIn(Collection<Long> parentIds);

    Optional<Work> findByProjectId(Long projectId);

    @Query("select w.id from Work w where w.projectId = :projectId")
    Optional<Long> findIdByProjectId(@Param("projectId") Long projectId);

    @EntityGraph(attributePaths = "author")
    List<Work> findTop12ByVisibilityOrderByPublishedAtDesc(Work.Visibility visibility);

    @Query("select coalesce(sum(w.likeCount), 0) from Work w where w.author.id = :authorId")
    long sumLikes(@Param("authorId") Long authorId);

    @Query("select coalesce(sum(w.cloneCount), 0) from Work w where w.author.id = :authorId")
    long sumClones(@Param("authorId") Long authorId);

    long countByAuthorIdAndKind(Long authorId, Work.Kind kind);

    long countByAuthorIdAndVisibility(Long authorId, Work.Visibility visibility);

    @Modifying
    @Query("update Work w set w.likeCount = w.likeCount + :delta where w.id = :id")
    void addLikes(@Param("id") Long id, @Param("delta") int delta);

    @Modifying
    @Query("update Work w set w.cloneCount = w.cloneCount + 1 where w.id = :id")
    void addClone(@Param("id") Long id);

    @Modifying
    @Query("update Work w set w.shareCount = w.shareCount + 1 where w.id = :id")
    void addShare(@Param("id") Long id);

    @Modifying
    @Query("update Work w set w.viewCount = w.viewCount + 1 where w.id = :id")
    void addView(@Param("id") Long id);

    @Modifying
    @Query("update Work w set w.commentCount = :count where w.id = :id")
    void setCommentCount(@Param("id") Long id, @Param("count") int count);

    @Query("select w.likeCount from Work w where w.id = :id")
    int likeCount(@Param("id") Long id);

    @Query("select w.cloneCount from Work w where w.id = :id")
    int cloneCount(@Param("id") Long id);

    @Query("select w.shareCount from Work w where w.id = :id")
    int shareCount(@Param("id") Long id);

    @Query("select w.viewCount from Work w where w.id = :id")
    long viewCount(@Param("id") Long id);

    /** 최근 반응 점수 순 ID (이슈 게시판). 추천 3, 댓글 2, Clone 5, 공유 2점 */
    @Query(value = """
            SELECT w.id
            FROM work w
                     LEFT JOIN (SELECT work_id, count(*) c FROM work_like WHERE created_at > :since GROUP BY work_id) l ON l.work_id = w.id
                     LEFT JOIN (SELECT work_id, count(*) c FROM comment WHERE work_id IS NOT NULL AND created_at > :since GROUP BY work_id) m ON m.work_id = w.id
                     LEFT JOIN (SELECT work_id, count(*) c FROM work_clone WHERE created_at > :since GROUP BY work_id) k ON k.work_id = w.id
                     LEFT JOIN (SELECT work_id, count(*) c FROM work_share WHERE created_at > :since GROUP BY work_id) s ON s.work_id = w.id
            WHERE w.visibility = 'PUBLIC'
              AND COALESCE(l.c, 0) + COALESCE(m.c, 0) + COALESCE(k.c, 0) + COALESCE(s.c, 0) > 0
            ORDER BY COALESCE(l.c, 0) * 3 + COALESCE(m.c, 0) * 2 + COALESCE(k.c, 0) * 5 + COALESCE(s.c, 0) * 2 DESC, w.published_at DESC
            LIMIT 12
            """, nativeQuery = true)
    List<Long> trendingIds(@Param("since") Instant since);
}
