package com.achievement.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import javax.imageio.ImageIO;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.achievement.config.TikaProperties;
import com.achievement.exception.TextExtractionException;
import com.achievement.service.ITextExtractionService.ExtractionResult;

class TextExtractionServiceImplTest {

    private TikaProperties properties;
    private TextExtractionServiceImpl service;

    @BeforeEach
    void setUp() {
        properties = new TikaProperties();
        properties.setMaxFileSizeBytes(10 * 1024 * 1024);
        properties.setMaxTextLength(100_000);
        service = new TextExtractionServiceImpl(properties);
    }

    @Test
    void extractPlainText() {
        byte[] content = "Hello, World!".getBytes(StandardCharsets.UTF_8);
        ExtractionResult result = service.extractText(content, "test.txt", "text/plain");

        assertEquals("Hello, World!", result.text().stripTrailing());
        assertFalse(result.truncated());
        assertTrue(result.detectedMimeType().startsWith("text/"));
    }

    @Test
    void extractPdf() throws IOException {
        byte[] pdfBytes = createTestPdf("PDF content for testing");
        ExtractionResult result = service.extractText(pdfBytes, "test.pdf", "application/pdf");

        assertNotNull(result.text());
        assertTrue(result.text().contains("PDF content for testing"));
        assertFalse(result.truncated());
    }

    @Test
    void extractDocx() throws IOException {
        byte[] docxBytes = createTestDocx("DOCX content for testing");
        ExtractionResult result = service.extractText(docxBytes, "test.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document");

        assertNotNull(result.text());
        assertTrue(result.text().contains("DOCX content for testing"));
    }

    @Test
    void extractExcel() throws IOException {
        byte[] xlsxBytes = createTestExcel("Cell A1", "Cell B1");
        ExtractionResult result = service.extractText(xlsxBytes, "test.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

        assertNotNull(result.text());
        assertTrue(result.text().contains("Cell A1"));
        assertTrue(result.text().contains("Cell B1"));
    }

    @Test
    void extractExceedsSizeLimit() {
        properties.setMaxFileSizeBytes(100);
        byte[] content = new byte[200];

        TextExtractionException ex = assertThrows(TextExtractionException.class,
                () -> service.extractText(content, "big.txt", "text/plain"));
        assertTrue(ex.getMessage().contains("超过限制"));
    }

    @Test
    void extractTruncatesLongText() {
        properties.setMaxTextLength(50);
        String longText = "A".repeat(200);
        byte[] content = longText.getBytes(StandardCharsets.UTF_8);

        ExtractionResult result = service.extractText(content, "long.txt", "text/plain");

        assertNotNull(result.text());
        assertTrue(result.text().length() <= 50);
        assertTrue(result.truncated());
    }

    @Test
    void extractImageWithOcrDisabledReturnsEmptyText() throws IOException {
        byte[] pngBytes = createTestPng();
        ExtractionResult result = service.extractText(pngBytes, "test.png", "image/png");

        assertNull(result.text());
        assertNotNull(result.detectedMimeType());
        assertTrue(result.detectedMimeType().startsWith("image/"));
    }

    @Test
    void extractWithNullFilenameAndMimeType() {
        byte[] content = "Plain text without hints".getBytes(StandardCharsets.UTF_8);
        ExtractionResult result = service.extractText(content, null, null);

        assertNotNull(result.text());
        assertTrue(result.text().contains("Plain text without hints"));
    }

    @Test
    void extractFromInputStream() throws IOException {
        byte[] content = "Stream content".getBytes(StandardCharsets.UTF_8);
        ExtractionResult result;
        try (var is = new ByteArrayInputStream(content)) {
            result = service.extractText(is, "stream.txt", "text/plain");
        }

        assertEquals("Stream content", result.text().stripTrailing());
    }

    @Test
    void extractNullByteArrayReturnsEmpty() {
        ExtractionResult result = service.extractText((byte[]) null, "empty.txt", "text/plain");
        assertNull(result.text());
    }

    @Test
    void extractNullInputStreamReturnsEmpty() {
        ExtractionResult result = service.extractText((java.io.InputStream) null, "empty.txt", "text/plain");
        assertNull(result.text());
    }

    // --- helper methods ---

    private byte[] createTestPdf(String text) throws IOException {
        try (PDDocument doc = new PDDocument();
                ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDPage page = new PDPage();
            doc.addPage(page);
            var font = new org.apache.pdfbox.pdmodel.font.PDType1Font(
                    org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName.HELVETICA);
            var contentStream = new org.apache.pdfbox.pdmodel.PDPageContentStream(doc, page);
            contentStream.beginText();
            contentStream.setFont(font, 12);
            contentStream.newLineAtOffset(50, 700);
            contentStream.showText(text);
            contentStream.endText();
            contentStream.close();
            doc.save(out);
            return out.toByteArray();
        }
    }

    private byte[] createTestDocx(String text) throws IOException {
        try (XWPFDocument doc = new XWPFDocument();
                ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            XWPFParagraph para = doc.createParagraph();
            para.createRun().setText(text);
            doc.write(out);
            return out.toByteArray();
        }
    }

    private byte[] createTestExcel(String... cellValues) throws IOException {
        try (Workbook wb = new XSSFWorkbook();
                ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Sheet1");
            Row row = sheet.createRow(0);
            for (int i = 0; i < cellValues.length; i++) {
                row.createCell(i).setCellValue(cellValues[i]);
            }
            wb.write(out);
            return out.toByteArray();
        }
    }

    private byte[] createTestPng() throws IOException {
        BufferedImage image = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", out);
            return out.toByteArray();
        }
    }
}
