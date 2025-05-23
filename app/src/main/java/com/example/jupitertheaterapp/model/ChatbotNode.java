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
    }

    /**
     * Choose the next node in the conversation based on the current state and user
     * message. Uses a robust approach without relying on level-based handler functions.
     * 
     * @return The next node in the conversation
     */
    public ChatbotNode chooseNextNode() {
        String userMessageText = (userMessage != null) ? userMessage.getMessage().toLowerCase() : "";
        ConversationState.State currentState = getCurrentState(); // Get current state from ConversationState singleton

        System.out.println("DEBUG: chooseNextNode: Current Node ID: " + this.id + ", Category: " + this.category + ", State: " + currentState);
        System.out.println("DEBUG: chooseNextNode: User Message: " + userMessageText);
        System.out.println("DEBUG: chooseNextNode: Available children: " + 
            children.stream().map(ChatbotNode::getId).reduce((s1, s2) -> s1 + ", " + s2).orElse("None"));

        if (!hasChildren()) {
            System.out.println("DEBUG: chooseNextNode: No children for node " + this.id + ". Returning null (stay on current node or end flow).");
            return null;
        }

        // --- State-based navigation using explicit children --- 

        if (currentState == ConversationState.State.LLM_GET_INFO) {
            boolean templateComplete = hasCompleteTemplateInformation();
            System.out.println("DEBUG: chooseNextNode (LLM_GET_INFO): Template complete? " + templateComplete);
            if (templateComplete) {
                // Template is full, look for a child that represents confirmation or completion step
                for (ChatbotNode child : children) {
                    if (child.getId().endsWith("_confirmation") || child.getId().endsWith("_complete")) {
                        System.out.println("DEBUG: chooseNextNode (LLM_GET_INFO): Template complete, found next step child: " + child.getId());
                        return handleNodeSelectionWithState(child);
                    }
                }
            } else {
                // Template is NOT full, look for a child that represents staying in incomplete state or getting more info
                // This could be a self-reference or a specific "_incomplete" or "_missing_info" child.
                for (ChatbotNode child : children) {
                    if (child.getId().equals(this.id) || child.getId().endsWith("_incomplete") || child.getId().endsWith("_missing_info")) {
                        System.out.println("DEBUG: chooseNextNode (LLM_GET_INFO): Template incomplete, found self-loop or incomplete child: " + child.getId());
                        return handleNodeSelectionWithState(child);
                    }
                }
            }
        } 
        else if (currentState == ConversationState.State.CONFIRMATION) {
            boolean isConfirmed = userMessageText.equals("ναι");
            boolean isRejected = userMessageText.equals("όχι");
            System.out.println("DEBUG: chooseNextNode (CONFIRMATION): User confirmed? " + isConfirmed + ", Rejected? " + isRejected);

            if (isConfirmed) {
                for (ChatbotNode child : children) {
                    if (child.getId().endsWith("_confirmed")) {
                        System.out.println("DEBUG: chooseNextNode (CONFIRMATION): User confirmed, found confirmed child: " + child.getId());
                        return handleNodeSelectionWithState(child);
                    }
                }
            } else if (isRejected) {
                for (ChatbotNode child : children) {
                    if (child.getId().endsWith("_rejected") || child.getId().endsWith("_cancelled")) {
                        System.out.println("DEBUG: chooseNextNode (CONFIRMATION): User rejected, found rejected/cancelled child: " + child.getId());
                        return handleNodeSelectionWithState(child);
                    }
                }
            } else {
                // If no clear confirmation/rejection, stay on current node to re-prompt
                System.out.println("DEBUG: chooseNextNode (CONFIRMATION): No clear yes/no, staying on current node: " + this.id);
                return null; // Stay on the current node
            }
        } 
        else if (currentState == ConversationState.State.GIVE_INFO || currentState == ConversationState.State.INITIAL || currentState == ConversationState.State.EXIT) {
            // After giving info, or if flow resets/exits, look for a child to continue (e.g., to root or a thank you node)
            // This is often a single child defined in the JSON for explicit navigation.
            if (hasChildren()) {
                 for (ChatbotNode child : children) {
                    // This could be a specific child like "go_to_root" or just the next logical step defined
                    // For example, after booking_confirmed, the child might be a "thank_you_node" or "root"
                    if (child.getId().equals("root") || child.getId().endsWith("_exit") || child.getId().endsWith("_thank_you")) {
                         System.out.println("DEBUG: chooseNextNode (GIVE_INFO/INITIAL/EXIT): Found explicit next step child: " + child.getId());
                    return handleNodeSelectionWithState(child);
                }
            }
                // If no specific exit/root child, but there are other children, take the first one as a general next step.
                // This assumes the JSON is structured to guide the flow.
                if (!children.isEmpty()) {
                    System.out.println("DEBUG: chooseNextNode (GIVE_INFO/INITIAL/EXIT): No specific exit/root, taking first child: " + children.get(0).getId());
                    return handleNodeSelectionWithState(children.get(0));
                }
            }
        }

        // Fallback: If current state logic didn't find a specific child, 
        // but children exist and one matches the current node's category (if updated by server for example)
        // This can be a way to transition if the state logic is not exhaustive for all tree structures.
        if (this.category != null && !this.category.isEmpty() && !this.category.equals(this.id) && !"root".equals(this.id)) {
            for (ChatbotNode child : getChildren()) {
                if (this.category.equals(child.getCategory())) {
                    System.out.println("DEBUG: chooseNextNode (Fallback): Found child matching current node's category: " + child.getId());
                    return handleNodeSelectionWithState(child);
                }
            }
        }
        
        // If no specific logic above navigated and children exist, but we are not supposed to stay (e.g. not waiting for confirmation)
        // and not in a state that has specific child routing, this implies the JSON should guide via a single child perhaps.
        // However, to prevent unintended loops if JSON isn't perfectly set up for this, let's be cautious.
        // If still no node chosen, and the current node is not meant to be a waiting point (like a confirmation node without a yes/no),
        // it implies an issue in tree design or this logic. For safety, return null (stay on node).
        System.out.println("DEBUG: chooseNextNode: No specific child chosen based on state/conditions. Current children: " + children.size() + ". Returning null.");
        return null; // Default to staying on the current node if no other rule applies
    }

    /**
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
            String category = jsonObj.optString("category", "unknown"); // Category from server response
            String errorMessage = jsonObj.has("error") && !jsonObj.isNull("error") ? jsonObj.getString("error") : null;

            // Set the category for this node based on server response, if different.
            // This node's `this.category` is what MsgTemplate.createTemplate will use.
            if (!category.isEmpty() && !this.category.equals(category)) {
                // this.category = category; // Keep this commented as per previous discussion on immutability
                                           // However, ensure MsgTemplate uses the SERVER's category for creation.
            }

            // Create a SystemMessage with the JSON response
            SystemMessage sysMsg = new SystemMessage(jsonResponse, messageType, true);
            sysMsg.setCategory(category); // Store server's category in SystemMessage

            // Update the systemMessage reference for this node
            this.systemMessage = sysMsg;

            // message1 from the JSON definition (conversation_tree.json) is the primary prompt/statement for this node.
            // It should NOT be overwritten by template processing.
            // String originalMessage1 = this.message1; 

            // message2 from the JSON definition (conversation_tree.json) is the TEMPLATE string.
            String templateStringFromJson = this.message2; // This IS the template.

            if (errorMessage != null && !errorMessage.isEmpty()) {
                // If there's an error, message2 should reflect that.
                // The JSON should define how errors are presented, e.g. by having a specific message2 for error nodes.
                // For now, if an error occurs during server response, we can prepend it to the template, or use a specific error template if available.
                // However, the user wants to remove hardcoded messages. So if `templateStringFromJson` is meant to show errors, it should.
                // If not, the server's error message should be used directly if no template is defined for it.
                // Let's assume `templateStringFromJson` can be a generic template and we might fill it with error details.
                // Or, if no template given (empty message2), then just use the error.
                 if (templateStringFromJson == null || templateStringFromJson.isEmpty()) {
                    this.message2 = "Σφάλμα: " + errorMessage; // Fallback if no template for error.
                 } else {
                    // Try to process the existing templateStringFromJson with error info.
                    // This assumes the template might have placeholders for errors.
                if (msgTemplate == null) {
                    try {
                            // Use this node's own category here as well if dealing with an error template defined in this node
                        msgTemplate = MsgTemplate.createTemplate(this.category);
                    } catch (IllegalArgumentException e) {
                            System.err.println("Failed to create template for category " + this.category + " (for error handling): " + e.getMessage());
                            this.message2 = "Σφάλμα: " + errorMessage; // Use error if template fails.
                        return true;
                    }
                }
                    // We need a way for MsgTemplate to understand it's processing an error.
                    // For now, let's assume valuesFromJson can extract an 'error' field if the template expects it.
                    msgTemplate.valuesFromJson(jsonResponse); // Populate with error if structure allows
                    this.message2 = msgTemplate.processTemplate(templateStringFromJson);
                 }
                System.out.println("ERROR in server response, message2 set to: " + this.message2);
                return true;
            }

            // Only try to apply template if we have details
            if (hasDetails) {
                if (msgTemplate == null) {
                    try {
                        // Use this node's own category to create the template, 
                        // as this.message2 (the template string) is designed for this node's category.
                        System.out.println("Creating template for node's own category: " + this.category);
                        msgTemplate = MsgTemplate.createTemplate(this.category); 
                    } catch (IllegalArgumentException e) {
                        System.err.println("Failed to create template for node category " + this.category + ": " + e.getMessage());
                        // If we can't create a template, and message2 (template from JSON) is empty,
                        // there's no template to process. message2 remains as defined in JSON (empty or not).
                        // If message2 was supposed to be a template, it will remain unprocessed.
                        // this.message2 = originalMessage1; // Fallback to message1 is not desired.
                        return true; // Return true, but message2 is unchanged or error.
                    }
                }

                // Apply template to the message2 field in the node
                // using templateStringFromJson (which is this.message2 from JSON)
                if (msgTemplate.valuesFromJson(jsonResponse)) { // Extracts values from JSON into template object
                    if (templateStringFromJson == null || templateStringFromJson.isEmpty()) {
                        // If message_2 in JSON is empty, it means there's no template defined for this node's successful response.
                        // In this case, message2 for display should remain empty or be explicitly set by processMessageByState based on GIVE_INFO logic.
                        // We should not attempt to process an empty template.
                        this.message2 = ""; // Explicitly empty as no template was provided.
                        System.out.println("No template string (message_2) defined in JSON for this node. message2 is empty.");
                                    } else {
                        String processedMessage = msgTemplate.processTemplate(templateStringFromJson);
                        this.message2 = processedMessage; // Store the processed template in message2
                        System.out.println("TEMPLATE APPLIED (using message_2 from JSON as template): " + processedMessage);
                    }
                    return true;
                } else {
                    System.out.println("Failed to parse JSON for template values. message2 (template from JSON) remains unprocessed.");
                    // If parsing fails, templateStringFromJson (this.message2) remains as it was from JSON.
                    return true; // Still true, but template not applied.
                }
            } else {
                // No details in JSON response, and no error.
                // This means the server call was successful but returned no data to populate a template.
                // `this.message2` (which is the template string from JSON) should remain as is.
                // If it was empty, it stays empty. If it had placeholders, they remain.
                System.out.println("No details in JSON response, skipping template application. message2 (template from JSON) is unchanged.");
                return true;
            }
        } catch (JSONException e) {
            System.err.println("Error parsing JSON: " + e.getMessage());
            e.printStackTrace();
            // In case of JSON error, message2 (template from JSON) remains as is.
            return false; // Indicate failure
        } catch (Exception e) {
            System.err.println("Error updating system message with JSON: " + e.getMessage());
            e.printStackTrace();
            return false; // Indicate failure
        }
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
        System.out.println("DEBUG: Current state (before): " + currentState + " for node " + this.id + " with category " + this.category);
        
        // Root node always resets to INITIAL
        if ("root".equals(this.id)) {
            newState = ConversationState.State.INITIAL;
        }
        // Level 1 nodes (main categories directly under root)
        else if (this.getParent() != null && "root".equals(this.getParent().getId())) {
            // These are the main service categories, typically start by needing info.
            // The user's JSON will define the message_1 for these nodes (e.g., "Τι πληροφορίες θα θέλατε...")
            // The state LLM_GET_INFO implies we expect to extract information from the user's *next* message.
            if ("kratisi".equals(this.id) || "akyrosi".equals(this.id) || "plirofories".equals(this.id) || 
                "axiologiseis_sxolia".equals(this.id) || "prosfores_ekptoseis".equals(this.id)) {
            newState = ConversationState.State.LLM_GET_INFO;
            }
        }
        // Specific node IDs for state setting based on conventions from conversation_tree.json
        else if (this.id.endsWith("_confirmation")) { 
            newState = ConversationState.State.CONFIRMATION;
        }
        else if (this.id.endsWith("_incomplete") || this.id.endsWith("_missing_info")) { 
            newState = ConversationState.State.LLM_GET_INFO;
        }
        else if (this.id.endsWith("_complete") || this.id.endsWith("_some") || this.id.endsWith("_details")) { 
            // e.g., info_complete, booking_complete (before confirmation), info_some, show_details
            // If template is full, it implies data is gathered and can be presented.
            // The actual presentation of info (message_1 + message_2) is handled by handleConversationTurn.
            // If this node is supposed to display collected info (e.g., "Here is what I found: <details>"),
            // then GIVE_INFO is appropriate.
            if (hasCompleteTemplateInformation() || "GIVE_INFO".equals(this.type)) { // explicit type from JSON or complete template
            newState = ConversationState.State.GIVE_INFO;
            } else {
                // If template is not complete and type isn't GIVE_INFO, it's likely still gathering.
            newState = ConversationState.State.LLM_GET_INFO;
            }
        }
        else if (this.id.endsWith("_confirmed")) { // e.g., booking_confirmed, info_confirmed (after user says yes)
            // After confirmation, we typically give a final piece of info or a thank you.
            newState = ConversationState.State.GIVE_INFO;
        }
        else if (this.id.endsWith("_rejected") || this.id.endsWith("_cancelled")) { 
            // After rejection, might give info about why, or reset.
            newState = ConversationState.State.GIVE_INFO; // To show rejection message / next steps.
        }
        else if ("exit_conversation".equals(this.id) || this.id.endsWith("_thank_you") || "EXIT".equals(this.type)) {
            newState = ConversationState.State.EXIT;
        }
        // Add more specific rules as needed based on your conversation_tree.json structure

        if (newState != currentState) {
            conversationState.setCurrentState(newState);
            System.out.println("DEBUG: State updated for node " + this.id + " from " + currentState + " to: " + newState);
        } else {
            System.out.println("DEBUG: State unchanged for node " + this.id + ": " + currentState);
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
        
        System.out.println("DEBUG: Processing message with state: " + currentState + " for node: " + this.id);
        // Ensure jsonResponse is not null for server message types to avoid NPE in fillMsg2FromTemplate
        if (messageType == ChatMessage.TYPE_SERVER && jsonResponse == null) {
            System.err.println("ERROR: jsonResponse is null for a SERVER message type. Cannot process.");
            // this.message2 might be set to an error or remain as per JSON if fillMsg2FromTemplate is not called or fails.
            // If we return false, handleConversationTurn will use fallback.
            // Or, we could try to set a generic error here.
            // For now, let fillMsg2FromTemplate handle it, or subsequent logic.
            // Let's ensure fillMsg2FromTemplate is robust to null jsonResponse if it's called.
            // Better: if jsonResponse is null for server type, we should probably not proceed with template filling.
             if (this.message2 == null || this.message2.isEmpty()){ // if message2 from JSON is also empty
                this.message2 = "Προέκυψε ένα σφάλμα επεξεργασίας της απάντησης του διακομιστή."; // A non-JSON defined fallback.
             } // else message2 from JSON will be used.
            return false; // Indicate an issue.
        }


        // Always call fillMsg2FromTemplate if there's a server response to process or if it's a bot turn that might use templates.
        // fillMsg2FromTemplate will populate this.message2 (the processed template)
        // It uses this.message2 (from JSON) as the template string.
        // It should only be called if there is a jsonResponse from the server.
        // For bot turns, message2 is typically already set from JSON and doesn't need server data.
        boolean templateProcessed = true; // Assume true for bot turns or if no server response to process
        if (jsonResponse != null && !jsonResponse.isEmpty() && messageType == ChatMessage.TYPE_SERVER) {
            templateProcessed = fillMsg2FromTemplate(jsonResponse, messageType);
        } else if (messageType == ChatMessage.TYPE_BOT) {
            // For bot turns, message1 and message2 are taken directly from the node's JSON definition.
            // No server response to process for templating at this stage for this node.
            System.out.println("DEBUG: Bot turn for node " + this.id + ". Using message1/message2 from JSON directly.");
        }


        // The role of processMessageByState is now less about setting message strings (they come from JSON via fillMsg2FromTemplate or directly)
        // and more about ensuring the state-specific logic (like DB queries for GIVE_INFO) is hinted at.
        // The actual messages (message1 as prefix, message2 as content) are assembled in handleConversationTurn.

        switch (currentState) {
            case INITIAL:
                System.out.println("DEBUG: Processing message with INITIAL state. Messages sourced from JSON.");
                // message1 and message2 are already set from JSON. fillMsg2FromTemplate handles server responses.
                break;
                
            case LLM_GET_INFO:
                System.out.println("DEBUG: Processing message with LLM_GET_INFO state. Messages/templates from JSON.");
                // If jsonResponse was provided, fillMsg2FromTemplate has updated message2.
                // If template is not complete, getMissingInfoPrompt will be called by handleConversationTurn.
                break;
                
            case GIVE_INFO:
                System.out.println("DEBUG: GIVE_INFO state. Message2 should contain results (from template or DB query).");
                // If jsonResponse was provided, fillMsg2FromTemplate has updated message2 with server details.
                // If this state implies a DB lookup is needed using data in msgTemplate, that would happen here.
                // For example:
                // if (msgTemplate != null && "ΠΛΗΡΟΦΟΡΙΕΣ".equals(this.category)) {
                //     // String dbResults = queryDatabase(msgTemplate);
                //     // this.message2 = dbResults; // Update message2 with actual results
                // }
                // For now, we assume fillMsg2FromTemplate handles direct server responses.
                // If GIVE_INFO means "give info previously gathered and stored in message2", then it's fine.
                break;
                
            case CONFIRMATION:
                System.out.println("DEBUG: CONFIRMATION state. Confirmation prompt should be in message1/message2 from JSON.");
                // The prompt for confirmation (e.g., "Are you sure? (yes/no)")
                // should be part of message1 or message2 of this node, as defined in conversation_tree.json.
                // fillMsg2FromTemplate would have processed message2 if it's a template.
                break;
                
            case EXIT:
                System.out.println("DEBUG: EXIT state. Exit message should be in message1/message2 from JSON.");
                // Exit messages from JSON.
                break;
                
            default:
                System.out.println("DEBUG: Unknown or default state. Messages/templates from JSON.");
                break;
        }
        return templateProcessed; // Return status of template processing if it occurred.
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
     * @param jsonResponse JSON response from the server (can be null if no server call was made for this turn)
     * @param messageType Type of message (BOT, SERVER)
     * @return A combined response message to show to the user
     */    public String handleConversationTurn(String jsonResponse, int messageType) {
        System.out.println("DEBUG: Handling conversation turn for node: " + this.id);
        
        ConversationState.State state = handleNodeTransition(); // This sets the state for the CURRENT node.
        System.out.println("DEBUG: Current state after transition for node " + this.id + ": " + state);
        
        // processMessageByState will call fillMsg2FromTemplate if jsonResponse is present and applicable.
        // fillMsg2FromTemplate (called within processMessageByState) is responsible for updating
        // this.message2 if it's a template and jsonResponse contains data for it.
        // If jsonResponse is null (e.g., when a node is just displaying its static message_1 after a transition),
        // processMessageByState should not attempt to fill a template from null data.
        boolean messageProcessedByState = processMessageByState(jsonResponse, messageType);
        
        if (!messageProcessedByState && jsonResponse != null) { // Only log warning if processing actual server data failed
            System.out.println("WARNING: Failed to process server message for node: " + this.id + ". Using fallback or JSON-defined messages.");
        }

        // Retrieve messages from the node. 
        // message1 is always from the JSON definition of this node.
        // message2 is from the JSON definition *or* it has been updated by fillMsg2FromTemplate if jsonResponse was processed.
        String nodeMessage1 = getMessage1(); 
        String nodeMessage2 = getMessage2(); // This will be the processed template result if applicable.

        System.out.println("DEBUG: handleConversationTurn: Node ID: " + this.id + ", State: " + state);
        System.out.println("DEBUG: handleConversationTurn: Retrieved from node: message1 (from JSON): '" + nodeMessage1 + "'");
        System.out.println("DEBUG: handleConversationTurn: Retrieved from node: message2 (from JSON or processed template): '" + nodeMessage2 + "'");

        String combinedMessage = "";

        // Construct the message based on user's desired ""+"" or "text"+"" format
        // This implies:
        // - If message_1 is empty, use message_2.
        // - If message_2 is empty, use message_1.
        // - If both are present, concatenate them.

        if (nodeMessage1 != null && !nodeMessage1.isEmpty()) {
            combinedMessage = nodeMessage1;
            if (nodeMessage2 != null && !nodeMessage2.isEmpty()) {
                // Direct concatenation as per user's examples like "" + "message" or "message" + ""
                // The JSON should define if a space or newline is implicitly part of message1 or message2.
                combinedMessage += nodeMessage2;
            }
        } else {
            // nodeMessage1 is empty or null
            combinedMessage = (nodeMessage2 != null) ? nodeMessage2 : "";
        }
        
        // If, after combining, the message is still empty, use fallback.
        if (combinedMessage.isEmpty()) {
            System.out.println("DEBUG: Combined message (message_1 + message_2) is empty. Using fallback for node " + this.id);
            combinedMessage = getFallback();
            if (combinedMessage == null || combinedMessage.isEmpty()) {
                System.out.println("DEBUG: Fallback is also empty for node " + this.id + ". Setting to generic error.");
                combinedMessage = "Συγγνώμη, δεν μπορώ να επεξεργαστώ το αίτημά σας αυτή τη στιγμή.";
            }
        }
        
        // Append missing info prompt if in LLM_GET_INFO state and template is not yet complete.
        if (state == ConversationState.State.LLM_GET_INFO) {
            // Also check if message2 (the template string from JSON for this node) was initially defined.
            // If message2 from JSON was empty, this node might be a general prompter, not an active template filler this turn.
            boolean hasTemplateStringToFill = this.message2 != null && !this.message2.trim().isEmpty();

            if (msgTemplate != null && !hasCompleteTemplateInformation() && hasTemplateStringToFill) {
                String missingInfoPromptText = getMissingInfoPrompt();
                if (!missingInfoPromptText.isEmpty()) {
                    if (!combinedMessage.isEmpty() && !combinedMessage.endsWith("\n") && !missingInfoPromptText.startsWith("\n")) {
                        combinedMessage += "\n"; // Add a separator if needed
                    }
                    combinedMessage += missingInfoPromptText;
                }
            }
        }
        
        System.out.println("DEBUG: Final combined message for node " + this.id + ": \"" + combinedMessage + "\"");
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

    /**
     * Ελέγχει αν το template του node έχει όλα τα απαραίτητα πεδία συμπληρωμένα
     * για την κατηγορία του
     * 
     * @return true αν το template είναι πλήρες, false διαφορετικά
     */
    public boolean hasCompleteTemplateInformation() {
        if (msgTemplate == null) {
            // If there's no template, we can't have complete information for it.
            // However, if the node type is GIVE_INFO and it has no template, 
            // it might be that it's designed to give static info from message1/message2 directly.
            // For the purpose of LLM_GET_INFO, no template means it's not waiting for template fields.
            // So, if the current state is LLM_GET_INFO and there's no template, it is effectively "complete"
            // in the sense that it won't ask for more via getMissingInfoPrompt.
            // This depends on how chooseNextNode handles this for LLM_GET_INFO state.
            // For now, let's stick to: no template means no fields to check, so not "incomplete" in terms of missing fields.
            // However, if the purpose of this method is strictly to check if a *defined* template is full, then this should be false.
            // Let's assume it means "are all required fields of the *defined* template full?"
            return false; 
        }
        
        // Έλεγχος με βάση την κατηγορία
        // The category for checking should be this.category (from JSON), not necessarily server's category for the turn.
        String nodeCategory = this.getCategory();

        if ("ΚΡΑΤΗΣΗ".equals(nodeCategory)) {
            if (msgTemplate instanceof BookingTemplate) {
                BookingTemplate bookingTemplate = (BookingTemplate) msgTemplate;
                return !bookingTemplate.getShowName().isEmpty() && 
                       !bookingTemplate.getDay().isEmpty() && 
                       !bookingTemplate.getTime().isEmpty() && 
                       (bookingTemplate.getPerson() != null && !bookingTemplate.getPerson().getName().isEmpty());
            }
        } 
        else if ("ΑΚΥΡΩΣΗ".equals(nodeCategory)) {
            if (msgTemplate instanceof CancellationTemplate) {
                CancellationTemplate cancelTemplate = (CancellationTemplate) msgTemplate;
                return !cancelTemplate.getReservationNumber().isEmpty() && 
                       !cancelTemplate.getPasscode().isEmpty();
            }
        } 
        else if ("ΠΛΗΡΟΦΟΡΙΕΣ".equals(nodeCategory)) {
            if (msgTemplate instanceof ShowInfoTemplate) {
                ShowInfoTemplate infoTemplate = (ShowInfoTemplate) msgTemplate;
                // For show information, consider it complete if at least a name or topic is present.
                // The user might just ask for "info on Hamlet" or "info on comedies".
                return !infoTemplate.getName().isEmpty() || 
                       !infoTemplate.getTopic().isEmpty();
            }
        } 
        else if ("ΑΞΙΟΛΟΓΗΣΕΙΣ & ΣΧΟΛΙΑ".equals(nodeCategory)) {
            if (msgTemplate instanceof ReviewTemplate) {
                ReviewTemplate reviewTemplate = (ReviewTemplate) msgTemplate;
                return !reviewTemplate.getReservationNumber().isEmpty() && 
                       // !reviewTemplate.getPasscode().isEmpty() && // Passcode might be optional for initiating a review
                       reviewTemplate.getStars() > 0;
            }
        } 
        else if ("ΠΡΟΣΦΟΡΕΣ & ΕΚΠΤΩΣΕΙΣ".equals(nodeCategory)) {
            if (msgTemplate instanceof DiscountTemplate) {
                DiscountTemplate discountTemplate = (DiscountTemplate) msgTemplate;
                // For discounts, any key piece of information is enough to consider it 'started'
                return !discountTemplate.getShowName().isEmpty() || 
                       !discountTemplate.getAge().isEmpty() || 
                       !discountTemplate.getDate().isEmpty() ||
                       discountTemplate.getNumberOfPeople() > 0;
            }
        }
        
        // If the category is not recognized or template type mismatch, assume incomplete or not applicable.
        return false;
    }
}