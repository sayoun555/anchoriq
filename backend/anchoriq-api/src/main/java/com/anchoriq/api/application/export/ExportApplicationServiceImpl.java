package com.anchoriq.api.application.export;

import com.anchoriq.ai.report.AiReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 내보내기 Application Service 구현체.
 */
@Service
@RequiredArgsConstructor
public class ExportApplicationServiceImpl implements ExportApplicationService {

    private final AiReportService aiReportService;

    @Override
    public byte[] generateRiskReportPdf() {
        return aiReportService.generateRiskReportPdf();
    }

    @Override
    public byte[] generateRiskReportCsv() {
        return aiReportService.generateRiskReportCsv();
    }

    @Override
    public byte[] generateVesselsCsv() {
        return aiReportService.generateVesselsCsv();
    }
}
