# memory/README.md

此目录保存仓库的“文件化记忆”。请按下列最小约定使用：

结构示例

- memory/
  - long/        # 长期记忆（决策、偏好、常用模板）
  - daily/       # 按天记录（2026-07-17.md）
  - tasks/       # 任务笔记（task-<id>.md）
  - sessions/    # 会话摘要（session-<timestamp>.md）
  - logs/        # agent 操作日志（可选）

操作指南

- 新任务：在 Issues 中创建任务并在 memory/tasks/ 新建对应文件。
- 今日记录：在 memory/daily/YYYY-MM-DD.md 中追加条目（时间 + 简要事件）。
- 长期记忆更新：在 memory/long/ 写入新的条目并在 daily 中记录变更参照。

示例命名

- memory/daily/2026-07-17.md
- memory/tasks/task-42.md
- memory/long/llm-preferences.md

