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
    private ChatMessage userMessage; // Message from the user (can be null for root node)

    private String content;
    private String fallback;
    private List<ChatbotNode> children;
    private ChatbotNode parent;
    private List<String> pendingChildIds; // For resolving references
    private MsgTemplate msgTemplate;

    /**
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
    }

    /**
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
    }

    /**
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
    }

    public String getId() {
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

    /**
     * Gets the first child of this node
     * This replaces the random selection with a deterministic approach
     * 
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
     * For CATEGORISE nodes, creates a JSON with type=CATEGORISE, empty category,
     * and the given message.
     * For EXTRACT nodes, creates a JSON with type=EXTRACT, category from parent
     * node or current category, and the given message.
     * 
     * @param userMessage The user's message to include in the request
     * @return JSONObject formatted for server communication
     */
    public JSONObject createRequestJson(String userMessage) {
        JSONObject jsonRequest = new JSONObject();
        try {
            if ("CATEGORISE".equals(type)) {
                jsonRequest.put("type", "CATEGORISE");
                jsonRequest.put("category", this.category); // Use this node's category instead of empty string
                jsonRequest.put("message", userMessage);
                System.out.println("CREATED CATEGORISE REQUEST: " + jsonRequest.toString());
            } else if ("EXTRACT".equals(type)) {
                jsonRequest.put("type", "EXTRACT");
                // Always use this node's category, not parent's
                String requestCategory = this.category;
                jsonRequest.put("category", requestCategory);
                jsonRequest.put("message", userMessage);
                System.out.println("CREATED EXTRACT REQUEST: " + jsonRequest.toString());
                System.out.println("CATEGORY FOR EXTRACT: " + requestCategory);
            } else {
                // Default to CATEGORISE as fallback
                jsonRequest.put("type", "CATEGORISE");
                jsonRequest.put("category", this.category); // Use this node's category
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
     * 
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
    }

    /**
     * Choose the next node in the conversation based on the current state and user
     * message
     * Uses the conversation path and category matching to make more context-aware
     * decisions
     * 
     * @return The next node in the conversation
     */
    public ChatbotNode chooseNextNode() {
        // Get the user message from this node
        String userMessageText = "";
        if (userMessage != null) {
            userMessageText = userMessage.getMessage();
        }

        System.out.println("DEBUG: Choosing next node with user message: " + userMessageText);
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
        }

        // Since server handles all node selection logic, we just need to match the
        // exact category
        // from server response to our node categories
        System.out.println("DEBUG: Looking for exact category match with server response: " + userMessageText);

        // These are the valid categories that can be returned by the server
        String[] validCategories = { "ΚΡΑΤΗΣΗ", "ΑΚΥΡΩΣΗ", "ΠΛΗΡΟΦΟΡΙΕΣ", "ΑΞΙΟΛΟΓΗΣΕΙΣ & ΣΧΟΛΙΑ",
                "ΠΡΟΣΦΟΡΕΣ & ΕΚΠΤΩΣΕΙΣ" };

        // First check if this input is a valid category from the server
        boolean isValidServerCategory = false;
        for (String validCategory : validCategories) {
            if (userMessageText.equals(validCategory)) {
                isValidServerCategory = true;
                System.out.println("DEBUG: Confirmed valid server category: " + validCategory);
                break;
            }
        }
        // Find the child with matching category or null if none found
        if (isValidServerCategory) {
            System.out.println("DEBUG: Checking each category explicitly for: '" + userMessageText + "'");

            // Explicit category checking for each main category
            if (userMessageText.equals("ΚΡΑΤΗΣΗ")) {
                System.out.println("DEBUG: Found ΚΡΑΤΗΣΗ category match");
                // Find child with ΚΡΑΤΗΣΗ category
                for (ChatbotNode child : getChildren()) {
                    if ("ΚΡΑΤΗΣΗ".equals(child.getCategory())) {
                        System.out.println("DEBUG: Returning ΚΡΑΤΗΣΗ node: " + child.getId());
                        return child;
                    }
                }
            } else if (userMessageText.equals("ΑΚΥΡΩΣΗ")) {
                System.out.println("DEBUG: Found ΑΚΥΡΩΣΗ category match");
                // Find child with ΑΚΥΡΩΣΗ category
                for (ChatbotNode child : getChildren()) {
                    if ("ΑΚΥΡΩΣΗ".equals(child.getCategory())) {
                        System.out.println("DEBUG: Returning ΑΚΥΡΩΣΗ node: " + child.getId());
                        return child;
                    }
                }
            } else if (userMessageText.equals("ΠΛΗΡΟΦΟΡΙΕΣ")) {
                System.out.println("DEBUG: Found ΠΛΗΡΟΦΟΡΙΕΣ category match");
                // Find child with ΠΛΗΡΟΦΟΡΙΕΣ category
                for (ChatbotNode child : getChildren()) {
                    if ("ΠΛΗΡΟΦΟΡΙΕΣ".equals(child.getCategory())) {
                        System.out.println("DEBUG: Returning ΠΛΗΡΟΦΟΡΙΕΣ node: " + child.getId());
                        return child;
                    }
                }
            } else if (userMessageText.equals("ΑΞΙΟΛΟΓΗΣΕΙΣ & ΣΧΟΛΙΑ")) {
                System.out.println("DEBUG: Found ΑΞΙΟΛΟΓΗΣΕΙΣ & ΣΧΟΛΙΑ category match");
                // Find child with ΑΞΙΟΛΟΓΗΣΕΙΣ & ΣΧΟΛΙΑ category
                for (ChatbotNode child : getChildren()) {
                    if ("ΑΞΙΟΛΟΓΗΣΕΙΣ & ΣΧΟΛΙΑ".equals(child.getCategory())) {
                        System.out.println("DEBUG: Returning ΑΞΙΟΛΟΓΗΣΕΙΣ & ΣΧΟΛΙΑ node: " + child.getId());
                        return child;
                    }
                }
            } else if (userMessageText.equals("ΠΡΟΣΦΟΡΕΣ & ΕΚΠΤΩΣΕΙΣ")) {
                System.out.println("DEBUG: Found ΠΡΟΣΦΟΡΕΣ & ΕΚΠΤΩΣΕΙΣ category match");
                // Find child with ΠΡΟΣΦΟΡΕΣ & ΕΚΠΤΩΣΕΙΣ category
                for (ChatbotNode child : getChildren()) {
                    if ("ΠΡΟΣΦΟΡΕΣ & ΕΚΠΤΩΣΕΙΣ".equals(child.getCategory())) {
                        System.out.println("DEBUG: Returning ΠΡΟΣΦΟΡΕΣ & ΕΚΠΤΩΣΕΙΣ node: " + child.getId());
                        return child;
                    }
                }
            }
        }

        System.out.println("DEBUG: No exact category match found for server response: " + userMessageText);
        return null;
    }// NLP and keyword matching methods have been removed since node selection is
     // handled by the server

    /**
     * Updates message_2 by processing it as a template with data from the node's
     * MsgTemplate.
     * If message_2 is empty or null, it will use the provided defaultTemplate
     * instead.
     * 
     * @param serverResponseJson JSON string containing server response data
     * @param defaultTemplate    Default template to use if message_2 is empty
     * @return true if template was successfully applied, false otherwise
     */
    public boolean applyTemplateToMessage2(String serverResponseJson, String defaultTemplate) {
        if (msgTemplate == null) {
            System.out.println("WARNING: No template found for node " + id);
            // Create a template based on the node's category
            try {
                msgTemplate = MsgTemplate.createTemplate(category);
                System.out.println("Created new template for category: " + category);
            } catch (Exception e) {
                System.err.println("Failed to create template for category " + category + ": " + e.getMessage());
                return false;
            }
        }

        try {
            System.out.println("Applying template for category: " + category);
            System.out.println("Server response: " + serverResponseJson);

            // Parse the JSON response from the server
            if (!msgTemplate.valuesFromJson(serverResponseJson)) {
                System.out.println("Failed to parse server response JSON for template values");
                return false;
            }

            // Choose the template (either message_2 if it exists or the default)
            String templateStr = (message_2 != null && !message_2.isEmpty() && message_2.contains("<"))
                    ? message_2
                    : defaultTemplate;

            if (templateStr == null || templateStr.isEmpty()) {
                System.out.println("WARNING: No template string available for node " + id);
                return false;
            }

            System.out.println("Using template string: " + templateStr);

            // Process the template with values from our template object
            String processedMessage = msgTemplate.processTemplate(templateStr);

            // Update message_2 with the processed template
            message_2 = processedMessage;

            // Also update the system message for consistency
            if (systemMessage != null) {
                systemMessage = new SystemMessage(message_2, ChatMessage.TYPE_BOT);
            }

            System.out.println("TEMPLATE APPLIED: " + message_2);
            return true;
        } catch (Exception e) {
            System.err.println("Error applying template: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    } // overrides toString method and shows all the fields of the class

    @Override
    public String toString() {
        return "ChatbotNode{\n" +
                "  id='" + id + '\'' + ",\n" +
                "  category='" + category + '\'' + ",\n" +
                "  type='" + type + '\'' + ",\n" +
                "  message='" + message + '\'' + ",\n" +
                "  message_1='" + message_1 + '\'' + ",\n" +
                "  message_2='" + message_2 + '\'' + ",\n" +
                "  systemMessage=" + systemMessage + ",\n" +
                "  userMessage=" + userMessage + ",\n" +
                "  content='" + content + '\'' + ",\n" +
                "  fallback='" + fallback + '\'' + ",\n" +
                "  childrenCount=" + (children != null ? children.size() : 0) + ",\n" +
                "  parent=" + (parent != null ? parent.getId() : "null") + ",\n" +
                "  pendingChildIds=" + pendingChildIds + "\n" +
                "  msgTemplate=" + (msgTemplate != null ? msgTemplate.toString() : "null") + "\n" +
                '}';
    }
}