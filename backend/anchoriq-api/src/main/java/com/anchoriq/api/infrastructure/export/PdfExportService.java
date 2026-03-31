package com.anchoriq.api.infrastructure.export;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;

/**
 * PDF 생성 유틸리티.
 * OpenPDF 라이브러리를 사용하여 PDF 문서를 생성한다.
 */
@Slf4j
public class PdfExportService {

    private static final Font TITLE_FONT = new Font(Font.HELVETICA, 18, Font.BOLD);
    private static final Font BODY_FONT = new Font(Font.HELVETICA, 12, Font.NORMAL);

    public byte[] generatePdf(String title, String content) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document();
            PdfWriter.getInstance(document, baos);
            document.open();

            document.add(new Paragraph(title, TITLE_FONT));
            document.add(new Paragraph(" "));
            document.add(new Paragraph(content, BODY_FONT));

            document.close();
            return baos.toByteArray();
        } catch (DocumentException e) {
            log.error("PDF generation failed: {}", e.getMessage(), e);
            throw new RuntimeException("PDF generation failed", e);
        } catch (Exception e) {
            log.error("Unexpected error during PDF generation: {}", e.getMessage(), e);
            throw new RuntimeException("PDF generation failed", e);
        }
    }
}
