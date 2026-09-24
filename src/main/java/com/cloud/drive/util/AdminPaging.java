package com.cloud.drive.util;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Set;

public final class AdminPaging {
    private static final int MAX_PAGE_SIZE = 100;

    private AdminPaging() {}

    public static Pageable bounded(int page, int size, Sort sort) {
        return PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE), sort);
    }

    public static Pageable bounded(Pageable requested, Set<String> allowedSorts, String defaultSort) {
        int page = Math.max(requested.getPageNumber(), 0);
        int size = Math.min(Math.max(requested.getPageSize(), 1), MAX_PAGE_SIZE);
        Sort sort = requested.getSort().isSorted()
                && requested.getSort().stream().allMatch(order -> allowedSorts.contains(order.getProperty()))
                ? requested.getSort()
                : Sort.by(Sort.Direction.DESC, defaultSort);
        return PageRequest.of(page, size, sort);
    }
}
