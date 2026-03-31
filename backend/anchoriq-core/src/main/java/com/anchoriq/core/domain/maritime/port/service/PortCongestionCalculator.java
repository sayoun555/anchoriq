package com.anchoriq.core.domain.maritime.port.service;

import com.anchoriq.core.domain.maritime.port.model.CongestionReport;
import com.anchoriq.core.domain.maritime.port.model.Locode;

import java.util.List;

/**
 * 항만 혼잡도 계산기 (Domain Service 인터페이스).
 * Redis GEO 기반 실시간 선박 위치와 UNCTAD 기준선을 결합하여
 * 항만별 혼잡도를 계산한다.
 */
public interface PortCongestionCalculator {

    /**
     * 특정 항만의 혼잡도를 계산한다.
     * Redis GEO에서 항만 반경 내 선박을 조회하고,
     * UNCTAD 기준선 대비 비율을 포함한 보고서를 생성한다.
     *
     * @param locode 항만 코드
     * @return 혼잡도 보고서
     */
    CongestionReport calculateCongestion(Locode locode);

    /**
     * 모든 항만의 혼잡도를 일괄 계산한다.
     *
     * @return 전체 항만 혼잡도 보고서 목록
     */
    List<CongestionReport> calculateAllPortsCongestion();
}
