# 过程系统对接 API（代码对齐版）

- 文档版本：v2.1（审核修订）
- 更新时间：2026-02-09
- 后端代码基准：`com.achievement.controller.ProcessSystemController`
- 接口前缀：`/api/v1/process-system`

## 1. 使用范围

本文档用于“过程成果系统 -> 成果管理后端（achmanager-backend）”联调。

请注意：本接口是过程系统专用接口，和 Strapi 内容管理 API 不是同一套协议。

## 2. 环境与地址

当前项目配置（`application.yaml`）中服务端口为 `8081`，因此本地联调地址为：

- `http://localhost:8081/api/v1/process-system`

生产/测试环境请按部署网关地址替换主机与端口，路径前缀保持不变。

## 3. 鉴权与签名

### 3.1 必填请求头

所有接口（包含 `GET`）都经过过滤器校验，必须带以下头：

- `X-API-Key` 或 `Authorization: Bearer <apiKey>`（二选一，推荐 `X-API-Key`）
- `X-Signature`
- `X-Timestamp`（Unix 秒级时间戳）

POST/PUT/PATCH 请求建议带：

- `Content-Type: application/json`

### 3.2 签名算法

签名字符串：

```text
HTTP_METHOD + "\n" + REQUEST_URI + "\n" + QUERY_STRING + "\n" + TIMESTAMP
```

- `REQUEST_URI` 示例：`/api/v1/process-system/submissions`
- `QUERY_STRING`：不带 `?`，无查询参数时为空字符串
- `TIMESTAMP`：与请求头 `X-Timestamp` 一致

签名值：

```text
Base64(HMAC-SHA256(signatureString, signatureSecret))
```

注意：查询参数顺序和编码必须与实际请求完全一致，否则会签名失败。

## 4. 统一响应格式

```json
{
  "code": 1,
  "msg": null,
  "data": {}
}
```

- `code = 1`：成功
- `code != 1`：失败（`msg` 给出原因）

说明：部分业务错误会返回 HTTP 200 + 业务错误码（例如 `code=404`），请同时判断 HTTP 状态码和 `code` 字段。

## 5. 类型取值与 Strapi 映射

### 5.1 本 API 接受的 `submission_type`

- `proposal`
- `application_attachment`
- `contract_template`
- `signed_contract`
- `deliverable_report`

### 5.2 与 Strapi 五类型编码映射（对接时必须做转换）

| Strapi type_code | 本API submission_type |
|---|---|
| `collection_attachment` | `application_attachment` |
| `proposal_file` | `proposal` |
| `contract_template` | `contract_template` |
| `signed_contract` | `signed_contract` |
| `deliverable_report` | `deliverable_report` |

结论：不要把 Strapi `type_code` 原样直接传给本接口，前两项会校验失败。

## 6. 提交物数据模型

### 6.1 创建/同步请求体（`POST /submissions`）

```json
{
  "submission_id": 202602090001,
  "application_id": 9001,
  "submission_type": "deliverable_report",
  "submission_stage": "execution",
  "submission_round": 1,
  "submission_version": 1,
  "project_info": {
    "project_name": "示例项目",
    "project_field": "数字化",
    "category_level": "重点",
    "category_specific": "科技计划",
    "research_period": 12,
    "project_keywords": "过程成果,归档",
    "project_description": "用于联调示例",
    "expected_results": "形成阶段性成果",
    "willing_adjust": "N"
  },
  "applicant_info": {
    "applicant_name": "张三",
    "id_card": "",
    "education_degree": "",
    "technical_title": "",
    "email": "",
    "phone": "13800000000",
    "work_unit": "某单位",
    "unit_address": "",
    "representative_achievements": ""
  },
  "files": {
    "proposal_file": {
      "file_id": "FILE_MAIN_001",
      "file_name": "成果物报告.pdf",
      "file_size": 204800,
      "file_type": "pdf",
      "file_url": "https://process.example.com/files/FILE_MAIN_001"
    },
    "other_attachments": [
      {
        "file_id": "FILE_ATT_001",
        "file_name": "佐证材料.zip",
        "file_size": 102400,
        "file_type": "zip",
        "file_url": "https://process.example.com/files/FILE_ATT_001"
      }
    ]
  },
  "upload_info": {
    "uploader_id": "u1001",
    "uploader_name": "系统账号",
    "submission_description": "阶段提交",
    "upload_time": "2026-02-09 10:00:00"
  }
}
```

