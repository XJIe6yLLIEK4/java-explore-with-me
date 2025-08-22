package ru.practicum.comment.mapper;

import org.springframework.stereotype.Component;
import ru.practicum.comment.dto.CommentDto;
import ru.practicum.comment.model.Comment;
import ru.practicum.user.mapper.UserMapper;

@Component
public class CommentMapper {

    private final UserMapper userMapper;

    public CommentMapper(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    public CommentDto toDto(Comment entity) {
        if (entity == null) return null;
        CommentDto dto = CommentDto.builder()
                .id(entity.getId())
                .eventId(entity.getEvent().getId())
                .author(userMapper.toShort(entity.getAuthor()))
                .status(entity.getStatus() == null ? null : entity.getStatus().name())
                .rejectReason(entity.getRejectReason())
                .text(entity.getText())
                .createdOn(entity.getCreatedOn())
                .updatedOn(entity.getUpdatedOn())
                .build();
        return dto;
    }
}
