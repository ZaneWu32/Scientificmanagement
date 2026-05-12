package com.achievement.service;

import java.util.List;

import com.achievement.domain.vo.ReportTaskVO;

public interface IReportService {
    String startGeneration(List<String> achievementDocIds);

    ReportTaskVO getTaskStatus(String taskId);

    String getTaskContent(String taskId);

    byte[] exportReport(String taskId, String format, String editedHtml);
}
