# 初始化个人 AI 工作空间 — PR 描述草稿

目的
- 把本仓库初始化为长期驻留的个人 AI 助手工作空间的最小起点。添加 agent 元数据、当日记录模板与 agent 任务 Issue 模板，方便后续在新 Copilot 会话中继承角色与记忆。

变更
- 添加 memory/agent_profile.json：agent 身份与默认行为。
- 添加 memory/daily/2026-07-17.md：初始化当日记录。
- 添加 .github/ISSUE_TEMPLATE/agent-task.md：agent/task Issue 模板。
- 更新 AGENTS.md：补充“最近操作摘要”。

检查要点
- 文件路径与命名是否符合你的习惯？
- AGENTS.md 的“最近操作摘要”是否需要更改措辞或移除？

后续计划
- 若本 PR 合并，agent 将定期在 memory/daily/ 中写入会话摘要，并在 Issue 创建/解决时同步更新 memory/tasks/。

由 Copilot 生成并提交。