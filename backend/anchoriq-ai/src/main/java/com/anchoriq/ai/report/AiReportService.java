package com.anchoriq.ai.report;

/**
 * AI 리포트 생성 서비스 인터페이스.
 */
public interface AiReportService {

    byte[] generateRiskReportPdf();

    byte[] generateVesselsCsv();

    byte[] generateRiskReportCsv();
}
