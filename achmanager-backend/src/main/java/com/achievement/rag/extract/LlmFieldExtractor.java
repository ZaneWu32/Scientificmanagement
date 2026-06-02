package com.achievement.rag.extract;

import com.achievement.client.LlmClient;
import com.achievement.constant.LlmUsage;
import com.achievement.domain.ChatMessage;
import com.achievement.domain.vo.AchTypeDef;
import com.achievement.rag.model.ExtractedFieldValue;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Pattern;

/**
 * 基于大模型的字段提取器。
 * <p>
 * 将清洗后的文本 + 动态字段定义构造为 prompt，调用 LLM 提取字段值。
 * LLM 不可用时自动降级到正则提取器 {@link FieldExtractor}。
 * </p>
 */
@Slf4j
@Component
public class LlmFieldExtractor {

    /** 单次传给 LLM 的最大文档字符数（过长则截断） */
    private static final int MAX_DOC_TEXT_LENGTH = 8000;

    private static final Pattern JSON_BLOCK = Pattern.compile(
            "(?s)```(?:json)?\\s*\\{([\\s\\S]*?)\\}```");
    private static final Pattern JSON_OBJECT = Pattern.compile(
            "(?s)\\{\\s*\"extracted_fields\"[\\s\\S]*?\\}");

    private final LlmClient llmClient;
    private final ObjectMapper objectMapper;
    private final FieldExtractor regexFallback;

    public LlmFieldExtractor(LlmClient llmClient, ObjectMapper objectMapper,
                              FieldExtractor regexFallback) {
        this.llmClient = llmClient;
        this.objectMapper = objectMapper;
        this.regexFallback = regexFallback;
    }

    /**
     * 提取字段——优先使用 LLM，失败时降级到正则
     *
     * @param text     清洗后的文档文本
     * @param fieldDefs 字段定义列表
     * @return 提取结果列表（顺序与 fieldDefs 一致）
     */
    public List<ExtractedFieldValue> extract(String text, List<AchTypeDef> fieldDefs) {
        if (text == null || text.isBlank() || fieldDefs == null || fieldDefs.isEmpty()) {
            return Collections.emptyList();
        }

        try {
            String prompt = buildPrompt(text, fieldDefs);
            List<ChatMessage> messages = List.of(
                    ChatMessage.system("你是一个文档字段提取专家。根据用户提供的字段定义，从文档文本中提取对应字段的值。"
                            + "只返回JSON格式的结果，不要任何额外说明文字。"),
                    ChatMessage.user(prompt)
            );

            String rawResponse = llmClient.chatCompletion(LlmUsage.DEFAULT, messages);
            String jsonContent = extractJsonContent(rawResponse);

            if (jsonContent != null) {
                List<ExtractedFieldValue> extracted = parseResponse(jsonContent, fieldDefs);
                if (extracted != null && !extracted.isEmpty()) {
                    long filledCount = extracted.stream()
                            .filter(f -> f.getExtractedValue() != null && !f.getExtractedValue().isBlank())
                            .count();
                    log.info("LLM extraction successful: {}/{} fields filled", filledCount, extracted.size());
                    return extracted;
                }
            }
            log.warn("LLM returned empty result, falling back to regex");
        } catch (Exception e) {
            log.warn("LLM extraction failed ({}), falling back to regex", e.getMessage());
        }

        return regexFallback.extract(text, fieldDefs);
    }

    // ==================== Prompt 构造 ====================

