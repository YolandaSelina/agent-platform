# 多智能体协作开发平台 (rk-agent-platform)

> 一个让多个 LLM Agent 协作完成需求分析 → 产品定位 → 开发设计 → 开发实现 → 测试 → Bug 修复 → 发布全流程的平台。
> 每个阶段可独立配置平台 (OpenAI/Anthropic/通义/DeepSeek) 和模型，支持多 Agent 投票、人工 review 介入、文档统一管理、项目/任务页面化管理。

## ✨ 核心特性

- 🎯 **多 Agent 协作 / 投票**：每个节点支持多个 Agent 并行调用，6 种聚合策略
  - `FIRST`：取第一个
  - `MAJORITY`：多数票（内容完全一致）
  - `WEIGHTED`：按 AgentDef.weight 加权
  - `MERGE`：合并所有
  - `CONSENSUS_BY_LLM`：用 LLM 综合
  - `QUALITY_SCORED`：用 LLM 评分取最高
- 🧑‍💼 **人在回路 (Human-in-the-Loop)**：节点可设 `reviewRequired`，reviewer 通过对话式交互对文档/产物进行 review、修改、批准、拒绝
- 🔄 **可重跑**：Pipeline 整体执行或从某节点重跑，断点续传
- 🔌 **工具调用**：抽象 `Tool` 接口
  - `BUILTIN`：echo / now / uuid / 字符串处理
  - `HTTP`：GET / POST 调用
  - `SHELL`：执行本地命令
  - `DB`：SQL 查询（占位）
  - **MCP (Model Context Protocol)**：stdio 传输，调用外部 MCP server
- 📚 **文档统一管理**：所有阶段（需求/产品/设计/测试/发布）的文档统一存储、版本化，节点执行时自动写文档
- 📋 **项目/任务/页面化管理**：以项目为单位的任务、标签页、子页面、文档
- 🌍 **多平台 LLM 适配**：OpenAI / Anthropic / 通义千问 (兼容 OpenAI 协议) / DeepSeek
- 🔒 **Spring Security + JWT 鉴权**
- 🎨 **可视化 Pipeline 编辑器**：拖拽编排节点，React Flow 驱动
- 🐳 **Docker Compose 一键部署**

## 📊 当前进度

### ✅ 第一批（已完成）
- 完整 6 服务 + 1 网关 + 1 前端的微服务架构
- 18 张表 DDL + 预置 8 Agent + 16 工具
- 完整 Pipeline 引擎（状态机/重跑/人在回路/SSE 事件流）
- OpenAI / Anthropic / Qwen / DeepSeek 4 个 LLM 适配器
- 前端基础：登录/项目/任务/流水线表单编辑/执行详情(SSE)/Agent/工具

### ✅ 第二批（已完成）
- MCP 协议实现（stdio 传输）
- 6 种投票策略（含 WEIGHTED、QUALITY_SCORED）
- 文档管理 Service + Controller + 版本化
- 节点执行时自动写文档到 biz_document
- 前端：文档管理页面（Markdown 编辑 + 版本历史）
- 前端：Pipeline 拖拽编辑器（React Flow）
- Docker Compose 部署
- 单元测试样例（投票策略、PipelineContext）

### ✅ 第三批（已完成）
- **Qdrant 向量检索**：抽象 SearchService 接口 + QdrantSearchService 实现（HTTP REST）
- **Elasticsearch 全文搜索**：ElasticsearchSearchService（multi_match + 高亮）
- **搜索聚合**：跨引擎合并结果，按 score 排序去重
- **Pipeline 模板市场**：保存/克隆/使用次数统计
- **Webhook 触发器**：Token 鉴权、变量替换（${body.xxx}）、调用日志
- **Actuator + Prometheus 指标**：依赖已加（management.endpoints 待按需开放）
- **监控仪表板**：状态分布饼图、节点耗时柱图、Run 趋势线图、投票策略分布
- **全局搜索 UI**：跨服务搜索框 + 引擎状态展示
- **集成测试**：14 个测试覆盖 Pipeline 状态机、上下文传递、投票策略

