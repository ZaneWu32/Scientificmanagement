# 过程系统 API 测试文档（ApiPost版）

- 版本：v1.1
- 日期：2026-02-11
- 适用系统：`achmanager-backend`
- 接口前缀：`/api/v1/process-system`
- 参考实现：`/Users/junhaowu/Desktop/Project/MyProject/ScienceManager/achmanager-backend/src/main/java/com/achievement/controller/ProcessSystemController.java`

## 1. 阅读结论与迭代说明

对 v1.0 文档的审核结论：

1. 原文档可用于命令行联调，但对 ApiPost 实操支持不足。
2. 缺少 ApiPost 的环境变量设计、前置签名脚本、断言脚本。
3. 缺少“集合执行顺序 + 变量传递 + 批量回归”方法。

本次迭代目标：将文档升级为“可直接在 ApiPost 落地执行”的测试手册。

## 2. 测试目标

1. 验证 5 类过程成果能正确创建、查询、关联文件。
2. 验证鉴权链路（API Key、签名、时间戳、限流）正确。
3. 验证错误码与异常行为符合后端实现。
4. 验证历史接口、文件接口稳定可用。
5. 验证过程系统接口不影响普通成果既有逻辑。

## 3. 测试范围

包含：

1. `GET /health`
2. 提交物：创建、列表、详情、按申报ID、按阶段、历史/版本查询
3. 文件：列表、元数据、下载链接、批量下载链接
4. 安全：缺失/错误 API Key、签名错误、时间戳过期、限流

不包含：

1. Strapi 管理台 UI 测试
2. 前端页面 UI 自动化测试
3. 文件真实下载内容完整性（仅检查返回链接和元数据）

## 4. ApiPost 工作空间准备

## 4.1 环境变量（建议）

在 ApiPost 新建环境 `process-system-dev`，加入下列变量：

| 变量名 | 示例值 | 说明 |
|---|---|---|
| `BASE_URL` | `http://localhost:8081` | 后端地址 |
| `API_KEY` | `process-system-key-001` | 过程系统 API Key |
| `SIGN_SECRET` | `***` | 签名密钥 |
| `APP_ID` | `9001` | 测试申报ID |
| `SUBMISSION_ID` | `202602110001` | 测试提交物ID（可动态覆盖） |
| `FILE_MAIN_ID` | `FILE_MAIN_202602110001` | 主文件ID |
| `FILE_ATT_ID` | `FILE_ATT_202602110001` | 附件ID |
| `X_TIMESTAMP` | 空 | 前置脚本自动写入 |
| `X_SIGNATURE` | 空 | 前置脚本自动写入 |
| `EXPECT_CODE` | `1` | 断言用预期业务码 |

## 4.2 集合目录建议

在 ApiPost 新建 Collection：`ProcessSystem API Tests`，按以下目录组织：

1. `00-Health`
2. `01-Submission-Create`
3. `02-Submission-Query`
4. `03-Submission-History`
5. `04-File-APIs`
6. `05-Security-Negative`
7. `06-Regression`

## 5. 类型映射规则（必须执行）

上游如果拿的是 Strapi `type_code`，请求时必须映射为 `submission_type`：

| 上游 type_code | submission_type |
|---|---|
| `collection_attachment` | `application_attachment` |
| `proposal_file` | `proposal` |
| `contract_template` | `contract_template` |
| `signed_contract` | `signed_contract` |
| `deliverable_report` | `deliverable_report` |

注意：直接传 `proposal_file` 或 `collection_attachment` 会触发参数校验失败（`code=10001`）。

## 6. ApiPost 签名配置

## 6.1 签名规则（与后端一致）

签名字符串：

```text
HTTP_METHOD + "\n" + REQUEST_URI + "\n" + QUERY_STRING + "\n" + TIMESTAMP
```

签名值：

```text
Base64(HMAC-SHA256(signatureString, SIGN_SECRET))
```

## 6.2 Collection 前置脚本（推荐）

把下面脚本放在 Collection 的前置脚本中（子请求自动继承）：

