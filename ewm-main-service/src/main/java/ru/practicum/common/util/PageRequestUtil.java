package ru.practicum.common.util;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public final class PageRequestUtil {
    private PageRequestUtil() {
    }

    public static Pageable of(int from, int size) {
        return of(from, size, Sort.unsorted());
    }

    public static Pageable of(int from, int size, Sort sort) {
        if (from < 0) throw new IllegalArgumentException("from must be >= 0");
        if (size <= 0) throw new IllegalArgumentException("size must be > 0");
        int page = from / size;
        return PageRequest.of(page, size, sort == null ? Sort.unsorted() : sort);
    }
}
