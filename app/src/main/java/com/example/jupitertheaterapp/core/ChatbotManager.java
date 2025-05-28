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
    private ChatbotNode currentNode;    private ConversationState conversationState;
    private Client client; // Client for server communications
    private static ChatbotManager instance;
    private String leftoverMessage; // Store completion messages for the root node

    public ChatbotManager(Context context) {
        instance = this;  // Store instance reference

        nodeMap = new HashMap<>();
        conversationState = ConversationState.getInstance();
        Log.d(TAG, "Conversation State: ");
        Log.d(TAG, conversationState.getCurrentStateAsString());
        loadConversationTree(context);

        // Initialize the client for server communications
        client = new Client(this);

        // Initialize the database
        SimpleDatabase.getInstance().initialize(context);
    }

    /**
     * Gets the singleton instance of the ChatbotManager
     * @return The ChatbotManager instance
     */
    public static synchronized ChatbotManager getInstance() {
        return instance;
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
    }    public String getInitialMessage() {
        Log.d(TAG, "Getting initial message from root node");
        
        if (rootNode != null) {
            Log.d(TAG, "Root node exists, checking messages");
            
            // Check current leftover message
            String currentLeftover = getLeftoverMessage();
            Log.d(TAG, "Current leftover message: " + currentLeftover);
            
            // Get both message1 and message2 from the root node
            String message1 = rootNode.getMessage();
            String message2 = rootNode.getMessage2();
            
            Log.d(TAG, "Root node message1 (after processing): " + message1);
            Log.d(TAG, "Root node message2: " + message2);
            
            // Build the combined message
            StringBuilder combinedMessage = new StringBuilder();
              // Add message1 if it exists (leftover message)
            if (message1 != null && !message1.isEmpty()) {
                Log.d(TAG, "Adding message1 to combined message");
                combinedMessage.append(message1);
            } else {
                Log.d(TAG, "Message1 is null or empty, skipping");
            }
            
            // Add message2 if it exists and is different from message1
            if (message2 != null && !message2.isEmpty()) {
                Log.d(TAG, "Processing message2");
                if (combinedMessage.length() > 0 && !message2.equals(message1)) {
                    combinedMessage.append("\n");
                    Log.d(TAG, "Added newline separator");
                }
                if (combinedMessage.length() == 0 || !message2.equals(message1)) {
                    combinedMessage.append(message2);
                    Log.d(TAG, "Added message2 to combined message");
                }
            } else {
                Log.d(TAG, "Message2 is null or empty, skipping");
            }
            
            // Return the combined message or fallback to message2 or default
            String result = combinedMessage.toString();
            String finalResult = !result.isEmpty() ? result : (message2 != null ? message2 : "Γεια σας! Πώς μπορώ να σας βοηθήσω;");
            
            Log.d(TAG, "Final initial message: " + finalResult);
            
            // Clear leftover message after using it
            if (currentLeftover != null && !currentLeftover.isEmpty()) {
                Log.d(TAG, "Clearing leftover message after use");
                clearLeftoverMessage();
            }
            
            return finalResult;
        }
        
        Log.d(TAG, "Root node is null, returning default message");
        return "Γεια σας! Πώς μπορώ να σας βοηθήσω;";
    }public String getResponseForNodeId(String category) {
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
        currentNode = foundNode;        // Get both message1 and message2 from the node
        String message1 = foundNode.getMessage();
        String message2 = foundNode.getMessage2();
        
        // Combine message1 and message2 with a newline between them, avoiding empty lines
        String combinedMessage = "";
        if (message1 != null && !message1.isEmpty()) {
            combinedMessage = message1;
        }
        if (message2 != null && !message2.isEmpty() && !message2.equals(message1)) {
            if (!combinedMessage.isEmpty()) {
                combinedMessage += "\n" + message2;
            } else {
                combinedMessage = message2;
            }
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

        if (node != null) {            // Get both message1 and message2 from the node
            String message1 = node.getMessage();
            String message2 = node.getMessage2();
            
            // Combine message1 and message2 with a newline between them, avoiding empty lines
            String combinedMessage = "";
            if (message1 != null && !message1.isEmpty()) {
                combinedMessage = message1;
            }
            if (message2 != null && !message2.isEmpty() && !message2.equals(message1)) {
                if (!combinedMessage.isEmpty()) {
                    combinedMessage += "\n" + message2;
                } else {
                    combinedMessage = message2;
                }
            }
            
            return combinedMessage;
        }
        return "Δεν βρέθηκε απάντηση.";
    }    public void reset() {
        // Reset to root node
        currentNode = rootNode;
        
        // Reset root node's template and category
        if (rootNode != null) {
            rootNode.setMessageTemplate(null);
            rootNode.setCategory("-"); // Use the consistent category identifier for root
            
            // Debug verification to confirm reset
            System.out.println("DEBUG: Reset chatbot - Template and category reset for root node");
            System.out.println("DEBUG: Root node state after reset: Template=" + 
                             (rootNode.getMessageTemplate() == null ? "null" : rootNode.getMessageTemplate().getClass().getSimpleName()) +
                             ", Category='" + rootNode.getCategory() + "'");
            
            // Double check if template somehow wasn't reset
            if (rootNode.getMessageTemplate() != null) {
                System.out.println("CRITICAL ERROR: Root template still not null after reset! Forcing null again.");
                rootNode.setMessageTemplate(null);
            }
        }
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
        String message1Preview = node.getMessage();
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
            result.append("- Message_1: ").append(node.getMessage()).append("\n");
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
     */    public void getResponse(String userMessage, ResponseCallback responseCallback) {
        Log.d(TAG, "Getting response for user message: " + userMessage);

        try {
            // Store the original user message for confirmation node navigation
            final String originalUserMessage = userMessage;
            
            // Check if we're at a confirmation node and user said "yes"
            // In this case, we want to skip server communication and go directly to root
            boolean isConfirmation = userMessage.toLowerCase().contains("yes") ||
                    userMessage.toLowerCase().contains("confirm") ||
                    userMessage.toLowerCase().contains("ναι") ||
                    userMessage.toLowerCase().contains("επιβεβαιώνω") ||
                    userMessage.toLowerCase().contains("ναί") ||
                    userMessage.toLowerCase().contains("οκ") ||
                    userMessage.toLowerCase().contains("ok");
            
            boolean isAtConfirmationNode = currentNode.getId().contains("confirmation");
              if (isAtConfirmationNode && isConfirmation) {
                Log.d(TAG, "User confirmed at confirmation node - navigating to root without server request");
                
                // PERFORM DATABASE OPERATIONS BEFORE NAVIGATION
                // Check which type of confirmation and execute appropriate database operation
                String currentNodeId = currentNode.getId();
                if (currentNode.getMessageTemplate() != null) {
                    Log.d(TAG, "Performing database operation for node: " + currentNodeId);
                    
                    SimpleDatabase database = SimpleDatabase.getInstance();
                    boolean operationSuccess = false;
                    String sepChar = "<sep>";
                    switch (currentNodeId) {                        case "booking_confirmation":
                            Log.d(TAG, "Validating booking details against shows database");
                            // First validate that the show exists with the provided details
                            boolean showExists = database.validateShowExists(currentNode.getMessageTemplate());
                            //sep char

                            if (showExists) {
                                Log.d(TAG, "Show validation passed, adding booking to database");
                                operationSuccess = database.addBooking(currentNode.getMessageTemplate());
                                if (operationSuccess) {
                                    Log.d(TAG, "Booking successfully added to database");
                                    // Log the current count for verification
                                    Log.d(TAG, "Bookings table now has " + database.getTableRecordCount("bookings") + " records");                                    // Set leftover message for booking completion
                                    MsgTemplate template = currentNode.getMessageTemplate();
                                    String showName = getTemplateField(template, "show_name");
                                    String leftoverMsg;
                                    if (!"N/A".equals(showName)) {
                                        leftoverMsg = "Η κράτησή σας για την παράσταση " + showName + " επιβεβαιώθηκε επιτυχώς!"+sepChar;
                                    } else {
                                        leftoverMsg = "Η κράτησή σας επιβεβαιώθηκε επιτυχώς!"+sepChar;
                                    }
                                    Log.d(TAG, "Setting leftover message for booking: " + leftoverMsg);
                                    setLeftoverMessage(leftoverMsg);
                                    Log.d(TAG, "Leftover message set, current value: " + getLeftoverMessage());                                } else {
                                    Log.e(TAG, "Failed to add booking to database");
                                    String leftoverMsg = "Υπήρξε πρόβλημα με την κράτησή σας. Παρακαλώ δοκιμάστε ξανά.";
                                    Log.d(TAG, "Setting leftover message for booking failure: " + leftoverMsg);
                                    setLeftoverMessage(leftoverMsg);
                                    Log.d(TAG, "Leftover message set, current value: " + getLeftoverMessage());
                                }
                            } else {
                                Log.e(TAG, "Show validation failed - no matching show found");
                                operationSuccess = false; // Mark operation as failed
                                String leftoverMsg = "συγγνωμη δεν υπάρχει παράσταση που να ικανοποιει τα κριτήριαπου θέσατε. Παρακαλώ μέσα απο το menu πληροφοριων, δειτε τις σωστές πληροφορίες και δοκιμάστε ξανα!"+sepChar;
                                Log.d(TAG, "Setting leftover message for show validation failure: " + leftoverMsg);
                                setLeftoverMessage(leftoverMsg);
                                Log.d(TAG, "Leftover message set, current value: " + getLeftoverMessage());
                            }
                            break;
                              case "cancel_confirmation":
                            Log.d(TAG, "Removing booking from database");
                            operationSuccess = database.removeBooking(currentNode.getMessageTemplate());
                            if (operationSuccess) {
                                Log.d(TAG, "Booking successfully removed from database");
                                Log.d(TAG, "Bookings table now has " + database.getTableRecordCount("bookings") + " records");                                // Set leftover message for cancellation completion
                                MsgTemplate template = currentNode.getMessageTemplate();
                                String reservationNumber = getTemplateField(template, "reservation_number");
                                String leftoverMsg;
                                if (!"N/A".equals(reservationNumber)) {
                                    leftoverMsg = "Η κράτησή σας με αριθμό " + reservationNumber + " ακυρώθηκε επιτυχώς."+ sepChar;
                                } else {
                                    leftoverMsg = "Η κράτησή σας ακυρώθηκε επιτυχώς."+ sepChar;
                                }
                                Log.d(TAG, "Setting leftover message for cancellation: " + leftoverMsg);
                                setLeftoverMessage(leftoverMsg);
                                Log.d(TAG, "Leftover message set, current value: " + getLeftoverMessage());                            } else {
                                Log.e(TAG, "Failed to remove booking from database");
                                String leftoverMsg = "Δεν βρέθηκε κράτηση με αυτόν τον συνδυασμό στοιχείων.<sep>";
                                Log.d(TAG, "Setting leftover message for cancellation failure: " + leftoverMsg);
                                setLeftoverMessage(leftoverMsg);
                                Log.d(TAG, "Leftover message set, current value: " + getLeftoverMessage());
                            }
                            break;                              case "review_confirmation":
                            Log.d(TAG, "Adding review to database");
                            operationSuccess = database.addReview(currentNode.getMessageTemplate());
                            if (operationSuccess) {
                                Log.d(TAG, "Review successfully added to database");
                                Log.d(TAG, "Reviews table now has " + database.getTableRecordCount("reviews") + " records");                                // Set leftover message for review completion
                                MsgTemplate template = currentNode.getMessageTemplate();
                                String reservationNumber = getTemplateField(template, "reservation_number");
                                String leftoverMsg;
                                if (!"N/A".equals(reservationNumber)) {
                                    leftoverMsg = "Η αξιολόγησή σας για την κράτηση " + reservationNumber + " καταχωρήθηκε επιτυχώς!"+ sepChar;
                                } else {
                                    leftoverMsg = "Η αξιολόγησή σας καταχωρήθηκε επιτυχώς!"+ sepChar;
                                }
                                Log.d(TAG, "Setting leftover message for review: " + leftoverMsg);
                                setLeftoverMessage(leftoverMsg);
                                Log.d(TAG, "Leftover message set, current value: " + getLeftoverMessage());                            } else {
                                Log.e(TAG, "Failed to add review to database");
                                String leftoverMsg = "Δεν βρέθηκε κράτηση με αυτόν τον συνδυασμό στοιχείων. Παρακαλώ ελέγξτε τα στοιχεία κράτησης και δοκιμάστε ξανά."+ sepChar;
                                Log.d(TAG, "Setting leftover message for review failure: " + leftoverMsg);
                                setLeftoverMessage(leftoverMsg);
                                Log.d(TAG, "Leftover message set, current value: " + getLeftoverMessage());
                            }
                            break;
                            
                        default:
                            Log.d(TAG, "No database operation needed for node: " + currentNodeId);
                            operationSuccess = true; // No operation needed, consider it successful
                            break;
                    }
                    
                    // Log operation result and current database state
                    if (operationSuccess) {
                        Log.d(TAG, "Database operation completed successfully for " + currentNodeId);
                        database.logDatabaseState(); // Log current state for debugging
                    } else {
                        Log.w(TAG, "Database operation failed for " + currentNodeId + " - continuing with navigation");
                    }
                } else {
                    Log.w(TAG, "No template available for database operation at node: " + currentNodeId);
                }
                  // Directly navigate to the next node (which should be root)
                ChatbotNode nextNode = currentNode.chooseNextNode(originalUserMessage);
                Log.d(TAG, "Chosen next node after confirmation: " + (nextNode != null ? nextNode.getId() : "null"));
                
                if (nextNode != null && "root".equals(nextNode.getId())) {
                    Log.d(TAG, "Successfully navigating to root node - skipping server communication");
                    currentNode = nextNode;
                    
                    // Return the root node's message without any server communication
                    String rootMessage = getInitialMessage();
                    
                    Log.d(TAG, "Returning root message without server request: " + rootMessage);
                    responseCallback.onResponseReceived(rootMessage, ChatMessage.TYPE_BOT);
                    return; // Skip the rest of the method
                } else {
                    Log.w(TAG, "Next node is not root or is null - continuing with normal flow");
                }
            }
            
            // For all other cases, proceed with normal server communication
            // Create JSON request using the current node
            JSONObject jsonRequest = currentNode.createRequestJson(userMessage);

            // Update the current node with the user message
            currentNode.setUserMessage(userMessage);

            System.out.println("Current node: " + currentNode);

            // Make the server request
            makeServerRequest(jsonRequest, new ServerRequestCallback() {@Override
                public void onSuccess(String category, String fullJsonResponse) {
                    try {
                        // IMPORTANT - First populate the template with the server response
                        // Process the JSON and populate the template for the current node
                        // This ensures template data is available before making navigation decisions
                        String initialResponse = currentNode.handleConversationTurn(fullJsonResponse, ChatMessage.TYPE_SERVER);
                        Log.d(TAG, "Initial JSON processing complete, template populated");
                          // Set the category as user message for node navigation purposes
                        currentNode.setUserMessage(category);
                        
                        // Clear leftover message for information requests
                        if ("ΠΛΗΡΟΦΟΡΙΕΣ".equals(category)) {
                            Log.d(TAG, "Information request detected - clearing leftover message");
                            clearLeftoverMessage();
                        }
                        
                        // Try to get the next node based on category AND template completeness
                        // Pass the original user input for proper confirmation node navigation
                        Log.d(TAG, "Navigation debug - Original user input: '" + originalUserMessage + "', Server category: '" + category + "'");
                        ChatbotNode nextNode = currentNode.chooseNextNode(originalUserMessage);
                        if (nextNode != null) {
                            Log.d(TAG, "Navigation successful - Moving from '" + currentNode.getId() + "' to '" + nextNode.getId() + "'");
                            // Update the current node to the next one
                            currentNode = nextNode;
                            
                            // Now process the conversation turn for the new node
                            // This handles state transition, message processing, and response building
                            String combinedMessage = currentNode.handleConversationTurn(fullJsonResponse, ChatMessage.TYPE_SERVER);
                            Log.d(TAG, "Conversation turn processed for node: " + currentNode.getId());
                            Log.d(TAG, "Current state: " + currentNode.getCurrentState());
                            
                            int messageType = currentNode.getSystemMessage().getType();

                            // Debug logging
                            Log.d(TAG, "Using system message from node: " + currentNode.getId());
                            Log.d(TAG, "Combined message: " + combinedMessage);

                            responseCallback.onResponseReceived(combinedMessage, messageType);
                        } else {
                            // No matching node - use getResponseForNodeId as fallback
                            Log.d(TAG, "No matching node found for category: " + category + ", using fallback");                            // Create a temporary ChatbotNode to handle the message
                            ChatbotNode tempNode = new ChatbotNode("temp", "CATEGORISE", "", "", "", "");
                            // Use the current state but with the temporary node
                            String combinedMessage = tempNode.handleConversationTurn(fullJsonResponse, ChatMessage.TYPE_SERVER);
                            Log.d(TAG, "Fallback handler processed message");
                            
                            Log.d(TAG, "Combined fallback message: " + combinedMessage);
                            responseCallback.onResponseReceived(combinedMessage, ChatMessage.TYPE_SERVER);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing server response", e);
                        responseCallback.onResponseReceived("Συγγνώμη, προέκυψε ένα σφάλμα.", ChatMessage.TYPE_BOT);
                    }
                }

                @Override
                public void onError(String errorMessage) {
                    Log.e(TAG, "Server error: " + errorMessage);
                    responseCallback.onResponseReceived("Συγγνώμη, προέκυψε ένα σφάλμα επικοινωνίας με τον διακομιστή.",
                            ChatMessage.TYPE_BOT);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error in getResponse", e);
            responseCallback.onResponseReceived("Συγγνώμη, προέκυψε ένα σφάλμα.", ChatMessage.TYPE_BOT);
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
        }    }

    /**
     * Gets a comprehensive debug information including tree structure, current node, and database state
     * @return A formatted string containing all debug information
     */
    public String getComprehensiveDebugInfo() {
        StringBuilder debugInfo = new StringBuilder();
        
        // Add tree structure
        debugInfo.append("=== CONVERSATION TREE STRUCTURE ===\n");
        debugInfo.append(getTreeAsString());
        debugInfo.append("\n");
        
        // Add current node information
        debugInfo.append("=== CURRENT NODE DETAILS ===\n");
        debugInfo.append(getCurrentNodeDebugInfo());
        debugInfo.append("\n");
        
        // Add conversation path
        debugInfo.append("=== CONVERSATION PATH ===\n");
        debugInfo.append(getConversationNodeList());
        debugInfo.append("\n");
        
        // Add database state
        debugInfo.append("=== DATABASE STATE ===\n");
        debugInfo.append(getDatabaseDebugInfo());
        
        return debugInfo.toString();
    }

    /**
     * Gets detailed information about the current node
     * @return A formatted string with current node details
     */
    public String getCurrentNodeDebugInfo() {
        if (currentNode == null) {
            return "Current Node: null\n";
        }
        
        StringBuilder info = new StringBuilder();
        info.append("Current Node Details:\n");
        info.append("- ID: ").append(currentNode.getId()).append("\n");
        info.append("- Category: ").append(currentNode.getCategory()).append("\n");
        info.append("- Type: ").append(currentNode.getType()).append("\n");
        info.append("- Current State: ").append(currentNode.getCurrentState()).append("\n");
        
        // System message info
        if (currentNode.getSystemMessage() != null) {
            info.append("- System Message Type: ");
            info.append(currentNode.getSystemMessage().getType() == ChatMessage.TYPE_BOT ? "BOT" : "SERVER");
            info.append("\n");
            info.append("- System Message: ").append(currentNode.getSystemMessage().getMessage()).append("\n");
        }
        
        // User message info
        if (currentNode.getUserMessage() != null) {
            info.append("- User Message: ").append(currentNode.getUserMessage().getMessage()).append("\n");
        }
        
        // Template info
        if (currentNode.getMessageTemplate() != null) {
            MsgTemplate template = currentNode.getMessageTemplate();
            info.append("- Template Type: ").append(template.getClass().getSimpleName()).append("\n");
            info.append("- Template Fields: ").append(template.getFieldValuesMap()).append("\n");
            
            List<String> missingFields = template.getMissingFields();
            if (!missingFields.isEmpty()) {
                info.append("- Missing Fields: ").append(missingFields).append("\n");
            }
        }
        
        // Children info
        info.append("- Children Count: ").append(currentNode.getChildren().size()).append("\n");
        if (!currentNode.getChildren().isEmpty()) {
            info.append("- Children IDs: ");
            for (ChatbotNode child : currentNode.getChildren()) {
                info.append(child.getId()).append(", ");
            }
            info.setLength(info.length() - 2); // Remove last comma and space
            info.append("\n");
        }
        
        return info.toString();
    }

    /**
     * Gets comprehensive database state information for debugging
     * @return A formatted string with database state details
     */
    public String getDatabaseDebugInfo() {
        SimpleDatabase db = SimpleDatabase.getInstance();
        StringBuilder dbInfo = new StringBuilder();
        
        dbInfo.append("Database State Information:\n");
        dbInfo.append("==========================================\n");
        
        // Table record counts
        dbInfo.append("TABLE RECORD COUNTS:\n");
        dbInfo.append("- Bookings: ").append(db.getTableRecordCount("bookings")).append(" records\n");
        dbInfo.append("- Reviews: ").append(db.getTableRecordCount("reviews")).append(" records\n");
        dbInfo.append("- Shows: ").append(db.getTableRecordCount("shows")).append(" records\n");
        dbInfo.append("- Discounts: ").append(db.getTableRecordCount("discounts")).append(" records\n");
        dbInfo.append("\n");
        
        // Recent database operations (from logs)
        dbInfo.append("RECENT DATABASE ACTIVITY:\n");
        dbInfo.append("(Check system logs for detailed operation history)\n");
        dbInfo.append("\n");
          // Sample data from each table
        dbInfo.append("ALL DATABASE ENTRIES:\n");
        
        // Bookings - all entries
        dbInfo.append("--- All Bookings ---\n");
        String bookingsSample = getDatabaseTableSample("bookings", Integer.MAX_VALUE);
        dbInfo.append(bookingsSample);
        
        // Reviews - all entries
        dbInfo.append("--- All Reviews ---\n");
        String reviewsSample = getDatabaseTableSample("reviews", Integer.MAX_VALUE);
        dbInfo.append(reviewsSample);
        
        // Shows - all entries
        dbInfo.append("--- All Shows ---\n");
        String showsSample = getDatabaseTableSample("shows", Integer.MAX_VALUE);
        dbInfo.append(showsSample);
        
        // Discounts - all entries
        dbInfo.append("--- All Discounts ---\n");
        String discountsSample = getDatabaseTableSample("discounts", Integer.MAX_VALUE);
        dbInfo.append(discountsSample);
        
        dbInfo.append("==========================================\n");
        return dbInfo.toString();
    }

    /**
     * Gets a sample of data from a specific database table
     * @param tableName The name of the table
     * @param limit Maximum number of records to return
     * @return Formatted string with sample data
     */
    private String getDatabaseTableSample(String tableName, int limit) {
        SimpleDatabase db = SimpleDatabase.getInstance();
        StringBuilder sample = new StringBuilder();
        
        try {
            // Query all records from the table (SimpleDatabase handles the limit internally)
            JSONArray results = db.queryRecords(tableName, null);
            
            if (results == null || results.length() == 0) {
                sample.append("No records found.\n");
            } else {
                int recordsToShow = Math.min(results.length(), limit);
                for (int i = 0; i < recordsToShow; i++) {
                    JSONObject record = results.getJSONObject(i);
                    sample.append("Record ").append(i + 1).append(": ");
                      // Format key fields based on table type
                    switch (tableName) {
                        case "bookings":
                            sample.append("Show: ").append(getFieldValue(record, "show_name"));
                            sample.append(", Day: ").append(getFieldValue(record, "day"));
                            sample.append(", Time: ").append(getFieldValue(record, "time"));
                            sample.append(", Room: ").append(getFieldValue(record, "room"));
                            sample.append(", Person: ").append(getNestedFieldValue(record, "person", "name"));
                            sample.append(", Seat: ").append(getNestedFieldValue(record, "person", "seat"));
                            break;
                        case "reviews":
                            sample.append("Show: ").append(getFieldValue(record, "show_name"));
                            sample.append(", Reservation: ").append(getFieldValue(record, "reservation_number"));
                            sample.append(", Rating: ").append(getFieldValue(record, "stars"));
                            String reviewText = getFieldValue(record, "review");
                            if (reviewText.length() > 50) reviewText = reviewText.substring(0, 47) + "...";
                            sample.append(", Review: ").append(reviewText);
                            break;
                        case "shows":
                            sample.append("Title: ").append(getFieldValue(record, "name"));
                            sample.append(", Day: ").append(getFieldValue(record, "day"));
                            sample.append(", Time: ").append(getFieldValue(record, "time"));
                            sample.append(", Room: ").append(getFieldValue(record, "room"));
                            sample.append(", Topic: ").append(getFieldValue(record, "topic"));
                            sample.append(", Duration: ").append(getFieldValue(record, "duration")).append("min");
                            break;
                        case "discounts":
                            sample.append("Show: ").append(getFieldValue(record, "show_name"));
                            sample.append(", Age: ").append(getFieldValue(record, "age"));
                            sample.append(", Discount: ").append(getFieldValue(record, "discount_percentage")).append("%");
                            sample.append(", Code: ").append(getFieldValue(record, "code"));
                            sample.append(", Date: ").append(getFieldValue(record, "date"));
                            break;
                        default:
                            sample.append(record.toString());
                            break;
                    }
                    sample.append("\n");                }
                
                // Only show "more records" message if we're actually limiting the results
                if (results.length() > limit && limit != Integer.MAX_VALUE) {
                    sample.append("... and ").append(results.length() - limit).append(" more records\n");
                }
            }
        } catch (Exception e) {
            sample.append("Error retrieving data: ").append(e.getMessage()).append("\n");
            Log.e(TAG, "Error getting database sample for table: " + tableName, e);
        }
        
        sample.append("\n");
        return sample.toString();
    }

    // Template handling methods have been removed as we now directly use message_1
    // and message_2 values


    public String processResultsTag(String message, String category, MsgTemplate template) {
        // Check if message contains <results> tag
        if (message == null || !message.contains("<results>")) {
            return message; // No processing needed
        }
        
        // Get the database instance
        SimpleDatabase db = SimpleDatabase.getInstance();
        
        // Get the corresponding table for this category
        String tableName = db.getCategoryTableMapping(category);
        if (tableName == null) {
            Log.e(TAG, "No table mapping found for category: " + category);
            return message.replace("<results>", "Δεν βρέθηκαν αποτελέσματα.");
        }
        
        // Check if template has queryable fields
        if (template == null || !template.hasQueryableFields()) {
            Log.e(TAG, "Template has no queryable fields for category: " + category);
            return message.replace("<results>", "Δεν υπάρχουν επαρκή στοιχεία για αναζήτηση.");
        }
        
        // Query the database
        JSONArray results = db.queryRecords(tableName, template);
        
        // Format the results
        String formattedResults;
        if (results == null || results.length() == 0) {
            formattedResults = "Δεν βρέθηκαν αποτελέσματα που να ταιριάζουν με τα κριτήρια σας.";
        } else {
            formattedResults = db.formatResults(results, template);
        }
        
        // Replace the <results> tag with formatted results
        return message.replace("<results>", formattedResults);
    }
    
    /**
     * Helper method to extract field values from the nested JSON structure
     * Each field in the database has format: {"value": "actual_value", "pvalues": [...]}
     * @param record The JSON record
     * @param fieldName The name of the field to extract
     * @return The actual value or "N/A" if not found
     */
    private String getFieldValue(JSONObject record, String fieldName) {
        try {
            if (record.has(fieldName)) {
                JSONObject field = record.getJSONObject(fieldName);
                return field.optString("value", "N/A");
            }
            return "N/A";
        } catch (Exception e) {
            return "N/A";
        }
    }
    
    /**
     * Helper method to extract nested field values (like person.name)
     * @param record The JSON record
     * @param parentField The parent field name (e.g., "person")
     * @param childField The child field name (e.g., "name")
     * @return The actual value or "N/A" if not found
     */
    private String getNestedFieldValue(JSONObject record, String parentField, String childField) {
        try {
            if (record.has(parentField)) {
                JSONObject parent = record.getJSONObject(parentField);
                if (parent.has(childField)) {
                    JSONObject field = parent.getJSONObject(childField);
                    return field.optString("value", "N/A");
                }
            }
            return "N/A";        } catch (Exception e) {
            return "N/A";
        }
    }
    
    /**
     * Sets the leftover message for the root node
     * @param message The completion message to display
     */
    public void setLeftoverMessage(String message) {
        this.leftoverMessage = message;
    }
    
    /**
     * Gets the current leftover message
     * @return The leftover message or null if not set
     */
    public String getLeftoverMessage() {
        return this.leftoverMessage;
    }
      /**
     * Clears the leftover message
     */
    public void clearLeftoverMessage() {
        this.leftoverMessage = null;
    }
      /**
     * Helper method to extract field values from a template
     * @param template The message template
     * @param fieldName The field name to extract
     * @return The field value or "N/A" if not found
     */
    private String getTemplateField(MsgTemplate template, String fieldName) {
        if (template == null) return "N/A";
        
        Map<String, List<String>> fieldValues = template.getFieldValuesMap();
        if (fieldValues.containsKey(fieldName)) {
            List<String> values = fieldValues.get(fieldName);
            if (values != null && !values.isEmpty()) {
                return values.get(0);
            }
        }
        return "N/A";
    }
}