package com.achievement.rag.extract;

import com.achievement.domain.vo.AchTypeDef;
import com.achievement.rag.model.ExtractedFieldValue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 基于正则的字段提取器。
 * <p>
 * 根据成果类型的动态字段定义（AchTypeDef），从清洗后的文本中提取每个字段的值。
 * 内置常用字段的 regex pattern；未匹配时返回空值 + 低置信度。
 * </p>
 */
@Slf4j
@Component
public class FieldExtractor {

    // ========== 预定义正则 pattern ==========

    /** DOI */
    private static final Pattern DOI = Pattern.compile("10\\.\\d{4,}/[\\S]+");
    /** 中文/英文论文标题探测（"论文题目：xxx" or "Title：xxx"） */
    private static final Pattern TITLE_LABEL = Pattern.compile(
            "(?:论文题目|论文标题|题目|标题|成果名称|报告题目)[：:\\s]*(.+?)(?:\\n|$)", Pattern.CASE_INSENSITIVE);
    /** 作者/发明人 */
    private static final Pattern AUTHORS_LABEL = Pattern.compile(
            "(?:作者|作者姓名|著者|发明人|设计人)[：:\\s]*(.+?)(?:\\n|$)", Pattern.CASE_INSENSITIVE);
    /** 摘要 */
    private static final Pattern ABSTRACT_LABEL = Pattern.compile(
            "(?:摘要|摘　要|内容提要|内容摘要|[Aa]bstract)[：:\\s]*(.+?)(?:\\n{2,}|关键词|$)", Pattern.CASE_INSENSITIVE);
    /** 关键词 */
    private static final Pattern KEYWORDS_LABEL = Pattern.compile(
            "(?:关键词|关 键 词|关键字|主题词|[Kk]eywords?)[：:\\s]*(.+?)(?:\\n{2,}|$)", Pattern.CASE_INSENSITIVE);
    /** 期刊名称 */
    private static final Pattern JOURNAL_LABEL = Pattern.compile(
            "(?:期刊名称|刊名|期刊|杂志|学报|会议名称|会议)[：:\\s]*(.+?)(?:\\n|$)", Pattern.CASE_INSENSITIVE);
    /** 日期（2024-01-01 / 2024年1月1日 / 2024.01.01） */
    private static final Pattern DATE = Pattern.compile(
            "(\\d{4}[-年.]\\d{1,2}[-月.]\\d{1,2}日?)");
    /** 年份（2024） */
    private static final Pattern YEAR = Pattern.compile("(\\d{4})");
    /** 页码（pp. 1-10 / pp 1-10 / pp1-10 / 第1-10页） */
    private static final Pattern PAGES = Pattern.compile(
            "(?:pp?\\.?\\s*)?(\\d+)\\s*-\\s*(\\d+)(?:\\s*页)?", Pattern.CASE_INSENSITIVE);
    /** 专利号（CN 开头） */
    private static final Pattern PATENT_NUMBER = Pattern.compile(
            "(CN\\d{7,13}[A-Z]?\\d?)", Pattern.CASE_INSENSITIVE);
    /** 专利类型（发明/实用新型/外观设计） */
    private static final Pattern PATENT_TYPE = Pattern.compile(
            "(发明|实用新型|外观设计)专利?");
    /** 申请人 */
    private static final Pattern APPLICANT_LABEL = Pattern.compile(
            "(?:申请人|专利权人|申请（专利权）人)[：:\\s]*(.+?)(?:\\n|$)", Pattern.CASE_INSENSITIVE);
    /** IPC 分类号 */
    private static final Pattern IPC_CLASS = Pattern.compile(
            "Int\\.?\\s*Cl\\.?\\s*[：:]?\\s*([A-Z]\\d{2}[\\d.]*[A-Z]?\\d*/?\\d*)", Pattern.CASE_INSENSITIVE);
    /** 申请日 */
    private static final Pattern APPL_DATE_LABEL = Pattern.compile(
            "(?:申请日|申请日期)[：:\\s]*(.+?)(?:\\n|$)", Pattern.CASE_INSENSITIVE);
    /** 公开日 */
    private static final Pattern PUB_DATE_LABEL = Pattern.compile(
            "(?:公开日|公开日期|公布日|公告日)[：:\\s]*(.+?)(?:\\n|$)", Pattern.CASE_INSENSITIVE);
    /** ISBN/ISBN号 */
    private static final Pattern ISBN = Pattern.compile(
            "ISBN\\s*[：:]?\\s*([\\d-]{10,17})", Pattern.CASE_INSENSITIVE);
    /** ISSN */
    private static final Pattern ISSN = Pattern.compile(
            "ISSN\\s*[：:]?\\s*(\\d{4}-?\\d{3}[\\dxX])", Pattern.CASE_INSENSITIVE);
    /** 基金信息 */
    private static final Pattern FUND_LABEL = Pattern.compile(
            "(?:基金|资助项目|项目编号)[：:\\s]*(.+?)(?:\\n|$)", Pattern.CASE_INSENSITIVE);
    /** 卷号/期号 */
    private static final Pattern VOL_ISSUE = Pattern.compile(
            "(?:卷|Vol\\.?)[\\s:]*?(\\d+)[,\\s]*(?:(?:期|No\\.?|Issue)[\\s:]*?(\\d+))?", Pattern.CASE_INSENSITIVE);
    /** 单位/机构 */
    private static final Pattern ORG_LABEL = Pattern.compile(
            "(?:单位|工作单位|所属机构|作者单位|作者机构)[：:\\s]*(.+?)(?:\\n|$)", Pattern.CASE_INSENSITIVE);