必填字段（代码校验）：

- 顶层：`submission_id`、`application_id`、`submission_type`、`submission_stage`、`project_info`、`applicant_info`、`files`、`upload_info`
- `project_info`：`project_name`、`category_level`、`category_specific`、`project_description`
- `applicant_info`：`applicant_name`、`phone`
- `files`：`proposal_file`
- `proposal_file`：`file_id`、`file_name`
- `upload_info`：`uploader_id`、`uploader_name`

校验规则（关键项）：

- `submission_stage` 仅支持：`application` / `review` / `execution`
- `category_level` 仅支持：`重点` / `一般`
- `phone` 必须是中国大陆手机号格式
- `file_type` 支持：`pdf/doc/docx/xls/xlsx/ppt/pptx/txt/zip/rar`
- 单文件大小上限：50MB

说明：当前实现对 5 种 `submission_type` 使用统一文件结构（`proposal_file + other_attachments`），并未按类型拆成不同键名。

### 6.2 创建成功响应

```json
{
  "code": 1,
  "msg": null,
  "data": {
    "submission_id": 202602090001,
    "application_id": 9001,
    "sync_time": "2026-02-09T10:00:01"
  }
}
```

## 7. 接口清单

### 7.1 健康检查

- `GET /health`

### 7.2 提交物接口

- `POST /submissions`：创建/同步提交物
- `GET /submissions`：按条件查询提交物（`applicationId/submissionStage/submissionType/projectName/applicantName`）
- `GET /submissions/{submissionId}`：查询单条提交物详情
- `GET /applications/{applicationId}/submissions`
- `GET /applications/{applicationId}/submissions/{submissionStage}`
- `GET /applications/{applicationId}/submissions/{submissionType}/{submissionStage}/rounds/{submissionRound}/versions`
- `GET /applications/{applicationId}/submissions/{submissionType}/{submissionStage}/rounds`
- `GET /applications/{applicationId}/submissions/{submissionType}/{submissionStage}/rounds/{submissionRound}/versions/{submissionVersion}`
- `GET /applications/{applicationId}/submissions/{submissionType}/{submissionStage}/history`

### 7.3 文件接口

- `GET /files`：按条件查询文件（`submissionId/fileCategory/fileName/fileType`）
- `GET /files/{fileId}`：文件元数据
- `GET /submissions/{submissionId}/files`：按提交物查询文件
- `GET /files/{fileId}/download-url`：获取单文件下载链接
- `POST /files/batch-download-urls`：批量下载链接（请求体为 `fileId` 字符串数组，最多100个）

## 8. 常见错误码

| code | HTTP | 含义 |
|---|---|---|
| `10001` | 400 | 参数/数据校验失败 |
| `10002` | 401 | API Key 无效 |
| `10003` | 401 | 签名验证失败 |
| `10004` | 429/403 | 请求过频或 IP 白名单不通过（以 msg 为准） |
| `10005` | 401 | 时间戳无效（过期或格式错误） |
| `10008` | 500 | 服务内部错误 |
| `404` | 200 | 业务资源不存在（如提交物或文件不存在） |
| `0` | 200 | 控制器内部 catch 的通用失败 |

## 9. 联调最小 Checklist

1. 先把 Strapi 类型编码转换成本 API 的 `submission_type`（见第5节映射表）。
2. 每次请求都带 `X-API-Key + X-Timestamp + X-Signature`。
3. 请求体必须使用本文档中的嵌套结构（`project_info/applicant_info/files/upload_info`）。
4. `files.proposal_file` 必传，即使是合同/成果报告类型。
5. `file_url` 需要是成果系统可访问地址，否则后续下载不可用。

## 10. 关于“只用 DocumentId 是否足够”

不够。

- 本对接 API 不使用 Strapi `documentId`，也不会返回 `documentId`。
- 要正常展示过程成果，至少需要：`submission_id`、`submission_type`、`project_name`、`applicant_name`、`upload_time` 以及文件信息。
- `documentId` 只适合你们前端内部做“类型定义关联”，不能替代过程提交数据本体。
