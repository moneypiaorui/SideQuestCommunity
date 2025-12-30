import requests
import uuid
import json

class SideQuestMcpClient:
    def __init__(self, base_url, token):
        self.base_url = base_url.rstrip('/')
        self.token = token
        self.headers = {
            "Authorization": f"Bearer {token}",
            "Content-Type": "application/json"
        }

    def _call_rpc(self, method, params=None):
        """执行标准的 JSON-RPC 2.0 调用"""
        payload = {
            "jsonrpc": "2.0",
            "method": method,
            "params": params or {},
            "id": str(uuid.uuid4())
        }
        
        # 针对写入操作注入幂等性 Key
        headers = self.headers.copy()
        if method == "call_tool":
            headers["X-Idempotency-Key"] = str(uuid.uuid4())

        try:
            response = requests.post(
                f"{self.base_url}/api/mcp/rpc",
                json=payload,
                headers=headers
            )
            response.raise_for_status()
            return response.json()
        except Exception as e:
            return {"error": {"code": -1, "message": str(e)}}

    def list_tools(self):
        """获取所有可用工具列表"""
        return self._call_rpc("list_tools")

    def call_tool(self, tool_name, arguments):
        """调用特定的 MCP 工具"""
        return self._call_rpc("call_tool", {
            "name": tool_name,
            "arguments": arguments
        })

    def search_posts(self, keyword):
        """搜索帖子的快捷方法"""
        return self.call_tool("search_posts", {"keyword": keyword})

    def create_post(self, title, content, section_id=None):
        """发布帖子的快捷方法"""
        return self.call_tool("create_post", {
            "title": title,
            "content": content,
            "sectionId": section_id
        })



