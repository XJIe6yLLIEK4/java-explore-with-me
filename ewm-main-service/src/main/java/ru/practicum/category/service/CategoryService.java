package ru.practicum.category.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.category.dto.CategoryDto;
import ru.practicum.category.dto.NewCategoryDto;
import ru.practicum.category.mapper.CategoryMapper;
import ru.practicum.category.model.Category;
import ru.practicum.category.repository.CategoryRepository;
import ru.practicum.category.repository.CategoryUsageDao;
import ru.practicum.common.errors.ConflictException;
import ru.practicum.common.errors.NotFoundException;
import ru.practicum.common.util.PageRequestUtil;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryUsageDao usageDao;
    private final CategoryMapper mapper;

    @Transactional
    public CategoryDto create(NewCategoryDto dto) {
        if (categoryRepository.existsByNameIgnoreCase(dto.getName())) {
            throw new ConflictException("Category name must be unique.");
        }
        Category saved = categoryRepository.save(mapper.fromNew(dto));
        return mapper.toDto(saved);
    }

    @Transactional
    public CategoryDto update(Long catId, NewCategoryDto dto) {
        Category entity = categoryRepository.findById(catId)
                .orElseThrow(() -> new NotFoundException("Category with id=" + catId + " was not found"));
        if (dto.getName() != null &&
                categoryRepository.existsByNameIgnoreCaseAndIdNot(dto.getName(), catId)) {
            throw new ConflictException("Category name must be unique.");
        }
        mapper.update(entity, dto);
        return mapper.toDto(categoryRepository.save(entity));
    }

    @Transactional
    public void delete(Long catId) {
        Category entity = categoryRepository.findById(catId)
                .orElseThrow(() -> new NotFoundException("Category with id=" + catId + " was not found"));
        long usage = usageDao.countEventsByCategoryId(catId);
        if (usage > 0) {
            throw new ConflictException("Category is used by events and cannot be deleted.");
        }
        categoryRepository.delete(entity);
    }

    @Transactional(readOnly = true)
    public CategoryDto get(Long catId) {
        Category entity = categoryRepository.findById(catId)
                .orElseThrow(() -> new NotFoundException("Category with id=" + catId + " was not found"));
        return mapper.toDto(entity);
    }

    @Transactional(readOnly = true)
    public List<CategoryDto> getAll(int from, int size) {
        return categoryRepository
                .findAll(PageRequestUtil.of(from, size, Sort.by("id").ascending()))
                .stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }
}
