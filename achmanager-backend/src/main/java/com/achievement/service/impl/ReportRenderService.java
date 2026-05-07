package com.achievement.service.impl;

import java.io.ByteArrayOutputStream;

import org.apache.poi.xwpf.usermodel.Borders;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;
import org.xhtmlrenderer.pdf.ITextRenderer;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ReportRenderService {

    public byte[] renderPdf(String html) {
        try {
            String xhtml = toXhtml(html);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ITextRenderer renderer = new ITextRenderer();
            renderer.setDocumentFromString(xhtml);
            renderer.layout();
            renderer.createPDF(out);
            return out.toByteArray();
        } catch (Exception e) {
            log.error("PDF rendering failed", e);
            throw new RuntimeException("PDF 生成失败: " + e.getMessage(), e);
        }
    }

    public byte[] renderWord(String html) {
        try {
            Document doc = Jsoup.parse(html);
            XWPFDocument document = new XWPFDocument();

            for (Element body : doc.select("body")) {
                processElement(document, body);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.write(out);
            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Word rendering failed", e);
            throw new RuntimeException("Word 生成失败: " + e.getMessage(), e);
        }
    }

    private void processElement(XWPFDocument document, Element element) {
        for (Node child : element.childNodes()) {
            if (child instanceof TextNode textNode) {
                String text = textNode.text().trim();
                if (!text.isEmpty()) {
                    XWPFParagraph p = document.createParagraph();
                    p.createRun().setText(text);
                }
            } else if (child instanceof Element el) {
                String tag = el.tagName().toLowerCase();
                switch (tag) {
                    case "h1" -> addHeading(document, el.text(), 1);
                    case "h2" -> addHeading(document, el.text(), 2);
                    case "h3" -> addHeading(document, el.text(), 3);
                    case "h4", "h5", "h6" -> addHeading(document, el.text(), 4);
                    case "p" -> addParagraph(document, el);
                    case "ul", "ol" -> addList(document, el, tag.equals("ol"));
                    case "table" -> addTable(document, el);
                    case "div", "section", "article", "header", "main", "footer" ->
                        processElement(document, el);
                    case "br" -> document.createParagraph();
                    case "hr" -> {
                        XWPFParagraph hr = document.createParagraph();
                        hr.setBorderBottom(Borders.SINGLE);
                    }
                    default -> {
                        String text = el.text().trim();
                        if (!text.isEmpty()) {
                            XWPFParagraph p = document.createParagraph();
                            p.createRun().setText(text);
                        }
                    }
                }
            }
        }
    }

    private void addHeading(XWPFDocument document, String text, int level) {
        XWPFParagraph p = document.createParagraph();
        p.setStyle("Heading" + level);
        XWPFRun run = p.createRun();
        run.setText(text);
        run.setBold(true);
        int fontSize = switch (level) {
            case 1 -> 22;
            case 2 -> 18;
            case 3 -> 14;
            default -> 12;
        };
        run.setFontSize(fontSize);
    }

    private void addParagraph(XWPFDocument document, Element el) {
        XWPFParagraph p = document.createParagraph();
        for (Node child : el.childNodes()) {
            if (child instanceof TextNode textNode) {
                String text = textNode.text();
                if (!text.trim().isEmpty()) {
                    p.createRun().setText(text);
                }
            } else if (child instanceof Element childEl) {
                XWPFRun run = p.createRun();
                run.setText(childEl.text());
                String childTag = childEl.tagName().toLowerCase();
                if ("strong".equals(childTag) || "b".equals(childTag)) {
                    run.setBold(true);
                } else if ("em".equals(childTag) || "i".equals(childTag)) {
                    run.setItalic(true);
                }
            }
        }
    }

    private void addList(XWPFDocument document, Element el, boolean ordered) {
        int idx = 1;
        for (Element li : el.select("> li")) {
            XWPFParagraph p = document.createParagraph();
            p.setIndentationLeft(720);
            String prefix = ordered ? (idx++ + ". ") : "• ";
            p.createRun().setText(prefix + li.text());
        }
    }

    private void addTable(XWPFDocument document, Element tableEl) {
        Elements rows = tableEl.select("tr");
        if (rows.isEmpty())
            return;

        int colCount = 0;
        for (Element row : rows) {
            int c = row.select("td, th").size();
            if (c > colCount)
                colCount = c;
        }
        if (colCount == 0)
            return;

        XWPFTable table = document.createTable(rows.size(), colCount);
        table.setWidth("100%");

        for (int i = 0; i < rows.size(); i++) {
            Elements cells = rows.get(i).select("td, th");
            XWPFTableRow row = table.getRow(i);
            for (int j = 0; j < cells.size(); j++) {
                XWPFTableCell cell = row.getCell(j);
                cell.setText(cells.get(j).text());
                if (cells.get(j).tagName().equalsIgnoreCase("th")) {
                    cell.getParagraphs().get(0).getRuns().forEach(r -> r.setBold(true));
                }
            }
        }
    }

    private static final String REPORT_CSS = """
            body { font-family: SimSun, serif; font-size: 12pt; line-height: 1.6; margin: 40px; }
            h1 { font-size: 22pt; text-align: center; margin-bottom: 20px; }
            h2 { font-size: 16pt; margin-top: 20px; border-bottom: 1px solid #333; padding-bottom: 5px; }
            h3 { font-size: 13pt; margin-top: 15px; }
            table { border-collapse: collapse; width: 100%; margin: 10px 0; }
            th, td { border: 1px solid #666; padding: 6px 10px; text-align: left; }
            th { background-color: #f0f0f0; font-weight: bold; }
            ul, ol { padding-left: 20px; }
            .cover { text-align: center; margin-top: 200px; }
            .cover h1 { font-size: 28pt; }
            """;

    private String toXhtml(String html) {
        Document doc = Jsoup.parse(html);
        doc.outputSettings()
                .syntax(Document.OutputSettings.Syntax.xml)
                .charset("UTF-8");

        // 始终注入报告样式
        doc.head().appendElement("style").attr("type", "text/css").text(REPORT_CSS);

        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\"\n"
                + "  \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">\n"
                + doc.html();
    }
}