    private String buildPrompt(String text, List<AchTypeDef> fieldDefs) {
        StringBuilder sb = new StringBuilder();

        // 文档内容（截断）
        sb.append("## 文档内容\n\n");
        String docText = text.length() > MAX_DOC_TEXT_LENGTH
                ? text.substring(0, MAX_DOC_TEXT_LENGTH) + "\n\n...（文档过长，已截断）"
                : text;
        sb.append(docText).append("\n\n");

        // 字段定义 + 示例输出模板
        sb.append("## 需要提取的字段\n\n");
        sb.append("请从以上文档中提取以下字段的值，严格按照下方 JSON 格式返回：\n\n");

        sb.append("{\n  \"extracted_fields\": [\n");
        for (int i = 0; i < fieldDefs.size(); i++) {
            AchTypeDef def = fieldDefs.get(i);
            sb.append("    {\"field_code\": \"").append(escape(def.getFieldCode())).append("\"");
            sb.append(", \"value\": \"\"");
            sb.append(", \"confidence\": 0}");
            if (i < fieldDefs.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append("  ]\n}\n\n");

        // 字段说明
        sb.append("### 字段说明\n\n");
        for (AchTypeDef def : fieldDefs) {
            sb.append("- **").append(escape(def.getFieldCode())).append("**");
            sb.append("（").append(escape(def.getFieldName())).append("）");
            sb.append(" 类型：").append(def.getFieldType());
            if (def.getDescription() != null && !def.getDescription().isBlank()) {
                sb.append("，说明：").append(escape(def.getDescription()));
            }
            sb.append("\n");
        }

        sb.append("\n### 提取规则\n");
        sb.append("1. 根据字段的中文名称和说明，在文档中查找对应的值\n");
        sb.append("2. 如果某字段在文档中未找到，value 设为空字符串 \"\"，confidence 设为 0\n");
        sb.append("3. confidence 取值 0~1，表示对该字段提取结果的置信度\n");
        sb.append("4. 只返回上述 JSON 格式，不要添加任何其他文字\n");

        return sb.toString();
    }

    // ==================== 响应解析 ====================

    /**
     * 从 LLM API 返回的原始响应中提取 JSON 内容
     */
    private String extractJsonContent(String rawResponse) {
        if (rawResponse == null || rawResponse.isBlank()) return null;

        // 尝试解析为标准 OpenAI 格式 { choices: [{ message: { content: "..." } }] }
        try {
            var root = objectMapper.readTree(rawResponse);
            var choices = root.get("choices");
            if (choices != null && choices.isArray() && !choices.isEmpty()) {
                var message = choices.get(0).get("message");
                if (message != null && message.has("content")) {
                    String content = message.get("content").asText();
                    if (content != null && !content.isBlank()) {
                        return extractJsonBlock(content);
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Response is not OpenAI format, trying as raw JSON: {}", e.getMessage());
        }

        // 直接尝试从响应体中提取 JSON
        return extractJsonBlock(rawResponse);
    }

    /**
     * 从文本中提取 JSON 块（支持 ```json 代码块 和 裸 JSON）
     */
    private String extractJsonBlock(String text) {
        // ```json ... ``` 代码块
        var m = JSON_BLOCK.matcher(text);
        if (m.find()) {
            return "{" + m.group(1) + "}";
        }

        // 裸 JSON 对象
        m = JSON_OBJECT.matcher(text);
        if (m.find()) {
            return m.group();
        }

        return null;
    }

    /**
     * 将 LLM 返回的 JSON 解析为 ExtractedFieldValue 列表
     */
    private List<ExtractedFieldValue> parseResponse(String json, List<AchTypeDef> fieldDefs) {
        try {
            var root = objectMapper.readTree(json);
            var fieldsArray = root.get("extracted_fields");
            if (fieldsArray == null || !fieldsArray.isArray()) {
                return Collections.emptyList();
            }

            // 按 fieldDefs 顺序构建初始结果（空值）
            Map<String, AchTypeDef> defMap = new LinkedHashMap<>();
            Map<String, ExtractedFieldValue> resultMap = new LinkedHashMap<>();
            for (AchTypeDef def : fieldDefs) {
                defMap.put(def.getFieldCode(), def);
                resultMap.put(def.getFieldCode(), ExtractedFieldValue.builder()
                        .fieldCode(def.getFieldCode())
                        .fieldName(def.getFieldName())
                        .fieldType(def.getFieldType())
                        .extractedValue("")
                        .confidence(0.0)
                        .extractMethod("")
                        .build());
            }

            // 填入 LLM 提取到的值
            for (var node : fieldsArray) {
                String code = node.has("field_code") ? node.get("field_code").asText() : null;
                if (code == null || !resultMap.containsKey(code)) continue;

                String value = node.has("value") ? node.get("value").asText("") : "";
                double conf = node.has("confidence") ? node.get("confidence").asDouble(0.0) : 0.0;
                AchTypeDef def = defMap.get(code);

                resultMap.put(code, ExtractedFieldValue.builder()
                        .fieldCode(code)
                        .fieldName(def != null ? def.getFieldName() : code)
                        .fieldType(def != null ? def.getFieldType() : "string")
                        .extractedValue(value != null ? value : "")
                        .confidence(conf)
                        .extractMethod(conf > 0 ? "llm" : "")
                        .build());
            }

            return new ArrayList<>(resultMap.values());
        } catch (Exception e) {
            log.warn("Failed to parse LLM JSON response: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
