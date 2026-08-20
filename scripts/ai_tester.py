#!/usr/bin/env python3
"""
HabitPulse AI Tester
终端式 AI 对话测试工具，基于 zai-sdk（智谱官方 SDK），支持流式输出与工具调用。
"""

import json
import os
import re
import sys
from datetime import datetime
from pathlib import Path

from colorama import Fore, Style, init
from dotenv import load_dotenv
from zai import ZhipuAiClient

init(autoreset=True)

SCRIPT_DIR = Path(__file__).parent
PROJECT_ROOT = SCRIPT_DIR.parent
SYSTEM_PROMPT_PATH = (
    PROJECT_ROOT
    / "app"
    / "src"
    / "main"
    / "assets"
    / "prompts"
    / "system_prompt.md"
)

WEEKDAY_CN = ["周一", "周二", "周三", "周四", "周五", "周六", "周日"]


def load_env():
    load_dotenv(SCRIPT_DIR / ".env")
    api_key = os.getenv("ZHIPU_API_KEY")
    model = os.getenv("ZHIPU_MODEL")
    missing = []
    if not api_key:
        missing.append("ZHIPU_API_KEY")
    if not model:
        missing.append("ZHIPU_MODEL")
    if missing:
        print(
            f"{Fore.RED}错误: 请在 scripts/.env 中配置: {', '.join(missing)}{Style.RESET_ALL}"
        )
        print(f"{Fore.YELLOW}参考: cp .env.example .env 然后编辑{Style.RESET_ALL}")
        sys.exit(1)
    return api_key, model


def get_system_prompt() -> str:
    if not SYSTEM_PROMPT_PATH.exists():
        print(
            f"{Fore.RED}错误: 未找到提示词文件: {SYSTEM_PROMPT_PATH}{Style.RESET_ALL}"
        )
        sys.exit(1)

    template = SYSTEM_PROMPT_PATH.read_text(encoding="utf-8")
    now = datetime.now()
    day_of_week = WEEKDAY_CN[datetime.today().weekday()]

    return (
        template.replace("{current_date}", now.strftime("%Y-%m-%d"))
        .replace("{current_time}", now.strftime("%H:%M"))
        .replace("{current_day_of_week}", day_of_week)
    )


def build_tools() -> list:
    return [
        {
            "type": "function",
            "function": {
                "name": "create_habit",
                "description": (
                    "创建新习惯。所有必填字段齐全后才能调用。"
                    "调用后系统自动保存并展示确认卡片。"
                ),
                "parameters": {
                    "type": "object",
                    "properties": {
                        "title": {
                            "type": "string",
                            "description": "习惯名称，只提取核心动作",
                        },
                        "repeat_cycle": {
                            "type": "string",
                            "enum": ["DAILY", "WEEKLY"],
                            "description": "重复周期",
                        },
                        "repeat_days": {
                            "type": "array",
                            "items": {"type": "integer"},
                            "description": "每周日期，仅在 WEEKLY 时必填",
                        },
                        "reminder_times": {
                            "type": "array",
                            "items": {"type": "string"},
                            "description": "提醒时间数组，格式 HH:mm",
                        },
                        "notes": {
                            "type": "string",
                            "description": "备注（可选）",
                        },
                    },
                    "required": ["title", "repeat_cycle", "reminder_times"],
                },
            },
        },
        {
            "type": "function",
            "function": {
                "name": "ask_question",
                "description": "向用户提问以获取缺失或模糊的信息。一次只问一个字段。",
                "parameters": {
                    "type": "object",
                    "properties": {
                        "type": {
                            "type": "string",
                            "enum": [
                                "time",
                                "time_of_day",
                                "choice",
                                "multi_choice",
                                "day_of_week",
                                "text",
                                "confirm",
                            ],
                            "description": "问题类型",
                        },
                        "prompt": {
                            "type": "string",
                            "description": "展示给用户的问题文本",
                        },
                        "options": {
                            "type": "array",
                            "items": {"type": "string"},
                            "description": "选项数组",
                        },
                    },
                    "required": ["type", "prompt"],
                },
            },
        },
    ]


def display_code_blocks_analysis(text: str, tool_calls: list = None):
    """
    分析并显示代码块信息，但不修改原始内容。
    完全保留原始 text 不变，仅用于调试显示。
    """
    dim = f"{Fore.WHITE}{Style.DIM}"
    reset = Style.RESET_ALL
    print(f"\n{dim}{'─' * 50}{reset}")
    print(f"{Fore.YELLOW}📦 代码块检测（仅用于调试）{reset}")
    
    # 统计代码块数量
    code_block_count = 0
    if text:
        code_block_count = len(re.findall(r'```', text)) // 2
    
    if code_block_count > 0:
        print(f"      发现 {code_block_count} 个代码块{reset}")
    else:
        print(f"      {Fore.GREEN}未发现代码块{reset}")
    
    if tool_calls:
        print(f"      {Fore.MAGENTA}存在 {len(tool_calls)} 个工具调用{reset}")
    
    print(f"{dim}{'─' * 50}{reset}")


