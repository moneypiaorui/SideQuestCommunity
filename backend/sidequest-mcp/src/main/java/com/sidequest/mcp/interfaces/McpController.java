package com.sidequest.mcp.interfaces;

import com.sidequest.mcp.application.ToolRegistry;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import com.sidequest.mcp.application.ToolRegistry;
import com.sidequest.mcp.infrastructure.feign.CoreClient;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/mcp")
@RequiredArgsConstructor
@Slf4j
public class McpController {
    private final ToolRegistry toolRegistry;
    private final CoreClient coreClient;

    @PostMapping("/rpc")
    public JsonRpcResponse handleRpc(@RequestBody JsonRpcRequest request) {
        log.info("Received MCP JSON-RPC request: {}", request.getMethod());
        
        JsonRpcResponse response = new JsonRpcResponse();
        response.setId(request.getId());
        response.setJsonrpc("2.0");

        if ("list_tools".equals(request.getMethod())) {
            response.setResult(toolRegistry.getTools());
        } else if ("call_tool".equals(request.getMethod())) {
            String toolName = (String) request.getParams().get("name");
            Map<String, Object> arguments = (Map<String, Object>) request.getParams().get("arguments");
            
            try {
                Object result = executeTool(toolName, arguments);
                response.setResult(result);
            } catch (Exception e) {
                log.error("Error executing tool: {}", toolName, e);
                response.setError(Map.of("code", -32000, "message", e.getMessage()));
            }
        } else {
            response.setError(Map.of("code", -32601, "message", "Method not found"));
        }

        return response;
    }

    private Object executeTool(String toolName, Map<String, Object> arguments) {
        if ("create_post".equals(toolName)) {
            CoreClient.CreatePostDTO dto = new CoreClient.CreatePostDTO();
            dto.setTitle((String) arguments.get("title"));
            dto.setContent((String) arguments.get("content"));
            dto.setSectionId(arguments.get("sectionId") != null ? Long.valueOf(arguments.get("sectionId").toString()) : null);
            return coreClient.createPost(dto);
        } else if ("add_comment".equals(toolName)) {
            Long postId = Long.valueOf(arguments.get("postId").toString());
            String content = (String) arguments.get("content");
            return coreClient.addComment(postId, content);
        }
        throw new IllegalArgumentException("Unknown tool: " + toolName);
    }

    @Data
    public static class JsonRpcRequest {
        private String jsonrpc;
        private String method;
        private Map<String, Object> params;
        private String id;
    }

    @Data
    public static class JsonRpcResponse {
        private String jsonrpc;
        private Object result;
        private Object error;
        private String id;
    }
}

