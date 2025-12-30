import os
import sys
import json
from dotenv import load_dotenv
from openai import OpenAI
from mcp_client import SideQuestMcpClient

# 加载环境变量
load_dotenv()

def main():
    # 配置信息
    BASE_URL = os.getenv("SIDEQUEST_API_URL", "http://localhost:8080")
    TOKEN = os.getenv("SIDEQUEST_TOKEN")
    OPENAI_API_KEY = os.getenv("OPENAI_API_KEY")
    OPENAI_BASE_URL = os.getenv("OPENAI_BASE_URL") # 支持自定义 API 地址
    OPENAI_MODEL = os.getenv("OPENAI_MODEL", "gpt-3.5-turbo-0125")

    if not TOKEN:
        print("错误: 请在 .env 中设置 SIDEQUEST_TOKEN")
        return
    if not OPENAI_API_KEY:
        print("错误: 请在 .env 中设置 OPENAI_API_KEY")
        return

    mcp_client = SideQuestMcpClient(BASE_URL, TOKEN)
    ai_client = OpenAI(api_key=OPENAI_API_KEY, base_url=OPENAI_BASE_URL)

    # 定义工具描述（给 OpenAI 识别）
    tools = [
        {
            "type": "function",
            "function": {
                "name": "search_posts",
                "description": "搜索社区内的帖子",
                "parameters": {
                    "type": "object",
                    "properties": {
                        "keyword": {"type": "string", "description": "搜索关键词"},
                        "page": {"type": "integer", "description": "页码"},
                        "size": {"type": "integer", "description": "每页数量"}
                    },
                    "required": ["keyword"]
                }
            }
        },
        {
            "type": "function",
            "function": {
                "name": "create_post",
                "description": "发布新帖子到社区",
                "parameters": {
                    "type": "object",
                    "properties": {
                        "title": {"type": "string", "description": "帖子标题"},
                        "content": {"type": "string", "description": "帖子正文内容"},
                        "sectionId": {"type": "integer", "description": "分区 ID"}
                    },
                    "required": ["title", "content"]
                }
            }
        },
        {
            "type": "function",
            "function": {
                "name": "add_comment",
                "description": "对指定的帖子发表评论",
                "parameters": {
                    "type": "object",
                    "properties": {
                        "postId": {"type": "integer", "description": "帖子 ID"},
                        "content": {"type": "string", "description": "评论内容"}
                    },
                    "required": ["postId", "content"]
                }
            }
        },
        {
            "type": "function",
            "function": {
                "name": "like_post",
                "description": "给指定的帖子点赞",
                "parameters": {
                    "type": "object",
                    "properties": {
                        "postId": {"type": "integer", "description": "帖子 ID"}
                    },
                    "required": ["postId"]
                }
            }
        },
        {
            "type": "function",
            "function": {
                "name": "list_sections",
                "description": "获取社区所有可用的分区列表",
                "parameters": {"type": "object", "properties": {}}
            }
        },
        {
            "type": "function",
            "function": {
                "name": "list_popular_tags",
                "description": "获取当前热门标签",
                "parameters": {"type": "object", "properties": {}}
            }
        }
    ]

    print("=== SideQuest AI 智能助手已启动 ===")
    print("你可以直接用自然语言对我下达指令，例如：")
    print("- '帮我搜一下关于塞尔达的帖子'")
    print("- '帮我发个贴，标题是XXX，内容是YYY'")
    print("- '这篇 ID 为 10 的帖子不错，帮我点个赞'")

    messages = [{"role": "system", "content": "你是一个 SideQuest 社区的助手。你可以调用工具来执行用户要求的搜索、发帖、点赞、评论等操作。"}]

    while True:
        user_input = input("\n用户: ").strip()
        if user_input.lower() in ['exit', 'quit', '退出']:
            break
        if not user_input:
            continue

        messages.append({"role": "user", "content": user_input})

        try:
            # 1. 让 OpenAI 决定调用哪个工具
            response = ai_client.chat.completions.create(
                model=OPENAI_MODEL,
                messages=messages,
                tools=tools,
                tool_choice="auto"
            )

            response_message = response.choices[0].message
            tool_calls = response_message.tool_calls

            # 2. 如果 OpenAI 决定调用工具
            if tool_calls:
                messages.append(response_message)
                for tool_call in tool_calls:
                    function_name = tool_call.function.name
                    function_args = json.loads(tool_call.function.arguments)
                    
                    print(f"[*] AI 正在调用工具: {function_name}，参数: {function_args}")
                    
                    # 执行实际的 MCP 调用
                    mcp_res = mcp_client.call_tool(function_name, function_args)
                    
                    # 将结果反馈给 AI
                    messages.append({
                        "tool_call_id": tool_call.id,
                        "role": "tool",
                        "name": function_name,
                        "content": json.dumps(mcp_res, ensure_ascii=False)
                    })

                # 3. 让 AI 根据工具执行结果给出自然语言回复
                second_response = ai_client.chat.completions.create(
                    model=OPENAI_MODEL,
                    messages=messages
                )
                final_answer = second_response.choices[0].message.content
                print(f"助手: {final_answer}")
                messages.append({"role": "assistant", "content": final_answer})
            else:
                # 如果不需要调用工具，直接回复
                final_answer = response_message.content
                print(f"助手: {final_answer}")
                messages.append({"role": "assistant", "content": final_answer})

        except Exception as e:
            print(f"发生错误: {str(e)}")

if __name__ == "__main__":
    main()



