"""
同步方式调用 Chat Completions：适合脚本与一次性实验。
运行前请在同目录配置 .env（参见 README.md）。
"""
from __future__ import annotations

import os
from pathlib import Path

from dotenv import load_dotenv
from openai import OpenAI

# 从 demo 目录加载 .env
_ENV = Path(__file__).resolve().parent / ".env"
load_dotenv(_ENV)


def main() -> None:
    api_key = os.environ.get("OPENAI_API_KEY")
    if not api_key:
        raise SystemExit(
            "未检测到 OPENAI_API_KEY。请复制 .env.example 为 .env 并填入密钥。"
        )

    # 模型名务必与你在平台账号中可用的名称一致
    model = os.environ.get("OPENAI_MODEL", "gpt-4o-mini")
    client = OpenAI()

    response = client.chat.completions.create(
        model=model,
        messages=[
            {
                "role": "system",
                "content": "你是一个简洁回答问题的助手，默认用中文回复。",
            },
            {"role": "user", "content": "用一句话解释什么是 token（大模型语境下）。"},
        ],
    )

    choice = response.choices[0]
    text = choice.message.content
    print("--- 助手回复 ---")
    print(text)

    if response.usage:
        u = response.usage
        print("\n--- usage（若 API 返回）---")
        print(
            f"prompt_tokens={u.prompt_tokens}, "
            f"completion_tokens={u.completion_tokens}, "
            f"total_tokens={u.total_tokens}"
        )

    if choice.finish_reason:
        print(f"\nfinish_reason={choice.finish_reason}")


if __name__ == "__main__":
    main()
