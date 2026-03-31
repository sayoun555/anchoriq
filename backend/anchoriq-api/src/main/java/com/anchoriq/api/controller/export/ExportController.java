package com.anchoriq.api.controller.export;

import com.anchoriq.api.application.export.ExportApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * PDF/CSV 내보내기 Controller.
 */
@RestController
@RequestMapping("/api/export")
@RequiredArgsConstructor
public class ExportController {

    private final ExportApplicationService exportApplicationService;

    @GetMapping("/risk-report/pdf")
    public ResponseEntity<byte[]> exportRiskReportPdf() {
        byte[] pdf = exportApplicationService.generateRiskReportPdf();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=risk-report.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping("/risk-report/csv")
    public ResponseEntity<byte[]> exportRiskReportCsv() {
        byte[] csv = exportApplicationService.generateRiskReportCsv();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=risk-report.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }

    @GetMapping("/vessels/csv")
    public ResponseEntity<byte[]> exportVesselsCsv() {
        byte[] csv = exportApplicationService.generateVesselsCsv();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=vessels.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }
}
