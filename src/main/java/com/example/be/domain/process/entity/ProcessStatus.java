package com.example.be.domain.process.entity;

/**
 * 공정 진행 상태.
 *
 * <ul>
 *   <li>{@link #RUNNING} — 정상 가동 중</li>
 *   <li>{@link #STOPPED} — 중단. 긴급조치(PROCESS_STOP)로도 이 상태가 된다.</li>
 * </ul>
 */
public enum ProcessStatus {
    RUNNING,
    STOPPED
}
