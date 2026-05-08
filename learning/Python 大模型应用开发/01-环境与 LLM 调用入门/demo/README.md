# 阶段 1 示例：`环境与 LLM 调用入门`

## 示例总览

| 文件 / 目录 | 对应知识点 | 建议顺序 |
|-------------|------------|----------|
| `sync_chat.py` | 虚拟环境与依赖、`OpenAI` 同步客户端、`messages`、打印 `usage` | 1 |
| `async_chat.py` | `AsyncOpenAI`、`asyncio.run`、异步调用形态 | 2 |
| `.env.example` | 环境变量配置模板（复制为 `.env`） | 准备步骤 |
| `requirements.txt` | 依赖列表 | 准备步骤 |

## 环境要求

- **Python**：3.10 或以上（与 `openai` 包要求一致即可）。
- **网络**：能访问你所使用的 OpenAI API 端点（默认官方云；若使用代理或自建兼容网关，需在客户端配置 `base_url`，本阶段未展开）。
- **账号**：在 [OpenAI Platform](https://platform.openai.com/)（或你的兼容服务商）创建 API Key。

## 准备步骤

1. 进入本 `demo` 目录，创建虚拟环境（任选其一）：

   ```bash
   python -m venv .venv
   ```

2. 激活虚拟环境：

   - Windows PowerShell：`.\.venv\Scripts\Activate.ps1`
   - Windows CMD：`.venv\Scripts\activate.bat`
   - macOS/Linux：`source .venv/bin/activate`

3. 安装依赖：

   ```bash
   pip install -r requirements.txt
   ```

4. 复制环境变量模板并填写密钥：

   ```bash
   copy .env.example .env
   ```

   编辑 `.env`，设置 `OPENAI_API_KEY`；`OPENAI_MODEL` 请填写你账号下**当前可用**的模型名（文档与定价页会列出）。

## 运行命令

在已激活虚拟环境且已配置 `.env` 的前提下：

```bash
python sync_chat.py
```

```bash
python async_chat.py
```

## 学习建议

1. 先运行 `sync_chat.py`，确认能收到助手回复，并观察是否打印 `usage`。
2. 修改 `messages` 里 `user` 的 `content`，观察输出变化。
3. 将 `system` 提示改为英文或其他约束，体会「系统指令」对风格的影响。
4. 再运行 `async_chat.py`，对照 `THEORY.md` 中「同步 / 异步」一节，思考你的后续 Web 服务应使用哪种客户端。
5. **切勿**将 `.env` 提交到 Git；若需多人协作，每人本地各自维护 `.env`。

## 常见问题

- **提示未检测到 OPENAI_API_KEY**：确认 `.env` 与脚本在同一 `demo` 目录，且变量名无误。
- **模型不存在或无权使用**：在控制台核对模型名称拼写与你的账号权限。
- **连接超时 / 网络错误**：检查本机网络、防火墙或是否需要配置企业代理。
