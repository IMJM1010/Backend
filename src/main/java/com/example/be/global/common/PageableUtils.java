package com.example.be.global.common;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * 목록 조회 공통 유틸.
 */
public final class PageableUtils {

    /** 모든 엔티티의 식별자 필드명. 정렬 동률을 끊는 보조 정렬 키로 쓴다. */
    private static final String ID = "id";

    private PageableUtils() {
    }

    /**
     * 클라이언트가 sort 를 보내지 않았을 때 기본 정렬을 적용하고, 마지막에 {@code id} 보조 정렬을 붙인다.
     *
     * <p>정렬 없이 페이징하면 DB 가 반환 순서를 보장하지 않아 1페이지와 2페이지에
     * 같은 행이 중복으로 나타날 수 있다.
     *
     * <p>정렬 키가 있어도 값이 같은 행(같은 시각에 일괄 등록된 작업자 등)끼리는 순서가 정해지지 않아
     * 같은 문제가 생긴다. 그래서 클라이언트가 보낸 정렬이든 기본 정렬이든 {@code id} 가 없으면
     * 마지막 정렬과 같은 방향으로 {@code id} 를 덧붙여 순서를 하나로 고정한다.
     */
    public static Pageable withDefaultSort(Pageable pageable, String property, Sort.Direction direction) {
        Sort sort = pageable.getSort().isSorted()
                ? pageable.getSort()
                : Sort.by(direction, property);
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), withIdTieBreaker(sort));
    }

    /** 기본값: 최신 등록순. */
    public static Pageable withLatestFirst(Pageable pageable) {
        return withDefaultSort(pageable, "createdAt", Sort.Direction.DESC);
    }

    private static Sort withIdTieBreaker(Sort sort) {
        if (sort.getOrderFor(ID) != null) {
            return sort;
        }
        Sort.Direction lastDirection = sort.stream()
                .reduce((first, second) -> second)
                .map(Sort.Order::getDirection)
                .orElse(Sort.Direction.ASC);
        return sort.and(Sort.by(lastDirection, ID));
    }
}
