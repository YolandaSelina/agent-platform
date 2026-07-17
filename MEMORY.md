# MEMORY.md

目的

- 说明仓库内“文件化记忆”的目录结构、写入规则与生命周期，供 agent 与使用者遵循。

主要目录

- memory/long/     : 长期记忆（准永久，需明确来源与审查）
- memory/daily/    : 每日记录（按日分文件，便于回溯）
- memory/tasks/    : 任务笔记（按 Issue 或任务 id 建档）
- memory/sessions/ : 会话摘要（用于在新 Copilot 会话中恢复上下文）
- memory/logs/     : 自动化 agent 日志（可选，短期内用以审计 agent 行为）

写入规则（简洁版）

1. 长期记忆只在满足两个条件后写入：可信来源 + 长期有效（或有计划每季复核）。
2. 每日记录仅追加，不随意删除或合并，合并到长期记忆需写入变更记录。
3. 文件修改必须有清晰 commit message 并关联 Issue（如存在）。
4. 不把 secrets 明文写入 memory；仅记录索引与访问方法。

基本模板（Markdown）

---
title: "<简短标题>"
created: 2026-07-17
updated: 2026-07-17
source: agent|user|external
tags: [tag1, tag2]
---

正文...

维护周期

- daily: 每日追加
- long: 按需更新，建议每季度审查一次重要长期记忆

