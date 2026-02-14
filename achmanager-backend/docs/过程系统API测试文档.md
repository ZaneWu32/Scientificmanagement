# 过程系统 API 测试文档（v2.0，CMS统一创建）

- 日期：2026-02-13
- 适用系统：`achmanager-backend`
- 接口前缀：`/api/v1/process-system`
- 核心目标：验证“过程系统上传 -> 成果物入库 -> 管理端过程成果展示”的全链路。

## 1. 本次测试重点

1. 新接口 `/achievements/createWithFiles` 能创建成果物并挂载附件。
2. 新接口 `/achievements/create` 能通过远程 URL 模式上传。
3. 默认状态为 `APPROVED`。
4. 幂等键可重放，且同键不同请求会冲突。
5. 管理端列表通过 `typeCodes` 能查到新写入的过程成果。

## 2. 环境变量（ApiPost）

| 变量名 | 示例值 |
|---|---|
| `BASE_URL` | `http://localhost:8081` |
| `API_KEY` | `process-system-key-001` |
| `IDEMPOTENCY_KEY` | `proc-9001-20260213-001` |
| `PROCESS_TYPE` | `deliverable_report` |
| `ACH_DOC_ID` | （测试中回填） |

## 3. 通用请求头

1. `Authorization: Bearer {{API_KEY}}`
2. `X-Idempotency-Key: {{IDEMPOTENCY_KEY}}`

## 4. 用例清单

## 4.1 创建（multipart 文件直传）

- Method: `POST`
- URL: `{{BASE_URL}}/api/v1/process-system/achievements/createWithFiles`
- Body: `form-data`
  - `data`（Text）:

```json
{
  "type_code": "{{PROCESS_TYPE}}",
  "project_code": "P-9001",
  "project_name": "过程成果联调项目",
  "title": "阶段成果报告",
  "summary": "阶段提交",
  "submitter_name": "张三"
}
```

  - `files`（File）: 任选 1~N 个

断言：

1. `code == 1`
2. `data.achievement_doc_id` 非空
3. `data.idempotent_hit == false`

提取变量脚本：

```javascript
const json = pm.response.json();
if (json.code === 1) {
  pm.environment.set('ACH_DOC_ID', json.data.achievement_doc_id);
}
```

## 4.2 幂等重放（同键同请求）

重复执行 4.1，不修改 `IDEMPOTENCY_KEY` 与 body。

断言：

1. `code == 1`
2. `data.idempotent_hit == true`
3. `data.achievement_doc_id` 与上次一致

## 4.3 幂等冲突（同键不同请求）

保持同一个 `IDEMPOTENCY_KEY`，修改 `title` 或 `project_name` 后再次请求。

断言：

1. `code == 409`
2. `msg` 包含“幂等键冲突”

## 4.4 创建（JSON + 远程 URL）

- Method: `POST`
- URL: `{{BASE_URL}}/api/v1/process-system/achievements/create`
- Body: `raw/json`

```json
{
  "idempotency_key": "proc-9001-20260213-URL-001",
  "type_code": "proposal_file",
  "project_code": "P-9001",
  "project_name": "过程成果联调项目",
  "remote_files": [
    {
      "url": "https://example.com/files/demo.pdf",
      "name": "申报书.pdf"
    }
  ]
}
```

断言：`code == 1`，返回 `achievement_doc_id`。

## 4.5 管理端列表可见性（后端接口）

调用管理员成果物分页接口：

- Method: `POST`
- URL: `{{BASE_URL}}/admin/achievement/pageList`
- Body:

```json
{
  "pageNum": 1,
  "pageSize": 20,
  "typeCodes": [
    "collection_attachment",
    "proposal_file",
    "contract_template",
    "deliverable_report",
    "signed_contract"
  ]
}
```

断言：

1. 返回列表包含新创建的 `ACH_DOC_ID` 或对应标题。
2. `auditStatus` 为 `APPROVED`。

## 5. 前端联调检查（管理员“过程成果”页）

路径：`/admin/interim-results`

检查项：

1. 新建后的过程成果出现在列表中。
2. 类型标签与 `type_code` 对应正确。
3. 详情页可打开，附件可下载/预览（有 URL 时）。
4. “刷新数据”按钮可刷新列表。
5. 导出可生成 CSV。

## 6. 失败排查

1. `code=10002`：API Key 不正确或 Header 格式错误。
2. `code=400` 且提示类型：`type_code` 不在 5 类内。
3. 提示“未找到已发布成果类型”：对应 `type_code` 尚未在成果类型中发布。
4. `code=409`：幂等键复用但请求内容已变化。

## 7. 兼容说明

旧接口 `/api/v1/process-system/submissions` 仍在，但新接入请使用 `/api/v1/process-system/achievements/*`。