```javascript
// ApiPost 常见脚本运行时兼容 pm.* API
const apiKey = pm.environment.get("API_KEY");
const signSecret = pm.environment.get("SIGN_SECRET");

const ts = Math.floor(Date.now() / 1000).toString();
const method = pm.request.method.toUpperCase();
const fullUrl = pm.request.url.toString();
const url = new URL(fullUrl);
const uri = url.pathname;
const query = url.search ? url.search.substring(1) : "";

const signStr = `${method}\n${uri}\n${query}\n${ts}`;
const signHash = CryptoJS.HmacSHA256(signStr, signSecret);
const signature = CryptoJS.enc.Base64.stringify(signHash);

pm.environment.set("X_TIMESTAMP", ts);
pm.environment.set("X_SIGNATURE", signature);

pm.request.headers.upsert({ key: "X-API-Key", value: apiKey });
pm.request.headers.upsert({ key: "X-Timestamp", value: ts });
pm.request.headers.upsert({ key: "X-Signature", value: signature });
```

每个请求头里保持：

- `X-API-Key: {{API_KEY}}`
- `X-Timestamp: {{X_TIMESTAMP}}`
- `X-Signature: {{X_SIGNATURE}}`

## 6.3 签名排错要点

1. 必须使用路径（例如 `/api/v1/process-system/submissions`），不能带域名。
2. Query 顺序必须与实际 URL 完全一致。
3. `X-Timestamp` 与签名中的时间戳必须一致。
4. 时间戳超过 300 秒会失败（`code=10005`）。

## 7. 通用断言脚本（建议）

将下面脚本放到每个请求的 Tests 中：

```javascript
pm.test("HTTP状态码合理", function () {
  pm.expect([200, 400, 401, 403, 429, 500]).to.include(pm.response.code);
});

const json = pm.response.json();
pm.test("返回结构包含 code", function () {
  pm.expect(json).to.have.property("code");
});

const expectCodeRaw = pm.variables.get("EXPECT_CODE");
if (expectCodeRaw !== undefined && expectCodeRaw !== null && expectCodeRaw !== "") {
  const expectCode = Number(expectCodeRaw);
  pm.test(`业务码应为 ${expectCode}`, function () {
    pm.expect(json.code).to.eql(expectCode);
  });
}
```

## 8. 核心请求模板（ApiPost）

## 8.1 创建提交物（模板）

- Method: `POST`
- URL: `{{BASE_URL}}/api/v1/process-system/submissions`
- Body: `raw/json`

```json
{
  "submission_id": {{SUBMISSION_ID}},
  "application_id": {{APP_ID}},
  "submission_type": "deliverable_report",
  "submission_stage": "execution",
  "submission_round": 1,
  "submission_version": 1,
  "project_info": {
    "project_name": "联调测试项目A",
    "project_field": "数字化",
    "category_level": "重点",
    "category_specific": "科技计划",
    "research_period": 12,
    "project_keywords": "过程成果,联调",
    "project_description": "用于ApiPost联调",
    "expected_results": "形成阶段成果",
    "willing_adjust": "N"
  },
  "applicant_info": {
    "applicant_name": "张三",
    "phone": "13800000000",
    "work_unit": "测试单位"
  },
  "files": {
    "proposal_file": {
      "file_id": "{{FILE_MAIN_ID}}",
      "file_name": "成果报告.pdf",
      "file_size": 204800,
      "file_type": "pdf",
      "file_url": "https://example.com/files/{{FILE_MAIN_ID}}"
    },
    "other_attachments": [
      {
        "file_id": "{{FILE_ATT_ID}}",
        "file_name": "附件说明.txt",
        "file_size": 2048,
        "file_type": "txt",
        "file_url": "https://example.com/files/{{FILE_ATT_ID}}"
      }
    ]
  },
  "upload_info": {
    "uploader_id": "u1001",
    "uploader_name": "系统联调账号",
    "submission_description": "联调首提",
    "upload_time": "2026-02-11 10:00:00"
  }
}
```

创建成功后在 Tests 中提取变量：

```javascript
const json = pm.response.json();
if (json.code === 1 && json.data) {
  pm.environment.set("SUBMISSION_ID", String(json.data.submission_id));
}
```

## 8.2 查询详情

- Method: `GET`
- URL: `{{BASE_URL}}/api/v1/process-system/submissions/{{SUBMISSION_ID}}`

