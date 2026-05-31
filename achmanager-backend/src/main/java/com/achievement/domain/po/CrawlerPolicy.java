package com.achievement.domain.po;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("crawler_policies")
public class CrawlerPolicy implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String crawlerId;

    private String sourceUrl;

    private String title;

    private String publishDate;

    private LocalDate publishDateParsed;

    private String content;

    private String hrefs;

    private String contentHash;

    private String keywordsExtracted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private String delFlag;
}
