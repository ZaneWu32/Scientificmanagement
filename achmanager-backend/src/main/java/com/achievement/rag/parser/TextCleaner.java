package com.achievement.rag.parser;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * 文本清洗器——去除噪声、统一格式
 */
@Slf4j
@Component
public class TextCleaner {

    /** 控制字符（保留 \t \n \r） */
    private static final Pattern CONTROL_CHARS = Pattern.compile("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]");
    /** 连续 3 个以上空行 → 2 个 */
    private static final Pattern MULTI_BLANK_LINES = Pattern.compile("\n{3,}");
    /** 连续 2 个以上空格 → 1 个 */
    private static final Pattern MULTI_SPACES = Pattern.compile("[ ]{2,}");
    /** BOM / 零宽字符 */
    private static final Pattern ZERO_WIDTH = Pattern.compile("[\\uFEFF\\u200B\\u200C\\u200D\\u2060]");

    public String clean(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            return "";
        }

        String text = rawText;

        // 1. 统一换行符
        text = text.replace("\r\n", "\n").replace("\r", "\n");

        // 2. 去除控制字符
        text = CONTROL_CHARS.matcher(text).replaceAll("");

        // 3. 去除 BOM / 零宽字符
        text = ZERO_WIDTH.matcher(text).replaceAll("");

        // 4. 多个连续空行 → 最多 2 个
        text = MULTI_BLANK_LINES.matcher(text).replaceAll("\n\n");

        // 5. 多个连续空格 → 1 个
        text = MULTI_SPACES.matcher(text).replaceAll(" ");

        // 6. 去除每行首尾空白
        StringBuilder sb = new StringBuilder(text.length());
        String[] lines = text.split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            if (i > 0) sb.append('\n');
            sb.append(lines[i].trim());
        }

        return sb.toString().trim();
    }
}
