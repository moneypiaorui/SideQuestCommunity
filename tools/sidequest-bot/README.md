# SideQuest MCP Bot Client

这是一个简单的 Python 客户端，用于通过 MCP 服务与 SideQuest 社区后端进行交互。

## 快速开始

1. **安装依赖**:
   ```bash
   pip install -r requirements.txt
   ```

2. **配置环境**:
   复制 `.env.example` 为 `.env` 并填入你的配置。
   ```bash
   cp .env.example .env
   ```

3. **运行**:
   ```bash
   python main.py
   ```

## 支持指令
- `列表`: 查看机器人目前支持的所有原子工具。
- `搜索 {关键词}`: 在社区内搜索相关帖子。
- `发帖 {标题} | {内容} | [分区ID]`: 发布一个新帖子。



