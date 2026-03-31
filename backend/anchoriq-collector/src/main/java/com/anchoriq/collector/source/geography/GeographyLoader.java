package com.anchoriq.collector.source.geography;

/**
 * 지리 데이터 로더 인터페이스.
 */
public interface GeographyLoader {

    /**
     * 정적 지리 데이터를 DB에 로드한다.
     */
    void load();

    /**
     * 로더 이름을 반환한다.
     */
    String loaderName();
}
