package com.example.be.global.common;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * 목록 조회 응답의 공통 페이징 래퍼.
 *
 * <p>Spring 의 {@link Page} 를 그대로 직렬화하면 내부 구현(pageable, sort 객체 등)이
 * 응답 스펙으로 굳어버리므로, 프론트가 실제로 쓰는 필드만 추려서 내려준다.
 *
 * @param content       현재 페이지의 데이터
 * @param page          현재 페이지 번호 (0부터 시작)
 * @param size          페이지 크기
 * @param totalElements 전체 데이터 수
 * @param totalPages    전체 페이지 수
 * @param first         첫 페이지 여부
 * @param last          마지막 페이지 여부
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast()
        );
    }

    /**
     * 엔티티 Page 를 Response DTO Page 로 변환하면서 감싼다.
     *
     * <pre>PageResponse.from(workerPage, WorkerResponse::from)</pre>
     */
    public static <E, R> PageResponse<R> from(Page<E> page, Function<E, R> mapper) {
        return from(page.map(mapper));
    }
}
