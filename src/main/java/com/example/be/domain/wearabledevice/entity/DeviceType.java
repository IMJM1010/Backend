package com.example.be.domain.wearabledevice.entity;

/**
 * 웨어러블 디바이스 종류.
 *
 * <p>한 작업자가 여러 종류를 동시에 착용할 수 있다. (밴드 + 헬멧)
 * 다만 <b>같은 종류를 둘 이상 착용할 수는 없다.</b>
 */
public enum DeviceType {
    /** 손목 밴드. 심박·체온·걸음수를 수집한다. */
    BAND,
    /** 스마트 헬멧. 충격·추락 감지. */
    HELMET,
    /** 위치 태그. 구역 이동 추적. */
    TAG
}
