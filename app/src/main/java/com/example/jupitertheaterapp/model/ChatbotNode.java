package com.example.jupitertheaterapp.model;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ChatbotNode {
    private String id; // Now in lowercase format like "kratisi", "kratisi1", etc.
    private String category; // New field for template matching like "ΚΡΑΤΗΣΗ", "ΠΛΗΡΟΦΟΡΙΕΣ", etc.
    private String type; // CATEGORISE or EXTRACT

    private ChatMessage systemMessage; // Message from the system (bot or server)
    private ChatMessage userMessage; // Message from the user (can be null for root node)

    // Node-specific message fields that were previously in SystemMessage
    private String message1; // Primary message
    private String message2; // Alternative/formatted message for display

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

        // Create system message with default content
        this.systemMessage = new SystemMessage(message, ChatMessage.TYPE_BOT);
        // Store message content in the node fields
        this.message1 = message;
        this.message2 = message;
        // User message can be null for system-initiated nodes
        this.userMessage = null;

        this.content = content;
        this.fallback = fallback;
        this.children = new ArrayList<>();
        this.pendingChildIds = new ArrayList<>();
    }

    /**
     * Constructor with separate message_1 and message_2 fields
     */
    public ChatbotNode(String id, String type, String message_1, String message_2, String content, String fallback) {
        this.id = id;
        this.category = id; // Default category to id, but should be overridden by proper category
        this.type = type;

        // Create system message with default content (message_1)
        this.systemMessage = new SystemMessage(message_1, ChatMessage.TYPE_BOT);
        // Store message content in the node fields
        this.message1 = message_1;
        this.message2 = message_2;
        // User message can be null for system-initiated nodes
        this.userMessage = null;

        this.content = content;
        this.fallback = fallback;
        this.children = new ArrayList<>();
        this.pendingChildIds = new ArrayList<>();
    }

    /**
     * Constructor that explicitly takes system and user messages
     */
    public ChatbotNode(String id, String type, ChatMessage systemMessage, ChatMessage userMessage,
            String content, String fallback) {
        this.id = id;
        this.category = id; // Default category to id, but should be overridden by proper category
        this.type = type;

        // Set messages
        this.systemMessage = systemMessage;
        this.userMessage = userMessage;

        // Set message fields with default values
        if (systemMessage != null) {
            this.message1 = systemMessage.getMessage();
            this.message2 = systemMessage.getMessage();

            // If it's a SystemMessage, we can check for JSON data and apply template
            if (systemMessage instanceof SystemMessage) {
                JSONObject details = ((SystemMessage) systemMessage).getDetails();
                if (details != null) {
                    // Apply template later if needed
                }
            }
        } else {
            this.message1 = "";
            this.message2 = "";
        }

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
        // Return message1 from the node directly
        return message1 != null ? message1 : "";
    }

    /**
     * Gets message_2 (the alternative message format, often all caps)
     */
    public String getMessage2() {
        // Return message2 from the node directly
        return message2 != null ? message2 : "";
    }

    /**
     * Sets the first message
     */
    public void setMessage1(String message1) {
        this.message1 = message1;
    }

    /**
     * Sets the second message
     */
    public void setMessage2(String message2) {
        this.message2 = message2;
    }

    /**
     * Gets the system message associated with this node
     */
    public ChatMessage getSystemMessage() {
        return systemMessage;
    }

    /**
     * Sets the system message for this node
     */
    public void setSystemMessage(ChatMessage systemMessage) {
        this.systemMessage = systemMessage;
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
    }

    // Overrides toString method and shows all the fields of the class
    @Override
    public String toString() {
        return "ChatbotNode{\n" +
                "  id='" + id + '\'' + ",\n" +
                "  category='" + category + '\'' + ",\n" +
                "  type='" + type + '\'' + ",\n" +
                "  message1='" + message1 + '\'' + ",\n" +
                "  message2='" + message2 + '\'' + ",\n" +
                "  systemMessage=" + systemMessage + ",\n" +
                "  userMessage=" + userMessage + ",\n" +
                "  content='" + content + '\'' + ",\n" +
                "  fallback='" + fallback + '\'' + ",\n" +
                "  childrenCount=" + (children != null ? children.size() : 0) + ",\n" +
                "  parent=" + (parent != null ? parent.getId() : "null") + ",\n" +
                "  pendingChildIds=" + pendingChildIds + ",\n" +
                "  msgTemplate=" + (msgTemplate != null ? msgTemplate.toString() : "null") + "\n" +
                '}';
    }

    /**
     * Updates the system message with a JSON response from the server
     * 
     * @param jsonResponse The JSON response from the server as a string
     * @param messageType  The type of message (BOT or SERVER)
     * @return True if successful, false otherwise
     */
    public boolean updateSystemMessageWithJson(String jsonResponse, int messageType) {
        try {
            // First, parse the JSON to extract category, error, and details
            JSONObject jsonObj = new JSONObject(jsonResponse);
            boolean hasDetails = jsonObj.has("details") && !jsonObj.isNull("details");
            String category = jsonObj.optString("category", "unknown");
            String errorMessage = jsonObj.has("error") && !jsonObj.isNull("error") ? jsonObj.getString("error") : null;

            // Set the category for this node
            if (!category.isEmpty()) {
                this.category = category;
            }

            // Create a SystemMessage with the JSON response
            SystemMessage sysMsg = new SystemMessage(jsonResponse, messageType, true);
            sysMsg.setCategory(category);

            // Override the default message in SystemMessage with our original message
            // This ensures systemMessage.getMessage() returns the proper template message
            if (this.message1 != null && !this.message1.isEmpty()) {
                sysMsg.setMessage(this.message1);
            }

            // Update the systemMessage reference
            this.systemMessage = sysMsg;

            // Preserve the original message1 (from conversation_tree.json)
            // Only set a default if it doesn't already have a value
            String basicMessage = this.message1;
            if (basicMessage == null || basicMessage.isEmpty()) {
                basicMessage = "Information about " + category;
                this.message1 = basicMessage;

                // Also update the SystemMessage
                sysMsg.setMessage(basicMessage);
            }

            // Only try to apply template if we have details or error
            if (hasDetails || errorMessage != null) {
                // Create or get template based on category from the response
                if (msgTemplate == null) {
                    try {
                        System.out.println("Creating template for category: " + category);
                        msgTemplate = MsgTemplate.createTemplate(category);
                    } catch (IllegalArgumentException e) {
                        System.err
                                .println("Failed to create template for category " + category + ": " + e.getMessage());
                        // If we can't create a template, preserve message2 or use message1 as fallback
                        if (this.message2 == null || this.message2.isEmpty()) {
                            this.message2 = basicMessage;
                        }
                        return true;
                    }
                }

                // Apply template to the message2 field in the node
                if (msgTemplate.valuesFromJson(jsonResponse)) {
                    // Use message2 from the node if it exists as the template
                    // This uses the message_2 from conversation_tree.json with placeholders
                    String templateStr = this.message2;

                    // If message2 is empty or null, handle error specifically or use fallback
                    // templates based on category
                    if (templateStr == null || templateStr.isEmpty()) {
                        // Handle error case specifically with proper error message
                        if (errorMessage != null && !errorMessage.isEmpty()) {
                            // Use actual error message instead of placeholder
                            templateStr = "Error: " + errorMessage;
                        } else {
                            // Create a more specific template based on category
                            switch (category) {
                                case "ΚΡΑΤΗΣΗ":
                                    templateStr = "Booking information for show <show_name> at <room> on <day> at <time>.";
                                    break;
                                case "ΑΚΥΡΩΣΗ":
                                    templateStr = "Cancellation information for reservation <reservation_number>.";
                                    break;
                                case "ΠΛΗΡΟΦΟΡΙΕΣ":
                                    templateStr = "Information about show <n> on <day> at <time> in <room>.";
                                    break;
                                case "ΑΞΙΟΛΟΓΗΣΕΙΣ & ΣΧΟΛΙΑ":
                                    templateStr = "Review information for reservation <reservation_number>: <stars> stars.";
                                    break;
                                case "ΠΡΟΣΦΟΡΕΣ & ΕΚΠΤΩΣΕΙΣ":
                                    templateStr = "Discount information for show <show_name> on <date>.";
                                    break;
                                default:
                                    templateStr = "Information for <category>: <details>";
                                    break;
                            }
                        }
                    }

                    // Process the template with values from our template object
                    String processedMessage = msgTemplate.processTemplate(templateStr);
                    this.message2 = processedMessage;

                    System.out.println("TEMPLATE APPLIED: " + processedMessage);
                    return true;
                } else {
                    System.out.println("Failed to parse JSON for template values");
                    // Keep the original message2 if it exists, otherwise use message1
                    if (this.message2 == null || this.message2.isEmpty()) {
                        this.message2 = basicMessage;
                    }
                    return true;
                }
            } else {
                // If no details or error, just use the message as is without applying a
                // template
                System.out.println("No details in JSON response, skipping template application");

                // Preserve existing message1 and message2
                if (this.message2 == null || this.message2.isEmpty()) {
                    this.message2 = basicMessage;
                }

                return true;
            }
        } catch (JSONException e) {
            System.err.println("Error parsing JSON: " + e.getMessage());
            e.printStackTrace();

            // Try to preserve existing messages when processing fails
            if (this.message1 != null && !this.message1.isEmpty()) {
                if (this.message2 == null || this.message2.isEmpty()) {
                    this.message2 = this.message1;
                }
                return true;
            }
            return false;
        } catch (Exception e) {
            System.err.println("Error updating system message with JSON: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}