package com.anchoriq.ai.report;

import com.anchoriq.core.domain.intelligence.risk.model.RiskScore;
import com.anchoriq.core.domain.intelligence.risk.service.SupplyChainRiskService;
import com.anchoriq.core.domain.maritime.vessel.model.Vessel;
import com.anchoriq.core.domain.maritime.vessel.repository.VesselRepository;
import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

/**
 * AI 리포트 생성 서비스 구현체.
 * OpenPDF로 PDF, 직접 구현으로 CSV를 생성한다.
 */
@Slf4j
@Service
public class AiReportServiceImpl implements AiReportService {

    private final VesselRepository vesselRepository;
    private final SupplyChainRiskService riskService;

    public AiReportServiceImpl(VesselRepository vesselRepository,
                                SupplyChainRiskService riskService) {
        this.vesselRepository = vesselRepository;
        this.riskService = riskService;
    }

    @Override
    public byte[] generateRiskReportPdf() {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, baos);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
            Font bodyFont = FontFactory.getFont(FontFactory.HELVETICA, 10);

            document.add(new Paragraph("AnchorIQ Risk Report", titleFont));
            document.add(new Paragraph("Date: " + LocalDate.now(), bodyFont));
            document.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(5);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{2, 3, 1.5f, 1.5f, 2});

            addTableHeader(table, headerFont, "IMO", "Name", "Flag", "Score", "Level");

            List<Vessel> vessels = vesselRepository.findAll(0, 50);
            for (Vessel vessel : vessels) {
                RiskScore score = riskService.assessVesselRisk(vessel);
                table.addCell(new PdfPCell(new Phrase(vessel.getImo().value(), bodyFont)));
                table.addCell(new PdfPCell(new Phrase(vessel.getName(), bodyFont)));
                table.addCell(new PdfPCell(new Phrase(vessel.getFlag().value(), bodyFont)));
                table.addCell(new PdfPCell(new Phrase(String.valueOf(score.getScore()), bodyFont)));
                table.addCell(new PdfPCell(new Phrase(score.getLevel().name(), bodyFont)));
            }

            document.add(table);
            document.close();

            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Failed to generate PDF risk report: {}", e.getMessage(), e);
            throw new RuntimeException("PDF generation failed", e);
        }
    }

    @Override
    public byte[] generateVesselsCsv() {
        StringBuilder csv = new StringBuilder();
        csv.append("IMO,Name,Flag,Type,Status,Risk Score,Risk Level\n");

        List<Vessel> vessels = vesselRepository.findAll();
        for (Vessel vessel : vessels) {
            RiskScore score = riskService.assessVesselRisk(vessel);
            csv.append(String.format("%s,%s,%s,%s,%s,%d,%s\n",
                    escapeCSV(vessel.getImo().value()),
                    escapeCSV(vessel.getName()),
                    escapeCSV(vessel.getFlag().value()),
                    vessel.getType(),
                    vessel.getStatus(),
                    score.getScore(),
                    score.getLevel().name()));
        }

        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public byte[] generateRiskReportCsv() {
        StringBuilder csv = new StringBuilder();
        csv.append("IMO,Name,Score,Level,Explanation\n");

        List<Vessel> vessels = vesselRepository.findAll(0, 100);
        for (Vessel vessel : vessels) {
            RiskScore score = riskService.assessVesselRisk(vessel);
            csv.append(String.format("%s,%s,%d,%s,%s\n",
                    escapeCSV(vessel.getImo().value()),
                    escapeCSV(vessel.getName()),
                    score.getScore(),
                    score.getLevel().name(),
                    escapeCSV(score.getExplanation())));
        }

        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private void addTableHeader(PdfPTable table, Font font, String... headers) {
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, font));
            cell.setGrayFill(0.9f);
            table.addCell(cell);
        }
    }

    private String escapeCSV(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