### ✅ 第四批（已完成 · LLM 配置面板）
- **页面级 LLM 配置**：左侧菜单新增「🔑 LLM 配置」入口，所有 `apiKey / baseUrl / model` 都可在线配置
- **三级覆盖优先级**：项目级 + 模型 > 项目级 + 平台 > 系统级 + 模型 > 系统级 + 平台 > yml 默认
- **5 个内置平台 + 任意 OpenAI 兼容自定义平台**：内置 OpenAI / Anthropic / 通义千问 / DeepSeek / **硅基流动 (SiliconFlow)**；自定义可填 ZHIPU / MOONSHOT / YI / 自建网关等任意 OpenAI 兼容协议
- **AES-GCM 加密存储 API Key**：密钥由 `agent.jwt.secret` SHA-256 派生；列表/详情/by-platform 全部 mask（首尾 4 位 + `****`），仅 ADMIN 可通过 `/reveal` 端点拿真实明文
- **连通性测试**：`/api/llm-config/{id}/test` 端点用真实配置发送一次请求，返回延迟 + 错误码
- **运行时覆盖**：`LlmClient` 抽象 `overrideApiKey / overrideBaseUrl / overrideModel` 三个 volatile 字段，**仅供 GenericOpenAiClient 等独立 client 内部使用**；内置 client 是 Spring singleton，**不通过字段覆盖避免污染**
- **客户端不缓存**：DB 配置可能在运行时被改（用户编辑/删除/新增），GenericOpenAiClient 每次 new；OkHttp 内部连接池复用
- **JWT 携带 roles**：登录时把用户角色 + 权限写入 token claim，下游服务鉴权零 DB 查询
- **Lombok 升级到 1.18.34**：兼容 JDK 21（1.18.30 + `--release 17` 在 JDK 21 下 annotation processor 不执行）

**端到端测试覆盖**（`logs/e2e_test.py`，10/10 PASS）：

| 场景 | 输入 | 期望 | 结果 |
|---|---|---|---|
| 1 | 登录 admin | 拿到带 roles=[ADMIN] 的 token | ✅ |
| 2 | DB 无配置，调 LLM | yml 兜底，REPLACE_ME 抛 `code=5101` | ✅ |
| 3 | 页面创建系统级 QWEN 配置 (baseUrl→mock) | 入库 id=N | ✅ |
| 4 | LLM(projectId=null) | 走系统级，mock 收到正确 sk-PAGE | ✅ |
| 5 | 创建项目 A (无项目级) + B (有项目级) | projA / projB 创建成功 | ✅ |
| 6 | LLM(projectId=projA) | 回退系统级 sk-PAGE | ✅ |
| 7 | LLM(projectId=projB) | 命中项目级 sk-PROJECTB | ✅ |
| 8 | admin /reveal | 拿到明文 | ✅ |
| 9 | 删系统级后 LLM(projectId=projA) | yml 抛 `code=5101` | ✅ |
| 10 | 删项目级后 LLM(projectId=projB) | yml 抛 `code=5101` | ✅ |

## 🏗️ 架构

```
┌────────────────────────────────────────────────────────┐
│             API Gateway (:9080)                         │
│   /api/auth/**      -> 9081 auth                        │
│   /api/project/**   -> 9082 project                     │
│   /api/pipeline/**  -> 9083 pipeline                    │
│   /api/llm/**       -> 9084 llm                         │
└────────────────────────────────────────────────────────┘

┌────────────┐  ┌─────────────┐  ┌──────────────┐  ┌────────────┐
│ auth :9081 │  │ project:9082│  │ pipeline:9083│  │ llm   :9084│
│ 登录/JWT   │  │ 项目/任务   │  │ Pipeline引擎 │  │ LLM 适配   │
│ 用户/角色  │  │ 页面        │  │ Agent模板    │  │ 工具注册    │
│            │  │             │  │ 节点执行器   │  │ 调用日志    │
│            │  │             │  │ 投票聚合器   │  │            │
│            │  │             │  │ 状态机       │  │            │
│            │  │             │  │ SSE 事件总线 │  │            │
└────────────┘  └─────────────┘  └──────────────┘  └────────────┘
        │               │              │               │
        └───────────────┴──────────────┴───────────────┘
                                │
                ┌───────────────┼───────────────┐
                ▼               ▼               ▼
            MySQL 8         Redis 6         Qdrant / ES
        rk_agent_platform  (会话/锁)       (留接口，预留)
```

## 🛠️ 技术栈

| 层 | 技术 |
|---|------|
| 后端 | Spring Boot 3.2.2 / Java 17 / Maven 3.8+ |
| ORM | MyBatis Plus 3.5.5 |
| 数据库 | MySQL 8.0+ |
| 缓存 | Redis 6.0+ (Redisson) |
| 安全 | Spring Security + JWT (jjwt 0.12) |
| LLM 客户端 | OkHttp 4.12 |
| API 文档 | SpringDoc OpenAPI 2.3.0 |
| 任务调度 | Spring Async + ThreadPoolTaskExecutor |
| SSE | Spring MVC SseEmitter |
| 前端 | React 18.3 + TypeScript 5.9 + Vite 6.4 |
| UI | Ant Design 5.21 + Tailwind CSS 4.1 |
| 路由 | React Router 7 |
| 状态 | Zustand |
| HTTP | Axios + @microsoft/fetch-event-source |

