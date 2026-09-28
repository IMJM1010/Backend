package com.example.be.domain.envsensor.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 환경 센서 종류.
 *
 * <p>종류마다 기본 단위를 둔다. 등록 요청에 단위를 생략하면 이 값을 쓴다.
 * 현장에서 같은 종류는 거의 항상 같은 단위로 측정하므로, 매번 입력받으면 오타만 늘어난다.
 */
@Getter
@RequiredArgsConstructor
public enum SensorType {
    TEMPERATURE("℃"),
    HUMIDITY("%"),
    FINE_DUST("㎍/㎥"),
    OXYGEN("%"),
    CHEMICAL("ppm"),
    NOISE("dB");

    private final String defaultUnit;
}
