package ru.practicum.comment.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.comment.dto.CommentDto;
import ru.practicum.comment.dto.NewCommentDto;
import ru.practicum.comment.dto.UpdateCommentDto;
import ru.practicum.comment.service.CommentService;

import java.util.List;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/users/{userId}")
public class PrivateCommentController {
    private final CommentService service;

    @PostMapping("/events/{eventId}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentDto add(@PathVariable Long userId,
                          @PathVariable Long eventId,
                          @Valid @RequestBody NewCommentDto body) {
        return service.add(userId, eventId, body);
    }

    @PatchMapping("/comments/{commentId}")
    public CommentDto updateOwn(@PathVariable Long userId,
                                @PathVariable Long commentId,
                                @Valid @RequestBody UpdateCommentDto body) {
        return service.updateOwn(userId, commentId, body);
    }

    @DeleteMapping("/comments/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteOwn(@PathVariable Long userId, @PathVariable Long commentId) {
        service.deleteOwn(userId, commentId);
    }

    @GetMapping("/comments")
    public List<CommentDto> myComments(@PathVariable Long userId,
                                       @RequestParam(defaultValue = "0") @Min(0) int from,
                                       @RequestParam(defaultValue = "10") @Positive int size) {
        return service.getUserComments(userId, from, size);
    }
}
