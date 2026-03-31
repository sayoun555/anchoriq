package com.anchoriq.api.application.export;

/**
 * 내보내기 Application Service 인터페이스.
 */
public interface ExportApplicationService {

    byte[] generateRiskReportPdf();

    byte[] generateRiskReportCsv();

    byte[] generateVesselsCsv();
}