## 8.3 文件元数据

- Method: `GET`
- URL: `{{BASE_URL}}/api/v1/process-system/files/{{FILE_MAIN_ID}}`

## 9. ApiPost 执行顺序（推荐）

## 9.1 冒烟（3条）

1. `GET /health`
2. `POST /submissions`（`EXPECT_CODE=1`）
3. `GET /submissions/{submissionId}`（`EXPECT_CODE=1`）

## 9.2 五类型正向用例（5条）

同一请求模板复制 5 份，仅修改 `submission_type`：

1. `proposal`
2. `application_attachment`
3. `contract_template`
4. `signed_contract`
5. `deliverable_report`

说明：每条用例都要使用不同 `SUBMISSION_ID`，避免命中重复校验。

## 9.3 负向与安全用例（关键）

| 用例ID | 设置方式（ApiPost） | 预期 |
|---|---|---|
| AUTH-001 | 删除 `X-API-Key` | HTTP 401, `code=10002` |
| AUTH-002 | 临时把 `API_KEY` 改为错误值 | HTTP 401, `code=10002` |
| AUTH-003 | 在请求级脚本覆盖 `X-Signature=abc` | HTTP 401, `code=10003` |
| AUTH-004 | 请求级脚本把 `X_TIMESTAMP` 改为 `now-10000` | HTTP 401, `code=10005` |
| SUB-006 | `submission_type=proposal_file` | HTTP 400, `code=10001` |
| SUB-007 | `submission_stage=finish` | HTTP 400, `code=10001` |
| SUB-010 | `phone=123` | HTTP 400, `code=10001` |
| SUB-012 | `file_size=52428801` | HTTP 400, `code=10001` |
| SUB-013 | 同版本重复提交 | HTTP 200, `code=0` |

## 9.4 文件与历史接口回归

1. `GET /submissions?applicationId={{APP_ID}}`
2. `GET /applications/{{APP_ID}}/submissions`
3. `GET /applications/{{APP_ID}}/submissions/execution`
4. `GET /applications/{{APP_ID}}/submissions/deliverable_report/execution/rounds`
5. `GET /applications/{{APP_ID}}/submissions/deliverable_report/execution/history`
6. `GET /files?submissionId={{SUBMISSION_ID}}`
7. `GET /submissions/{{SUBMISSION_ID}}/files`
8. `GET /files/{{FILE_MAIN_ID}}/download-url`
9. `POST /files/batch-download-urls`

## 10. 完整用例矩阵

| 分组 | 数量 | 通过标准 |
|---|---|---|
| 冒烟 | 3 | 全部通过 |
| 五类型正向 | 5 | 全部 `code=1` |
| 安全负向 | 9 | 错误码与HTTP状态匹配 |
| 查询历史 | 4 | 返回结构正确且有数据 |
| 文件接口 | 8 | 返回结构正确 |

建议门槛：

1. P0/P1 缺陷为 0。
2. 冒烟、鉴权、安全用例 100% 通过。
3. 其余用例通过率 >= 95%。

## 11. 数据库核验 SQL

```sql
SELECT submission_id, application_id, submission_type, submission_stage, submission_round, submission_version, sync_time
FROM process_submissions
WHERE submission_id = 202602110001;

SELECT file_id, submission_id, file_category, file_name, file_url, storage_status
FROM process_submission_files
WHERE submission_id = 202602110001
ORDER BY upload_time ASC;
```

## 12. 缺陷记录模板

```text
[缺陷ID] BUG-PS-001
[环境] dev / test / prod
[接口] POST /api/v1/process-system/submissions
[前置条件] ...
[步骤] 1...2...3...
[期望] ...
[实际] ...
[请求报文] ...
[响应报文] ...
[日志位置] ...
[严重级别] P1/P2/P3/P4
```

## 13. 常见问题

1. 报 `10003`：优先检查 URI、Query 顺序、时间戳是否参与签名。
2. 报 `10005`：客户端时间偏差过大或用了旧时间戳。
3. 报 `10001` 且提示类型不支持：确认已做 Strapi -> submission_type 映射。
4. 报 `404`（HTTP 200）：是业务资源不存在，不是路由不存在。

