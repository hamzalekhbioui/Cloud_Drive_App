package com.cloud.drive.util;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PageLimitsTest {

    @Test
    void capsPageSizeAndAddsStableIdTieBreaker() {
        var bounded = PageLimits.bounded(
                PageRequest.of(2, 500, Sort.by(Sort.Direction.ASC, "createdAt")),
                Set.of("createdAt", "id"), "createdAt", Sort.Direction.DESC);

        assertThat(bounded.getPageNumber()).isEqualTo(2);
        assertThat(bounded.getPageSize()).isEqualTo(100);
        assertThat(bounded.getSort().getOrderFor("createdAt").getDirection()).isEqualTo(Sort.Direction.ASC);
        assertThat(bounded.getSort().getOrderFor("id")).isNotNull();
    }

    @Test
    void rejectsUnapprovedSortFields() {
        var bounded = PageLimits.bounded(
                PageRequest.of(0, 20, Sort.by("blobFileName")),
                Set.of("createdAt", "id"), "createdAt", Sort.Direction.DESC);

        assertThat(bounded.getSort().getOrderFor("blobFileName")).isNull();
        assertThat(bounded.getSort().getOrderFor("createdAt").getDirection()).isEqualTo(Sort.Direction.DESC);
    }
}