    /**
     * 提取字段——对每个 fieldDef 尝试从 text 中取值
     *
     * @param text        清洗后的文本
     * @param fieldDefs   该成果类型的字段定义列表
     * @return 提取结果列表
     */
    public List<ExtractedFieldValue> extract(String text, List<AchTypeDef> fieldDefs) {
        if (text == null || text.isBlank() || fieldDefs == null || fieldDefs.isEmpty()) {
            return Collections.emptyList();
        }

        List<ExtractedFieldValue> results = new ArrayList<>();

        for (AchTypeDef field : fieldDefs) {
            String value = matchByFieldCode(text, field.getFieldCode());
            double confidence = 0.0;

            if (value != null && !value.isBlank()) {
                confidence = calculateConfidence(value, field);
            }

            results.add(ExtractedFieldValue.builder()
                    .fieldCode(field.getFieldCode())
                    .fieldName(field.getFieldName())
                    .fieldType(field.getFieldType())
                    .extractedValue(value != null ? value.trim() : "")
                    .confidence(confidence)
                    .extractMethod(value != null ? "regex" : "")
                    .build());
        }

        log.info("Field extraction completed: {} fields extracted from {} field defs",
                results.stream().filter(f -> f.getConfidence() > 0).count(), fieldDefs.size());

        return results;
    }

