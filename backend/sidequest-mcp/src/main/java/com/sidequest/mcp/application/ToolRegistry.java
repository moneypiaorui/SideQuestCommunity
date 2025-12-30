package com.sidequest.mcp.application;

import lombok.Data;
import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.Map;
import java.util.Collection;

@Service
public class ToolRegistry {
    private final Map<String, ToolDefinition> tools = new HashMap<>();

    public ToolRegistry() {
        registerTool("create_post", "Create a new post in the community. Arguments: title, content, sectionId");
        registerTool("add_comment", "Add a comment to a post. Arguments: postId, content");
        registerTool("like_post", "Like a post. Arguments: postId");
        registerTool("list_sections", "List all available sections for posting");
        registerTool("list_popular_tags", "List popular tags");
        registerTool("search_posts", "Search for posts using keywords. Arguments: keyword, page, size");
    }

    public Collection<ToolDefinition> getTools() {
        return tools.values();
    }

    private void registerTool(String name, String description) {
        tools.put(name, new ToolDefinition(name, description));
    }

    @Data
    static class ToolDefinition {
        private final String name;
        private final String description;
    }
}

