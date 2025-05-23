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
     * message. Uses a robust approach without relying on level-based handler
     * functions.
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

            // If no direct match, try to find a child with matching category regardless of
            // ID
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
                System.out
                        .println("DEBUG: No category match at root, returning first child: " + getFirstChild().getId());
                return handleNodeSelectionWithState(getFirstChild());
            }

            return null;
        }
        // Case 2: If we're at a main category node, check for template completeness
        else if (validCategoryNode(this)) {
            System.out.println("DEBUG: At category node " + this.id + ", checking template completeness"); // Check if
                                                                                                           // the
                                                                                                           // template
                                                                                                           // has
                                                                                                           // complete
                                                                                                           // information
            boolean hasCompleteInfo = hasCompleteTemplateInformation();
            System.out.println("DEBUG: Template completeness: " + hasCompleteInfo);

            // Detailed logging about the template state
            if (msgTemplate != null) {
                List<String> missingFields = msgTemplate.getMissingFields();
                System.out.println("DEBUG: Template class: " + msgTemplate.getClass().getSimpleName() +
                        ", Missing fields count: " + missingFields.size() +
                        ", Category: " + this.category);

                if ("ΠΛΗΡΟΦΟΡΙΕΣ".equals(this.category)) {
                    System.out.println("DEBUG: ΠΛΗΡΟΦΟΡΙΕΣ category has " +
                            (missingFields.size() < 8 ? "SOME" : "NO") + " information available");
                }

                if (!missingFields.isEmpty()) {
                    System.out.println("DEBUG: Missing fields: " + missingFields);
                } else {
                    System.out.println("DEBUG: Template has all required fields populated");
                }
            } else {
                System.out.println("DEBUG: No template available");
            }

            System.out.println("DEBUG: Template completeness check result: " + hasCompleteInfo);
            // Log the current state of the template for debugging
            if (msgTemplate != null) {
                System.out.println("DEBUG: Current template state:");
                System.out.println("DEBUG:   - Class: " + msgTemplate.getClass().getSimpleName());
                System.out.println("DEBUG:   - Missing fields: " + msgTemplate.getMissingFieldsAsGreekString());
                System.out.println("DEBUG:   - Has complete info: " + hasCompleteInfo);
            }

            // Generic pattern matching based on category and template completeness
            String completePattern = null;
            String incompletePattern = null;

            // Determine patterns based on category
            if ("ΚΡΑΤΗΣΗ".equals(this.getCategory()) || "ΑΚΥΡΩΣΗ".equals(this.getCategory())
                    || "ΑΞΙΟΛΟΓΗΣΕΙΣ & ΣΧΟΛΙΑ".equals(this.getCategory())) {
                completePattern = "confirmation";
                incompletePattern = "incomplete";
            } else if ("ΠΛΗΡΟΦΟΡΙΕΣ".equals(this.getCategory()) || "ΠΡΟΣΦΟΡΕΣ & ΕΚΠΤΩΣΕΙΣ".equals(this.getCategory())) {
                completePattern = "cofirmation";
                incompletePattern = "incomplete";
                System.out.println(
                        "DEBUG: For ΠΛΗΡΟΦΟΡΙΕΣ category - Using 'cofirmation' pattern for available info, 'incomplete' for no info");
                System.out.println("DEBUG: Template state: hasCompleteInfo=" + hasCompleteInfo +
                        ", Will search for child with '" + (hasCompleteInfo ? completePattern : incompletePattern)
                        + "' in ID");
                // Additional logging to show exactly what's in the template
                if (msgTemplate != null
                        && msgTemplate instanceof com.example.jupitertheaterapp.model.ShowInfoTemplate) {
                    com.example.jupitertheaterapp.model.ShowInfoTemplate template = (com.example.jupitertheaterapp.model.ShowInfoTemplate) msgTemplate;

                    String name = template.getName().isEmpty() ? "" : template.getName().get(0);
                    String day = template.getDay().isEmpty() ? "" : template.getDay().get(0);
                    String topic = template.getTopic().isEmpty() ? "" : template.getTopic().get(0);
                    String time = template.getTime().isEmpty() ? "" : template.getTime().get(0);
                    String cast = template.getCast().isEmpty() ? "" : template.getCast().get(0);
                    String room = template.getRoom().isEmpty() ? "" : template.getRoom().get(0);
                    String duration = template.getDuration().isEmpty() ? "" : template.getDuration().get(0);
                    String stars = template.getStars().isEmpty() ? "" : template.getStars().get(0);

                    System.out.println("DEBUG: ΠΛΗΡΟΦΟΡΙΕΣ - ShowInfoTemplate content:");
                    System.out.println("DEBUG:   name: '" + name + "'");
                    System.out.println("DEBUG:   day: '" + day + "'");
                    System.out.println("DEBUG:   time: '" + time + "'");
                    System.out.println("DEBUG:   room: '" + room + "'");
                    System.out.println("DEBUG:   topic: '" + topic + "'");
                    System.out.println("DEBUG:   cast: '" + cast + "'");
                    System.out.println("DEBUG:   duration: '" + duration + "'");
                    System.out.println("DEBUG:   stars: '" + stars + "'");

                    // Check if any fields are populated
                    boolean hasAtLeastOneField = !name.isEmpty() || !day.isEmpty() || !topic.isEmpty()
                            || !time.isEmpty() ||
                            !cast.isEmpty() || !room.isEmpty() || !duration.isEmpty() || !stars.isEmpty();

                    System.out.println("DEBUG: ΠΛΗΡΟΦΟΡΙΕΣ - Has at least one field populated: " + hasAtLeastOneField);
                }
                // Add logging for the ΠΡΟΣΦΟΡΕΣ & ΕΚΠΤΩΣΕΙΣ category as well
                else if (msgTemplate != null
                        && msgTemplate instanceof com.example.jupitertheaterapp.model.DiscountTemplate) {
                    com.example.jupitertheaterapp.model.DiscountTemplate template = (com.example.jupitertheaterapp.model.DiscountTemplate) msgTemplate;

                    // Get all field values
                    String showName = template.getShowName().isEmpty() ? "" : template.getShowName().get(0);
                    int numberOfPeople = template.getNumberOfPeople();
                    String age = template.getAge().isEmpty() ? "" : template.getAge().get(0);
                    String date = template.getDate().isEmpty() ? "" : template.getDate().get(0);

                    System.out.println("DEBUG: ΠΡΟΣΦΟΡΕΣ & ΕΚΠΤΩΣΕΙΣ - DiscountTemplate content:");
                    System.out.println("DEBUG:   showName: '" + showName + "'");
                    System.out.println("DEBUG:   numberOfPeople: " + numberOfPeople);
                    System.out.println("DEBUG:   age: '" + age + "'");
                    System.out.println("DEBUG:   date: '" + date + "'");

                    // Check if any fields are populated
                    boolean hasAtLeastOneField = !showName.isEmpty() || numberOfPeople > 0 ||
                            !age.isEmpty() || !date.isEmpty();

                    System.out.println(
                            "DEBUG: ΠΡΟΣΦΟΡΕΣ & ΕΚΠΤΩΣΕΙΣ - Has at least one field populated: " + hasAtLeastOneField);

                    if (hasAtLeastOneField) {
                        System.out.println(
                                "DEBUG: ΠΡΟΣΦΟΡΕΣ & ΕΚΠΤΩΣΕΙΣ has some information available - using discount_some");
                    } else {
                        System.out.println(
                                "DEBUG: ΠΡΟΣΦΟΡΕΣ & ΕΚΠΤΩΣΕΙΣ has NO information available - using discount_none");
                    }
                }
            }
            // Find the appropriate child based on completeness and category-specific
            // patterns
            if (completePattern != null && incompletePattern != null) {
                String patternToFind = hasCompleteInfo ? completePattern : incompletePattern;
                System.out.println("DEBUG: Looking for child with pattern: " + patternToFind +
                        " based on completeness check: " + hasCompleteInfo);

                // Log all available children for debugging
                System.out.println("DEBUG: Available children to match pattern:");
                for (ChatbotNode child : getChildren()) {
                    System.out.println("DEBUG:   - Child ID: " + child.getId() +
                            ", matches pattern? " + child.getId().contains(patternToFind));
                } // Now try to find matching child
                for (ChatbotNode child : getChildren()) {
                    if (child.getId().contains(patternToFind)) {
                        System.out.println("DEBUG: Found matching child node: " + child.getId() +
                                " for pattern: " + patternToFind);

                        if ("ΠΛΗΡΟΦΟΡΙΕΣ".equals(this.getCategory())) {
                            if (patternToFind.equals("confirmation")) {
                                System.out.println("DEBUG: Selecting info_confirmation node because information is available");
                            } else {
                                System.out
                                        .println("DEBUG: Selecting info_none node because no information is available");
                            }
                        }

                        return handleNodeSelectionWithState(child);
                    }
                }

                // If no match was found
                System.out.println("DEBUG: No child with pattern '" + patternToFind + "' was found!");
            }
        }

        // Case 3: If we're at a "confirmation" node, handle confirmation/rejection
        else if (this.getId().contains("confirmation")) {
            System.out.println("DEBUG: At confirmation node: " + this.getId());

            if (isConfirmation) {
                System.out.println("DEBUG: At confirmation node with confirmation");
                // Look for confirmation node
                for (ChatbotNode child : getChildren()) {
                    if (child.getId().contains("confirmed")) {
                        System.out.println("DEBUG: Found confirmation node: " + child.getId());
                        return handleNodeSelectionWithState(child);
                    }
                }
            } else if (isRejection) {
                System.out.println("DEBUG: At confirmation node with rejection");
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
        else if (this.getId().contains("incomplete")) {
            System.out.println("DEBUG: At incomplete node: " + this.getId());

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
                // Here you could implement logic to select a child based on missing template
                // data
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
                    System.out.println(
                            "DEBUG: Found confirmation-related node through pattern matching: " + child.getId());
                    return handleNodeSelectionWithState(child);
                }
            }
        } else if (isRejection) {
            for (ChatbotNode child : getChildren()) {
                if (child.getId().contains("reject") || child.getId().contains("cancel")) {
                    System.out
                            .println("DEBUG: Found rejection-related node through pattern matching: " + child.getId());
                    return handleNodeSelectionWithState(child);
                }
            }
        }

        // If a category was determined from the user message, try to find a matching
        // child
        if (this.category != null && !this.category.isEmpty()) {
            for (ChatbotNode child : getChildren()) {
                if (this.category.equals(child.getCategory())) {
                    System.out.println("DEBUG: Found child with matching category: " + child.getId());
                    return handleNodeSelectionWithState(child);
                }
            }
        }

        // Fallback: If no specific condition matched but we have children, return first
        // child
        if (hasChildren()) {
            System.out
                    .println("DEBUG: No specific condition matched, returning first child: " + getFirstChild().getId());
            return handleNodeSelectionWithState(getFirstChild());
        }

        System.out.println("DEBUG: No next node found, returning null");
        return null;
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

    // NOTE: The level-based handler methods (handleRootNodeSelection,
    // handleLevel1Selection,
    // and handleDeepLevelSelection) have been removed as part of refactoring the
    // node
    // selection logic to use a more robust case-based approach. If you encounter
    // any compile warnings about these methods, you can safely ignore them as they
    // are artifacts from the previous implementation that may be referenced
    // elsewhere
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
    }

    /**
     * Updates the system message with a JSON response from the server.
     * This method is now primarily used internally by processMessageByState()
     * but can still be called directly if needed.
     * 
     * @param jsonResponse The JSON response from the server as a string
     * @param messageType  The type of message (BOT or SERVER)
     * @return True if successful, false otherwise
     */
    public boolean fillMsg2FromTemplate(String jsonResponse, int messageType) {
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
                    // Log missing fields after populating from JSON
                    List<String> missingFields = msgTemplate.getMissingFields();
                    System.out.println(
                            "DEBUG: Template populated from JSON - Type: " + msgTemplate.getClass().getSimpleName() +
                                    ", Missing fields count: " + missingFields.size());

                    if (missingFields.isEmpty()) {
                        System.out.println("DEBUG: Template is complete! All fields populated successfully.");
                    } else {
                        System.out.println("DEBUG: Template still has missing fields: " +
                                msgTemplate.getMissingFieldsAsGreekString());
                    }

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

    /**
     * Ελέγχει αν το template του node έχει όλα τα απαραίτητα πεδία συμπληρωμένα
     * για την κατηγορία του
     * 
     * @return true αν το template είναι πλήρες, false διαφορετικά
     */
    public boolean hasCompleteTemplateInformation() {
        if (msgTemplate == null) {
            System.out.println("DEBUG: Template is null, considering incomplete");
            return false;
        }

        // Use the getMissingFields method from MsgTemplate to check for completeness
        List<String> missingFields = msgTemplate.getMissingFields();

        // Special handling for categories that need only partial information

        // ΠΛΗΡΟΦΟΡΙΕΣ - Consider "some" information as complete (info_some node)
        if ("ΠΛΗΡΟΦΟΡΙΕΣ".equals(category)) {
            // This is a ShowInfoTemplate, check if we have any populated fields at all
            if (msgTemplate instanceof com.example.jupitertheaterapp.model.ShowInfoTemplate) {
                com.example.jupitertheaterapp.model.ShowInfoTemplate template = (com.example.jupitertheaterapp.model.ShowInfoTemplate) msgTemplate;

                // Get all field values
                String name = template.getName().isEmpty() ? "" : template.getName().get(0);
                String day = template.getDay().isEmpty() ? "" : template.getDay().get(0);
                String topic = template.getTopic().isEmpty() ? "" : template.getTopic().get(0);
                String time = template.getTime().isEmpty() ? "" : template.getTime().get(0);
                String cast = template.getCast().isEmpty() ? "" : template.getCast().get(0);
                String room = template.getRoom().isEmpty() ? "" : template.getRoom().get(0);
                String duration = template.getDuration().isEmpty() ? "" : template.getDuration().get(0);
                String stars = template.getStars().isEmpty() ? "" : template.getStars().get(0);

                // Check if any fields are non-empty
                boolean hasName = !name.isEmpty();
                boolean hasDay = !day.isEmpty();
                boolean hasTopic = !topic.isEmpty();
                boolean hasTime = !time.isEmpty();
                boolean hasCast = !cast.isEmpty();
                boolean hasRoom = !room.isEmpty();
                boolean hasDuration = !duration.isEmpty();
                boolean hasStars = !stars.isEmpty();

                boolean hasAtLeastOneField = hasName || hasDay || hasTopic || hasTime ||
                        hasCast || hasRoom || hasDuration || hasStars;

                System.out.println("DEBUG: ΠΛΗΡΟΦΟΡΙΕΣ template field check - " +
                        "name: '" + name + "', day: '" + day + "', topic: '" + topic +
                        "', time: '" + time + "', cast: '" + cast + "', room: '" + room +
                        "', duration: '" + duration + "', stars: '" + stars + "'");

                System.out.println("DEBUG: ΠΛΗΡΟΦΟΡΙΕΣ template check - " +
                        "Total fields: 8, Missing fields: " + missingFields.size() +
                        ", Has at least one field: " + hasAtLeastOneField);

                if (hasAtLeastOneField) {
                    System.out.println("DEBUG: ΠΛΗΡΟΦΟΡΙΕΣ has some information available - using info_some");
                } else {
                    System.out.println("DEBUG: ΠΛΗΡΟΦΟΡΙΕΣ has NO information available - using info_none");
                }

                return hasAtLeastOneField;
            } else {
                System.out.println("DEBUG: Expected ShowInfoTemplate but got " +
                        msgTemplate.getClass().getSimpleName());
                return false;
            }
        }

        // ΠΡΟΣΦΟΡΕΣ & ΕΚΠΤΩΣΕΙΣ - Consider "some" information as complete
        // (discount_some node)
        else if ("ΠΡΟΣΦΟΡΕΣ & ΕΚΠΤΩΣΕΙΣ".equals(category)) {
            // This is a DiscountTemplate, check if we have any populated fields at all
            if (msgTemplate instanceof com.example.jupitertheaterapp.model.DiscountTemplate) {
                com.example.jupitertheaterapp.model.DiscountTemplate template = (com.example.jupitertheaterapp.model.DiscountTemplate) msgTemplate;

                // Get all field values
                String showName = template.getShowName().isEmpty() ? "" : template.getShowName().get(0);
                int numberOfPeople = template.getNumberOfPeople();
                String age = template.getAge().isEmpty() ? "" : template.getAge().get(0);
                String date = template.getDate().isEmpty() ? "" : template.getDate().get(0);

                // Check if any fields are populated
                boolean hasAtLeastOneField = !showName.isEmpty() || numberOfPeople > 0 ||
                        !age.isEmpty() || !date.isEmpty();

                System.out.println(
                        "DEBUG: ΠΡΟΣΦΟΡΕΣ & ΕΚΠΤΩΣΕΙΣ - Has at least one field populated: " + hasAtLeastOneField);

                if (hasAtLeastOneField) {
                    System.out.println(
                            "DEBUG: ΠΡΟΣΦΟΡΕΣ & ΕΚΠΤΩΣΕΙΣ has some information available - using discount_some");
                } else {
                    System.out
                            .println("DEBUG: ΠΡΟΣΦΟΡΕΣ & ΕΚΠΤΩΣΕΙΣ has NO information available - using discount_none");
                }

                return hasAtLeastOneField;
            } else {
                System.out.println("DEBUG: Expected DiscountTemplate but got " +
                        msgTemplate.getClass().getSimpleName());
                return false;
            }
        }

        // Normal behavior for other categories - all fields must be present
        boolean isComplete = missingFields.isEmpty();

        System.out.println("DEBUG: Template completeness check - Category: " + category +
                ", Node ID: " + id +
                ", Template type: " + msgTemplate.getClass().getSimpleName() +
                ", Missing fields: " + missingFields.size() +
                ", isComplete: " + isComplete);

        if (!isComplete) {
            System.out.println("DEBUG: Missing fields in Greek: " + msgTemplate.getMissingFieldsAsGreekString());
        } else {
            System.out.println("DEBUG: All required fields are present in the template");
        }

        return isComplete;
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
        } else if ("info_none".equals(this.id)) {
            newState = ConversationState.State.LLM_GET_INFO;
            System.out.println("DEBUG: Setting state to LLM_GET_INFO for info_none node");
        }

        // BOOKING NODES - Set appropriate states for booking flow
        else if ("booking_complete".equals(this.id)) {
            newState = ConversationState.State.CONFIRMATION;
            System.out.println("DEBUG: Setting state to CONFIRMATION for booking_complete node");
        } else if ("booking_incomplete".equals(this.id)) {
            newState = ConversationState.State.LLM_GET_INFO;
            System.out.println("DEBUG: Setting state to LLM_GET_INFO for booking_incomplete node");
        }

        // CANCELLATION NODES - Set appropriate states for cancellation flow
        else if ("cancel_complete".equals(this.id)) {
            newState = ConversationState.State.CONFIRMATION;
            System.out.println("DEBUG: Setting state to CONFIRMATION for cancel_complete node");
        } else if ("cancel_incomplete".equals(this.id)) {
            newState = ConversationState.State.LLM_GET_INFO;
            System.out.println("DEBUG: Setting state to LLM_GET_INFO for cancel_incomplete node");
        }

        // REVIEW NODES - Set appropriate states for review flow
        else if ("review_complete".equals(this.id)) {
            newState = ConversationState.State.CONFIRMATION;
            System.out.println("DEBUG: Setting state to CONFIRMATION for review_complete node");
        } else if ("review_incomplete".equals(this.id)) {
            newState = ConversationState.State.LLM_GET_INFO;
            System.out.println("DEBUG: Setting state to LLM_GET_INFO for review_incomplete node");
        }

        // DISCOUNT NODES - Set appropriate states for discount flow
        else if ("discount_some".equals(this.id)) {
            newState = ConversationState.State.GIVE_INFO;
            System.out.println("DEBUG: Setting state to GIVE_INFO for discount_some node");
        } else if ("discount_none".equals(this.id)) {
            newState = ConversationState.State.LLM_GET_INFO;
            System.out.println("DEBUG: Setting state to LLM_GET_INFO for discount_none node");
        }

        // CONFIRMATION NODES - Handle confirmation results
        else if (this.id.contains("confirmed")) {
            newState = ConversationState.State.GIVE_INFO;
            System.out.println("DEBUG: Setting state to GIVE_INFO for confirmation node");
        } else if (this.id.contains("rejected")) {
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
    }

    /**
     * Processes a message based on the current conversation state.
     * This method acts as a dispatcher that selects the appropriate processing
     * method
     * based on the current conversation state and node type.
     * 
     * @param jsonResponse The JSON response from the server
     * @param messageType  The type of message (BOT or SERVER)
     * @return True if successful, false otherwise
     */
    public boolean processMessageByState(String jsonResponse, int messageType) {
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
                } else if ("ΑΚΥΡΩΣΗ".equals(this.category)) {
                    System.out.println("DEBUG: Processing cancellation info request");
                    // TODO: Use LLM to extract cancellation information more effectively
                } else if ("ΠΛΗΡΟΦΟΡΙΕΣ".equals(this.category)) {
                    System.out.println("DEBUG: Processing show info request");
                    // TODO: Use LLM to extract show information queries more effectively
                } else if ("ΑΞΙΟΛΟΓΗΣΕΙΣ & ΣΧΟΛΙΑ".equals(this.category)) {
                    System.out.println("DEBUG: Processing review info request");
                    // TODO: Use LLM to extract review information more effectively
                }

                // For now, fall back to the template processing
                return fillMsg2FromTemplate(jsonResponse, messageType);

            case GIVE_INFO:
                // When we need to provide info to the user (database lookup)
                System.out.println("DEBUG: GIVE_INFO state - Should search database with template data");
                // First apply the template to extract data
                boolean templateApplied = fillMsg2FromTemplate(jsonResponse, messageType);
                if (!templateApplied || msgTemplate == null) {
                    System.out.println("DEBUG: Failed to apply template or no template available");
                    return templateApplied;
                }
                // After applying template, check for missing fields
                if (msgTemplate != null) {
                    List<String> missingFields = msgTemplate.getMissingFields();
                    System.out.println("DEBUG: After template population in GIVE_INFO state - " +
                            "Missing fields: " + missingFields.size() +
                            ", Template type: " + msgTemplate.getClass().getSimpleName());
                    if (!missingFields.isEmpty()) {
                        System.out.println("DEBUG: Missing fields in Greek: " +
                                msgTemplate.getMissingFieldsAsGreekString());
                    } else {
                        System.out.println("DEBUG: Template is complete after population - ready for confirmation");
                    }
                }

                // Different database queries based on category
                if ("ΠΛΗΡΟΦΟΡΙΕΣ".equals(this.category)) {
                    System.out.println("DEBUG: Should query show information from database");
                    // TODO: Query database for show information using msgTemplate
                    // Example:
                    // if (msgTemplate instanceof ShowInfoTemplate) {
                    // ShowInfoTemplate infoTemplate = (ShowInfoTemplate) msgTemplate;
                    // String showName = infoTemplate.getName();
                    // String day = infoTemplate.getDay();
                    // // Query database with these parameters
                    // // Update message2 with results
                    // }
                } else if ("ΠΡΟΣΦΟΡΕΣ & ΕΚΠΤΩΣΕΙΣ".equals(this.category)) {
                    System.out.println("DEBUG: Should query discount information from database");
                    // TODO: Query database for discount information using msgTemplate
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
                    if ("booking_complete".equals(this.id)) {
                        // Add confirmation prompt to the end of message2
                        this.message2 += "\n\nΘέλετε να επιβεβαιώσετε αυτή την κράτηση; (ναι/όχι)";
                    } else if ("cancel_complete".equals(this.id)) {
                        // Add confirmation prompt for cancellation
                        this.message2 += "\n\nΘέλετε να επιβεβαιώσετε αυτήν την ακύρωση; (ναι/όχι)";
                    } else if ("review_complete".equals(this.id)) {
                        // Add confirmation prompt for review submission
                        this.message2 += "\n\nΘέλετε να υποβάλετε αυτή την αξιολόγηση; (ναι/όχι)";
                    }
                }

                return success;

            case EXIT:
                // When we're exiting the conversation
                System.out.println("DEBUG: EXIT state - Should prepare exit message");

                // Apply template first
                boolean templateSuccess = fillMsg2FromTemplate(jsonResponse, messageType);

                // Add exit message
                this.message2 += "\n\nΕυχαριστούμε που χρησιμοποιήσατε το σύστημα του Jupiter Theater!";

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
     * Modifies the chooseNextNode method to add a wrapper that captures the return
     * value
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
    }

    /**
     * Handles a complete conversation turn, processing the user message,
     * updating the conversation state, and formulating a response.
     * 
     * @param jsonResponse JSON response from the server
     * @param messageType  Type of message (BOT, SERVER)
     * @return A combined response message to show to the user
     */
    public String handleConversationTurn(String jsonResponse, int messageType) {
        System.out.println("DEBUG: Handling conversation turn for node: " + this.id);

        // IMPORTANT: First process the JSON and populate the template
        // This ensures the template data is available before making node decisions
        if (jsonResponse != null && !jsonResponse.isEmpty()) {
            System.out.println("DEBUG: Processing JSON response to populate template");

            try {
                // First create a template if needed
                if (msgTemplate == null && !jsonResponse.isEmpty()) {
                    JSONObject jsonObj = new JSONObject(jsonResponse);
                    String category = jsonObj.optString("category", this.category);
                    if (!category.isEmpty()) {
                        try {
                            msgTemplate = MsgTemplate.createTemplate(category);
                            System.out.println("DEBUG: Created new template for category: " + category);
                        } catch (IllegalArgumentException e) {
                            System.out.println("DEBUG: Could not create template: " + e.getMessage());
                        }
                    }
                }

                // Then populate the template from JSON - CRITICAL to do this before choosing
                // the next node
                if (msgTemplate != null) {
                    boolean populated = msgTemplate.valuesFromJson(jsonResponse);
                    System.out.println("DEBUG: Template populated: " + populated); // Log missing fields after
                                                                                   // populating
                    List<String> missingFields = msgTemplate.getMissingFields();
                    System.out.println("DEBUG: After JSON population - Missing fields: " + missingFields.size());
                    if (!missingFields.isEmpty()) {
                        System.out.println("DEBUG: Missing fields in Greek: " +
                                msgTemplate.getMissingFieldsAsGreekString());
                    } else {
                        System.out.println("DEBUG: Template is complete - All fields populated");
                    }
                    // Special case for ΠΛΗΡΟΦΟΡΙΕΣ
                    if ("ΠΛΗΡΟΦΟΡΙΕΣ".equals(category)) {
                        // Count populated fields more precisely
                        if (msgTemplate instanceof com.example.jupitertheaterapp.model.ShowInfoTemplate) {
                            com.example.jupitertheaterapp.model.ShowInfoTemplate template = (com.example.jupitertheaterapp.model.ShowInfoTemplate) msgTemplate;

                            // Get actual values to check
                            String name = template.getName().isEmpty() ? "" : template.getName().get(0);
                            String day = template.getDay().isEmpty() ? "" : template.getDay().get(0);
                            String topic = template.getTopic().isEmpty() ? "" : template.getTopic().get(0);
                            String time = template.getTime().isEmpty() ? "" : template.getTime().get(0);
                            String cast = template.getCast().isEmpty() ? "" : template.getCast().get(0);
                            String room = template.getRoom().isEmpty() ? "" : template.getRoom().get(0);
                            String duration = template.getDuration().isEmpty() ? "" : template.getDuration().get(0);
                            String stars = template.getStars().isEmpty() ? "" : template.getStars().get(0);

                            // Check if any fields are populated
                            boolean hasAtLeastOneField = !name.isEmpty() || !day.isEmpty() || !topic.isEmpty()
                                    || !time.isEmpty() ||
                                    !cast.isEmpty() || !room.isEmpty() || !duration.isEmpty() || !stars.isEmpty();

                            System.out.println("DEBUG: For ΠΛΗΡΟΦΟΡΙΕΣ category - Template has " +
                                    (hasAtLeastOneField ? "SOME" : "NO") + " information available");
                            System.out.println(
                                    "DEBUG: Should navigate to: info_" + (hasAtLeastOneField ? "some" : "none"));
                        } else {
                            boolean hasAtLeastOneField = missingFields.size() < 8;
                            System.out.println("DEBUG: For ΠΛΗΡΟΦΟΡΙΕΣ category - Template has " +
                                    (hasAtLeastOneField ? "SOME" : "NO") + " information available");
                            System.out.println(
                                    "DEBUG: Should navigate to: info_" + (hasAtLeastOneField ? "some" : "none"));
                        }
                    }

                    // Special case for ΠΡΟΣΦΟΡΕΣ & ΕΚΠΤΩΣΕΙΣ
                    else if ("ΠΡΟΣΦΟΡΕΣ & ΕΚΠΤΩΣΕΙΣ".equals(category)) {
                        if (msgTemplate instanceof com.example.jupitertheaterapp.model.DiscountTemplate) {
                            com.example.jupitertheaterapp.model.DiscountTemplate template = (com.example.jupitertheaterapp.model.DiscountTemplate) msgTemplate;

                            boolean hasAtLeastOneField = missingFields.size() < template.getTotalFieldCount();
                            System.out.println("DEBUG: For ΠΡΟΣΦΟΡΕΣ & ΕΚΠΤΩΣΕΙΣ category - Template has " +
                                    (hasAtLeastOneField ? "SOME" : "NO") + " information available");
                            System.out.println(
                                    "DEBUG: Should navigate to: discount_" + (hasAtLeastOneField ? "some" : "none"));
                        }
                    }
                }
            } catch (Exception e) {
                System.out.println("DEBUG: Error pre-processing JSON: " + e.getMessage());
            }
        }

        // 1. Process the message by state - this handles all the state-specific logic
        // Note: It's important we do this AFTER populating the template, but BEFORE
        // deciding navigation
        boolean processResult = processMessageByState(jsonResponse, messageType);
        System.out.println("DEBUG: First process result: " + processResult);

        // 2. Update the state based on the current node
        ConversationState.State state = handleNodeTransition();
        System.out.println("DEBUG: Current state after transition: " + state);

        // 3. Process the message again if needed - this is a safety check
        // We ensure we have the final processed message for output
        boolean processed = true;
        if (!processResult) {
            processed = processMessageByState(jsonResponse, messageType);
            if (!processed) {
                System.out.println("WARNING: Failed to process message for node: " + this.id);
                return "Sorry, I could not process that message properly.";
            }
        }

        // 3. Combine message1 and message2 with a newline between them
        String combinedMessage = getMessage();
        String message2 = getMessage2();
        if (message2 != null && !message2.isEmpty() && !message2.equals(combinedMessage)) {
            combinedMessage += "\n" + message2;
        } // 4. Add state-specific modifications to the response
        switch (state) {
            case CONFIRMATION:
                // If we're in confirmation state but the message doesn't already include a
                // prompt
                if (!combinedMessage.contains("(ναι/όχι)")) {
                    combinedMessage += "\n\nΠαρακαλώ επιβεβαιώστε (ναι/όχι).";
                }
                break;

            case LLM_GET_INFO:
                // Don't add hardcoded prompt - use only messages from conversation tree
                break;

            // Other state-specific modifications can be added here
            default:
                // No additional modifications for other states
                break;
        }

        return combinedMessage;
    }

    /**
     * Generates a prompt asking for specific missing information based on the
     * template
     * 
     * @return A prompt for the missing information
     */
}