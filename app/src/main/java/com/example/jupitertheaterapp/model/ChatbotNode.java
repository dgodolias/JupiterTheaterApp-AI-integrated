package com.example.jupitertheaterapp.model;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

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
    private ChatbotNode parent;
    private List<String> pendingChildIds; // For resolving references
    private Random random = new Random();
    private MsgTemplate msgTemplate;    /**
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
    
    /**
     * Creates a JSON object for server communication based on the node type.
     * For CATEGORISE nodes, creates a JSON with type=CATEGORISE, empty category, and the given message.
     * For EXTRACT nodes, creates a JSON with type=EXTRACT, category from parent node ID, and the given message.
     * 
     * @param userMessage The user's message to include in the request
     * @return JSONObject formatted for server communication
     */
    public JSONObject createRequestJson(String userMessage) {
        JSONObject jsonRequest = new JSONObject();
        try {
            // Get random categories to use when needed
            String[] categories = {"ΚΡΑΤΗΣΗ", "ΠΛΗΡΟΦΟΡΙΕΣ", "ΩΡΑΡΙΟ", "ΤΙΜΕΣ", "ΠΡΟΓΡΑΜΜΑ"};
            Random random = new Random();
            
            if ("CATEGORISE".equals(type)) {
                jsonRequest.put("type", "CATEGORISE");
                jsonRequest.put("category", ""); // Empty for CATEGORISE requests
                jsonRequest.put("message", userMessage);
                System.out.println("CREATED CATEGORISE REQUEST: " + jsonRequest.toString());
            } else if ("EXTRACT".equals(type)) {
                jsonRequest.put("type", "EXTRACT");
                  // Get parent category or use a random one if no parent
                String requestCategory;
                if (parent != null) {
                    requestCategory = parent.getCategory();
                } else {
                    // If no parent, use this node's category or a random one as fallback
                    requestCategory = this.category != null ? this.category : categories[random.nextInt(categories.length)];
                }
                  jsonRequest.put("category", requestCategory);
                jsonRequest.put("message", userMessage);
                System.out.println("CREATED EXTRACT REQUEST: " + jsonRequest.toString());
                System.out.println("CATEGORY FOR EXTRACT: " + requestCategory);
            } else {
                // Default to CATEGORISE with random values as fallback
                jsonRequest.put("type", random.nextBoolean() ? "CATEGORISE" : "EXTRACT");
                
                if (jsonRequest.getString("type").equals("EXTRACT")) {
                    // For EXTRACT, we need a category
                    jsonRequest.put("category", categories[random.nextInt(categories.length)]);
                } else {
                    // For CATEGORISE, empty category
                    jsonRequest.put("category", "");
                }
                
                jsonRequest.put("message", userMessage);
                System.out.println("CREATED DEFAULT REQUEST: " + jsonRequest.toString());
            }
        } catch (Exception e) {
            System.err.println("Error creating request JSON: " + e.getMessage());
        }
        return jsonRequest;
    }
}