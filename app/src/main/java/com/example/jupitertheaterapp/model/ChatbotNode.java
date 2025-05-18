package com.example.jupitertheaterapp.model;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ChatbotNode {
    private String id; // Now in lowercase format like "kratisi", "kratisi1", etc.
    private String category; // New field for template matching like "ΚΡΑΤΗΣΗ", "ΠΛΗΡΟΦΟΡΙΕΣ", etc.
    private String type; // CATEGORISE or EXTRACT
    // Legacy message fields - kept for backward compatibility
    private String message; 
    private String message_1;
    private String message_2;
    
    // New message fields for back-and-forth interaction
    private ChatMessage systemMessage; // Message from the system (bot or server)
    private ChatMessage userMessage;   // Message from the user (can be null for root node)
    
    private String content;
    private String fallback;
    private List<ChatbotNode> children;
    private ChatbotNode parent;    private List<String> pendingChildIds; // For resolving references
    private MsgTemplate msgTemplate;    /**
     * Legacy constructor for backward compatibility
     */
    public ChatbotNode(String id, String type, String message, String content, String fallback) {
        this.id = id;
        this.category = id; // Default category to id, but should be overridden by proper category
        this.type = type;
        // Legacy fields
        this.message = message;
        this.message_1 = message;
        this.message_2 = message;
        
        // New fields
        this.systemMessage = new SystemMessage(message, ChatMessage.TYPE_BOT);
        // User message can be null for system-initiated nodes
        this.userMessage = null;
        
        this.content = content;
        this.fallback = fallback;
        this.children = new ArrayList<>();
        this.pendingChildIds = new ArrayList<>();
    }    /**
     * Legacy constructor with separate message_1 and message_2 fields
     */
    public ChatbotNode(String id, String type, String message_1, String message_2, String content, String fallback) {
        this.id = id;
        this.category = id; // Default category to id, but should be overridden by proper category
        this.type = type;
        // Legacy fields
        this.message = message_1; // For backward compatibility
        this.message_1 = message_1;
        this.message_2 = message_2;
        
        // New fields
        this.systemMessage = new SystemMessage(message_1, ChatMessage.TYPE_BOT);
        // User message can be null for system-initiated nodes
        this.userMessage = null;
        
        this.content = content;
        this.fallback = fallback;
        this.children = new ArrayList<>();
        this.pendingChildIds = new ArrayList<>();
    }      /**
     * New constructor that explicitly takes system and user messages
     */
    public ChatbotNode(String id, String type, ChatMessage systemMessage, ChatMessage userMessage, 
                      String content, String fallback) {
        this.id = id;
        this.category = id; // Default category to id, but should be overridden by proper category
        this.type = type;
        
        // Set new fields
        this.systemMessage = systemMessage;
        this.userMessage = userMessage;
        
        // Set legacy fields for backward compatibility
        this.message = systemMessage.getMessage();
        this.message_1 = systemMessage.getMessage(); 
        this.message_2 = systemMessage.getMessage();
        
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
    }    public String getId() {
        return id;
    }
    
    public String getCategory() {
        return category;
    }
    
    public void setCategory(String category) {
        this.category = category;
    }

    public String getType() {
        return type;
    }

    public String getMessage() {
        // Default to message_1 if available for backward compatibility
        return message_1 != null ? message_1 : message;
    }

    /**
     * Gets message_2 (the alternative message format, often all caps)
     */
    public String getMessage2() {
        return message_2 != null ? message_2 : getMessage();
    }
    
    /**
     * Gets the system message associated with this node
     */
    public ChatMessage getSystemMessage() {
        // If systemMessage hasn't been set yet, create it from legacy fields
        if (systemMessage == null) {
            systemMessage = new SystemMessage(getMessage(), ChatMessage.TYPE_BOT);
        }
        return systemMessage;
    }
    
    /**
     * Sets the system message for this node
     */
    public void setSystemMessage(ChatMessage systemMessage) {
        this.systemMessage = systemMessage;
        // Update legacy fields for compatibility
        this.message = systemMessage.getMessage();
        this.message_1 = systemMessage.getMessage();
    }
    
    /**
     * Gets the user message associated with this node
     */
    public ChatMessage getUserMessage() {
        return userMessage;
    }
    
    /**
     * Sets the user message for this node
     */
    public void setUserMessage(ChatMessage userMessage) {
        this.userMessage = userMessage;
    }
    
    /**
     * Sets the user message for this node from a string
     */
    public void setUserMessage(String message) {
        this.userMessage = new UserMessage(message);
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
    }    /**
     * Gets the first child of this node
     * This replaces the random selection with a deterministic approach
     * @return The first child node, or null if no children
     */
    public ChatbotNode getFirstChild() {
        if (children.isEmpty()) {
            return null;
        }
        return children.get(0);
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
      /**
     * Creates a JSON object for server communication based on the node type.
     * For CATEGORISE nodes, creates a JSON with type=CATEGORISE, empty category, and the given message.
     * For EXTRACT nodes, creates a JSON with type=EXTRACT, category from parent node or current category, and the given message.
     * 
     * @param userMessage The user's message to include in the request
     * @return JSONObject formatted for server communication
     */
    public JSONObject createRequestJson(String userMessage) {
        JSONObject jsonRequest = new JSONObject();
        try {
            if ("CATEGORISE".equals(type)) {
                jsonRequest.put("type", "CATEGORISE");
                jsonRequest.put("category", ""); // Empty for CATEGORISE requests
                jsonRequest.put("message", userMessage);
                System.out.println("CREATED CATEGORISE REQUEST: " + jsonRequest.toString());
            } else if ("EXTRACT".equals(type)) {
                jsonRequest.put("type", "EXTRACT");
                // Get parent category or use current category
                String requestCategory;
                if (parent != null) {
                    requestCategory = parent.getCategory();
                } else {
                    // If no parent, use this node's category
                    requestCategory = this.category;
                }
                jsonRequest.put("category", requestCategory);
                jsonRequest.put("message", userMessage);
                System.out.println("CREATED EXTRACT REQUEST: " + jsonRequest.toString());
                System.out.println("CATEGORY FOR EXTRACT: " + requestCategory);
            } else {
                // Default to CATEGORISE as fallback
                jsonRequest.put("type", "CATEGORISE");
                jsonRequest.put("category", "");
                jsonRequest.put("message", userMessage);
                System.out.println("CREATED DEFAULT REQUEST: " + jsonRequest.toString());
            }
        } catch (Exception e) {
            System.err.println("Error creating request JSON: " + e.getMessage());
        }
        return jsonRequest;
    }
    
    /**
     * Gets the conversation path from the root node to this node
     * @return List of nodes in the conversation path (from root to this node)
     */
    public List<ChatbotNode> getConversationPath() {
        List<ChatbotNode> path = new ArrayList<>();
        
        // Add the current node as the first element
        path.add(0, this);
        
        // Traverse backwards to build the path
        ChatbotNode current = this;
        while (current.getParent() != null) {
            current = current.getParent();
            path.add(0, current); // Add to the beginning of the list
        }
        
        return path;
    }    /**
     * Choose the next node in the conversation based on the current state and user input
     * Uses the conversation path and category matching to make more context-aware decisions
     * 
     * @param userInput The user's message or server response
     * @return The next node in the conversation
     */
    public ChatbotNode chooseNextNode(String userInput) {
        System.out.println("DEBUG: Choosing next node with input: " + userInput);
        System.out.println("DEBUG: Current node ID: " + this.id + ", Category: " + this.category);
        
        // If no children, there's nowhere to go
        if (!hasChildren()) {
            System.out.println("DEBUG: No children found for node " + this.id);
            return null;
        }
        
        System.out.println("DEBUG: Available children count: " + this.children.size());
        System.out.println("DEBUG: Available child nodes for matching:");
        for (ChatbotNode child : getChildren()) {
            System.out.println("DEBUG:   - ID: " + child.getId() + ", Category: " + child.getCategory());
        }        // Since server handles all node selection logic, we just need to match the exact category
        // from server response to our node categories
        System.out.println("DEBUG: Looking for exact category match with server response: " + userInput);
        
        // These are the valid categories that can be returned by the server
        String[] validCategories = {"ΚΡΑΤΗΣΗ", "ΑΚΥΡΩΣΗ", "ΠΛΗΡΟΦΟΡΙΕΣ", "ΑΞΙΟΛΟΓΗΣΕΙΣ & ΣΧΟΛΙΑ", "ΠΡΟΣΦΟΡΕΣ & ΕΚΠΤΩΣΕΙΣ"};
        
        // First check if this input is a valid category from the server
        boolean isValidServerCategory = false;
        for (String validCategory : validCategories) {
            if (userInput.equals(validCategory)) {
                isValidServerCategory = true;
                System.out.println("DEBUG: Confirmed valid server category: " + validCategory);
                break;
            }
        }
        
        // Find the child with matching category or null if none found
        if (isValidServerCategory) {
            for (ChatbotNode child : getChildren()) {
                System.out.println("DEBUG: Comparing server category '" + userInput + 
                                  "' with child category '" + child.getCategory() + "'");
                if (userInput.equals(child.getCategory())) {
                    System.out.println("DEBUG: Found exact category match with child: " + child.getCategory() + 
                                      " (ID: " + child.getId() + ")");
                    return child;
                }
            }
        }
        
        System.out.println("DEBUG: No exact category match found for server response: " + userInput);
        return null;
    }    // NLP and keyword matching methods have been removed since node selection is handled by the server
    
    
}