package ru.practicum.comment.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.comment.dto.CommentDto;
import ru.practicum.comment.dto.ModerateCommentRequest;
import ru.practicum.comment.dto.NewCommentDto;
import ru.practicum.comment.dto.UpdateCommentDto;
import ru.practicum.comment.mapper.CommentMapper;
import ru.practicum.comment.model.Comment;
import ru.practicum.comment.model.CommentStatus;
import ru.practicum.comment.repository.CommentRepository;
import ru.practicum.common.errors.ForbiddenOperationException;
import ru.practicum.common.errors.NotFoundException;
import ru.practicum.event.model.Event;
import ru.practicum.event.model.EventState;
import ru.practicum.event.repository.EventRepository;
import ru.practicum.user.model.User;
import ru.practicum.user.repository.UserRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentService {
    private final CommentRepository commentRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final CommentMapper mapper;

    @Transactional
    public CommentDto add(Long userId, Long eventId, NewCommentDto body) {
        User author = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User with id=" + userId + " was not found"));
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));
        if (event.getState() != EventState.PUBLISHED) {
            throw new ForbiddenOperationException("Comments are allowed only for published events");
        }
        Comment c = Comment.builder()
                .event(event)
                .author(author)
                .text(body.getText())
                .status(CommentStatus.PENDING)
                .build();
        return mapper.toDto(commentRepository.save(c));
    }

    @Transactional
    public CommentDto updateOwn(Long userId, Long commentId, UpdateCommentDto body) {
        Comment c = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("Comment with id=" + commentId + " was not found"));
        if (!c.getAuthor().getId().equals(userId)) {
            throw new ForbiddenOperationException("Only author can edit the comment");
        }
        c.setText(body.getText());
        // any edit moves comment back to PENDING for re-moderation
        c.setStatus(CommentStatus.PENDING);
        c.setRejectReason(null);
        return mapper.toDto(commentRepository.save(c));
    }

    @Transactional
    public void deleteOwn(Long userId, Long commentId) {
        Comment c = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("Comment with id=" + commentId + " was not found"));
        if (!c.getAuthor().getId().equals(userId)) {
            throw new ForbiddenOperationException("Only author can delete the comment");
        }
        commentRepository.deleteById(commentId);
    }

    @Transactional(readOnly = true)
    public List<CommentDto> getPublicForEvent(Long eventId, int from, int size) {
        if (!eventRepository.existsById(eventId)) {
            throw new NotFoundException("Event with id=" + eventId + " was not found");
        }
        int page = from / size;
        var pageable = PageRequest.of(page, size, Sort.by("createdOn").descending());
        return commentRepository.findAllByEvent_IdAndStatusOrderByCreatedOnDesc(eventId, CommentStatus.APPROVED, pageable)
                .getContent().stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CommentDto> getUserComments(Long userId, int from, int size) {
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("User with id=" + userId + " was not found");
        }
        int page = from / size;
        var pageable = PageRequest.of(page, size, Sort.by("id").descending());
        return commentRepository.findAllByAuthor_Id(userId, pageable)
                .getContent().stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public CommentDto moderate(Long commentId, ModerateCommentRequest req) {
        Comment c = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("Comment with id=" + commentId + " was not found"));
        switch (req.getAction()) {
            case APPROVE -> {
                c.setStatus(CommentStatus.APPROVED);
                c.setRejectReason(null);
            }
            case REJECT -> {
                c.setStatus(CommentStatus.REJECTED);
                c.setRejectReason(req.getReason());
            }
        }
        return mapper.toDto(commentRepository.save(c));
    }
}
