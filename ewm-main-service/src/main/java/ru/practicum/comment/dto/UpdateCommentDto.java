package ru.practicum.comment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateCommentDto {
    @NotBlank
    @Size(min = 1, max = 4000)
    private String text;
}
