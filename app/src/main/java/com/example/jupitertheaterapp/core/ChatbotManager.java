package com.example.jupitertheaterapp.core;

import android.content.Context;
import android.util.Log;

import com.example.jupitertheaterapp.model.ChatMessage;
import com.example.jupitertheaterapp.model.ChatbotNode;
import com.example.jupitertheaterapp.model.ConversationState;
import com.example.jupitertheaterapp.model.MsgTemplate;
import com.example.jupitertheaterapp.util.Client;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ChatbotManager {
    private static final String TAG = "ChatbotManager";
    private static final String CONVERSATION_FILE = "conversation_tree.json";
    private JSONObject jsonTree; // Keep for reference
    private ChatbotNode rootNode; // Root node of conversation tree
    private Map<String, ChatbotNode> nodeMap;
    private ChatbotNode currentNode;
    private ConversationState conversationState;
    private Client client; // Client for server communications

    public ChatbotManager(Context context) {
        nodeMap = new HashMap<>();
        conversationState = ConversationState.getInstance();
        Log.d(TAG, "Conversation State: ");
        Log.d(TAG, conversationState.getCurrentStateAsString());
        loadConversationTree(context);

        // Initialize the client for server communications
        client = new Client(this);
    }

    private void loadConversationTree(Context context) {
        try {
            String jsonString = readJSONFromAsset(context, CONVERSATION_FILE);
            if (jsonString != null) {
                jsonTree = new JSONObject(jsonString);
                rootNode = convertJsonToNodeStructure(jsonTree.getJSONObject("root"));
                resolveNodeReferences(); // Resolve all node references
                currentNode = rootNode;
                Log.d(TAG, "Conversation tree loaded successfully");
            } else {
                Log.e(TAG, "Failed to read conversation tree JSON");
                createMinimalStructure();
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing conversation tree JSON", e);
            createMinimalStructure();
        }
    }

    private ChatbotNode convertJsonToNodeStructure(JSONObject jsonNode) throws JSONException {
        String id = jsonNode.getString("id");
        String category = jsonNode.optString("category", id);
        String type = jsonNode.getString("type");
        String content = jsonNode.optString("content", "");
        String fallback = jsonNode.optString("fallback", "I didn't understand that.");

        // Log.d(TAG, "Processing node: " + id + ", category: " + category + ", type: "
        // + type);

        // Log the children array for this node
        if (jsonNode.has("children")) {
            Object childrenObj = jsonNode.get("children");
            if (childrenObj instanceof JSONArray) {
                JSONArray childArray = (JSONArray) childrenObj;
                // Log.d(TAG, "Node " + id + " has " + childArray.length() + " children in
                // JSON");

                for (int i = 0; i < childArray.length(); i++) {
                    Object childObj = childArray.get(i);
                    if (childObj instanceof JSONObject) {
                        JSONObject childJson = (JSONObject) childObj;
                        // Log.d(TAG, " Child " + i + " is object with ID: " + childJson.optString("id",
                        // "unknown"));
                    } else if (childObj instanceof String) {
                        String childId = (String) childObj;
                        // Log.d(TAG, " Child " + i + " is string reference to: " + childId);
                    } else {
                        // Log.d(TAG, " Child " + i + " is unknown type: " +
                        // childObj.getClass().getName());
                    }
                }
            } else {
                // Log.d(TAG, "Node " + id + " has children but not in JSONArray format");
            }
        } else {
            // Log.d(TAG, "Node " + id + " has no children in JSON");
        }

        ChatbotNode node;

        // Check if we have the new dual message format
        if (jsonNode.has("message_1") && jsonNode.has("message_2")) {
            String message1 = jsonNode.getString("message_1");
            String message2 = jsonNode.getString("message_2");

            // Log.d(TAG, "Node " + id + " has dual messages: message_1="
            // + message1.substring(0, Math.min(20, message1.length())) + "...");
            // Use the constructor that accepts both message formats
            node = new ChatbotNode(id, type, message1, message2, content, fallback);
            // Explicitly set the category after node creation to ensure it's properly used
            node.setCategory(category);
            // Log.d(TAG, "Set category for node " + id + " to: " + category);
        } else {
            // Fall back to the original format
            String message;
            if (jsonNode.has("message_1")) {
                message = jsonNode.getString("message_1");
                // Log.d(TAG, "Node " + id + " has only message_1");
            } else if (jsonNode.has("message")) {
                // For backward compatibility with old JSON format
                message = jsonNode.getString("message");
                // Log.d(TAG, "Node " + id + " has only message (old format)");
            } else {
                // Default message if neither format is present
                message = "No message available.";
                // Log.d(TAG, "Node " + id + " has no message fields");
            }
            node = new ChatbotNode(id, type, message, content, fallback);
        }
        // Explicitly set the category from JSON to ensure it's properly used
        node.setCategory(category);
        // Log.d(TAG, "Set category for node " + id + " to: " + category);

        // Try to assign the appropriate MsgTemplate based on the node's category
        try {
            MsgTemplate template = MsgTemplate.createTemplate(category);
            node.setMessageTemplate(template);
            // Log.d(TAG, "Assigned template to node: " + id + " with category: " +
            // category);
        } catch (IllegalArgumentException e) {
            // No template available for this node category; that's ok
            // Log.d(TAG, "No template available for node: " + id + " with category: " +
            // category);
        }

        // Add node to the map - important to do this before processing children
        // to handle circular references
        nodeMap.put(id, node);

        // Process children after creating the node
        if (jsonNode.has("children")) {
            Object childrenObj = jsonNode.get("children");
            if (childrenObj instanceof JSONArray) {
                JSONArray childArray = (JSONArray) childrenObj;
                for (int i = 0; i < childArray.length(); i++) {
                    Object childObj = childArray.get(i);
                    if (childObj instanceof JSONObject) {
                        ChatbotNode childNode = convertJsonToNodeStructure((JSONObject) childObj);
                        childNode.setParent(node);
                        node.addChild(childNode);
                    } else if (childObj instanceof String) {
                        // Handle string references (like "root")
                        String childId = (String) childObj;
                        // We'll link these after all nodes are created
                        node.addPendingChildId(childId);
                    }
                }
            }
        }

        return node;
    }// Called after all nodes are created to resolve string references

    private void resolveNodeReferences() {
        Log.d(TAG, "Starting to resolve node references. Node map size: " + nodeMap.size());

        // First, log all nodes in the map
        for (String key : nodeMap.keySet()) {
            ChatbotNode mapNode = nodeMap.get(key);
            // .d(TAG, "Node in map: " + key + ", children: " + mapNode.getChildren().size()
            // +
            // ", pending children: " + mapNode.getPendingChildIds().size());
        }

        // Now process the pending children
        for (ChatbotNode node : nodeMap.values()) {
            List<String> pendingIds = node.getPendingChildIds();
            // Log.d(TAG, "Resolving " + pendingIds.size() + " pending child IDs for node: "
            // + node.getId());

            for (String id : pendingIds) {
                ChatbotNode referencedNode = nodeMap.get(id);
                if (referencedNode != null) {
                    node.addChild(referencedNode);
                    // Log.d(TAG, " Added child " + id + " to parent " + node.getId());
                } else {
                    // Log.e(TAG, " Failed to find node with ID: " + id + " for parent: " +
                    // node.getId());
                }
            }
            node.clearPendingChildIds();
        }

        Log.d(TAG, "After resolving references, root node has " + rootNode.getChildren().size() + " children");

        // Second pass to propagate templates from parents to children
        propagateTemplatesToChildren(rootNode);
    }

    /**
     * Propagates templates from parent nodes to their children.
     * The root node is exempt from receiving templates.
     * 
     * @param node The current node to process
     */
    private void propagateTemplatesToChildren(ChatbotNode node) {
        // Use a helper method with a Set to track visited nodes
        propagateTemplatesToChildren(node, new HashSet<String>());
    }
    
    /**
     * Helper method that uses a Set to track visited nodes and avoid infinite recursion.
     * 
     * @param node The current node to process
     * @param visitedNodes Set of node IDs that have already been visited
     */
    private void propagateTemplatesToChildren(ChatbotNode node, HashSet<String> visitedNodes) {
        if (node == null)
            return;
            
        // Add this node's ID to the visited set to avoid processing it again
        if (!visitedNodes.add(node.getId())) {
            // If we couldn't add the ID because it's already in the set, return immediately
            return;
        }
        
        // We're no longer propagating templates, but we'll keep the structure for
        // traversing children

        // Continue recursively for all children
        for (ChatbotNode child : node.getChildren()) {
            // Skip processing root node to prevent circular reference issues
            if (child.getId().equals("root"))
                continue;

            // Pass the visitedNodes set to the recursive call
            propagateTemplatesToChildren(child, visitedNodes);
        }
    }

    private String readJSONFromAsset(Context context, String filePath) {
        String json = null;
        try {
            InputStream is = context.getAssets().open(filePath);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            json = new String(buffer, "UTF-8");
        } catch (IOException e) {
            Log.e(TAG, "Error reading JSON file", e);
            return null;
        }
        return json;
    }    private void createMinimalStructure() {
        rootNode = new ChatbotNode("root", "CATEGORISE",
                "Γεια σας! Πώς μπορώ να σας βοηθήσω;", "",
                "Δεν κατάλαβα την ερώτησή σας.");
        rootNode.setCategory("root"); // Set category explicitly
        nodeMap.put("root", rootNode);
        currentNode = rootNode;
    }

    public String getInitialMessage() {
        if (rootNode != null) {
            // The initial message should be the root node's primary message.
            // message2 was used before, but message1 is more conventional for initial prompts if not empty.
            String initialMsg = rootNode.getMessage1();
            if (initialMsg == null || initialMsg.isEmpty()){
                initialMsg = rootNode.getMessage2(); // Fallback to message2 if message1 is empty
            }
            if (initialMsg == null || initialMsg.isEmpty()){
                initialMsg = "Welcome!"; // Absolute fallback
            }
             System.out.println("DEBUG: Initial message from root node: " + initialMsg);
            return initialMsg;
        }
        return "Chatbot is not initialized.";
    }

    public String getResponseForNodeId(String category) {
        Log.d(TAG, "Getting response for category: " + category);

        // Find node by category
        ChatbotNode foundNode = null;
        for (ChatbotNode node : nodeMap.values()) {
            if (category.equals(node.getCategory())) {
                foundNode = node;
                break;
            }
        }

        if (foundNode == null) {
            Log.e(TAG, "Node with category not found: " + category);
            return "Συγγνώμη, δεν βρέθηκε απάντηση.";
        }
        currentNode = foundNode;

        // Get both message1 and message2 from the node
        String message1 = foundNode.getMessage1();
        String message2 = foundNode.getMessage2();
        
        // Combine message1 and message2 with a newline between them
        String combinedMessage = message1;
        if (message2 != null && !message2.isEmpty() && !message2.equals(message1)) {
            combinedMessage += "\n" + message2;
        }

        Log.d(TAG, "Using system message from node: " + foundNode.getId());
        Log.d(TAG, "Combined message: " + combinedMessage);

        return combinedMessage;
    }

    /**
     * This method has been moved to ChatbotNode class.
     * The ChatbotNode.chooseNextNode() method is now responsible for
     * selecting the next conversation node based on user input and conversation
     * context.
     * 
     * @deprecated Use ChatbotNode.chooseNextNode() instead for history-based node
     *             selection
     */
    @Deprecated
    private ChatbotNode legacyChooseNextNode(String userInput) {
        // This is kept for backward compatibility but not used.
        // We now use currentNode.chooseNextNode(userInput) from the node class
        return currentNode.getChildren().isEmpty() ? rootNode : currentNode.getChildren().get(0);
    }

    private void logAvailableChildren(List<ChatbotNode> children) {
        StringBuilder childrenInfo = new StringBuilder("Available children: ");
        for (ChatbotNode child : children) {
            childrenInfo.append(child.getId()).append(", ");
        }
        Log.d(TAG, childrenInfo.toString());
    }    public String findNodeResponseById(String nodeIdOrCategory) {
        // First try to get by direct ID
        ChatbotNode node = nodeMap.get(nodeIdOrCategory);

        // If not found, try to find by category
        if (node == null) {
            for (ChatbotNode n : nodeMap.values()) {
                if (nodeIdOrCategory.equals(n.getCategory())) {
                    node = n;
                    break;
                }
            }
        }

        if (node != null) {
            // Get both message1 and message2 from the node
            String message1 = node.getMessage1();
            String message2 = node.getMessage2();
            
            // Combine message1 and message2 with a newline between them
            String combinedMessage = message1;
            if (message2 != null && !message2.isEmpty() && !message2.equals(message1)) {
                combinedMessage += "\n" + message2;
            }
            
            return combinedMessage;
        }
        return "Δεν βρέθηκε απάντηση.";
    }

    public void reset() {
        currentNode = rootNode;
    }

    public ChatbotNode getCurrentNode() {
        return currentNode;
    }

    public String getParentNodeId(String nodeId) {
        ChatbotNode node = nodeMap.get(nodeId);
        if (node != null && node.getParent() != null) {
            return node.getParent().getId();
        }
        return "";
    }

    /**
     * Gets a node by its ID
     * 
     * @param nodeId The ID of the node to retrieve
     * @return The ChatbotNode with the given ID, or null if not found
     */
    public ChatbotNode getNodeById(String nodeId) {
        return nodeMap.get(nodeId);
    }

    /**
     * Prints the entire conversation tree structure to the log.
     * This is useful for debugging and visualizing the chatbot tree.
     */
    public void printTree() {
        Log.d(TAG, "Printing Conversation Tree Structure:");
        Log.d(TAG, "======================================");
        if (rootNode != null) {
            Log.d(TAG, "Root node has " + rootNode.getChildren().size() + " children");
            for (ChatbotNode child : rootNode.getChildren()) {
                Log.d(TAG, "Root child: " + child.getId());
            }
            printNodeTree(rootNode, 0, new HashSet<String>());
        } else {
            Log.d(TAG, "Tree is empty (rootNode is null)");
        }
        Log.d(TAG, "======================================");
    }

    /**
     * Helper method to recursively print a node and its children with proper
     * indentation.
     * Handles circular references by keeping track of visited nodes.
     * 
     * @param node    The node to print
     * @param depth   The current depth in the tree (for indentation)
     * @param visited Set of already visited node IDs to prevent infinite recursion
     */
    private void printNodeTree(ChatbotNode node, int depth, Set<String> visited) {
        if (node == null)
            return;

        // Check if this node was already visited to prevent infinite recursion
        if (visited.contains(node.getId())) {
            StringBuilder indent = new StringBuilder();
            for (int i = 0; i < depth; i++) {
                indent.append("    ");
            }
            indent.append("└── [CIRCULAR REF to: ").append(node.getId()).append("]");
            Log.d(TAG, indent.toString());
            return;
        }

        // Add this node to visited set
        visited.add(node.getId());

        StringBuilder indent = new StringBuilder();
        StringBuilder connector = new StringBuilder();
        for (int i = 0; i < depth; i++) {
            indent.append("    ");
            if (i == depth - 1) {
                connector.append("└── ");
            } else {
                connector.append("    ");
            }
        } // Print current node details
        String currentMarker = (node == currentNode) ? " [CURRENT]" : "";
        String display = String.format("%s%s [ID: %s, Category: %s, Type: %s]%s",
                indent, connector, node.getId(), node.getCategory(), node.getType(), currentMarker);
        Log.d(TAG, display);

        // Print system message preview (truncated if too long)
        ChatMessage sysMsg = node.getSystemMessage();
        String sysMsgType = (sysMsg.getType() == ChatMessage.TYPE_BOT) ? "BOT" : "SERVER";
        String sysMsgPreview = sysMsg.getMessage();
        if (sysMsgPreview.length() > 40) {
            sysMsgPreview = sysMsgPreview.substring(0, 37) + "...";
        }
        Log.d(TAG, indent + "    ├── System [" + sysMsgType + "]: " + sysMsgPreview);

        // Print user message if available
        if (node.getUserMessage() != null) {
            String userMsgPreview = node.getUserMessage().getMessage();
            if (userMsgPreview.length() > 40) {
                userMsgPreview = userMsgPreview.substring(0, 37) + "...";
            }
            Log.d(TAG, indent + "    ├── User: " + userMsgPreview);
        }

        // For backward compatibility, also print message_1 and message_2
        String message1Preview = node.getMessage1();
        if (message1Preview.length() > 40) {
            message1Preview = message1Preview.substring(0, 37) + "...";
        }
        Log.d(TAG, indent + "    ├── Legacy Msg1: " + message1Preview);

        String message2Preview = node.getMessage2();
        if (message2Preview.length() > 40) {
            message2Preview = message2Preview.substring(0, 37) + "...";
        }
        Log.d(TAG, indent + "    ├── Legacy Msg2: " + message2Preview);

        // Print template info if available
        MsgTemplate template = node.getMessageTemplate();
        if (template != null) {
            Log.d(TAG, indent + "    ├── Template: " + template.getClass().getSimpleName());
        }

        // Print children count
        int childCount = node.getChildren().size();
        Log.d(TAG, indent + "    └── Children: " + childCount);

        // Recursively print children
        for (ChatbotNode child : node.getChildren()) {
            // Create a new copy of the visited set for each branch
            Set<String> branchVisited = new HashSet<>(visited);
            printNodeTree(child, depth + 1, branchVisited);
        }
    }

    public String getTreeAsString() {
        StringBuilder result = new StringBuilder();
        result.append("Conversation Tree Structure:\n");
        result.append("======================================\n");
        if (rootNode != null) {
            getNodeTreeAsString(rootNode, 0, new HashSet<String>(), result);
        } else {
            result.append("Tree is empty (rootNode is null)\n");
        }
        result.append("======================================\n");
        return result.toString();
    }

    /**
     * Helper method to recursively build a string representation of a node and its
     * children.
     * Handles circular references by keeping track of visited nodes.
     * 
     * @param node    The node to process
     * @param depth   The current depth in the tree (for indentation)
     * @param visited Set of already visited node IDs to prevent infinite recursion
     * @param sb      StringBuilder to accumulate the result
     */
    private void getNodeTreeAsString(ChatbotNode node, int depth, Set<String> visited, StringBuilder sb) {
        if (node == null)
            return;

        // Check if this node was already visited to prevent infinite recursion
        if (visited.contains(node.getId())) {
            StringBuilder indent = new StringBuilder();
            for (int i = 0; i < depth; i++) {
                indent.append("    ");
            }
            indent.append("└── [CIRCULAR REF to: ").append(node.getId()).append("]");
            sb.append(indent).append("\n");
            return;
        }

        // Add this node to visited set
        visited.add(node.getId());

        StringBuilder indent = new StringBuilder();
        for (int i = 0; i < depth; i++) {
            indent.append("    ");
        } // Build current node details
        String currentMarker = (node == currentNode) ? " [CURRENT]" : "";
        String prefix = indent.toString() + "└── ";
        String display = String.format("%s[ID: %s, Category: %s, Type: %s]%s",
                prefix, node.getId(), node.getCategory(), node.getType(), currentMarker);
        sb.append(display).append("\n"); // Add system message preview (truncated if too long)
        ChatMessage sysMsg = node.getSystemMessage();
        String sysMsgType = (sysMsg.getType() == ChatMessage.TYPE_BOT) ? "BOT" : "SERVER";
        String sysMsgPreview = sysMsg.getMessage();
        if (sysMsgPreview.length() > 40) {
            sysMsgPreview = sysMsgPreview.substring(0, 37) + "...";
        }
        sb.append(indent).append("    ├── System [").append(sysMsgType).append("]: ").append(sysMsgPreview)
                .append("\n");

        // Add user message if available
        if (node.getUserMessage() != null) {
            String userMsgPreview = node.getUserMessage().getMessage();
            if (userMsgPreview.length() > 40) {
                userMsgPreview = userMsgPreview.substring(0, 37) + "...";
            }
            sb.append(indent).append("    ├── User: ").append(userMsgPreview).append("\n");
        }

        // Add template info if available
        MsgTemplate template = node.getMessageTemplate();
        if (template != null) {
            sb.append(indent).append("    ├── Template: ").append(template.getClass().getSimpleName()).append("\n");
        }

        // Add children count
        int childCount = node.getChildren().size();
        sb.append(indent).append("    └── Children: ").append(childCount).append("\n");

        // Recursively process children
        for (ChatbotNode child : node.getChildren()) {
            // Create a new copy of the visited set for each branch
            Set<String> branchVisited = new HashSet<>(visited);
            getNodeTreeAsString(child, depth + 1, branchVisited, sb);
        }
    }

    /**
     * Detects and returns a list of nodes that have circular references.
     * 
     * @return List of node IDs that have circular references
     */
    public List<String> detectCircularReferences() {
        List<String> circularRefNodes = new ArrayList<>();
        if (rootNode != null) {
            for (String nodeId : nodeMap.keySet()) {
                ChatbotNode node = nodeMap.get(nodeId);
                Set<String> visited = new HashSet<>();
                visited.add(nodeId);
                checkNodeForCircularReferences(node, visited, circularRefNodes);
            }
        }
        return circularRefNodes;
    }

    private void checkNodeForCircularReferences(ChatbotNode node, Set<String> visited, List<String> circularRefNodes) {
        if (node == null)
            return;

        for (ChatbotNode child : node.getChildren()) {
            if (visited.contains(child.getId())) {
                if (!circularRefNodes.contains(node.getId())) {
                    circularRefNodes.add(node.getId());
                }
            } else {
                Set<String> newVisited = new HashSet<>(visited);
                newVisited.add(child.getId());
                checkNodeForCircularReferences(child, newVisited, circularRefNodes);
            }
        }
    }

    /**
     * Gets the current conversation as a list of nodes, each with system and user
     * messages.
     * This shows only the conversation path that has been traversed, not the entire
     * tree.
     * 
     * @return A string representation of the conversation nodes
     */
    public String getConversationNodeList() {
        StringBuilder result = new StringBuilder();
        result.append("Conversation Node List:\n");
        result.append("======================================\n");

        // Get the path from root node to current node
        List<ChatbotNode> conversationPath = getPathToCurrentNode();

        // Display each node in the path
        for (int i = 0; i < conversationPath.size(); i++) {
            ChatbotNode node = conversationPath.get(i);
            result.append("NODE ").append(i + 1).append(":\n");

            // Node details (ID, Category, Type)
            result.append("- ID: ").append(node.getId()).append("\n");
            result.append("- Category: ").append(node.getCategory()).append("\n");
            result.append("- Type: ").append(node.getType()).append("\n");

            // System message details (including message_1 and message_2)
            result.append("- System Message: ").append(node.getSystemMessage().getMessage()).append("\n");
            result.append("- Message_1: ").append(node.getMessage1()).append("\n");
            result.append("- Message_2: ").append(node.getMessage2()).append("\n");

            // User message if available
            if (node.getUserMessage() != null) {
                result.append("- User Response: ").append(node.getUserMessage().getMessage()).append("\n");
            } else {
                result.append("- User Response: [No response yet]\n");
            }

            // Template if available
            if (node.getMessageTemplate() != null) {
                result.append("- Template: ").append(node.getMessageTemplate().getClass().getSimpleName()).append("\n");
            }

            // Add a separator between nodes
            result.append("--------------------------------------\n");
        }

        result.append("======================================\n");
        return result.toString();
    }

    /**
     * Finds the path from root node to the current node.
     * Uses parent references to build the path, ensuring we can track the full
     * conversation.
     * 
     * @return A list of nodes representing the conversation path
     */
    private List<ChatbotNode> getPathToCurrentNode() {
        List<ChatbotNode> path = new ArrayList<>();
        Set<String> visitedNodeIds = new HashSet<>();

        // If we're still at the root node or no conversation happened yet
        if (currentNode == rootNode || currentNode == null) {
            path.add(rootNode);
            return path;
        }

        // Add the current node as we know it's part of the path
        path.add(currentNode);
        visitedNodeIds.add(currentNode.getId());

        // Try to find the path by working backward from the current node
        ChatbotNode node = currentNode;
        while (node != null && node.getParent() != null) {
            ChatbotNode parent = node.getParent();

            // Check for cycles
            if (visitedNodeIds.contains(parent.getId())) {
                break; // Stop if we detect a cycle
            }

            // Add the parent to the beginning of the path
            path.add(0, parent);
            visitedNodeIds.add(parent.getId());

            // Move up to the parent
            node = parent;
        }

        return path;
    }

    /**
     * Single entry point to process user messages and continue the conversation.
     * Handles both local and server responses based on current settings.
     * 
     * @param userMessage The user's message to process
     * @param callback    Callback to handle the bot's response or errors
     */
    public void processUserMessage(String userMessage, MessageResponseCallback callback) {
        Log.d(TAG, "Processing user message: " + userMessage);

        try {
            // Always use server processing
            callback.onReadyForServerRequest(userMessage, currentNode);
        } catch (Exception e) {
            Log.e(TAG, "Error preparing server request", e);
            callback.onError("Error preparing server request: " + e.getMessage());
        }
    }

    /**
     * Callback interface for message processing
     */
    public interface MessageResponseCallback {
        /**
         * Called when a response is ready
         * 
         * @param response    The response message
         * @param messageType The type of message (BOT or SERVER)
         */
        void onResponse(String response, int messageType);

        /**
         * Called when a server request should be made
         * 
         * @param userMessage The user's message
         * @param currentNode The current node in the conversation
         */
        void onReadyForServerRequest(String userMessage, ChatbotNode currentNode);

        /**
         * Called when an error occurs
         * 
         * @param errorMessage Error message
         */
        void onError(String errorMessage);
    }

    /**
     * Gets a response for the provided user message.
     * This method handles both local and server responses internally,
     * making it the only method MainActivity needs to call.
     * 
     * @param userMessage      The message from the user
     * @param responseCallback Callback to receive the response
     */
    public void getResponse(String userMessage, ResponseCallback responseCallback) {
        Log.d(TAG, "Getting response for user message: " + userMessage);
        final ChatbotNode previousNode = currentNode; // Keep track of the node before processing

        try {
            JSONObject jsonRequest = currentNode.createRequestJson(userMessage);
            currentNode.setUserMessage(userMessage);
            System.out.println("Current node (before server request): " + currentNode);

            makeServerRequest(jsonRequest, new ServerRequestCallback() {
                @Override
                public void onSuccess(String category, String fullJsonResponse) {
                    try {
                        ChatbotNode previouslyCurrentNode = currentNode; // Node that was current when server request was made

                        Log.d(TAG, "Server category response: " + category + " for node: " + previouslyCurrentNode.getId());
                        Log.d(TAG, "Server fullJsonResponse: " + fullJsonResponse);

                        // Determine the next node based on the interaction with the previouslyCurrentNode
                        // Note: chooseNextNode() internally might call previouslyCurrentNode.handleConversationTurn() 
                        // if its logic depends on processed results, but that internal call is for decision-making.
                        // The message displayed to the user will be from the *final* chosen node for this turn.
                        ChatbotNode nextNodeCandidate = previouslyCurrentNode.chooseNextNode(); // Pass jsonResponse if chooseNextNode needs it
                        Log.d(TAG, "Next node candidate chosen by " + previouslyCurrentNode.getId() + " is: " + (nextNodeCandidate != null ? nextNodeCandidate.getId() : "null"));

                        String messageToDisplayToUser;
                        int messageTypeForDisplay = ChatMessage.TYPE_BOT; // Default for bot-originated messages

                        if (nextNodeCandidate == null) {
                            // This might happen if at a terminal node or if chooseNextNode explicitly returns null to stay.
                            // In this case, the response should be from the previouslyCurrentNode's processing of the server data.
                            Log.d(TAG, "No next node candidate, or staying on node: " + previouslyCurrentNode.getId());
                            currentNode = previouslyCurrentNode; // Stay on the current node
                            // Get the message from this node, processing the current server response
                            messageToDisplayToUser = currentNode.handleConversationTurn(fullJsonResponse, ChatMessage.TYPE_SERVER);
                            messageTypeForDisplay = ChatMessage.TYPE_SERVER; // As it's based on direct server response processing
                        } else {
                            // We have a next node candidate.
                            currentNode = nextNodeCandidate;
                            Log.d(TAG, "Transitioning to node: " + currentNode.getId());

                            // Now, the message to display should come from this NEW currentNode.
                            // If this new currentNode is supposed to process the *same* fullJsonResponse
                            // (e.g., an info_confirmation node that uses details extracted by plirofories from this server response),
                            // then pass fullJsonResponse. 
                            // If this new currentNode is just a prompt (e.g., transition from root to plirofories, or info_confirmation to info_complete)
                            // then pass null for jsonResponse to its handleConversationTurn.
                            
                            // Decision: When do we pass fullJsonResponse to the new current node?
                            // - If previouslyCurrentNode was root AND new node is a category (EXTRACT/CATEGORISE) -> null (it's a prompt)
                            // - If new node is for CONFIRMATION and previous was EXTRACT -> pass fullJsonResponse (confirmation uses details from extract)
                            // - If new node is for GIVING INFO (e.g. info_complete) and previous was CONFIRMATION -> null (or new server call would happen before this)
                            // This logic is getting complicated here. The most straightforward approach for your desired flow
                            // is that each node defines what it needs.
                            // If the next node is supposed to show the details gathered by the *previous* node from *this* server call,
                            // then the previous node's handleConversationTurn should have already set its message2, and the new node will just use it if needed.

                            // For your desired flow: "message_1 + message_2" from the NEW current node.
                            // If new node is like "info_confirmation", its message_2 template should be filled by this jsonResponse.
                            // If new node is just a prompt like "plirofories" after root, it should not process jsonResponse from root's turn.
                            
                            if (previouslyCurrentNode == rootNode && currentNode != rootNode) {
                                // Transition from root: new node displays its static message (usually a prompt)
                                messageToDisplayToUser = currentNode.handleConversationTurn(null, ChatMessage.TYPE_BOT);
                                Log.d(TAG, "Message from new node (post-root) " + currentNode.getId() + ": " + messageToDisplayToUser);
                            } else if (currentNode.getType().equals("EXTRACT") && previouslyCurrentNode.getType().equals("EXTRACT") && fullJsonResponse != null) {
                                 // Case: Plirofories (EXTRACT) -> Info_Confirmation (EXTRACT using same details)
                                 // The new currentNode (info_confirmation) should process the *same* jsonResponse to fill its template.
                                messageToDisplayToUser = currentNode.handleConversationTurn(fullJsonResponse, ChatMessage.TYPE_SERVER);
                                messageTypeForDisplay = ChatMessage.TYPE_SERVER; 
                                Log.d(TAG, "Message from EXTRACT node " + currentNode.getId() + " processing existing jsonResponse: " + messageToDisplayToUser);
                            } else {
                                // Default: new node displays its static message or processes a new server call later.
                                // For this turn, it displays its static message (message_1 + message_2 from JSON, message_2 not template-filled now)
                                messageToDisplayToUser = currentNode.handleConversationTurn(null, ChatMessage.TYPE_BOT);
                                Log.d(TAG, "Message from new node " + currentNode.getId() + " (no new JSON processing this turn): " + messageToDisplayToUser);
                            }
                        }

                        responseCallback.onResponseReceived(messageToDisplayToUser, messageTypeForDisplay);
                        System.out.println("Current Node after turn processing: " + (currentNode != null ? currentNode.getId() : "null"));

                    } catch (Exception e) {
                        Log.e(TAG, "Error processing server response: " + e.getMessage(), e);
                        responseCallback.onResponseReceived("Σφάλμα επεξεργασίας απάντησης διακομιστή.", ChatMessage.TYPE_BOT);
                    }
                }

                @Override
                public void onError(String errorMessage) {
                    Log.e(TAG, "Server error: " + errorMessage);
                    responseCallback.onResponseReceived("Σφάλμα επικοινωνίας με τον διακομιστή: " + errorMessage, ChatMessage.TYPE_BOT);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error in getResponse", e);
            responseCallback.onResponseReceived("Προέκυψε σφάλμα κατά την επεξεργασία του αιτήματός σας.", ChatMessage.TYPE_BOT);
        }
    }

    /**
     * Internal callback for server requests
     */
    private interface ServerRequestCallback {
        void onSuccess(String category, String fullJsonResponse);

        void onError(String errorMessage);
    }

    /**
     * Internal method to handle server communication
     * 
     * @param jsonRequest The JSON request to send to the server
     * @param callback    Callback to handle the server response
     */
    private void makeServerRequest(JSONObject jsonRequest, ServerRequestCallback callback) {
        // Use the client instance that was created in the constructor
        final String userMessage = jsonRequest.optString("message", "");

        try {
            // Use our class-level client instance
            client.sendMessage(userMessage, new Client.ServerResponseCallback() {
                @Override
                public void onServerResponse(String category, String fullJsonResponse) {
                    callback.onSuccess(category, fullJsonResponse);
                }

                @Override
                public void onError(String errorMessage) {
                    callback.onError(errorMessage);
                }
            });
        } catch (Exception e) {
            callback.onError("Error sending message to server: " + e.getMessage());
        }
    }

    /**
     * Callback interface for receiving responses from the ChatbotManager
     */
    public interface ResponseCallback {
        /**
         * Called when a response is ready
         * 
         * @param response    The response message
         * @param messageType The type of message (BOT or SERVER)
         */
        void onResponseReceived(String response, int messageType);
    }

    /**
     * Prints the current node's content to the system output
     * Helpful for debugging and understanding the current state of the conversation
     */
    public void printCurrentNode() {
        if (currentNode != null) {
            System.out.println("Current Node: " + currentNode);
        } else {
            System.out.println("Current Node is null");
        }
    }
    // Template handling methods have been removed as we now directly use message_1
    // and message_2 values

    /**
     * Process local response without server communication
     * 
     * @param userMessage      The user's message
     * @param responseCallback Callback to receive the response
     */
}