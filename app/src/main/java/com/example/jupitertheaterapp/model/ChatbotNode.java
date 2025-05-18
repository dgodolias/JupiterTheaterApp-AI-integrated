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
    private MsgTemplate msgTemplate;/**
     * Legacy constructor for backward compatibility
     */
    public ChatbotNode(String id, String type, String message, String content, String fallback) {
        this.id = id;
        this.category = id; // Initialize category with id for backward compatibility
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
        this.category = id; // Initialize category with id for backward compatibility
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
    }
      /**
     * New constructor that explicitly takes system and user messages
     */
    public ChatbotNode(String id, String type, ChatMessage systemMessage, ChatMessage userMessage, 
                      String content, String fallback) {
        this.id = id;
        this.category = id; // Initialize category with id for backward compatibility
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
     * @param userInput The user's message
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
        
        System.out.println("DEBUG: Children count: " + this.children.size());
        for (ChatbotNode child : getChildren()) {
            System.out.println("DEBUG: Child node - ID: " + child.getId() + ", Category: " + child.getCategory());
        }
        
        // For the root node, use keyword matching to find the most relevant category
        if (getId().equals("root")) {
            System.out.println("DEBUG: Processing root node logic");
            
            // First, try direct text match with child categories
            for (ChatbotNode child : getChildren()) {
                if (userInput.toLowerCase().contains(child.getCategory().toLowerCase())) {
                    System.out.println("DEBUG: Direct match found with category: " + child.getCategory());
                    return child;
                }
            }
            
            // If no direct match found, try to match by category keywords
            ChatbotNode matchedNode = findNodeByCategoryKeywords(userInput);
            if (matchedNode != null) {
                System.out.println("DEBUG: Keyword match found with category: " + matchedNode.getCategory());
                return matchedNode;
            }
            
            System.out.println("DEBUG: No matching node found for: " + userInput);
            return null;
        }
        
        // For non-root nodes with server response (like "ΠΡΟΣΦΟΡΕΣ & ΕΚΠΤΩΣΕΙΣ")
        // Try exact category matching first
        for (ChatbotNode child : getChildren()) {
            if (userInput.equalsIgnoreCase(child.getCategory())) {
                System.out.println("DEBUG: Exact category match found: " + child.getCategory());
                return child;
            }
        }
        
        // If no exact match, try partial matching
        ChatbotNode bestMatch = null;
        for (ChatbotNode child : getChildren()) {
            if (userInput.toLowerCase().contains(child.getCategory().toLowerCase()) || 
                child.getCategory().toLowerCase().contains(userInput.toLowerCase())) {
                System.out.println("DEBUG: Partial category match found: " + child.getCategory());
                bestMatch = child;
                break;
            }
        }
        
        System.out.println("DEBUG: Returning " + (bestMatch != null ? "matched node: " + bestMatch.getCategory() : "null"));
        return bestMatch;
    }
    
    /**
     * Finds a child node that matches the given input by keywords in categories
     * 
     * @param input The user input or server response to match
     * @return The matching child node or null if no match found
     */
    private ChatbotNode findNodeByCategoryKeywords(String input) {
        System.out.println("DEBUG: Looking for keyword matches in: " + input);
        
        // Define common keywords for different categories
        // Add your category-keyword mappings here
        if (input.toLowerCase().contains("προσφορ") || input.toLowerCase().contains("εκπτωσ")) {
            for (ChatbotNode child : getChildren()) {
                if (child.getCategory().toLowerCase().contains("προσφορ") || 
                    child.getCategory().toLowerCase().contains("εκπτωσ")) {
                    return child;
                }
            }
        }
        
        if (input.toLowerCase().contains("πληροφορ") || input.toLowerCase().contains("ερωτησ")) {
            for (ChatbotNode child : getChildren()) {
                if (child.getCategory().toLowerCase().contains("πληροφορ")) {
                    return child;
                }
            }
        }
        
        if (input.toLowerCase().contains("κρατησ") || input.toLowerCase().contains("θεσ")) {
            for (ChatbotNode child : getChildren()) {
                if (child.getCategory().toLowerCase().contains("κρατησ")) {
                    return child;
                }
            }
        }
        
        // Add more category keyword mappings as needed
        
        return null;
    }
    
    
}