def stream_chat(
    client: ZhipuAiClient, model: str, messages: list, tools: list
):
    """
    流式调用，返回 (content, tool_calls_list, finish_reason)
    确保 content 完全按原样返回，不做任何修改。
    """
    content_parts = []
    tool_calls_map: dict[int, dict] = {}
    finish_reason = None
    printed_header = False

    response = client.chat.completions.create(
        model=model,
        messages=messages,
        tools=tools,
        stream=True,
        temperature=0.3,
    )

    for chunk in response:
        choice = chunk.choices[0] if chunk.choices else None
        if not choice:
            continue

        delta = choice.delta
        finish_reason = choice.finish_reason

        # 文本内容流式打印 - 完全原样输出
        if delta.content:
            if not printed_header:
                print(
                    f"{Fore.CYAN}🤖 AI > {Style.RESET_ALL}",
                    end="",
                    flush=True,
                )
                printed_header = True
            # 直接输出原始内容，不做任何处理
            print(delta.content, end="", flush=True)
            content_parts.append(delta.content)

        # 工具调用：静默累积，不展示
        if delta.tool_calls:
            for tc in delta.tool_calls:
                idx = tc.index
                if idx not in tool_calls_map:
                    tool_calls_map[idx] = {
                        "id": "",
                        "type": "function",
                        "function": {"name": "", "arguments": ""},
                    }

                if tc.id:
                    tool_calls_map[idx]["id"] = tc.id
                if tc.function:
                    if tc.function.name:
                        tool_calls_map[idx]["function"]["name"] += (
                            tc.function.name
                        )
                    if tc.function.arguments:
                        tool_calls_map[idx]["function"]["arguments"] += (
                            tc.function.arguments
                        )

    if printed_header:
        print()

    # 合并所有内容，保持原始顺序和格式
    content = "".join(content_parts)
    
    tool_calls = [
        {
            "id": tool_calls_map[idx]["id"],
            "type": tool_calls_map[idx]["type"],
            "function": {
                "name": tool_calls_map[idx]["function"]["name"],
                "arguments": tool_calls_map[idx]["function"]["arguments"],
            },
        }
        for idx in sorted(tool_calls_map.keys())
    ]

    return content, tool_calls, finish_reason


def handle_ask_question(args: dict) -> str:
    """展示提问内容，返回用户回答。"""
    prompt = args.get("prompt", "（无问题内容）")
    options = args.get("options", [])

    if options:
        print(f"{Fore.MAGENTA}❓ 提问 > {prompt}{Style.RESET_ALL}")
        for i, opt in enumerate(options, 1):
            print(f"      {Fore.MAGENTA}{i}. {opt}{Style.RESET_ALL}")
    else:
        print(f"{Fore.MAGENTA}❓ 提问 > {prompt}{Style.RESET_ALL}")

    return input(f"{Fore.GREEN}👤 你 > {Style.RESET_ALL}").strip()


VALID_CYCLES = ("DAILY", "WEEKLY")
TIME_RE = re.compile(r"^\d{2}:\d{2}$")


def handle_create_habit(args: dict) -> str:
    """校验并展示创建结果。返回模拟响应 JSON。"""
    errors = []
    title = args.get("title", "")
    cycle = args.get("repeat_cycle", "")
    times = args.get("reminder_times", [])
    days = args.get("repeat_days", [])
    notes = args.get("notes", "")

    if not isinstance(title, str) or not title.strip():
        errors.append(f"title: 必须是非空字符串 (当前: {title!r})")

    if cycle not in VALID_CYCLES:
        errors.append(f"repeat_cycle: 必须为 DAILY/WEEKLY (当前: {cycle!r})")

    if not isinstance(times, list) or len(times) == 0:
        errors.append(f"reminder_times: 必须是非空数组 (当前: {times!r})")
    else:
        bad = [t for t in times if not (isinstance(t, str) and TIME_RE.match(t))]
        if bad:
            errors.append(f"reminder_times 含无效元素, 需要 HH:mm 格式: {bad}")

    if cycle == "WEEKLY":
        if not isinstance(days, list) or len(days) == 0:
            errors.append(f"repeat_days: WEEKLY 周期时必须提供非空数组 (当前: {days!r})")
        else:
            bad = [d for d in days if not (isinstance(d, int) and 0 <= d <= 6)]
            if bad:
                errors.append(f"repeat_days 含无效元素, 需要 0-6 整数: {bad}")

    if notes and not isinstance(notes, str):
        errors.append(f"notes: 必须是字符串 (当前: {notes!r})")

    if errors:
        print(f"{Fore.YELLOW}🔧 工具 > ❌ 参数校验失败:{Style.RESET_ALL}")
        for e in errors:
            print(f"      {Fore.RED}• {e}{Style.RESET_ALL}")
        err_text = "参数校验失败，请修正后重新调用 create_habit：\n" + "\n".join(f"- {e}" for e in errors)
        return json.dumps({
            "success": False,
            "message": err_text,
            "errors": errors,
            "hint": "请修正 arguments 后重新调用 create_habit，不要重复使用相同的错误参数",
        }, ensure_ascii=False)

    if cycle == "WEEKLY" and days:
        day_names = [WEEKDAY_CN[d] for d in days if 0 <= d <= 6]
        cycle_str = f"每周（{'、'.join(day_names)}）"
    else:
        cycle_str = "每天"

    parts = [f"✅ 习惯已创建：{title}（{cycle_str}，{'、'.join(times)}）"]
    if notes:
        parts.append(f"      📝 备注：{notes}")
    print(f"{Fore.YELLOW}🔧 工具 > {chr(10).join(parts)}{Style.RESET_ALL}")

    return json.dumps(
        {
            "success": True,
            "message": f"习惯「{title}」已成功创建",
            "habit": {
                "title": title,
                "repeat_cycle": cycle,
                "reminder_times": times,
                "repeat_days": days,
                "notes": notes,
            },
        },
        ensure_ascii=False,
    )


