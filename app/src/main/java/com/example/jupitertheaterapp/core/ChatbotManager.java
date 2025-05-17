package com.example.jupitertheaterapp.core;

import android.content.Context;
import android.util.Log;

import com.example.jupitertheaterapp.model.ChatbotNode;
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
import java.util.Random;
import java.util.Set;

public class ChatbotManager {
    private static final String TAG = "ChatbotManager";
    private static final String CONVERSATION_FILE = "conversation_tree.json";

    private JSONObject jsonTree; // Keep for reference
    private ChatbotNode rootNode;
    private Map<String, ChatbotNode> nodeMap;
    private ChatbotNode currentNode;
    private Random random = new Random();
    private boolean useServerForResponses = true;

    public ChatbotManager(Context context) {
        nodeMap = new HashMap<>();
        loadConversationTree(context);
    }    private void loadConversationTree(Context context) {
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
        String type = jsonNode.getString("type");
        String message = jsonNode.getString("message");
        String content = jsonNode.optString("content", "");
        String fallback = jsonNode.optString("fallback", "I didn't understand that.");

        ChatbotNode node = new ChatbotNode(id, type, message, content, fallback);
        nodeMap.put(id, node);

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
    }

    // Called after all nodes are created to resolve string references
    private void resolveNodeReferences() {
        for (ChatbotNode node : nodeMap.values()) {
            List<String> pendingIds = node.getPendingChildIds();
            for (String id : pendingIds) {
                ChatbotNode referencedNode = nodeMap.get(id);
                if (referencedNode != null) {
                    node.addChild(referencedNode);
                }
            }
            node.clearPendingChildIds();
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
    }

    private void createMinimalStructure() {
        rootNode = new ChatbotNode("root", "CATEGORISE",
            "Γεια σας! Πώς μπορώ να σας βοηθήσω;", "",
            "Δεν κατάλαβα την ερώτησή σας.");
        nodeMap.put("root", rootNode);
        currentNode = rootNode;
    }

    public String getInitialMessage() {
        if (rootNode != null) {
            return rootNode.getMessage();
        }
        return "Γεια σας! Πώς μπορώ να σας βοηθήσω;";
    }

    public String getResponseForNodeId(String nodeId) {
        Log.d(TAG, "Getting response for node ID: " + nodeId);

        ChatbotNode node = nodeMap.get(nodeId);
        if (node == null) {
            Log.e(TAG, "Node ID not found: " + nodeId);
            return "Συγγνώμη, δεν βρέθηκε απάντηση.";
        }

        currentNode = node;
        return node.getMessage();
    }

    public String getLocalResponse(String userInput) {
        try {
            ChatbotNode nextNode = chooseNextNode(userInput);
            if (nextNode != null) {
                currentNode = nextNode;
                return nextNode.getMessage();
            } else {
                return currentNode.getFallback();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting local response", e);
            return "Συγγνώμη, προέκυψε ένα σφάλμα.";
        }
    }

    public ChatbotNode chooseNextNode(String userInput) {
        if (!currentNode.hasChildren()) {
            return rootNode;
        }

        List<ChatbotNode> children = currentNode.getChildren();
        if (children.isEmpty()) {
            return rootNode;
        }

        // Log available children for debugging
        logAvailableChildren(children);

        // Simple selection - could be improved with NLP
        int idx = random.nextInt(children.size());
        ChatbotNode nextNode = children.get(idx);
        // Assign messageTemplate for nextNode based on previous node's ID
        try {
            nextNode.setMessageTemplate(MsgTemplate.createTemplate(currentNode.getId()));
        } catch (IllegalArgumentException e) {
            // No template available for this previous node ID; skip
        }
        return nextNode;
    }

    private void logAvailableChildren(List<ChatbotNode> children) {
        StringBuilder childrenInfo = new StringBuilder("Available children: ");
        for (ChatbotNode child : children) {
            childrenInfo.append(child.getId()).append(", ");
        }
        Log.d(TAG, childrenInfo.toString());
    }

    public String findNodeResponseById(String nodeId) {
        ChatbotNode node = nodeMap.get(nodeId);
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
    }

    public String getParentNodeId(String nodeId) {
        ChatbotNode node = nodeMap.get(nodeId);
        if (node != null && node.getParent() != null) {
            return node.getParent().getId();
        }
        return "";
    }    /**
     * Prints the entire conversation tree structure to the log.
     * This is useful for debugging and visualizing the chatbot tree.
     */
    public void printTree() {
        Log.d(TAG, "Printing Conversation Tree Structure:");
        Log.d(TAG, "======================================");
        if (rootNode != null) {
            printNodeTree(rootNode, 0, new HashSet<String>());
        } else {
            Log.d(TAG, "Tree is empty (rootNode is null)");
        }
        Log.d(TAG, "======================================");
    }

    /**
     * Helper method to recursively print a node and its children with proper indentation.
     * Handles circular references by keeping track of visited nodes.
     * 
     * @param node The node to print
     * @param depth The current depth in the tree (for indentation)
     * @param visited Set of already visited node IDs to prevent infinite recursion
     */
    private void printNodeTree(ChatbotNode node, int depth, Set<String> visited) {
        if (node == null) return;
        
        // Check if this node was already visited to prevent infinite recursion
        if (visited.contains(node.getId())) {            StringBuilder indent = new StringBuilder();
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
        }

        // Print current node details
        String currentMarker = (node == currentNode) ? " [CURRENT]" : "";
        String display = String.format("%s%s [ID: %s, Type: %s]%s", 
                indent, connector, node.getId(), node.getType(), currentMarker);
        Log.d(TAG, display);

        // Print message preview (truncated if too long)
        String messagePreview = node.getMessage();
        if (messagePreview.length() > 40) {
            messagePreview = messagePreview.substring(0, 37) + "...";
        }
        Log.d(TAG, indent + "    ├── Message: " + messagePreview);

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
     * Helper method to recursively build a string representation of a node and its children.
     * Handles circular references by keeping track of visited nodes.
     * 
     * @param node The node to process
     * @param depth The current depth in the tree (for indentation)
     * @param visited Set of already visited node IDs to prevent infinite recursion
     * @param sb StringBuilder to accumulate the result
     */
    private void getNodeTreeAsString(ChatbotNode node, int depth, Set<String> visited, StringBuilder sb) {
        if (node == null) return;
        
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
        }

        // Build current node details
        String currentMarker = (node == currentNode) ? " [CURRENT]" : "";
        String prefix = indent.toString() + "└── ";
        String display = String.format("%s[ID: %s, Type: %s]%s", 
                prefix, node.getId(), node.getType(), currentMarker);
        sb.append(display).append("\n");

        // Add message preview (truncated if too long)
        String messagePreview = node.getMessage();
        if (messagePreview.length() > 40) {
            messagePreview = messagePreview.substring(0, 37) + "...";
        }
        sb.append(indent).append("    ├── Message: ").append(messagePreview).append("\n");

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
        if (node == null) return;
        
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
}