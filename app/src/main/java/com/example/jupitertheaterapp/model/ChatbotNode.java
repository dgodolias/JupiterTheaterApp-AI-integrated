package com.example.jupitertheaterapp.model;

import org.json.JSONArray;
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

        this.message1 = "";
        this.message2 = "";


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

    public String getMessage1() {
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
    }    /**
     * Choose the next node in the conversation based on the current state and user
     * message. Uses a robust approach without relying on level-based handler functions.
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

        // Get the conversation path from root to current node
        List<ChatbotNode> pathToCurrentNode = getConversationPath();
        System.out.println("DEBUG: Path length: " + pathToCurrentNode.size());
        System.out.println("DEBUG: Path from root to current node: ");
        for (ChatbotNode node : pathToCurrentNode) {
            System.out.println("DEBUG:   - " + node.getId() + " (" + node.getCategory() + ")");
        }

        // These are the valid categories that can be returned by the server
        String[] validCategories = { "ΚΡΑΤΗΣΗ", "ΑΚΥΡΩΣΗ", "ΠΛΗΡΟΦΟΡΙΕΣ", "ΑΞΙΟΛΟΓΗΣΕΙΣ & ΣΧΟΛΙΑ",
                "ΠΡΟΣΦΟΡΕΣ & ΕΚΠΤΩΣΕΙΣ" };

        // Check if user message is a valid category from the server
        boolean isValidServerCategory = false;
        for (String validCategory : validCategories) {
            if (userMessageText.equals(validCategory)) {
                isValidServerCategory = true;
                System.out.println("DEBUG: Confirmed valid server category: " + validCategory);
                break;
            }
        }

        // Check for user confirmation or rejection keywords
        boolean isConfirmation = userMessageText.toLowerCase().contains("yes") || 
                                userMessageText.toLowerCase().contains("confirm") || 
                                userMessageText.toLowerCase().contains("ναι") || 
                                userMessageText.toLowerCase().contains("επιβεβαιώνω");
        
        boolean isRejection = userMessageText.toLowerCase().contains("no") || 
                             userMessageText.toLowerCase().contains("cancel") || 
                             userMessageText.toLowerCase().contains("όχι") || 
                             userMessageText.toLowerCase().contains("ακύρωση");

        System.out.println("DEBUG: isConfirmation: " + isConfirmation + ", isRejection: " + isRejection);

        // CASE-BASED NODE SELECTION APPROACH
        
        // Case 1: If we're at the root node, we need to match with a category node
        if ("root".equals(this.id)) {
            System.out.println("DEBUG: At root node, looking for category match");
            if (isValidServerCategory) {
                // Find matching category node
                for (ChatbotNode child : getChildren()) {
                    if (userMessageText.equals(child.getCategory())) {
                        System.out.println("DEBUG: Found matching category node: " + child.getId());
                        return handleNodeSelectionWithState(child);
                    }
                }
            }
            
            // If no direct match, try to find a child with matching category regardless of ID
            if (this.category != null && !this.category.isEmpty()) {
                for (ChatbotNode child : getChildren()) {
                    if (this.category.equals(child.getCategory())) {
                        System.out.println("DEBUG: Found child with matching category: " + child.getId());
                        return handleNodeSelectionWithState(child);
                    }
                }
            }
            
            // If still no match but we have children, return the first one as fallback
            if (hasChildren()) {
                System.out.println("DEBUG: No category match at root, returning first child: " + getFirstChild().getId());
                return handleNodeSelectionWithState(getFirstChild());
            }
            
            return null;
        }
        
        // Case 2: If we're at a main category node, check for template completeness
        else if (validCategoryNode(this)) {
            System.out.println("DEBUG: At category node " + this.id + ", checking template completeness");
            boolean hasCompleteInfo = hasCompleteTemplateInformation();
            
            // Fallback if template is null
            if (!hasCompleteInfo && getMessageTemplate() == null) {
                System.out.println("DEBUG: Template is null, using message text as fallback");
                hasCompleteInfo = userMessageText.contains("complete") || userMessageText.contains("ολοκληρωμένη") || 
                                 userMessageText.contains("some") || userMessageText.contains("κάποιες");
            }
            
            System.out.println("DEBUG: Template completeness check result: " + hasCompleteInfo);
            
            // Generic pattern matching based on category and template completeness
            String completePattern = null;
            String incompletePattern = null;
            
            // Determine patterns based on category
            if ("ΚΡΑΤΗΣΗ".equals(this.getCategory()) || "ΑΚΥΡΩΣΗ".equals(this.getCategory()) || "ΑΞΙΟΛΟΓΗΣΕΙΣ & ΣΧΟΛΙΑ".equals(this.getCategory())) {
                completePattern = "complete";
                incompletePattern = "incomplete";
            } else if ("ΠΛΗΡΟΦΟΡΙΕΣ".equals(this.getCategory()) || "ΠΡΟΣΦΟΡΕΣ & ΕΚΠΤΩΣΕΙΣ".equals(this.getCategory())) {
                completePattern = "some";
                incompletePattern = "none";
            }
            
            // Find the appropriate child based on completeness and category-specific patterns
            if (completePattern != null && incompletePattern != null) {
                String patternToFind = hasCompleteInfo ? completePattern : incompletePattern;
                System.out.println("DEBUG: Looking for child with pattern: " + patternToFind);
                
                for (ChatbotNode child : getChildren()) {
                    if (child.getId().contains(patternToFind)) {
                        System.out.println("DEBUG: Found matching child node: " + child.getId());
                        return handleNodeSelectionWithState(child);
                    }
                }
            }
        }
        
        // Case 3: If we're at a "complete/some" node, handle confirmation/rejection
        else if (this.getId().contains("complete") || this.getId().contains("some")) {
            System.out.println("DEBUG: At complete/some node: " + this.getId());
            
            if (isConfirmation) {
                System.out.println("DEBUG: At complete/some node with confirmation");
                // Look for confirmation node
                for (ChatbotNode child : getChildren()) {
                    if (child.getId().contains("confirmed")) {
                        System.out.println("DEBUG: Found confirmation node: " + child.getId());
                        return handleNodeSelectionWithState(child);
                    }
                }
            }
            else if (isRejection) {
                System.out.println("DEBUG: At complete/some node with rejection");
                // Look for rejection node
                for (ChatbotNode child : getChildren()) {
                    if (child.getId().contains("rejected") || child.getId().contains("cancel")) {
                        System.out.println("DEBUG: Found rejection node: " + child.getId());
                        return handleNodeSelectionWithState(child);
                    }
                }
            }
            
            // If user hasn't confirmed or rejected yet, we stay at the current node
            // by returning null (no navigation) or could return the most appropriate child
            System.out.println("DEBUG: No confirmation/rejection detected, staying at current node");
        }
        
        // Case 4: If we're at an "incomplete/none" node, find node for more info
        else if (this.getId().contains("incomplete") || this.getId().contains("none")) {
            System.out.println("DEBUG: At incomplete/none node: " + this.getId());
            
            // Try to find a node specifically designed to gather more information
            for (ChatbotNode child : getChildren()) {
                if (child.getId().contains("more")) {
                    System.out.println("DEBUG: Found more info node: " + child.getId());
                    return handleNodeSelectionWithState(child);
                }
            }
            
            // If no specific "more" node, we might want to ask for specific information
            // based on the template data we already have
            if (this.msgTemplate != null) {
                System.out.println("DEBUG: Using template to determine what information to request next");
                // Here you could implement logic to select a child based on missing template data
            }
        }
        
        // Case 5: If we're at a "confirmed" or "rejected" node, return to root
        else if (this.getId().contains("confirmed") || this.getId().contains("rejected")) {
            System.out.println("DEBUG: At confirmed/rejected node: " + this.getId() + ", looking for root");
            
            // Look for a direct link to root
            for (ChatbotNode child : getChildren()) {
                if ("root".equals(child.getId())) {
                    System.out.println("DEBUG: Found direct link to root node");
                    return handleNodeSelectionWithState(child);
                }
            }
            
            // If no direct link, we may need to traverse back through parent nodes
            // until we find the root
            ChatbotNode current = this;
            while (current != null && !current.getId().equals("root")) {
                if (current.getParent() != null && current.getParent().getId().equals("root")) {
                    System.out.println("DEBUG: Found root node via parent traversal");
                    return handleNodeSelectionWithState(current.getParent());
                }
                current = current.getParent();
            }
        }
        
        // Case 6: Terminal nodes that should return to root when we're done with them
        else if (isTerminalNode(this.getId())) {
            System.out.println("DEBUG: At terminal node: " + this.getId() + ", looking for root");
            
            // Look for a direct link to root
            for (ChatbotNode child : getChildren()) {
                if ("root".equals(child.getId())) {
                    System.out.println("DEBUG: Found direct link to root node");
                    return handleNodeSelectionWithState(child);
                }
            }
            
            // If no direct link, see if root is accessible through the parent
            if (this.getParent() != null && this.getParent().getId().equals("root")) {
                System.out.println("DEBUG: Returning to root via parent");
                return handleNodeSelectionWithState(this.getParent());
            }
        }
        
        // General pattern matching for any node not caught by specific cases above
        // This helps with custom node transitions not covered by our standard cases
        if (isConfirmation) {
            for (ChatbotNode child : getChildren()) {
                if (child.getId().contains("confirm")) {
                    System.out.println("DEBUG: Found confirmation-related node through pattern matching: " + child.getId());
                    return handleNodeSelectionWithState(child);
                }
            }
        }
        else if (isRejection) {
            for (ChatbotNode child : getChildren()) {
                if (child.getId().contains("reject") || child.getId().contains("cancel")) {
                    System.out.println("DEBUG: Found rejection-related node through pattern matching: " + child.getId());
                    return handleNodeSelectionWithState(child);
                }
            }
        }
        
        // If a category was determined from the user message, try to find a matching child
        if (this.category != null && !this.category.isEmpty()) {
            for (ChatbotNode child : getChildren()) {
                if (this.category.equals(child.getCategory())) {
                    System.out.println("DEBUG: Found child with matching category: " + child.getId());
                    return handleNodeSelectionWithState(child);
                }
            }
        }
        
        // Fallback: If no specific condition matched but we have children, return first child
        if (hasChildren()) {
            System.out.println("DEBUG: No specific condition matched, returning first child: " + getFirstChild().getId());
            return handleNodeSelectionWithState(getFirstChild());
        }
        
        System.out.println("DEBUG: No next node found, returning null");
        return null;
    }    /**
     * Checks if this is a valid category node (main category)
     * 
     * @param node The node to check
     * @return true if it's a main category node, false otherwise
     */
    private boolean validCategoryNode(ChatbotNode node) {
        String category = node.getCategory();
        return "ΚΡΑΤΗΣΗ".equals(category) || 
               "ΑΚΥΡΩΣΗ".equals(category) || 
               "ΠΛΗΡΟΦΟΡΙΕΣ".equals(category) || 
               "ΑΞΙΟΛΟΓΗΣΕΙΣ & ΣΧΟΛΙΑ".equals(category) || 
               "ΠΡΟΣΦΟΡΕΣ & ΕΚΠΤΩΣΕΙΣ".equals(category);
    }
    
    /**
     * Checks if this is a terminal node that should return to root
     * 
     * @param nodeId The node ID to check
     * @return true if it's a terminal node, false otherwise
     */
    private boolean isTerminalNode(String nodeId) {
        return nodeId.equals("booking_complete") || 
               nodeId.equals("booking_incomplete") ||
               nodeId.equals("cancel_complete") || 
               nodeId.equals("cancel_incomplete") || 
               nodeId.equals("info_some") || 
               nodeId.equals("info_none") || 
               nodeId.equals("review_complete") ||
               nodeId.equals("review_incomplete") ||
               nodeId.equals("discount_some") ||
               nodeId.equals("discount_none");
    }
    
    // NOTE: The level-based handler methods (handleRootNodeSelection, handleLevel1Selection, 
    // and handleDeepLevelSelection) have been removed as part of refactoring the node
    // selection logic to use a more robust case-based approach. If you encounter
    // any compile warnings about these methods, you can safely ignore them as they
    // are artifacts from the previous implementation that may be referenced elsewhere
    // but are no longer needed.

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
    }    /**
     * Updates the system message with a JSON response from the server.
     * This method is now primarily used internally by processMessageByState()
     * but can still be called directly if needed.
     * 
     * @param jsonResponse The JSON response from the server as a string
     * @param messageType  The type of message (BOT or SERVER)
     * @return True if successful, false otherwise
     */    public boolean fillMsg2FromTemplate(String jsonResponse, int messageType) {
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
            this.systemMessage = sysMsg;            // Preserve the original message1 (from conversation_tree.json)
            String basicMessage = this.message1;
            
            // Only set a default if it doesn't already have a value AND we're not processing a template response
            // This avoids setting the generic "Information about category" message
            if ((basicMessage == null || basicMessage.isEmpty()) && !hasDetails) {
                // Leave it empty if there's no meaningful message to display
                basicMessage = "";
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
                    String templateStr = this.message2;                    // If message2 is empty or null, handle error specifically or use fallback
                    // templates based on category
                    if (templateStr == null || templateStr.isEmpty()) {
                        // Handle error case specifically with proper error message
                        if (errorMessage != null && !errorMessage.isEmpty()) {
                            // Use actual error message instead of placeholder
                            templateStr = "Error: " + errorMessage;
                        } else {
                            // Use details from JSON response to create a dynamic message based on MsgTemplate
                            JSONObject details = jsonObj.optJSONObject("details");
                            if (details != null) {
                                // Extract formatted data from msgTemplate rather than hardcoding
                                StringBuilder sb = new StringBuilder();
                                
                                // Get all available fields from the template
                                if (msgTemplate != null) {
                                    String formatStr = this.formatTemplateBasedOnCategory(category);
                                    if (!formatStr.isEmpty()) {
                                        templateStr = formatStr;
                                    } else {
                                        // Fallback to direct extraction from JSON
                                        JSONArray names = details.names();
                                        if (names != null) {
                                            for (int i = 0; i < names.length(); i++) {
                                                String key = names.optString(i);
                                                String value = details.optString(key);
                                                if (value != null && !value.isEmpty()) {
                                                    if (sb.length() > 0) sb.append(", ");
                                                    sb.append(key).append(": ").append(value);
                                                }
                                            }
                                        }
                                        
                                        String extractedInfo = sb.toString().trim();
                                        if (!extractedInfo.isEmpty()) {
                                            templateStr = extractedInfo;
                                        } else {
                                            // If we couldn't extract anything meaningful, use a minimal message
                                            templateStr = category;
                                        }
                                    }
                                } else {
                                    // No template available
                                    // Fallback to direct extraction from JSON
                                    JSONArray names = details.names();
                                    if (names != null) {
                                        for (int i = 0; i < names.length(); i++) {
                                            String key = names.optString(i);
                                            String value = details.optString(key);
                                            if (value != null && !value.isEmpty()) {
                                                if (sb.length() > 0) sb.append(", ");
                                                sb.append(key).append(": ").append(value);
                                            }
                                        }
                                    }
                                    
                                    String extractedInfo = sb.toString().trim();
                                    if (!extractedInfo.isEmpty()) {
                                        templateStr = extractedInfo;
                                    } else {
                                        // If we couldn't extract anything meaningful, use a minimal message
                                        templateStr = category;
                                    }
                                }
                            } else {
                                // No details available
                                templateStr = "";
                            }
                        }
                    }

                    // Process the template with values from our template object
                    String processedMessage = msgTemplate.processTemplate(templateStr);
                    this.message2 = processedMessage;
                    
                    // Update message1 as well to ensure consistency
                    if (this.message1 == null || this.message1.isEmpty()) {
                        this.message1 = this.message2;
                    }                    System.out.println("TEMPLATE APPLIED: " + processedMessage);
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

    /**
     * Creates a format string with placeholders based on the node's category
     * @param category The category to create a format string for
     * @return A format string with appropriate placeholders
     */
    private String formatTemplateBasedOnCategory(String category) {
        if (msgTemplate == null) {
            return "";
        }
        
        // Create placeholders based on the template type using the standard format:
        // <placeholder_name> for each field
        
        if ("ΚΡΑΤΗΣΗ".equals(category)) {
            return "Παράσταση: <show_name>, Αίθουσα: <room>, Ημέρα: <day>, Ώρα: <time>, Όνομα: <person_name>";
        } 
        else if ("ΑΚΥΡΩΣΗ".equals(category)) {
            return "Ακύρωση κράτησης με αριθμό: <reservation_number>, Κωδικός: <passcode>";
        } 
        else if ("ΠΛΗΡΟΦΟΡΙΕΣ".equals(category)) {
            return "Πληροφορίες για: <name>, Ημέρα: <day>, Ώρα: <time>";
        } 
        else if ("ΑΞΙΟΛΟΓΗΣΕΙΣ & ΣΧΟΛΙΑ".equals(category)) {
            return "Αξιολόγηση για κράτηση: <reservation_number>, Βαθμολογία: <stars> αστέρια, Σχόλιο: <review>";
        } 
        else if ("ΠΡΟΣΦΟΡΕΣ & ΕΚΠΤΩΣΕΙΣ".equals(category)) {
            return "Προσφορά για: <show_name>, Ημερομηνία: <date>, Ηλικιακή κατηγορία: <age>, Άτομα: <no_of_people>";
        }
        
        return "";
    }

    /**
     * Ελέγχει αν το template του node έχει όλα τα απαραίτητα πεδία συμπληρωμένα
     * για την κατηγορία του
     * 
     * @return true αν το template είναι πλήρες, false διαφορετικά
     */
    public boolean hasCompleteTemplateInformation() {
        if (msgTemplate == null) {
            return false;
        }
        
        // Έλεγχος με βάση την κατηγορία
        if ("ΚΡΑΤΗΣΗ".equals(category)) {
            if (msgTemplate instanceof BookingTemplate) {
                BookingTemplate bookingTemplate = (BookingTemplate) msgTemplate;
                return !bookingTemplate.getShowName().isEmpty() && 
                       !bookingTemplate.getDay().isEmpty() && 
                       !bookingTemplate.getTime().isEmpty() && 
                       !bookingTemplate.getPerson().getName().isEmpty();
            }
        } 
        else if ("ΑΚΥΡΩΣΗ".equals(category)) {
            if (msgTemplate instanceof CancellationTemplate) {
                CancellationTemplate cancelTemplate = (CancellationTemplate) msgTemplate;
                return !cancelTemplate.getReservationNumber().isEmpty() && 
                       !cancelTemplate.getPasscode().isEmpty();
            }
        } 
        else if ("ΠΛΗΡΟΦΟΡΙΕΣ".equals(category)) {
            if (msgTemplate instanceof ShowInfoTemplate) {
                ShowInfoTemplate infoTemplate = (ShowInfoTemplate) msgTemplate;
                // Για πληροφορίες, θεωρούμε ότι έχουμε κάποιες πληροφορίες αν έχουμε τουλάχιστον ένα από τα βασικά πεδία
                return !infoTemplate.getName().isEmpty() || 
                       !infoTemplate.getDay().isEmpty() || 
                       !infoTemplate.getTopic().isEmpty();
            }
        } 
        else if ("ΑΞΙΟΛΟΓΗΣΕΙΣ & ΣΧΟΛΙΑ".equals(category)) {
            if (msgTemplate instanceof ReviewTemplate) {
                ReviewTemplate reviewTemplate = (ReviewTemplate) msgTemplate;
                return !reviewTemplate.getReservationNumber().isEmpty() && 
                       !reviewTemplate.getPasscode().isEmpty() && 
                       reviewTemplate.getStars() > 0;
            }
        } 
        else if ("ΠΡΟΣΦΟΡΕΣ & ΕΚΠΤΩΣΕΙΣ".equals(category)) {
            if (msgTemplate instanceof DiscountTemplate) {
                DiscountTemplate discountTemplate = (DiscountTemplate) msgTemplate;
                // Για προσφορές, θεωρούμε ότι έχουμε κάποιες πληροφορίες αν έχουμε τουλάχιστον ένα από τα βασικά πεδία
                return !discountTemplate.getShowName().isEmpty() || 
                       !discountTemplate.getAge().isEmpty() || 
                       !discountTemplate.getDate().isEmpty() ||
                       discountTemplate.getNumberOfPeople() > 0;
            }
        }
        
        return false;
    }

    /**
     * Updates the conversation state based on the current node.
     * This method should be called whenever transitioning to a new node.
     * 
     * @return The new state that was set
     */
    public ConversationState.State handleNodeTransition() {
        ConversationState conversationState = ConversationState.getInstance();
        ConversationState.State currentState = conversationState.getCurrentState();
        ConversationState.State newState = currentState; // Default to keeping the same state
        
        System.out.println("DEBUG: Handling node transition for node " + this.id);
        System.out.println("DEBUG: Current state: " + currentState);
        
        // ===================================================================
        // STATE TRANSITION LOGIC BASED ON NODE ID
        // ===================================================================
        
        // ROOT NODE - Initialize to INITIAL state
        if ("root".equals(this.id)) {
            newState = ConversationState.State.INITIAL;
            System.out.println("DEBUG: Setting state to INITIAL for root node");
        }
        
        // INFORMATION NODES - Set appropriate states for information flow
        else if ("info_some".equals(this.id)) {
            newState = ConversationState.State.GIVE_INFO;
            System.out.println("DEBUG: Setting state to GIVE_INFO for info_some node");
        }
        else if ("info_none".equals(this.id)) {
            newState = ConversationState.State.LLM_GET_INFO;
            System.out.println("DEBUG: Setting state to LLM_GET_INFO for info_none node");
        }
        
        // BOOKING NODES - Set appropriate states for booking flow
        else if ("booking_complete".equals(this.id)) {
            newState = ConversationState.State.CONFIRMATION;
            System.out.println("DEBUG: Setting state to CONFIRMATION for booking_complete node");
        }
        else if ("booking_incomplete".equals(this.id)) {
            newState = ConversationState.State.LLM_GET_INFO;
            System.out.println("DEBUG: Setting state to LLM_GET_INFO for booking_incomplete node");
        }
        
        // CANCELLATION NODES - Set appropriate states for cancellation flow
        else if ("cancel_complete".equals(this.id)) {
            newState = ConversationState.State.CONFIRMATION;
            System.out.println("DEBUG: Setting state to CONFIRMATION for cancel_complete node");
        }
        else if ("cancel_incomplete".equals(this.id)) {
            newState = ConversationState.State.LLM_GET_INFO;
            System.out.println("DEBUG: Setting state to LLM_GET_INFO for cancel_incomplete node");
        }
        
        // REVIEW NODES - Set appropriate states for review flow
        else if ("review_complete".equals(this.id)) {
            newState = ConversationState.State.CONFIRMATION;
            System.out.println("DEBUG: Setting state to CONFIRMATION for review_complete node");
        }
        else if ("review_incomplete".equals(this.id)) {
            newState = ConversationState.State.LLM_GET_INFO;
            System.out.println("DEBUG: Setting state to LLM_GET_INFO for review_incomplete node");
        }
        
        // DISCOUNT NODES - Set appropriate states for discount flow
        else if ("discount_some".equals(this.id)) {
            newState = ConversationState.State.GIVE_INFO;
            System.out.println("DEBUG: Setting state to GIVE_INFO for discount_some node");
        }
        else if ("discount_none".equals(this.id)) {
            newState = ConversationState.State.LLM_GET_INFO;
            System.out.println("DEBUG: Setting state to LLM_GET_INFO for discount_none node");
        }
        
        // CONFIRMATION NODES - Handle confirmation results
        else if (this.id.contains("confirmed")) {
            newState = ConversationState.State.GIVE_INFO;
            System.out.println("DEBUG: Setting state to GIVE_INFO for confirmation node");
        }
        else if (this.id.contains("rejected")) {
            newState = ConversationState.State.INITIAL;
            System.out.println("DEBUG: Setting state to INITIAL for rejection node");
        }
        
        // If state changed, update the ConversationState singleton
        if (newState != currentState) {
            conversationState.setCurrentState(newState);
            System.out.println("DEBUG: State updated to: " + newState);
        } else {
            System.out.println("DEBUG: State unchanged: " + currentState);
        }
        
        return newState;
    }    /**
     * Processes a message based on the current conversation state.
     * This method acts as a dispatcher that selects the appropriate processing method
     * based on the current conversation state and node type.
     * 
     * @param jsonResponse The JSON response from the server
     * @param messageType The type of message (BOT or SERVER)
     * @return True if successful, false otherwise
     */    public boolean processMessageByState(String jsonResponse, int messageType) {
        ConversationState conversationState = ConversationState.getInstance();
        ConversationState.State currentState = conversationState.getCurrentState();
        
        System.out.println("DEBUG: Processing message with state: " + currentState);
        System.out.println("DEBUG: Current node: " + this.id + ", Category: " + this.category);
        
        // Process message based on the current state
        switch (currentState) {
            case INITIAL:
                // In the initial state, just use the existing template processing
                System.out.println("DEBUG: Processing message with INITIAL state");
                return fillMsg2FromTemplate(jsonResponse, messageType);
                
            case LLM_GET_INFO:
                // When we need to get info from the user with LLM help
                System.out.println("DEBUG: Processing message with LLM_GET_INFO state");
                
                // Different handling based on category
                if ("ΚΡΑΤΗΣΗ".equals(this.category)) {
                    System.out.println("DEBUG: Processing booking info request");
                    // TODO: Use LLM to extract booking information more effectively
                }
                else if ("ΑΚΥΡΩΣΗ".equals(this.category)) {
                    System.out.println("DEBUG: Processing cancellation info request");
                    // TODO: Use LLM to extract cancellation information more effectively
                }
                else if ("ΠΛΗΡΟΦΟΡΙΕΣ".equals(this.category)) {
                    System.out.println("DEBUG: Processing show info request");
                    // TODO: Use LLM to extract show information queries more effectively
                }
                else if ("ΑΞΙΟΛΟΓΗΣΕΙΣ & ΣΧΟΛΙΑ".equals(this.category)) {
                    System.out.println("DEBUG: Processing review info request");
                    // TODO: Use LLM to extract review information more effectively
                }
                
                // For now, fall back to the template processing
                boolean llmProcessed = fillMsg2FromTemplate(jsonResponse, messageType);
                // Ensure message1 is also updated with meaningful content
                if (llmProcessed && this.message1 == null || this.message1.isEmpty()) {
                    this.message1 = "Χρειάζομαι περισσότερες πληροφορίες";
                }
                return llmProcessed;
                
            case GIVE_INFO:
                // When we need to provide info to the user (database lookup)
                System.out.println("DEBUG: GIVE_INFO state - Should search database with template data");
                
                // First apply the template to extract data
                boolean templateApplied = fillMsg2FromTemplate(jsonResponse, messageType);
                if (!templateApplied || msgTemplate == null) {
                    System.out.println("DEBUG: Failed to apply template or no template available");
                    return templateApplied;
                }
                
                // Different database queries based on category
                if ("ΠΛΗΡΟΦΟΡΙΕΣ".equals(this.category)) {
                    System.out.println("DEBUG: Should query show information from database");
                    // TODO: Query database for show information using msgTemplate
                    // Example:
                    // if (msgTemplate instanceof ShowInfoTemplate) {
                    //     ShowInfoTemplate infoTemplate = (ShowInfoTemplate) msgTemplate;
                    //     String showName = infoTemplate.getName();
                    //     String day = infoTemplate.getDay();
                    //     // Query database with these parameters
                    //     // Update message2 with results
                    // }
                }
                else if ("ΠΡΟΣΦΟΡΕΣ & ΕΚΠΤΩΣΕΙΣ".equals(this.category)) {
                    System.out.println("DEBUG: Should query discount information from database");
                    // TODO: Query database for discount information using msgTemplate
                }
                
                // Ensure message1 has meaningful content
                if (this.message1 == null || this.message1.isEmpty()) {
                    this.message1 = "Ορίστε οι πληροφορίες που ζητήσατε";
                }
                return true;
                
            case CONFIRMATION:
                // When we need to ask for confirmation
                System.out.println("DEBUG: CONFIRMATION state - Should prepare confirmation message");
                
                // First apply the template
                boolean success = fillMsg2FromTemplate(jsonResponse, messageType);
                
                // Then enhance the message to make it clear confirmation is needed
                if (success) {
                    // Different confirmation messages based on node/category
                    String confirmationPrompt = "";
                    if ("booking_complete".equals(this.id)) {
                        // Add confirmation prompt to the end of message2
                        confirmationPrompt = "\n\nΘέλετε να επιβεβαιώσετε αυτή την κράτηση; (ναι/όχι)";
                    }
                    else if ("cancel_complete".equals(this.id)) {
                        // Add confirmation prompt for cancellation
                        confirmationPrompt = "\n\nΘέλετε να επιβεβαιώσετε αυτήν την ακύρωση; (ναι/όχι)";
                    }
                    else if ("review_complete".equals(this.id)) {
                        // Add confirmation prompt for review submission
                        confirmationPrompt = "\n\nΘέλετε να υποβάλετε αυτή την αξιολόγηση; (ναι/όχι)";
                    }
                    
                    // Add to both message1 and message2 for consistency
                    if (!this.message1.contains(confirmationPrompt)) {
                        this.message1 += confirmationPrompt;
                    }
                    
                    if (!this.message2.contains(confirmationPrompt)) {
                        this.message2 += confirmationPrompt;
                    }
                }
                
                return success;
                
            case EXIT:
                // When we're exiting the conversation
                System.out.println("DEBUG: EXIT state - Should prepare exit message");
                
                // Apply template first
                boolean templateSuccess = fillMsg2FromTemplate(jsonResponse, messageType);
                
                // Add exit message to both message fields
                String exitMessage = "\n\nΕυχαριστούμε που χρησιμοποιήσατε το σύστημα του Jupiter Theater!";
                if (!this.message1.contains(exitMessage)) {
                    this.message1 += exitMessage;
                }
                
                if (!this.message2.contains(exitMessage)) {
                    this.message2 += exitMessage;
                }
                
                return templateSuccess;
                
            default:
                // Fallback to regular template processing
                System.out.println("DEBUG: Unknown state - Falling back to template processing");
                return fillMsg2FromTemplate(jsonResponse, messageType);
        }
    }

    /**
     * Gets the current conversation state
     * 
     * @return The current conversation state
     */
    public ConversationState.State getCurrentState() {
        ConversationState conversationState = ConversationState.getInstance();
        return conversationState.getCurrentState();
    }

    /**
     * Modifies the chooseNextNode method to add a wrapper that captures the return value
     * and performs state transition handling before returning it.
     * This ensures state is always updated when a new node is selected.
     * 
     * @param nextNode The node that was selected by chooseNextNode
     * @return The same node, after handling any needed state transitions
     */
    private ChatbotNode handleNodeSelectionWithState(ChatbotNode nextNode) {
        if (nextNode != null) {
            System.out.println("DEBUG: Selected next node: " + nextNode.getId() + ", handling state transition");
            // This will update the state based on the node's type
            nextNode.handleNodeTransition();
        } else {
            System.out.println("DEBUG: No node selected, state unchanged");
        }
        return nextNode;
    }    /**
     * Handles a complete conversation turn, processing the user message,
     * updating the conversation state, and formulating a response.
     * 
     * @param jsonResponse JSON response from the server
     * @param messageType Type of message (BOT, SERVER)
     * @return A combined response message to show to the user
     */    public String handleConversationTurn(String jsonResponse, int messageType) {
        System.out.println("DEBUG: Handling conversation turn for node: " + this.id);
        
        ConversationState.State state = handleNodeTransition();
        System.out.println("DEBUG: Current state after transition: " + state);
        
        boolean processed = processMessageByState(jsonResponse, messageType);
        if (!processed) {
            System.out.println("WARNING: Failed to process message for node: " + this.id);
            return getMessage1() != null && !getMessage1().isEmpty() ? getMessage1() : fallback;
        }
        
        String combinedMessage = "";
        String detailsMessage = "";

        if (messageType == ChatMessage.TYPE_SERVER) {
            System.out.println("DEBUG: Server response. message2 (template applied): '" + getMessage2() + "', message1: '" + getMessage1() + "'");
            detailsMessage = getMessage2();
            if (detailsMessage == null || detailsMessage.isEmpty()) {
                 // If message2 is empty, it implies template application might have yielded nothing, or no details were applicable.
                 // We don't want to use message1 from the current node here if it's a generic prompt for information
                 // that the server response was supposed to fulfill. Let detailsMessage remain empty or rely on specific prompts later.
                 System.out.println("DEBUG: message2 from server response is empty.");
            }
        } else { // This is a BOT turn, not a direct server response processing for the current node.
            System.out.println("DEBUG: Bot turn. message1: '" + getMessage1() + "', message2: '" + getMessage2() + "'");
            combinedMessage = getMessage1(); // Primary message for bot turns
            String message2Text = getMessage2();
            // Append message2 if it's different and not a generic system prefix
            if (message2Text != null && !message2Text.isEmpty() && !message2Text.equals(combinedMessage) &&
                !message2Text.startsWith("Information about") &&
                !message2Text.startsWith("Server response for category")) {
                combinedMessage += "\n" + message2Text;
            }
            if (combinedMessage == null || combinedMessage.isEmpty()) {
                combinedMessage = fallback;
            }
            System.out.println("DEBUG: Combined message (bot turn, returning early): " + combinedMessage);
            return combinedMessage; // For non-server triggered turns, this is usually it.
        }

        // Now, handle server response (detailsMessage) and potentially add missing info prompt
        String missingInfoPromptText = "";
        // Check states where we might need to ask for more info
        if (state == ConversationState.State.LLM_GET_INFO) {
            if (msgTemplate != null) {
                missingInfoPromptText = getMissingInfoPrompt(); // Formatted as "Για να ολοκληρωθεί... <fields> ..."
            }
        }

        // Construct the final message
        if (detailsMessage != null && !detailsMessage.isEmpty()) {
            combinedMessage = detailsMessage;
            if (!missingInfoPromptText.isEmpty()) {
                // Append missing info prompt if it's not already essentially in detailsMessage
                if (!detailsMessage.contains(missingInfoPromptText.substring(0, Math.min(missingInfoPromptText.length(), 20)))) { // Check for partial overlap
                    combinedMessage += "\n" + missingInfoPromptText;
                }
            }
        } else { // No detailsMessage from server (or it was empty)
            // Use the current node's message1 as a prefix if it exists and is relevant
            String currentMessage1 = getMessage1();
            if (!missingInfoPromptText.isEmpty()) {
                if (currentMessage1 != null && !currentMessage1.isEmpty() && !currentMessage1.equals(missingInfoPromptText) && !missingInfoPromptText.contains(currentMessage1)) {
                     // e.g. currentMessage1 = "Για να ολοκληρωθεί η κράτησή σας, χρειάζομαι περισσότερες πληροφορίες."
                     // missingInfoPromptText = "Για να ολοκληρωθεί η κράτηση χρειάζομαι: <fields>..."
                     // Check if currentMessage1 is the generic lead-in for the specific missing prompt.
                    if (currentMessage1.startsWith("Για να ολοκληρωθεί η κράτησή σας")) {
                        combinedMessage = missingInfoPromptText; // The prompt itself is comprehensive
                    } else {
                        combinedMessage = currentMessage1 + "\n" + missingInfoPromptText;
                    }
                } else {
                    combinedMessage = missingInfoPromptText; // Only the prompt
                }
            } else if (currentMessage1 != null && !currentMessage1.isEmpty()) {
                combinedMessage = currentMessage1; // No details, no missing info, just current node's message1
            } else {
                combinedMessage = fallback; // Absolute last resort
            }
        }

        if (combinedMessage == null || combinedMessage.isEmpty()) {
            combinedMessage = fallback; 
        }

        // Clean up welcome messages (existing logic)
        if (!this.getId().equals("root") && 
            (combinedMessage.contains("Καλωσορίσατε") || 
             combinedMessage.contains("Καλώς ήρθατε") ||
             combinedMessage.contains("Welcome"))) {
            if (combinedMessage.length() < 50) {
                combinedMessage = "Επεξεργάζομαι την ερώτησή σας...";
            } else {
                int firstLineBreak = combinedMessage.indexOf("\n");
                if (firstLineBreak > 0) {
                    combinedMessage = combinedMessage.substring(firstLineBreak + 1).trim();
                }
            }
        }

        // Add confirmation prompt if in CONFIRMATION state (restored logic)
        if (state == ConversationState.State.CONFIRMATION) {
            if (!combinedMessage.contains("(ναι/όχι)")) {
                combinedMessage += "\n\nΠαρακαλώ επιβεβαιώστε (ναι/όχι).";
            }
        }
        
        System.out.println("DEBUG: Final combined message after all processing: " + combinedMessage);
        return combinedMessage;
    }
    
    /**
     * Generates a prompt asking for specific missing information based on the template
     * 
     * @return A prompt for the missing information
     */
    private String getMissingInfoPrompt() {
        if (msgTemplate == null) {
            return "";
        }

        String missingFieldsGreek = msgTemplate.getMissingFieldsAsGreekString();
        if (missingFieldsGreek == null || missingFieldsGreek.isEmpty()) {
            return "";
        }

        return "Για να ολοκληρωθεί η κράτηση χρειάζομαι: " + missingFieldsGreek + ". Παρακαλώ δώστε τις πληροφορίες που λείπουν.";
    }
}