def main():
    api_key, model = load_env()
    system_prompt = get_system_prompt()
    tools = build_tools()
    client = ZhipuAiClient(api_key=api_key)

    messages = [{"role": "system", "content": system_prompt}]

    sep = f"{Fore.WHITE}{Style.DIM}{'=' * 55}{Style.RESET_ALL}"
    print(f"\n{sep}")
    print(f"{Fore.WHITE}{Style.DIM}   HabitPulse AI 测试工具{Style.RESET_ALL}")
    print(f"{Fore.WHITE}{Style.DIM}   模型: {model}{Style.RESET_ALL}")
    print(
        f"{Fore.WHITE}{Style.DIM}   输入 exit / quit / 退出 结束对话{Style.RESET_ALL}"
    )
    print(f"{sep}\n")

    try:
        while True:
            raw = input(f"{Fore.GREEN}👤 你 > {Style.RESET_ALL}").strip()
            if not raw:
                continue
            if raw.lower() in ("exit", "quit", "退出"):
                print(
                    f"{Fore.WHITE}{Style.DIM}💬 系统 > 对话结束{Style.RESET_ALL}\n"
                )
                break

            messages.append({"role": "user", "content": raw})

            tool_rounds = 0
            create_habit_fails = 0
            while True:
                try:
                    content, tool_calls, finish_reason = stream_chat(
                        client, model, messages, tools
                    )
                except Exception as e:
                    print(
                        f"{Fore.RED}错误: API 请求失败 - {e}{Style.RESET_ALL}"
                    )
                    messages.pop()
                    break

                if finish_reason == "stop":
                    if content:
                        messages.append(
                            {"role": "assistant", "content": content}
                        )
                    # 只显示分析，不修改内容
                    display_code_blocks_analysis(content)
                    break

                elif finish_reason == "tool_calls" and tool_calls:
                    tool_rounds += 1
                    if tool_rounds > 20:
                        print(f"  {Fore.RED}⚠️  工具调用轮次过多 ({tool_rounds})，自动中断{Style.RESET_ALL}")
                        break
                    if create_habit_fails >= 5:
                        print(f"  {Fore.RED}⚠️  create_habit 连续失败 {create_habit_fails} 次，自动中断{Style.RESET_ALL}")
                        break
                    
                    assistant_msg = {"role": "assistant", "content": None}
                    if content:
                        assistant_msg["content"] = content
                    
                    # 显示工具调用信息但不修改原始内容
                    display_code_blocks_analysis(content, tool_calls)
                    
                    assistant_msg["tool_calls"] = [
                        {
                            "id": tc["id"],
                            "type": tc["type"],
                            "function": {
                                "name": tc["function"]["name"],
                                "arguments": tc["function"]["arguments"],
                            },
                        }
                        for tc in tool_calls
                    ]
                    messages.append(assistant_msg)

                    for tc in tool_calls:
                        fn_name = tc["function"]["name"]
                        fn_args = json.loads(tc["function"]["arguments"])

                        if fn_name == "ask_question":
                            ans = handle_ask_question(fn_args)
                            messages.append(
                                {
                                    "role": "tool",
                                    "tool_call_id": tc["id"],
                                    "content": ans,
                                }
                            )

                        elif fn_name == "create_habit":
                            result = handle_create_habit(fn_args)
                            parsed = json.loads(result)
                            if not parsed.get("success"):
                                create_habit_fails += 1
                            else:
                                create_habit_fails = 0
                            messages.append(
                                {
                                    "role": "tool",
                                    "tool_call_id": tc["id"],
                                    "content": result,
                                }
                            )

                    continue

                else:
                    break

    except KeyboardInterrupt:
        print(
            f"\n{Fore.WHITE}{Style.DIM}💬 系统 > 对话中断{Style.RESET_ALL}"
        )


if __name__ == "__main__":
    main()