package ru.practicum.comment.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.practicum.comment.model.Comment;
import ru.practicum.comment.model.CommentStatus;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    Page<Comment> findAllByEvent_IdAndStatusOrderByCreatedOnDesc(Long eventId, CommentStatus status, Pageable pageable);

    Page<Comment> findAllByAuthor_Id(Long authorId, Pageable pageable);
}
