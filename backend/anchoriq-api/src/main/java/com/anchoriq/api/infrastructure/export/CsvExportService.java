package com.anchoriq.api.infrastructure.export;

import com.opencsv.CSVWriter;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * CSV 생성 유틸리티.
 * OpenCSV 라이브러리를 사용하여 CSV 데이터를 생성한다.
 */
@Slf4j
public class CsvExportService {

    public byte[] generateCsv(String[] headers, List<String[]> rows) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             OutputStreamWriter osw = new OutputStreamWriter(baos, StandardCharsets.UTF_8);
             CSVWriter csvWriter = new CSVWriter(osw)) {

            csvWriter.writeNext(headers);
            for (String[] row : rows) {
                csvWriter.writeNext(row);
            }
            csvWriter.flush();
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("CSV generation failed: {}", e.getMessage(), e);
            throw new RuntimeException("CSV generation failed", e);
        }
    }
}
