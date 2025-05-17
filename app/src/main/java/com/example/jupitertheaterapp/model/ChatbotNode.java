package com.example.jupitertheaterapp.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ChatbotNode {
    private String id;
    private String type; // CATEGORISE or EXTRACT
    private String message; // Legacy field for backward compatibility
    private String message_1; // New field for normal message
    private String message_2; // New field for alternative message (e.g., all caps)
    private String content;
    private String fallback;
    private List<ChatbotNode> children;
    private ChatbotNode parent;
    private List<String> pendingChildIds; // For resolving references
    private Random random = new Random();
    private MsgTemplate msgTemplate;

    public ChatbotNode(String id, String type, String message, String content, String fallback) {
        this.id = id;
        this.type = type;
        this.message = message;
        this.message_1 = message; // Set the new message_1 field to be the same as message for backwards
                                  // compatibility
        this.message_2 = message; // Set the new message_2 field to be the same as message for backwards
                                  // compatibility
        this.content = content;
        this.fallback = fallback;
        this.children = new ArrayList<>();
        this.pendingChildIds = new ArrayList<>();
    }

    /**
     * Creates a node with separate message_1 and message_2 fields
     */
    public ChatbotNode(String id, String type, String message_1, String message_2, String content, String fallback) {
        this.id = id;
        this.type = type;
        this.message = message_1; // For backward compatibility, use message_1 as the default message
        this.message_1 = message_1;
        this.message_2 = message_2;
        this.content = content;
        this.fallback = fallback;
        this.children = new ArrayList<>();
        this.pendingChildIds = new ArrayList<>();
    }

    public void addChild(ChatbotNode child) {
        children.add(child);
    }

    public void addPendingChildId(String id) {
        pendingChildIds.add(id);
    }

    public List<String> getPendingChildIds() {
        return pendingChildIds;
    }

    public void clearPendingChildIds() {
        pendingChildIds.clear();
    }

    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public String getMessage() {
        // Default to message_1 if available
        return message_1 != null ? message_1 : message;
    }

    /**
     * Gets message_2 (the alternative message format, often all caps)
     */
    public String getMessage2() {
        return message_2 != null ? message_2 : getMessage();
    }

    public String getContent() {
        return content;
    }

    public String getFallback() {
        return fallback;
    }

    public boolean hasChildren() {
        return !children.isEmpty();
    }

    public List<ChatbotNode> getChildren() {
        return children;
    }

    public ChatbotNode getRandomChild() {
        if (children.isEmpty()) {
            return null;
        }
        return children.get(random.nextInt(children.size()));
    }

    public ChatbotNode getParent() {
        return parent;
    }

    public void setParent(ChatbotNode parent) {
        this.parent = parent;
    }

    public boolean isExtractNode() {
        return "EXTRACT".equals(type);
    }

    public boolean isCategoriseNode() {
        return "CATEGORISE".equals(type);
    }

    public void setMessageTemplate(MsgTemplate msgTemplate) {
        this.msgTemplate = msgTemplate;
    }

    public MsgTemplate getMessageTemplate() {
        return msgTemplate;
    }
}