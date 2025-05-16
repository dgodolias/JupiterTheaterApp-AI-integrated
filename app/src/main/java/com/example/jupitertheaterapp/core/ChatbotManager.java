package com.example.jupitertheaterapp.core;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;

public class ChatbotManager {
    private static final String TAG = "ChatbotManager";
    private static final String CONVERSATION_FILE = "conversation_tree.json";

    private JSONObject conversationTree;
    private Map<String, JSONObject> nodeMap;
    private JSONObject currentNode;
    private Random random = new Random();
    private boolean useServerForResponses = true; // Default to use server

    public ChatbotManager(Context context) {
        loadConversationTree(context);
    }

    private void loadConversationTree(Context context) {
        try {
            // Read JSON file from assets
            String jsonString = readJSONFromAsset(context, CONVERSATION_FILE);
            if (jsonString != null) {
                conversationTree = new JSONObject(jsonString);
                nodeMap = new HashMap<>();

                // Start with the root node
                currentNode = conversationTree.getJSONObject("root");

                // Map all nodes by their IDs for easy reference
                mapAllNodes(conversationTree);

                Log.d(TAG, "Conversation tree loaded successfully with " + nodeMap.size() + " nodes");
                // Log available root children
                try {
                    JSONArray rootChildren = currentNode.getJSONArray("children");
                    Log.d(TAG, "Root has " + rootChildren.length() + " children");
                    for (int i = 0; i < rootChildren.length(); i++) {
                        Object child = rootChildren.get(i);
                        if (child instanceof JSONObject) {
                            Log.d(TAG, "Root child " + i + ": " + ((JSONObject) child).getString("id"));
                        } else {
                            Log.d(TAG, "Root child " + i + ": " + child.toString());
                        }
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "Error logging root children", e);
                }
            } else {
                Log.e(TAG, "Failed to load conversation tree JSON");
                createMinimalStructure();
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing conversation tree JSON", e);
            createMinimalStructure();
        }
    }

    // Recursively map all nodes by their IDs
    private void mapAllNodes(JSONObject parentObject) throws JSONException {
        // Check if this is a node with an ID
        if (parentObject.has("id")) {
            String id = parentObject.getString("id");
            nodeMap.put(id, parentObject);
            Log.d(TAG, "Mapped node with ID: " + id);

            // Process children
            if (parentObject.has("children")) {
                JSONArray children = parentObject.getJSONArray("children");
                for (int i = 0; i < children.length(); i++) {
                    Object child = children.get(i);
                    if (child instanceof JSONObject) {
                        // Recursive mapping for embedded child objects
                        mapAllNodes((JSONObject) child);
                    }
                }
            }
        } else {
            // Handle the root object which might not have an ID field itself
            for (Iterator<String> it = parentObject.keys(); it.hasNext();) {
                String key = it.next();
                Object value = parentObject.get(key);
                if (value instanceof JSONObject) {
                    mapAllNodes((JSONObject) value);
                }
            }
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
        } catch (IOException ex) {
            Log.e(TAG, "Error reading JSON file: " + filePath, ex);
            return null;
        }
        return json;
    }

    private void createMinimalStructure() {
        try {
            String minimalJson = "{\"root\":{\"id\":\"root\",\"message\":\"Configuration error: Please check your conversation_tree.json file.\",\"children\":[],\"fallback\":\"The chatbot is not properly configured.\"}}";
            conversationTree = new JSONObject(minimalJson);
            currentNode = conversationTree.getJSONObject("root");
            nodeMap = new HashMap<>();
            nodeMap.put("root", currentNode);
        } catch (JSONException e) {
            Log.e(TAG, "Error creating minimal structure", e);
        }
    }

    public String getInitialMessage() {
        try {
            if (currentNode != null && currentNode.has("message")) {
                return currentNode.getString("message");
            } else {
                return "Configuration error: Please check your conversation_tree.json file.";
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error getting initial message", e);
            return "Configuration error: Please check your conversation_tree.json file.";
        }
    }

    // Get response based on node ID from server
    public String getResponseForNodeId(String nodeId) throws JSONException {
        Log.d(TAG, "Getting response for node ID: " + nodeId);

        if (!nodeMap.containsKey(nodeId)) {
            Log.e(TAG, "Node ID not found: " + nodeId);
            return "Error: Could not find response for " + nodeId;
        }

        JSONObject node = nodeMap.get(nodeId);
        currentNode = node;
        String message = node.getString("message");
        Log.d(TAG, "Found message for " + nodeId + ": " + message);

        return message;
    }

    public String getLocalResponse(String userInput) {
        try {
            if (currentNode == null) {
                return "Configuration error: Conversation tree not properly loaded.";
            }

            // Choose next node
            JSONObject nextNode = chooseNextNode(userInput);

            if (nextNode != null) {
                currentNode = nextNode;
                return nextNode.getString("message");
            } else {
                // Return fallback message if no valid node found
                return currentNode.has("fallback") ?
                    currentNode.getString("fallback") :
                    "I'm not sure how to respond to that.";
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error getting local response", e);
            return "Error processing your request.";
        }
    }

    // Function to choose the next node based on user input
    public JSONObject chooseNextNode(String userInput) throws JSONException {
        if (!currentNode.has("children") || currentNode.getJSONArray("children").length() == 0) {
            return null;
        }

        JSONArray children = currentNode.getJSONArray("children");

        // Log available children for debugging
        logAvailableChildren(children);

        // Randomly select a child
        return getRandomChild(children);
    }

    // Log available children for debugging
    private void logAvailableChildren(JSONArray children) throws JSONException {
        StringBuilder childrenInfo = new StringBuilder("Available children: ");

        for (int i = 0; i < children.length(); i++) {
            Object child = children.get(i);
            if (child instanceof String) {
                childrenInfo.append((String) child);
            } else if (child instanceof JSONObject && ((JSONObject) child).has("id")) {
                childrenInfo.append(((JSONObject) child).getString("id"));
            }

            if (i < children.length() - 1) {
                childrenInfo.append(", ");
            }
        }

        Log.d(TAG, childrenInfo.toString());
    }

    // Get a random child from the children array
    private JSONObject getRandomChild(JSONArray children) throws JSONException {
        if (children.length() == 0) {
            return null;
        }

        int randomIndex = random.nextInt(children.length());
        Object selectedChild = children.get(randomIndex);

        if (selectedChild instanceof String) {
            // Reference to an existing node by ID
            String nodeId = (String) selectedChild;
            Log.d(TAG, "Selected child by ID: " + nodeId);
            return nodeMap.get(nodeId);
        } else if (selectedChild instanceof JSONObject) {
            // Embedded node object
            Log.d(TAG, "Selected embedded child node");
            return (JSONObject) selectedChild;
        }

        return null;
    }

    // Find a specific node child of root by ID
    public String findNodeResponseById(String nodeId) {
        try {
            Log.d(TAG, "Finding node with ID: " + nodeId);

            // Get the root node
            JSONObject root = conversationTree.getJSONObject("root");
            JSONArray children = root.getJSONArray("children");

            // Look through children for matching ID
            for (int i = 0; i < children.length(); i++) {
                Object child = children.get(i);
                if (child instanceof JSONObject) {
                    JSONObject childObj = (JSONObject) child;
                    if (childObj.has("id") && childObj.getString("id").equals(nodeId)) {
                        currentNode = childObj;
                        Log.d(TAG, "Found matching node: " + nodeId);
                        return childObj.getString("message");
                    }
                }
            }

            // If not found, return fallback
            Log.d(TAG, "Node not found among root children: " + nodeId);
            return root.getString("fallback");

        } catch (JSONException e) {
            Log.e(TAG, "Error finding node by ID", e);
            return "Error processing your request.";
        }
    }

    // Determines whether to use server for responses
    public boolean shouldUseServer() {
        return useServerForResponses;
    }

    // Enable/disable server responses
    public void setUseServerForResponses(boolean useServer) {
        this.useServerForResponses = useServer;
    }

    public void reset() {
        try {
            currentNode = conversationTree.getJSONObject("root");
        } catch (JSONException e) {
            Log.e(TAG, "Error resetting to root node", e);
        }
    }
}