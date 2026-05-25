/**
 * 成果物自动补全 Mock 数据与 Mock Service
 * 模拟 AI 从附件中识别字段的结果，供甲方演示使用
 */

import type { AutoFillField, AutoFillResult } from '@/api/autoFill'

// ── Mock 识别结果（论文场景）────────────────────────────────────

const paperAutoFillResult: AutoFillResult = {
  fileId: 'mock-file-paper-001',
  fileName: '面向连续生产线的织物缺陷智能检测方法.pdf',
  recognizedAt: new Date().toISOString(),
  fields: [
    {
      key: 'title',
      label: '成果标题',
      value: '面向连续生产线的织物缺陷智能检测方法',
      confidence: 0.97,
      sourceSnippet: '（文档标题第1行）面向连续生产线的织物缺陷智能检测方法',
      needsConfirm: false
    },
    {
      key: 'authors',
      label: '作者',
      value: '陈明; 王蕾; 刘佳',
      confidence: 0.95,
      sourceSnippet: '（摘要页作者栏）陈明¹, 王蕾², 刘佳¹',
      needsConfirm: false
    },
    {
      key: 'year',
      label: '年份',
      value: '2025',
      confidence: 0.99,
      sourceSnippet: '（页眉）© 2025 IEEE. Published in...',
      needsConfirm: false
    },
    {
      key: 'abstract',
      label: '摘要',
      value: '本文提出一种面向连续生产线的织物缺陷智能检测方法，通过多尺度特征融合与轻量化边缘推理网络，实现对断经、纬斜、污渍等常见织物缺陷的实时识别与定位，在桐乡地区某纺织企业生产线上验证，检测准确率达97.3%，推理延迟低于15ms。',
      confidence: 0.96,
      sourceSnippet: '（Abstract 第1段）This paper proposes an intelligent detection method...',
      needsConfirm: false
    },
    {
      key: 'keywords',
      label: '关键词',
      value: '机器视觉; 缺陷检测; 纺织制造; 边缘计算; 轻量化网络',
      confidence: 0.94,
      sourceSnippet: '（Index Terms）Machine Vision, Defect Detection, Textile Manufacturing...',
      needsConfirm: false
    },
    {
      key: 'doi',
      label: 'DOI',
      value: '10.1109/TASE.2025.1234567',
      confidence: 0.99,
      sourceSnippet: '（页脚）DOI: 10.1109/TASE.2025.1234567',
      needsConfirm: false
    },
    {
      key: 'journalName',
      label: '期刊/会议名称',
      value: 'IEEE Transactions on Automation Science and Engineering',
      confidence: 0.98,
      sourceSnippet: '（页眉）IEEE Transactions on Automation Science and Engineering',
      needsConfirm: false
    },
    {
      key: 'institution',
      label: '所属单位',
      value: '智能制造研究所',
      confidence: 0.72,
      sourceSnippet: '（作者信息）¹ Institute of Intelligent Manufacturing... 桐乡',
      needsConfirm: true
    },
    {
      key: 'fundingSource',
      label: '资助来源',
      value: '国家自然科学基金(62201xxx)、浙江省科技厅重点研发项目',
      confidence: 0.81,
      sourceSnippet: '（致谢）This work was supported by NSFC (62201xxx) and...',
      needsConfirm: true
    }
  ],
  pendingConfirmations: [
    '所属单位识别置信度较低（72%），请确认是"智能制造研究所"还是其他院系',
    '资助来源中项目编号可能有误，请与原始文件核对',
    '系统未能从附件中确认成果是否已正式发表，请手动确认出版状态'
  ]
}

// ── Mock 识别结果（软著场景）────────────────────────────────────

const softwareAutoFillResult: AutoFillResult = {
  fileId: 'mock-file-software-001',
  fileName: '工业视觉边缘推理网关及部署方法_软著申请书.docx',
  recognizedAt: new Date().toISOString(),
  fields: [
    {
      key: 'title',
      label: '成果标题',
      value: '工业视觉边缘推理网关软件 V1.0',
      confidence: 0.98,
      sourceSnippet: '（封面）软件名称：工业视觉边缘推理网关软件 V1.0',
      needsConfirm: false
    },
    {
      key: 'authors',
      label: '著作权人',
      value: '王蕾; 张辉',
      confidence: 0.96,
      sourceSnippet: '（著作权人信息）王蕾、张辉',
      needsConfirm: false
    },
    {
      key: 'year',
      label: '首次发表年份',
      value: '2024',
      confidence: 0.97,
      sourceSnippet: '（开发完成日期）2024年2月11日',
      needsConfirm: false
    },
    {
      key: 'registrationNumber',
      label: '登记号',
      value: '2024SR0XXXXXX（待确认）',
      confidence: 0.60,
      sourceSnippet: '（申请书附页）登记号一栏为空白，系统无法识别',
      needsConfirm: true
    },
    {
      key: 'softwareType',
      label: '软件类型',
      value: '应用软件',
      confidence: 0.95,
      sourceSnippet: '（分类）√ 应用软件 □ 系统软件 □ 支撑软件',
      needsConfirm: false
    },
    {
      key: 'abstract',
      label: '功能简介',
      value: '本软件实现工业视觉推理网关功能，支持多路视频流接入、模型热更新和本地告警推送，适用于工业产线边缘部署场景。',
      confidence: 0.93,
      sourceSnippet: '（软件简介）本软件通过边缘计算节点实现...',
      needsConfirm: false
    }
  ],
  pendingConfirmations: [
    '登记号识别失败，请在软著证书到达后手动补全',
    '著作权归属需确认是个人还是单位（申请书填写为个人）'
  ]
}

// ── Mock Service（模拟网络延迟）──────────────────────────────────

/**
 * 模拟从附件识别字段
 * 会根据文件扩展名选择不同的演示数据
 */
export async function mockAutoFillFromAttachment(
  fileName: string,
  _resultTypeCode?: string
): Promise<AutoFillResult> {
  // 模拟网络延迟
  await delay(2200)

  // 根据文件名猜测场景（演示用）
  const lowerName = fileName.toLowerCase()
  if (lowerName.includes('软著') || lowerName.includes('software') || lowerName.endsWith('.docx')) {
    return { ...softwareAutoFillResult, fileName }
  }

  // 默认返回论文场景
  return { ...paperAutoFillResult, fileName }
}

function delay(ms: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms))
}

// ── 导出演示配置 ─────────────────────────────────────────────

/** 给字段置信度打上颜色标记 */
export function getConfidenceLevel(confidence: number): 'high' | 'medium' | 'low' {
  if (confidence >= 0.90) return 'high'
  if (confidence >= 0.75) return 'medium'
  return 'low'
}

export function getConfidenceText(confidence: number): string {
  return `${Math.round(confidence * 100)}%`
}