    /**
     * 根据 fieldCode 匹配文本
     */
    private String matchByFieldCode(String text, String fieldCode) {
        if (fieldCode == null) return null;

        return switch (fieldCode.toLowerCase()) {
            // ===== 通用字段 =====
            case "title", "projectname", "achievementname",
                 "reporttitle", "thesistitle", "name" -> {
                Matcher m = TITLE_LABEL.matcher(text);
                yield m.find() ? m.group(1).trim() : extractFirstLine(text);
            }
            case "author", "authors", "authorlist", "author_name" -> {
                Matcher m = AUTHORS_LABEL.matcher(text);
                yield m.find() ? m.group(1).trim() : null;
            }
            case "abstract", "summary", "description", "contentabstract" -> {
                Matcher m = ABSTRACT_LABEL.matcher(text);
                yield m.find() ? m.group(1).trim() : null;
            }
            case "keyword", "keywords", "keywordlist" -> {
                Matcher m = KEYWORDS_LABEL.matcher(text);
                yield m.find() ? m.group(1).trim() : null;
            }
            case "publicationdate", "pubdate", "publishdate",
                 "date", "year" -> matchDate(text);

            // ===== 论文专有 =====
            case "journal", "journalname", "conference", "conferencename",
                 "publicationname", "source" -> {
                Matcher m = JOURNAL_LABEL.matcher(text);
                yield m.find() ? m.group(1).trim() : null;
            }
            case "doi" -> {
                Matcher m = DOI.matcher(text);
                yield m.find() ? m.group(0).trim() : null;
            }
            case "volume", "vol" -> matchVolume(text);
            case "issue", "number", "no" -> matchIssue(text);
            case "pages", "page", "pagecount" -> {
                Matcher m = PAGES.matcher(text);
                yield m.find() ? m.group(1) + "-" + m.group(2) : null;
            }
            case "isbn" -> {
                Matcher m = ISBN.matcher(text);
                yield m.find() ? m.group(1).trim() : null;
            }
            case "issn" -> {
                Matcher m = ISSN.matcher(text);
                yield m.find() ? m.group(1).trim() : null;
            }
            case "fund", "fundinfo", "funding" -> {
                Matcher m = FUND_LABEL.matcher(text);
                yield m.find() ? m.group(1).trim() : null;
            }
            case "organization", "affiliation", "unit", "institution" -> {
                Matcher m = ORG_LABEL.matcher(text);
                yield m.find() ? m.group(1).trim() : null;
            }

            // ===== 专利专有 =====
            case "patentnumber", "patentno", "applicationnumber",
                 "appnumber", "patent_id" -> {
                Matcher m = PATENT_NUMBER.matcher(text);
                yield m.find() ? m.group(1).trim() : null;
            }
            case "patenttype", "patentcategory" -> {
                Matcher m = PATENT_TYPE.matcher(text);
                yield m.find() ? m.group(1).trim() : null;
            }
            case "applicant", "patentee", "applicant_name" -> {
                Matcher m = APPLICANT_LABEL.matcher(text);
                yield m.find() ? m.group(1).trim() : null;
            }
            case "ipc", "ipcclass", "classification",
                 "ipcclassfication" -> {
                Matcher m = IPC_CLASS.matcher(text);
                yield m.find() ? m.group(1).trim() : null;
            }
            case "applicationdate", "appdate", "applydate" -> {
                Matcher m = APPL_DATE_LABEL.matcher(text);
                yield m.find() ? m.group(1).trim() : null;
            }
            case "publicationdate_patent", "pubdate_patent", "opendate" -> {
                Matcher m = PUB_DATE_LABEL.matcher(text);
                yield m.find() ? m.group(1).trim() : null;
            }

            // ===== 未知字段 =====
            default -> null;
        };
    }

    private String matchDate(String text) {
        Matcher m = DATE.matcher(text);
        return m.find() ? m.group(1).trim() : null;
    }

    private String matchVolume(String text) {
        Matcher m = VOL_ISSUE.matcher(text);
        return m.find() ? m.group(1).trim() : null;
    }

    private String matchIssue(String text) {
        Matcher m = VOL_ISSUE.matcher(text);
        return m.find() && m.group(2) != null ? m.group(2).trim() : null;
    }

    /** 取第一段非空行作为标题 */
    private String extractFirstLine(String text) {
        String[] lines = text.split("\n", -1);
        for (String line : lines) {
            String t = line.trim();
            if (!t.isEmpty() && t.length() < 200) {
                return t;
            }
        }
        return null;
    }

    /**
     * 计算置信度——有值且非空时 0.6（有标签匹配加分）
     */
    private double calculateConfidence(String value, AchTypeDef field) {
        if (value == null || value.isBlank()) return 0.0;
        // 默认基础分
        double score = 0.6;
        // 值越短，置信度越低（可能是误匹配）
        if (value.length() < 3) score -= 0.2;
        if (value.length() > 100) score = Math.min(score + 0.1, 1.0);
        // fieldType 为 date 时校验格式
        if ("date".equals(field.getFieldType())) {
            score = value.matches(".*\\d{4}[-年].*") ? 0.8 : 0.3;
        }
        return Math.min(score, 1.0);
    }
}
