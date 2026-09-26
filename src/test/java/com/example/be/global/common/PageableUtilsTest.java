package com.example.be.global.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("PageableUtils")
class PageableUtilsTest {

    @Test
    @DisplayName("정렬이 없으면 기본 정렬 뒤에 같은 방향으로 id 를 붙인다")
    void defaultSort_addsIdTieBreaker() {
        Pageable result = PageableUtils.withLatestFirst(PageRequest.of(2, 20));

        assertEquals(Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")), result.getSort());
        assertEquals(2, result.getPageNumber());
        assertEquals(20, result.getPageSize());
    }

    @Test
    @DisplayName("클라이언트가 보낸 정렬은 유지하고 마지막 정렬 방향으로 id 를 붙인다")
    void clientSort_keptAndIdAppended() {
        Pageable request = PageRequest.of(0, 10, Sort.by(Sort.Order.desc("safetyStatus"), Sort.Order.asc("name")));

        Pageable result = PageableUtils.withLatestFirst(request);

        assertEquals(Sort.by(Sort.Order.desc("safetyStatus"), Sort.Order.asc("name"), Sort.Order.asc("id")),
                result.getSort());
    }

    @Test
    @DisplayName("이미 id 로 정렬하면 그대로 둔다")
    void idAlreadySorted_unchanged() {
        Pageable request = PageRequest.of(0, 10, Sort.by(Sort.Order.asc("id")));

        Pageable result = PageableUtils.withLatestFirst(request);

        assertEquals(Sort.by(Sort.Order.asc("id")), result.getSort());
    }

    @Test
    @DisplayName("기본 정렬 키가 오름차순이면 id 도 오름차순이다")
    void ascendingDefault() {
        Pageable result = PageableUtils.withDefaultSort(PageRequest.of(0, 10), "zoneCode", Sort.Direction.ASC);

        assertEquals(Sort.by(Sort.Order.asc("zoneCode"), Sort.Order.asc("id")), result.getSort());
    }
}
