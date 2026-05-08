"""
异步方式调用 Chat Completions：适合与 FastAPI 等 async 框架组合。
用法与 sync_chat.py 相同，需配置 .env。
"""
from __future__ import annotations

import asyncio
import os
from pathlib import Path

from dotenv import load_dotenv
from openai import AsyncOpenAI

_ENV = Path(__file__).resolve().parent / ".env"
load_dotenv(_ENV)


async def main() -> None:
    if not os.environ.get("OPENAI_API_KEY"):
        raise SystemExit(
            "未检测到 OPENAI_API_KEY。请复制 .env.example 为 .env 并填入密钥。"
        )

    model = os.environ.get("OPENAI_MODEL", "gpt-4o-mini")
    client = AsyncOpenAI()

    response = await client.chat.completions.create(
        model=model,
        messages=[
            {"role": "system", "content": "你是一个简洁的助手，默认用中文回复。"},
            {"role": "user", "content": "异步客户端 AsyncOpenAI 适合什么场景？一句话。"},
        ],
    )

    msg = response.choices[0].message.content
    print("--- 助手回复 ---")
    print(msg)

    if response.usage:
        u = response.usage
        print("\n--- usage ---")
        print(
            f"prompt_tokens={u.prompt_tokens}, "
            f"completion_tokens={u.completion_tokens}, "
            f"total_tokens={u.total_tokens}"
        )


if __name__ == "__main__":
    asyncio.run(main())
