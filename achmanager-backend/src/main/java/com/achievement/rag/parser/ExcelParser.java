package com.achievement.rag.parser;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Excel 文档解析器（支持 .xls 和 .xlsx）
 * - 每个 Sheet 转为 Markdown 表格
 * - 保留行列结构
 */
@Slf4j
@Component
public class ExcelParser {

    public ParsedDocument parse(byte[] content, String fileName, boolean isXlsx) throws IOException {
        List<ParsedDocument.TableData> tables = new ArrayList<>();

        try (Workbook wb = isXlsx
                ? new XSSFWorkbook(new ByteArrayInputStream(content))
                : new HSSFWorkbook(new ByteArrayInputStream(content))) {

            StringBuilder rawSb = new StringBuilder();
            DataFormatter formatter = new DataFormatter();

            for (int i = 0; i < wb.getNumberOfSheets(); i++) {
                Sheet sheet = wb.getSheetAt(i);
                String sheetName = sheet.getSheetName();

                rawSb.append("=== Sheet: ").append(sheetName).append(" ===\n");

                // 提取该 Sheet 的行数据
                List<List<String>> rows = new ArrayList<>();
                for (Row row : sheet) {
                    List<String> cells = new ArrayList<>();
                    for (int c = 0; c < row.getLastCellNum(); c++) {
                        Cell cell = row.getCell(c);
                        cells.add(cell != null ? formatter.formatCellValue(cell).trim() : "");
                    }
                    rows.add(cells);
                }

                if (rows.isEmpty()) continue;

                // 转 Markdown 表格
                String md = convertToMarkdown(rows);
                rawSb.append(md).append("\n\n");

                tables.add(ParsedDocument.TableData.builder()
                        .sheetName(sheetName)
                        .rowCount(rows.size())
                        .colCount(rows.isEmpty() ? 0 : rows.get(0).size())
                        .markdown(md)
                        .rows(rows)
                        .build());
            }

            log.info("Excel parsed: {} sheets, {} tables", wb.getNumberOfSheets(), tables.size());

            return ParsedDocument.builder()
                    .fileName(fileName)
                    .mimeType(isXlsx ? "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                            : "application/vnd.ms-excel")
                    .fileSize(content.length)
                    .rawText(rawSb.toString())
                    .tables(tables)
                    .build();
        }
    }

    /** List< rows > → Markdown 表格 */
    private String convertToMarkdown(List<List<String>> rows) {
        StringBuilder sb = new StringBuilder();
        if (rows.isEmpty()) return "";

        // Header
        sb.append('|');
        for (String cell : rows.get(0)) {
            sb.append(' ').append(cell).append(" |");
        }
        sb.append('\n');

        // Separator
        sb.append('|');
        for (String ignored : rows.get(0)) {
            sb.append(" --- |");
        }
        sb.append('\n');

        // Data
        for (int r = 1; r < rows.size(); r++) {
            sb.append('|');
            for (String cell : rows.get(r)) {
                sb.append(' ').append(cell).append(" |");
            }
            sb.append('\n');
        }
        return sb.toString();
    }
}
