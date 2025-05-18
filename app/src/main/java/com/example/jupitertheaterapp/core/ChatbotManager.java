package com.example.jupitertheaterapp.core;

import android.content.Context;
import android.util.Log;

import com.example.jupitertheaterapp.model.ChatMessage;
import com.example.jupitertheaterapp.model.ChatbotNode;
import com.example.jupitertheaterapp.model.ConversationState;
import com.example.jupitertheaterapp.model.MsgTemplate;

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
    private static final String CONVERSATION_FILE = "conversation_tree.json";    private JSONObject jsonTree; // Keep for reference
    private ChatbotNode rootNode; // Root node of conversation tree
    private Map<String, ChatbotNode> nodeMap;
    private ChatbotNode currentNode;
    private boolean useServerForResponses = true;
    private ConversationState conversationState;

    public ChatbotManager(Context context) {
        nodeMap = new HashMap<>();
        conversationState = ConversationState.getInstance();
        Log.d(TAG, "Conversation State: ");
        Log.d(TAG, conversationState.getCurrentStateAsString());
        loadConversationTree(context);
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
    }    private ChatbotNode convertJsonToNodeStructure(JSONObject jsonNode) throws JSONException {
        String id = jsonNode.getString("id");
        String category = jsonNode.optString("category", id);
        String type = jsonNode.getString("type");
        String content = jsonNode.optString("content", "");
        String fallback = jsonNode.optString("fallback", "I didn't understand that.");

        Log.d(TAG, "Processing node: " + id + ", category: " + category + ", type: " + type);
        
        // Log the children array for this node
        if (jsonNode.has("children")) {
            Object childrenObj = jsonNode.get("children");
            if (childrenObj instanceof JSONArray) {
                JSONArray childArray = (JSONArray) childrenObj;
                Log.d(TAG, "Node " + id + " has " + childArray.length() + " children in JSON");
                
                for (int i = 0; i < childArray.length(); i++) {
                    Object childObj = childArray.get(i);
                    if (childObj instanceof JSONObject) {
                        JSONObject childJson = (JSONObject) childObj;
                        Log.d(TAG, "  Child " + i + " is object with ID: " + childJson.optString("id", "unknown"));
                    } else if (childObj instanceof String) {
                        String childId = (String) childObj;
                        Log.d(TAG, "  Child " + i + " is string reference to: " + childId);
                    } else {
                        Log.d(TAG, "  Child " + i + " is unknown type: " + childObj.getClass().getName());
                    }
                }
            } else {
                Log.d(TAG, "Node " + id + " has children but not in JSONArray format");
            }
        } else {
            Log.d(TAG, "Node " + id + " has no children in JSON");
        }

        ChatbotNode node;

        // Check if we have the new dual message format
        if (jsonNode.has("message_1") && jsonNode.has("message_2")) {
            String message1 = jsonNode.getString("message_1");
            String message2 = jsonNode.getString("message_2");
            
            Log.d(TAG, "Node " + id + " has dual messages: message_1=" + message1.substring(0, Math.min(20, message1.length())) + "...");
            
            // Use the constructor that accepts both message formats
            node = new ChatbotNode(id, type, message1, message2, content, fallback);
        } else {
            // Fall back to the original format
            String message;
            if (jsonNode.has("message_1")) {
                message = jsonNode.getString("message_1");
                Log.d(TAG, "Node " + id + " has only message_1");
            } else if (jsonNode.has("message")) {
                // For backward compatibility with old JSON format
                message = jsonNode.getString("message");
                Log.d(TAG, "Node " + id + " has only message (old format)");
            } else {
                // Default message if neither format is present
                message = "No message available.";
                Log.d(TAG, "Node " + id + " has no message fields");
            }
            
            node = new ChatbotNode(id, type, message, content, fallback);
        }        // Try to assign the appropriate MsgTemplate based on the node's category
        try {
            MsgTemplate template = MsgTemplate.createTemplate(category);
            node.setMessageTemplate(template);
            Log.d(TAG, "Assigned template to node: " + id + " with category: " + category);
        } catch (IllegalArgumentException e) {
            // No template available for this node category; that's ok
            Log.d(TAG, "No template available for node: " + id + " with category: " + category);
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
            Log.d(TAG, "Node in map: " + key + ", children: " + mapNode.getChildren().size() + 
                  ", pending children: " + mapNode.getPendingChildIds().size());
        }
        
        // Now process the pending children
        for (ChatbotNode node : nodeMap.values()) {
            List<String> pendingIds = node.getPendingChildIds();
            Log.d(TAG, "Resolving " + pendingIds.size() + " pending child IDs for node: " + node.getId());
            
            for (String id : pendingIds) {
                ChatbotNode referencedNode = nodeMap.get(id);
                if (referencedNode != null) {
                    node.addChild(referencedNode);
                    Log.d(TAG, "  Added child " + id + " to parent " + node.getId());
                } else {
                    Log.e(TAG, "  Failed to find node with ID: " + id + " for parent: " + node.getId());
                }
            }
            node.clearPendingChildIds();
        }
        
        Log.d(TAG, "After resolving references, root node has " + rootNode.getChildren().size() + " children");

        // Second pass to propagate templates from parents to children
        propagateTemplatesToChildren(rootNode);
    }    /**
     * Propagates templates from parent nodes to their children.
     * The root node is exempt from receiving templates.
     * 
     * @param node The current node to process
     */
    private void propagateTemplatesToChildren(ChatbotNode node) {
        if (node == null)
            return;

        MsgTemplate template = node.getMessageTemplate();

        // Propagate template to children if it exists
        if (template != null) {
            for (ChatbotNode child : node.getChildren()) {
                // Skip if child is the root node
                if (child.getId().equals("root"))
                    continue;

                // Set the parent's template to the child
                child.setMessageTemplate(template);
                Log.d(TAG, "Propagated template from " + node.getCategory() + " to child: " + child.getCategory());
            }
        }

        // Continue recursively for all children
        for (ChatbotNode child : node.getChildren()) {
            // Skip processing already visited root node to prevent circular reference
            // issues
            if (child.getId().equals("root"))
                continue;

            propagateTemplatesToChildren(child);
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
    }public String getInitialMessage() {
        if (rootNode != null) {
            return rootNode.getSystemMessage().getMessage();
        }
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

        currentNode = foundNode;
        // Return system message from the node
        return foundNode.getSystemMessage().getMessage();
    }public String getLocalResponse(String userInput) {
        try {
            // Create JSON request using the current node (for debugging/consistency)
            JSONObject jsonRequest = currentNode.createRequestJson(userInput);
            Log.d(TAG, "Local response using JSON request: " + jsonRequest.toString());
            
            // Update the current node with the user message
            currentNode.setUserMessage(userInput);
            
            // Process the request locally - find the next node using the node's own logic
            ChatbotNode nextNode = currentNode.chooseNextNode(userInput);
            if (nextNode != null) {
                // Apply the message template from the current node to the next node
                try {
                    nextNode.setMessageTemplate(MsgTemplate.createTemplate(currentNode.getCategory()));
                } catch (IllegalArgumentException e) {
                    // No template available for this node category; that's ok
                }
                
                currentNode = nextNode;
                // Now the system message is what we want to return
                return nextNode.getSystemMessage().getMessage();
            } else {
                return currentNode.getFallback();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting local response", e);
            return "Συγγνώμη, προέκυψε ένα σφάλμα.";
        }
    }    /**
     * This method has been moved to ChatbotNode class.
     * The ChatbotNode.chooseNextNode() method is now responsible for
     * selecting the next conversation node based on user input and conversation context.
     * 
     * @deprecated Use ChatbotNode.chooseNextNode() instead for history-based node selection
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
            return node.getMessage();
        }
        return "Δεν βρέθηκε απάντηση.";
    }

    public boolean shouldUseServer() {
        return useServerForResponses;
    }

    public void setUseServerForResponses(boolean useServer) {
        this.useServerForResponses = useServer;
    }

    public void reset() {
        currentNode = rootNode;
    }

    public ChatbotNode getCurrentNode() {
        return currentNode;
    }    public String getParentNodeId(String nodeId) {
        ChatbotNode node = nodeMap.get(nodeId);
        if (node != null && node.getParent() != null) {
            return node.getParent().getId();
        }
        return "";
    }
    
    /**
     * Gets a node by its ID
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
        Log.d(TAG, "======================================");        if (rootNode != null) {
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
        }        // Print current node details
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
        }        // Build current node details
        String currentMarker = (node == currentNode) ? " [CURRENT]" : "";
        String prefix = indent.toString() + "└── ";
        String display = String.format("%s[ID: %s, Category: %s, Type: %s]%s",
                prefix, node.getId(), node.getCategory(), node.getType(), currentMarker);
        sb.append(display).append("\n");        // Add system message preview (truncated if too long)
        ChatMessage sysMsg = node.getSystemMessage();
        String sysMsgType = (sysMsg.getType() == ChatMessage.TYPE_BOT) ? "BOT" : "SERVER";
        String sysMsgPreview = sysMsg.getMessage();
        if (sysMsgPreview.length() > 40) {
            sysMsgPreview = sysMsgPreview.substring(0, 37) + "...";
        }
        sb.append(indent).append("    ├── System [").append(sysMsgType).append("]: ").append(sysMsgPreview).append("\n");
        
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
     * Gets the current conversation as a list of nodes, each with system and user messages.
     * This shows only the conversation path that has been traversed, not the entire tree.
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
     * Uses parent references to build the path, ensuring we can track the full conversation.
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
                break;  // Stop if we detect a cycle
            }
            
            // Add the parent to the beginning of the path
            path.add(0, parent);
            visitedNodeIds.add(parent.getId());
            
            // Move up to the parent
            node = parent;
        }
        
        return path;
    }
}