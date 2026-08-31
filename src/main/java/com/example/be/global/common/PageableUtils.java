package com.example.be.global.common;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * 목록 조회 공통 유틸.
 */
public final class PageableUtils {

    private PageableUtils() {
    }

    /**
     * 클라이언트가 sort 를 보내지 않았을 때 기본 정렬을 적용한다.
     *
     * <p>정렬 없이 페이징하면 DB 가 반환 순서를 보장하지 않아 1페이지와 2페이지에
     * 같은 행이 중복으로 나타날 수 있다.
     */
    public static Pageable withDefaultSort(Pageable pageable, String property, Sort.Direction direction) {
        if (pageable.getSort().isSorted()) {
            return pageable;
        }
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                Sort.by(direction, property));
    }

    /** 기본값: 최신 등록순. */
    public static Pageable withLatestFirst(Pageable pageable) {
        return withDefaultSort(pageable, "createdAt", Sort.Direction.DESC);
    }
}
