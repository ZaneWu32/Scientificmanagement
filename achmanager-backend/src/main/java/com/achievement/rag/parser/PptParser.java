package com.achievement.rag.parser;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.hslf.usermodel.HSLFShape;
import org.apache.poi.hslf.usermodel.HSLFSlide;
import org.apache.poi.hslf.usermodel.HSLFSlideShow;
import org.apache.poi.hslf.usermodel.HSLFTextShape;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * PPT 文档解析器（支持 .ppt 和 .pptx）
 * - 提取每张幻灯片的文本内容
 */
@Slf4j
@Component
public class PptParser {

    public ParsedDocument parse(byte[] content, String fileName, boolean isPptx) throws IOException {
        return isPptx ? parsePptx(content, fileName) : parsePpt(content, fileName);
    }

    private ParsedDocument parsePpt(byte[] content, String fileName) throws IOException {
        List<ParsedDocument.TextSegment> segments = new ArrayList<>();

        try (HSLFSlideShow slideShow = new HSLFSlideShow(new ByteArrayInputStream(content))) {
            StringBuilder rawSb = new StringBuilder();
            int segIdx = 0;

            for (HSLFSlide slide : slideShow.getSlides()) {
                rawSb.append("--- Slide ").append(slide.getSlideNumber()).append(" ---\n");
                segments.add(ParsedDocument.TextSegment.builder()
                        .index(segIdx++)
                        .heading("Slide " + slide.getSlideNumber())
                        .headingLevel(1)
                        .content("")
                        .build());

                for (HSLFShape shape : slide.getShapes()) {
                    if (shape instanceof HSLFTextShape textShape) {
                        String text = textShape.getText();
                        if (!text.isBlank()) {
                            rawSb.append(text).append('\n');
                            segments.add(ParsedDocument.TextSegment.builder()
                                    .index(segIdx++)
                                    .content(text)
                                    .build());
                        }
                    }
                }
                rawSb.append('\n');
            }

            log.info("PPT parsed: {} slides", slideShow.getSlides().size());

            return ParsedDocument.builder()
                    .fileName(fileName)
                    .mimeType("application/vnd.ms-powerpoint")
                    .fileSize(content.length)
                    .rawText(rawSb.toString())
                    .segments(segments)
                    .build();
        }
    }

    private ParsedDocument parsePptx(byte[] content, String fileName) throws IOException {
        List<ParsedDocument.TextSegment> segments = new ArrayList<>();

        try (XMLSlideShow slideShow = new XMLSlideShow(new ByteArrayInputStream(content))) {
            StringBuilder rawSb = new StringBuilder();
            int segIdx = 0;

            for (XSLFSlide slide : slideShow.getSlides()) {
                rawSb.append("--- Slide ").append(slide.getSlideNumber()).append(" ---\n");
                segments.add(ParsedDocument.TextSegment.builder()
                        .index(segIdx++)
                        .heading("Slide " + slide.getSlideNumber())
                        .headingLevel(1)
                        .content("")
                        .build());

                for (XSLFShape shape : slide.getShapes()) {
                    if (shape instanceof XSLFTextShape textShape) {
                        String text = textShape.getText();
                        if (!text.isBlank()) {
                            rawSb.append(text).append('\n');
                            segments.add(ParsedDocument.TextSegment.builder()
                                    .index(segIdx++)
                                    .content(text)
                                    .build());
                        }
                    }
                }
                rawSb.append('\n');
            }

            log.info("PPTX parsed: {} slides", slideShow.getSlides().size());

            return ParsedDocument.builder()
                    .fileName(fileName)
                    .mimeType("application/vnd.openxmlformats-officedocument.presentationml.presentation")
                    .fileSize(content.length)
                    .rawText(rawSb.toString())
                    .segments(segments)
                    .build();
        }
    }
}
