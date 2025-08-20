package ru.practicum.user.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.common.errors.ConflictException;
import ru.practicum.common.errors.NotFoundException;
import ru.practicum.common.util.PageRequestUtil;
import ru.practicum.user.dto.NewUserRequest;
import ru.practicum.user.dto.UserDto;
import ru.practicum.user.mapper.UserMapper;
import ru.practicum.user.model.User;
import ru.practicum.user.repository.UserRepository;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper mapper;

    @Transactional
    public UserDto create(NewUserRequest dto) {
        if (userRepository.existsByEmailIgnoreCase(dto.getEmail())) {
            throw new ConflictException("Email must be unique.");
        }
        User saved = userRepository.save(mapper.fromNew(dto));
        return mapper.toDto(saved);
    }

    @Transactional(readOnly = true)
    public List<UserDto> get(List<Long> ids, int from, int size) {
        var pageable = PageRequestUtil.of(from, size, Sort.by("id").ascending());
        if (ids == null || ids.isEmpty()) {
            return userRepository.findAll(pageable)
                    .stream().map(mapper::toDto).collect(Collectors.toList());
        } else {
            Collection<Long> safe = ids.stream()
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            if (safe.isEmpty()) return Collections.emptyList();
            return userRepository.findAllByIdIn(safe, pageable)
                    .stream().map(mapper::toDto).collect(Collectors.toList());
        }
    }

    @Transactional
    public void delete(Long userId) {
        User entity = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User with id=" + userId + " was not found"));
        userRepository.delete(entity);
    }
}
