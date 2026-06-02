# 需求洞察 RAG 本地端到端联调

日期：2026-06-01
适用范围：本地验证“成果上传附件 -> 审核通过 -> RAG 索引 -> 需求匹配 -> 前端展示”主链路

## 1. 启动基础服务

```bash
docker compose -f docker-compose.local.yml up -d redis elasticsearch keycloak
```

端口约定：

- MySQL：优先使用本机 `127.0.0.1:3306`，库名 `strapi`，用户 `root`，密码 `Wjh0921@`
- Redis：`127.0.0.1:6379`
- Elasticsearch：`127.0.0.1:9200`，本地关闭安全认证
- Keycloak：`127.0.0.1:8080`，管理账号 `admin/admin`

当前 Mac 已检测到本机 MySQL：`/usr/local/mysql/bin/mysqld` 监听 `3306`，因此不要同时启动 compose 中的 `mysql` 服务。若另一台机器没有本机 MySQL，可以改为：

```bash
docker compose --profile docker-mysql -f docker-compose.local.yml up -d mysql redis elasticsearch keycloak
```

Keycloak 会导入 `dev/keycloak/realm-research-management.json`，包含：

- Realm：`research-management`
- 前端 client：`research-management-portal`
- 后端 client：`achmanager-backend`，secret 为 `local-dev-secret`
- 本地用户：`admin/admin`、`expert/expert`、`leader/leader`

## 2. 启动 Strapi

```bash
cd my-strapi-project
npm run develop
```

首次启动会在 `strapi` 数据库中创建 Strapi 表。启动后确认：

```bash
curl http://127.0.0.1:1337/admin
```

## 3. 启动后端

```bash
cd achmanager-backend
KEYCLOAK_CLIENT_SECRET=local-dev-secret \
RAG_LLM_ENABLED=false \
./mvnw spring-boot:run
```

说明：

- `RAG_LLM_ENABLED=false` 用于本地无大模型 key 时先验证 ES + 规则匹配链路。
- 如果要验证 LLM 重排，补充 `LLM_DEFAULT_BASE_URL`、`LLM_DEFAULT_API_KEY`、`LLM_DEFAULT_MODEL_NAME` 后改为 `RAG_LLM_ENABLED=true`。

健康检查：

```bash
curl http://127.0.0.1:8081/health
```

`/rag/**` 接口需要登录 token，获取 token 后再检查 ES 健康：

```bash
curl -H "Authorization: Bearer <access_token>" \
  http://127.0.0.1:8081/rag/es/health
```

## 4. 启动前端

```bash
cd research-management-system
npm run dev
```

前端默认访问 `http://localhost:5173`，API 通过 Vite 代理转发到 `http://localhost:8081`。

## 5. 获取本地 token

当前前端登录页默认跳转统一登录门户；本地端到端联调可以直接从 Keycloak 获取 token，并写入浏览器存储。

```bash
curl -s \
  -d grant_type=password \
  -d client_id=research-management-portal \
  -d username=admin \
  -d password=admin \
  http://127.0.0.1:8080/realms/research-management/protocol/openid-connect/token
```

在浏览器控制台写入：

```js
sessionStorage.setItem("access_token", "<access_token>");
sessionStorage.setItem("refresh_token", "<refresh_token>");
sessionStorage.setItem("user_info", JSON.stringify({
  id: 1,
  uuid: "local-admin",
  username: "admin",
  name: "本地管理员",
  email: "admin@example.local",
  roles: ["research_admin"]
}));
location.href = "/admin/dashboard";
```

## 6. 验证主链路

1. 打开前端，进入“创建成果”，上传一个包含目标关键词的 PDF/TXT/DOCX 附件并提交。
2. 使用专家或管理员审核通过该成果。审核通过会自动调用 `rebuildAndIndexAchievementDoc` 写入 ES。
3. 如果需要手动修复单条索引，可调用：

```bash
curl -X POST \
  -H "Authorization: Bearer <access_token>" \
  http://127.0.0.1:8081/rag/search-docs/achievements/<achievementDocId>/sync
```

4. 如果本地没有采集器写入需求，可临时插入一条需求：

```sql
INSERT INTO demand_items (
  demand_code, title, raw_content, summary, llm_summary,
  keywords_json, tags_json, industry, region,
  source_category, source_site, captured_at,
  priority, confidence, best_match_score, status,
  is_delete, created_at, updated_at
) VALUES (
  'LOCAL-RAG-001',
  '无人机电力巡检缺陷识别需求',
  '企业希望基于无人机巡检影像识别输电线路缺陷，并形成巡检调度方案。',
  '需要无人机巡检、缺陷识别和调度能力。',
  '寻找可支撑无人机电力巡检缺陷识别的科研成果。',
  '["无人机","巡检","缺陷识别"]',
  '["电力","低空智能"]',
  '电力',
  '浙江',
  'manual',
  'local',
  NOW(),
  'high',
  0.900,
  0.000,
  'new',
  0,
  NOW(),
  NOW()
);
```

5. 执行需求匹配：

```bash
curl -X POST \
  -H "Authorization: Bearer <access_token>" \
  http://127.0.0.1:8081/demand/<demandId>/rematch
```

6. 打开前端“需求洞察”，确认候选成果卡片展示：

- 匹配度
- 匹配理由
- 附件证据片段
- 标签
- “确认匹配”和“查看成果”动作

## 7. 判断通过

本地端到端通过标准：

- `achievement_search_docs` 有对应成果快照，`attachment_extract_status = 'success'` 或合理的跳过/失败原因。
- ES `science_achievement_docs` 能检索到该成果。
- `demand_matches` 出现对应 `demand_id + achievement_doc_id` 的匹配记录。
- 前端“需求洞察”能展示该匹配，确认后需求状态变为 `in_follow_up`。
