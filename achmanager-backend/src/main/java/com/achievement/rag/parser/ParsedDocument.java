package com.achievement.rag.parser;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 文档解析结果——保留段落结构和表格信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParsedDocument {
    private String fileName;
    private String mimeType;
    private long fileSize;

    /** 解析原始文本（未清洗） */
    private String rawText;

    /** 结构化段落 */
    @Builder.Default
    private List<TextSegment> segments = new ArrayList<>();

    /** 表格数据（Excel/Word 中的表格→Markdown） */
    @Builder.Default
    private List<TableData> tables = new ArrayList<>();

    /** 文档元数据 */
    @Builder.Default
    private Map<String, String> metadata = new HashMap<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TextSegment {
        private int index;
        private String heading;
        private int headingLevel; // 0=无标题, 1-6
        private String content;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TableData {
        private String sheetName;
        private int rowCount;
        private int colCount;
        private String markdown; // Markdown 格式表格
        private List<List<String>> rows;
    }
}
