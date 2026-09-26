package com.cloud.drive.util;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Set;

/** Shared, defensive pagination rules for public list endpoints. */
public final class PageLimits {
    public static final int MAX_PAGE_SIZE = 100;

    private PageLimits() {}

    public static Pageable bounded(Pageable requested, Set<String> allowedSorts,
                                   String defaultSort, Sort.Direction defaultDirection) {
        int page = Math.max(requested.getPageNumber(), 0);
        int size = Math.min(Math.max(requested.getPageSize(), 1), MAX_PAGE_SIZE);
        Sort sort = requested.getSort().isSorted()
                && requested.getSort().stream().allMatch(order -> allowedSorts.contains(order.getProperty()))
                ? requested.getSort()
                : Sort.by(defaultDirection, defaultSort);

        // Make offset paging deterministic when multiple rows share the primary sort value.
        if (sort.getOrderFor("id") == null) {
            sort = sort.and(Sort.by(defaultDirection, "id"));
        }
        return PageRequest.of(page, size, sort);
    }
}
