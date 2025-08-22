package ru.practicum.comment.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ModerateCommentRequest {
    @NotNull
    private Action action;
    private String reason;

    public enum Action { APPROVE, REJECT }
}
