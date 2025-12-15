# 科研成果管理系统 - 前端项目

基于 Vue3 + TypeScript + Element Plus 构建的科研成果管理系统前端应用。

## ✨ 最新更新

### 🛡️ 完整的错误处理系统 (2025-12-08)

本项目已集成**企业级错误处理系统**,包括:

- ✅ **全局错误捕获** - Vue 错误、Promise 错误、资源加载错误
- ✅ **组件错误边界** - 防止单个组件错误导致应用崩溃
- ✅ **统一错误处理** - HTTP 错误、业务错误、权限错误自动分类处理
- ✅ **错误日志记录** - 自动记录所有错误,支持导出到监控服务
- ✅ **Composable Hooks** - 提供 `useRequest`、`useFormSubmit`、`useList` 等开箱即用的 Hooks
- ✅ **开发友好** - 开发环境显示详细错误信息,生产环境用户友好提示

**快速开始:** [错误处理快速开始](./docs/错误处理快速开始.md)  
**完整文档:** [错误处理系统使用指南](./docs/错误处理系统使用指南.md)  
**演示页面:** [ErrorDemo.vue](./src/views/ErrorDemo.vue)

## 技术栈

- **框架**: Vue 3 (Composition API + TypeScript)
- **UI 组件库**: Element Plus
- **状态管理**: Pinia
- **路由**: Vue Router 4
- **HTTP 客户端**: Axios
- **图表**: ECharts
- **构建工具**: Vite
- **语言**: TypeScript / JavaScript (ESM)
- **错误处理**: 自研企业级错误处理系统

## 项目结构

```
src/
├── api/                    # API 接口定义
│   ├── auth.ts             # 认证相关接口
│   └── result.ts           # 成果管理接口
├── assets/                 # 静态资源
│   └── main.css            # 全局样式
├── components/             # 通用组件
│   └── ErrorBoundary.vue   # 错误边界组件 🆕
├── composables/            # 组合式函数 🆕
│   └── useErrorHandler.ts  # 错误处理 Hooks
├── layouts/                # 布局组件
│   └── MainLayout.vue      # 主布局（三栏）
├── router/                 # 路由配置
│   └── index.ts            # 路由定义与守卫
├── stores/                 # 状态管理
│   └── user.ts             # 用户状态 (已优化)
├── types/                  # 业务常量（角色、状态等）
│   └── index.ts            # 全局常量
├── utils/                  # 工具函数
│   ├── request.ts          # Axios 封装 (已优化)
│   └── errorHandler.ts     # 错误处理系统 🆕
├── views/                  # 页面组件
│   ├── Login.vue           # 登录页 (已优化)
│   ├── Dashboard.vue       # 个人概览
│   ├── ErrorDemo.vue       # 错误处理演示 🆕
│   ├── result/             # 成果管理相关页面
│   │   ├── CreateResult.vue    # 创建成果（多步骤向导）
│   │   ├── MyResults.vue       # 个人成果列表
│   │   ├── SearchResults.vue   # 成果检索
│   │   └── ResultDetail.vue    # 成果详情
│   ├── expert/             # 专家功能
│   │   └── Reviews.vue         # 成果审核
│   ├── admin/              # 管理员功能
│   │   ├── Dashboard.vue       # 科研看台
│   │   ├── Results.vue         # 成果管理
│   │   ├── ReviewAssign.vue    # 审核分配
│   │   └── ResultTypes.vue     # 成果类型配置
│   └── error/              # 错误页面
│       ├── 403.vue     # 无权限
│       └── 404.vue     # 页面不存在
├── App.vue             # 根组件
└── main.js             # 应用入口
```

## 核心功能模块

### 1. 用户认证与权限
- 登录/登出
- 基于角色的路由守卫（普通用户、专家、管理员）
- Token 管理

### 2. 个人成果管理
- 个人成果概览（统计卡片 + 图表）
- 多步骤成果创建向导
  - 选择成果类型
  - 填写基础信息（动态表单）
  - 智能补全（DOI/arXiv/万方）
  - 上传附件与设置可见范围
- 个人成果列表（筛选、编辑、删除）
- 成果详情查看

### 3. 成果检索
- 多维度高级检索
- 分页展示
- 详情查看

### 4. 专家审核
- 待审核/已审核列表
- 审核操作（通过/驳回 + 意见）

### 5. 管理员功能
- 科研看台（全局统计与可视化）
- 成果管理（全局成果列表）
- 审核分配（分配专家）
- 成果类型配置（动态字段管理）

## 开发指南

### 安装依赖

```bash
npm install
```

### 开发模式

```bash
npm run dev
```

访问 http://localhost:5173

### 构建生产版本

```bash
npm run build
```

### 预览生产构建

```bash
npm run preview
```

## 设计特点

### 1. 可扩展架构
- 模块化组件设计
- 动态表单渲染（支持配置化成果类型）
- 统一的 API 封装
- 业务常量集中管理（角色、状态、可见性），避免魔法字符串

### 2. 用户体验
- 响应式设计
- 骨架屏加载
- 友好的错误提示
- 操作反馈（loading、toast、确认对话框）
- 空态引导

### 3. 权限控制
- 路由级权限（路由守卫）
- 组件级权限（v-if 指令）
- 菜单动态渲染（基于角色）

### 4. 视觉风格
- 科技蓝主色调 (#2266EE)
- 卡片式布局
- 渐变色统计卡片
- 轻量阴影与圆角

## API 接口说明

所有 API 请求通过 `src/utils/request.js` 统一处理：
- 自动添加 Authorization 头
- 统一错误处理
- 401 自动跳转登录
- 响应拦截与提示

### 接口基础路径
- 开发环境: `http://localhost:3000/api`
- 生产环境: `/api`

### 主要接口模块
- `/api/auth/*` - 认证相关
- `/api/results/*` - 成果管理
- `/api/result-types/*` - 成果类型配置

## 环境变量

在 `.env.development` 和 `.env.production` 中配置：

```
VITE_API_BASE_URL=http://localhost:3000/api
```

## 待完善功能

- [ ] 消息中心完整实现
- [ ] 成果编辑功能
- [ ] 附件上传进度与预览
- [ ] 智能补全差异对比界面
- [ ] 草稿自动保存
- [ ] 报表导出
- [ ] 移动端适配
- [ ] 单元测试

## 注意事项

1. 本项目为前端应用，需要配合后端 API 使用
2. 开发时请确保后端服务已启动
3. 生产部署时需配置正确的 API 地址
4. 建议使用 Node.js 16+ 版本

## License

MIT
