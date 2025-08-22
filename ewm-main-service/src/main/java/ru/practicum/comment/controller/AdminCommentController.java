package ru.practicum.comment.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.comment.dto.CommentDto;
import ru.practicum.comment.dto.ModerateCommentRequest;
import ru.practicum.comment.service.CommentService;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/comments")
public class AdminCommentController {
    private final CommentService service;

    @PatchMapping("/{commentId}")
    public CommentDto moderate(@PathVariable Long commentId, @Valid @RequestBody ModerateCommentRequest body) {
        return service.moderate(commentId, body);
    }
}
