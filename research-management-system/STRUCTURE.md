# 项目结构说明

## 目录结构

```
research-management-system/
├── public/                      # 静态资源目录
├── src/                         # 源代码目录
│   ├── api/                     # API 接口层
│   │   ├── auth.js             # 认证接口
│   │   └── result.js           # 成果管理接口
│   │
│   ├── assets/                  # 资源文件
│   │   └── main.css            # 全局样式
│   │
│   ├── layouts/                 # 布局组件
│   │   └── MainLayout.vue      # 主布局（顶部+侧边+内容）
│   │
│   ├── router/                  # 路由配置
│   │   └── index.js            # 路由定义、守卫
│   │
│   ├── stores/                  # 状态管理（Pinia）
│   │   └── user.js             # 用户状态
│   │
│   ├── types/                   # 业务常量/枚举
│   │   └── index.js            # 角色、成果状态、可见性等
│   │
│   ├── utils/                   # 工具函数
│   │   └── request.js          # Axios 封装
│   │
│   ├── views/                   # 页面组件
│   │   ├── Login.vue           # 登录页
│   │   ├── Dashboard.vue       # 个人概览
│   │   │
│   │   ├── result/             # 成果管理模块
│   │   │   ├── CreateResult.vue    # 创建成果（多步骤）
│   │   │   ├── MyResults.vue       # 个人成果列表
│   │   │   ├── SearchResults.vue   # 成果检索
│   │   │   └── ResultDetail.vue    # 成果详情
│   │   │
│   │   ├── expert/             # 专家功能模块
│   │   │   └── Reviews.vue         # 成果审核
│   │   │
│   │   ├── admin/              # 管理员功能模块
│   │   │   ├── Dashboard.vue       # 科研看台
│   │   │   ├── Results.vue         # 成果管理
│   │   │   ├── ReviewAssign.vue    # 审核分配
│   │   │   └── ResultTypes.vue     # 成果类型配置
│   │   │
│   │   └── error/              # 错误页面
│   │       ├── 403.vue             # 无权限
│   │       └── 404.vue             # 页面不存在
│   │
│   ├── App.vue                  # 根组件
│   └── main.js                  # 应用入口
│
├── .env.development             # 开发环境配置
├── .env.production              # 生产环境配置
├── index.html                   # HTML 模板
├── package.json                 # 项目依赖
├── jsconfig.json                # 路径别名/IDE 支持
├── vite.config.js               # Vite 配置
├── README.md                    # 项目说明
├── 项目说明.md                  # 详细说明（中文）
└── STRUCTURE.md                 # 本文件
```

## 模块说明

### API 层 (`src/api/`)

负责与后端接口交互，每个模块对应一个文件：

- `auth.ts`: 登录、登出、获取用户信息
- `result.ts`: 成果的增删改查、审核、统计等

所有接口函数返回 Promise，统一使用 `request` 工具发送请求。

### 业务常量 (`src/types/`)

集中维护角色、成果状态、可见性、字段类型等常量，避免魔法字符串并方便跨模块复用。

### 状态管理 (`src/stores/`)

使用 Pinia 管理全局状态：

- `user.ts`: 用户信息、登录状态、Token、权限检查

### 路由配置 (`src/router/`)

- 路由定义（嵌套路由）
- 路由守卫（登录检查、权限检查）
- 路由元信息（标题、角色）

### 工具函数 (`src/utils/`)

- `request.js`: Axios 实例配置、请求/响应拦截器

### 布局组件 (`src/layouts/`)

- `MainLayout.vue`: 主布局，包含顶部导航、侧边菜单、内容区

### 页面组件 (`src/views/`)

按功能模块组织：

#### 公共页面
- `Login.vue`: 登录页

#### 普通用户页面
- `Dashboard.vue`: 个人概览
- `result/CreateResult.vue`: 创建成果
- `result/MyResults.vue`: 个人成果
- `result/SearchResults.vue`: 成果检索
- `result/ResultDetail.vue`: 成果详情

#### 专家页面
- `expert/Reviews.vue`: 成果审核

#### 管理员页面
- `admin/Dashboard.vue`: 科研看台
- `admin/Results.vue`: 成果管理
- `admin/ReviewAssign.vue`: 审核分配
- `admin/ResultTypes.vue`: 成果类型配置

#### 错误页面
- `error/403.vue`: 无权限
- `error/404.vue`: 页面不存在

## 数据流

```
用户操作
  ↓
页面组件 (views/)
  ↓
调用 API (api/)
  ↓
HTTP 请求 (utils/request.ts)
  ↓
后端服务
  ↓
响应数据
  ↓
更新状态 (stores/) 或 组件状态
  ↓
视图更新
```

## 权限控制流程

```
用户访问路由
  ↓
路由守卫 (router/index.js)
  ↓
检查登录状态 (stores/user.js)
  ↓
检查角色权限
  ↓
允许访问 / 跳转登录 / 跳转 403
```

## 组件通信

- **父子组件**: Props + Emit
- **跨组件**: Pinia Store
- **路由传参**: Route Params / Query

## 样式组织

- **全局样式**: `src/assets/main.css`
- **组件样式**: `<style scoped>` 在组件内
- **UI 框架**: Element Plus 主题

## 构建流程

```
开发模式:
npm run dev → Vite Dev Server → 热更新

生产构建:
npm run build → Vite Build → dist/
  ├── index.html
  ├── assets/
  │   ├── index-[hash].js
  │   ├── index-[hash].css
  │   └── ...
  └── ...
```

## 环境变量

- `.env.development`: 开发环境
- `.env.production`: 生产环境

变量以 `VITE_` 开头，在代码中通过 `import.meta.env.VITE_XXX` 访问。

## 依赖说明

### 核心依赖
- `vue`: Vue 3 框架
- `vue-router`: 路由管理
- `pinia`: 状态管理
- `element-plus`: UI 组件库
- `axios`: HTTP 客户端
- `echarts`: 图表库

### 开发依赖
- `vite`: 构建工具
- `typescript`: 类型检查
- `@vitejs/plugin-vue`: Vue 插件
- `eslint`: 代码检查

## 扩展指南

### 添加新页面

1. 在 `src/views/` 创建组件
2. 在 `src/router/index.ts` 添加路由
3. 在 `src/layouts/MainLayout.vue` 添加菜单项（如需要）

### 添加新 API

1. 在 `src/api/` 对应文件添加接口函数
2. 在 `src/types/` 定义相关类型
3. 在页面组件中调用

### 添加新状态

1. 在 `src/stores/` 创建新的 store
2. 使用 `defineStore` 定义
3. 在组件中通过 `useXxxStore()` 使用

### 添加新角色

1. 在 `src/types/index.ts` 的 `UserRole` 枚举添加
2. 在路由 meta 中配置权限
3. 在菜单配置中添加角色判断
