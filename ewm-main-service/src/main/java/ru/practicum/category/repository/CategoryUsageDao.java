package ru.practicum.category.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Лёгкий DAO без зависимости от Event-сущности.
 * Используется для проверки, что категория не используется событиями при удалении.
 */
@Repository
@RequiredArgsConstructor
public class CategoryUsageDao {

    private final JdbcTemplate jdbcTemplate;

    @Transactional(readOnly = true)
    public long countEventsByCategoryId(Long catId) {
        Long cnt = jdbcTemplate.queryForObject(
                "select count(*) from events where category_id = ?",
                Long.class, catId);
        return cnt == null ? 0L : cnt;
    }
}