## 📦 工程结构

```
agent-platform/
├── agent-platform-parent/           # Maven parent
├── agent-platform-common/           # 公共模块（统一响应、异常、JWT、Redis 配置）
├── agent-platform-auth/             # 认证服务 :9081
├── agent-platform-project/          # 项目服务 :9082
├── agent-platform-pipeline/         # 流水线引擎 :9083
├── agent-platform-llm/              # LLM 适配 :9084
├── agent-platform-api/              # 统一 API 网关 :9080
├── agent-platform-frontend/         # React 前端
├── sql/                             # 数据库脚本
│   ├── V1__init.sql                 # 完整 DDL + 初始化数据
│   └── example-pipeline-dsl.json    # 示例 Pipeline
└── docs/                            # 设计文档
```

## 🚀 快速开始

### 1. 环境准备

- **JDK 17+**
- **Maven 3.8+**
- **Node.js 18+**
- **MySQL 8.0+** （本地 `3306`）
- **Redis 6.0+** （本地 `6379`）

### 2. 初始化数据库

```bash
mysql -u root -p
# 密码：xiaozei0207
# 创建数据库
mysql -uroot -pxiaozei0207 -e "CREATE DATABASE IF NOT EXISTS rk_agent_platform DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_unicode_ci;"
# 导入脚本
mysql -uroot -pxiaozei0207 rk_agent_platform < sql/V1__init.sql
```

### 3. 启动后端

在 `agent-platform-parent` 目录下分别启动各模块（推荐用 IDE 启动，或在 5 个服务目录分别执行）：

```bash
# 公共模块先 install
mvn -pl agent-platform-common -am clean install -DskipTests

# 各服务启动（分别在各自目录）
cd agent-platform-auth && mvn spring-boot:run
cd agent-platform-project && mvn spring-boot:run
cd agent-platform-pipeline && mvn spring-boot:run
cd agent-platform-llm && mvn spring-boot:run
cd agent-platform-api && mvn spring-boot:run
```

**服务端口**：

| 服务 | 端口 |
|------|------|
| API Gateway | 9080 |
| auth-service | 9081 |
| project-service | 9082 |
| pipeline-service | 9083 |
| llm-service | 9084 |

### 4. 启动前端

```bash
cd agent-platform-frontend
npm install
npm run dev
# 访问 http://localhost:5173
```

### 5. 登录

打开 `http://localhost:5173`，使用默认账号登录：

- **用户名**：`admin`
- **密码**：`admin123`

## 🔑 LLM API Key 配置

LLM 凭据支持 **两种配置方式**，页面配置优先于 yml：

1. **页面配置（推荐）**：登录后访问左侧菜单「🔑 LLM 配置」，新增 / 编辑任意平台与模型的 key，支持：
   - 系统级（所有人共享）+ 项目级（仅该项目使用）两种作用域
   - 内置 5 平台：OpenAI / Anthropic / 通义千问 / DeepSeek / **硅基流动**
   - 任意 OpenAI 兼容自定义平台：智谱 / 月之暗面 / 零一万物 / 自建网关 …
   - API Key AES-256-GCM 加密入库；列表只显示 mask（首尾 4 位 + `****`），仅 ADMIN 可查看完整 Key
2. **yml 默认配置**（兜底）：当数据库中没有匹配的配置时，使用 `application.yml` 里的 `agent.llm.*` 段

`agent-platform-llm` / `agent-platform-pipeline` 模块的 `application.yml`：

```yaml
agent:
  llm:
    openai:
      api-key: ${OPENAI_API_KEY:REPLACE_ME}
      base-url: https://api.openai.com/v1
      default-model: gpt-4o
    anthropic:
      api-key: ${ANTHROPIC_API_KEY:REPLACE_ME}
      base-url: https://api.anthropic.com
      default-model: claude-3-5-sonnet-20241022
    qwen:
      api-key: ${QWEN_API_KEY:REPLACE_ME}
      base-url: https://dashscope.aliyuncs.com/compatible-mode/v1
      default-model: qwen-plus
    deepseek:
      api-key: ${DEEPSEEK_API_KEY:REPLACE_ME}
      base-url: https://api.deepseek.com/v1
      default-model: deepseek-chat
    siliconflow:
      api-key: ${SILICONFLOW_API_KEY:REPLACE_ME}
      base-url: https://api.siliconflow.cn/v1
      default-model: Qwen/Qwen2.5-72B-Instruct
```

环境变量支持：`OPENAI_API_KEY / ANTHROPIC_API_KEY / QWEN_API_KEY / DEEPSEEK_API_KEY / SILICONFLOW_API_KEY`

## 📐 数据库设计

15 张表（详见 `sql/V1__init.sql`）：

