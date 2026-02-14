# 过程系统对接 API（Headless CMS 主链路）

- 文档版本：v3.0（CMS统一创建版）
- 更新时间：2026-02-13
- 接口前缀：`/api/v1/process-system`
- 目标：过程系统直接复用成果物创建链路，过程成果在管理端按 `type_code` 筛选展示。

## 1. 设计结论

本次迭代后，过程成果采用“与科研成果一致”的创建模式：

1. 写入端：调用新接口创建 `achievement_mains + achievement_files`。
2. 读取端：管理端列表直接查询成果物表，并按 5 个过程类型过滤。
3. 状态：过程成果默认 `APPROVED`。
4. 幂等：必须使用幂等键，支持重复请求安全重试。

> 旧接口 `POST /api/v1/process-system/submissions` 保留用于兼容，不作为新接入首选。

## 2. 鉴权

所有过程系统接口沿用 API Key 鉴权（默认 `auth-only-mode=true`）：

- `Authorization: Bearer <apiKey>`

可选：

- `X-API-Key: <apiKey>`

## 3. 过程成果类型（固定 5 类）

仅允许以下 `type_code`：

1. `collection_attachment`
2. `proposal_file`
3. `contract_template`
4. `deliverable_report`
5. `signed_contract`

## 4. 新增接口

## 4.1 文件直传模式（推荐）

- Method: `POST`
- Path: `/api/v1/process-system/achievements/createWithFiles`
- Content-Type: `multipart/form-data`
- Headers:
  - `Authorization: Bearer <apiKey>`
  - `X-Idempotency-Key: <唯一幂等键>`（推荐放 Header）
- FormData:
  - `data`：JSON 字符串
  - `files`：可选，多文件

`data` 示例：

```json
{
  "idempotency_key": "proc-9001-20260213-001",
  "type_code": "deliverable_report",
  "title": "项目阶段成果报告",
  "summary": "阶段提交说明",
  "project_code": "P-9001",
  "project_name": "XX项目",
  "year": "2026",
  "visibility_range": "ALL",
  "submitter_name": "张三",
  "keywords": ["阶段成果", "联调"],
  "fields": []
}
```

响应示例：

```json
{
  "code": 1,
  "msg": null,
  "data": {
    "achievement_doc_id": "abc123docid",
    "type_code": "deliverable_report",
    "idempotent_hit": false
  }
}
```

## 4.2 远程文件 URL 模式

- Method: `POST`
- Path: `/api/v1/process-system/achievements/create`
- Content-Type: `application/json`
- Headers:
  - `Authorization: Bearer <apiKey>`
  - `X-Idempotency-Key: <唯一幂等键>`（也可在 body 用 `idempotency_key`）

请求示例：

```json
{
  "idempotency_key": "proc-9001-20260213-002",
  "type_code": "proposal_file",
  "project_code": "P-9001",
  "project_name": "XX项目",
  "submitter_name": "李四",
  "remote_files": [
    {
      "url": "https://example.com/files/a.pdf",
      "name": "申报书.pdf"
    }
  ]
}
```

说明：后端会先下载远程文件，再按成果物上传逻辑入库。

## 5. 幂等规则

幂等唯一键为：`apiKey + idempotencyKey`

1. 首次请求：创建成果，返回 `idempotent_hit=false`。
2. 同键同内容重复请求：直接返回已创建成果，`idempotent_hit=true`。
3. 同键不同内容请求：返回冲突（业务码 `409`）。

## 6. 后端写入映射（自动）

新接口会自动构建成果主数据：

1. `achievementStatus` 固定写 `APPROVED`。
2. `typeDocId` 由 `type_code` 自动映射（读取已发布成果类型）。
3. 附件统一走 `achievement-files` 媒体关联。

## 7. 管理端读取方式（前端展示）

过程成果页面统一按成果物查询，并使用：

- `typeCodes = [collection_attachment, proposal_file, contract_template, deliverable_report, signed_contract]`

这样即可在管理员端“过程成果”页面展示。

## 8. 错误码

| code | HTTP | 说明 |
|---|---|---|
| `1` | 200 | 成功 |
| `400` | 200 | 参数校验失败 |
| `409` | 200 | 幂等键冲突（同键不同请求） |
| `10002` | 401 | API Key 无效 |
| `0` | 200 | 其他失败（`msg` 查看原因） |

## 9. 接入最小清单

1. 必传 `Authorization: Bearer <apiKey>`。
2. 必传幂等键（`X-Idempotency-Key` 或 `idempotency_key`）。
3. `type_code` 必须是上述 5 类之一。
4. 若文件直传，使用 `createWithFiles`。
5. 若仅有 URL，使用 `create` + `remote_files`。
