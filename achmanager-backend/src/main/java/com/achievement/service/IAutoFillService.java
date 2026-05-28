package com.achievement.service;

import org.springframework.web.multipart.MultipartFile;

import com.achievement.domain.vo.AutoFillResultVO;

public interface IAutoFillService {

    AutoFillResultVO recognizeFields(MultipartFile file, String resultTypeCode);
}