| 表 | 说明 |
|----|------|
| `sys_user / sys_role / sys_user_role` | 认证 |
| `biz_project / biz_project_member / biz_task` | 项目/任务 |
| `biz_project_page` | 项目页面（树形） |
| `biz_document / biz_document_version` | 文档（统一管理） |
| `pipeline_agent` | Agent 模板 |
| `pipeline_definition / pipeline_run` | Pipeline 定义与执行 |
| `pipeline_node_run / pipeline_node_log` | 节点执行与日志 |
| `pipeline_human_review / pipeline_review_message` | 人工审核 |
| `pipeline_tool` | 工具注册 |
| `llm_call_log` | LLM 调用日志 |

ER 关系：

```
sys_user ──┬── sys_user_role ── sys_role
           │
biz_project ──── biz_project_member ── sys_user
   │     │
   │     ├── biz_task
   │     ├── biz_project_page
   │     ├── biz_document ── biz_document_version
   │     └── pipeline_definition
   │
   └── pipeline_run ─┬── pipeline_node_run ── pipeline_node_log
                     ├── pipeline_human_review ── pipeline_review_message
                     └── pipeline_tool (Agent 配置)
```

## 🧠 Pipeline DSL

一个完整 Pipeline 例子（`sql/example-pipeline-dsl.json`）：

```json
{
  "startNode": "requirement",
  "nodes": [
    {
      "id": "requirement",
      "name": "需求分析",
      "type": "REQUIREMENT",
      "aggregation": "MERGE",
      "reviewRequired": true,
      "outputKey": "requirement_doc",
      "outputType": "MARKDOWN",
      "agents": [
        { "id": "ra-1", "template": "requirement-analyst", "platform": "OPENAI", "model": "gpt-4o" },
        { "id": "ra-2", "template": "requirement-analyst", "platform": "ANTHROPIC", "model": "claude-3-5-sonnet-20241022" }
      ],
      "next": "product"
    },
    {
      "id": "product",
      "name": "产品定位",
      "type": "PRODUCT",
      "aggregation": "CONSENSUS_BY_LLM",
      "reviewRequired": true,
      "agents": [
        { "id": "pm-1", "template": "product-manager", "platform": "OPENAI" }
      ],
      "next": "design"
    }
  ]
}
```

### 节点类型

| 类型 | 说明 |
|------|------|
| `REQUIREMENT` | 需求分析 |
| `PRODUCT` | 产品定位 |
| `DEVELOP_DESIGN` | 开发设计 |
| `DEVELOP_IMPL` | 开发实现 |
| `TEST_CASE` | 测试用例 |
| `TEST_RUN` | 测试执行 |
| `BUGFIX` | Bug 修复 |
| `RELEASE` | 发布 |
| `DOCUMENT` | 文档生成 |
| `HUMAN_REVIEW` | 人工审核节点 |
| `CUSTOM` | 自定义 |

### 聚合策略

| 策略 | 说明 |
|------|------|
| `FIRST` | 取第一个 Agent 输出（默认） |
| `MAJORITY` | 多数票（内容完全一致才算同一票） |
| `MERGE` | 合并所有 Agent 输出，Markdown 拼接 |
| `CONSENSUS_BY_LLM` | 用 LLM 综合所有 Agent 输出 |

## 🔄 状态机

```
PENDING → RUNNING → COMPLETED
                ↘ AWAITING_REVIEW → RUNNING (resume)
                ↘ FAILED (→ onFailure 节点)
                ↘ CANCELED
```

## 🧪 验证

启动后，可按以下流程验证：

1. 登录 `http://localhost:5173`
2. **Agent 模板**：在「Agent 模板」里检查预置的 8 个 Agent
3. **创建项目**：在「项目管理」新建一个项目
4. **创建 Pipeline 定义**：在项目下创建 Pipeline（用 `example-pipeline-dsl.json` 内容）
5. **触发执行**：点击「运行」输入参数
6. **观察实时事件**：在执行详情页查看 SSE 事件流
7. **人工审核**：节点进入 `AWAITING_REVIEW` 时，发送消息、通过/拒绝/修改

## 🛣️ 未来可扩展

- [ ] 中文分词（IK Analyzer）提升 ES 中文搜索质量
- [ ] 文档 Embedding 自动化（用 LLM 生成向量）
- [ ] 完整 6 个阶段节点示例（开发/测试/Bug 修复/发布）
- [ ] Pipeline 模板市场 UI 完善
- [ ] Webhook 安全增强（HMAC 签名校验）
- [ ] 监控（Micrometer + Prometheus + Grafana 完整接入）
- [ ] 多租户隔离
- [ ] 移动端 H5

## 📜 License

Apache 2.0
