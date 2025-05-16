package com.example.jupitertheaterapp.core;

import android.content.Context;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class ChatbotManager {
    private static final String TAG = "ChatbotManager";
    private static final String CONVERSATION_FILE = "conversation_tree.json";
    private static final String DEFAULT_CONVERSATION_FILE = "default_conversation_tree.json";

    private JSONObject conversationTree;
    private JSONObject nodesObject;
    private Map<String, String> keywordMap;
    private String currentNodeId = "root";
    private Random random = new Random();
    private boolean useServerForResponses = false;
    private boolean isConfigured = false;

    public ChatbotManager(Context context) {
        loadConversationTree(context);
    }

    private void loadConversationTree(Context context) {
        try {
            // Read JSON file from assets
            String jsonString = readJSONFromAsset(context, CONVERSATION_FILE);
            if (jsonString != null) {
                initializeFromJson(jsonString);
                isConfigured = true;
            } else {
                // Try to load default conversation tree
                String defaultJsonString = readJSONFromAsset(context, DEFAULT_CONVERSATION_FILE);
                if (defaultJsonString != null) {
                    initializeFromJson(defaultJsonString);
                    isConfigured = true;
                } else {
                    // Create minimal structure if no JSON files are available
                    createMinimalStructure();
                    isConfigured = false;
                }
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing conversation tree JSON", e);
            createMinimalStructure();
            isConfigured = false;
        }
    }

    private void initializeFromJson(String jsonString) throws JSONException {
        conversationTree = new JSONObject(jsonString);
        nodesObject = conversationTree.getJSONObject("nodes");

        // Load keywords map
        keywordMap = new HashMap<>();
        if (conversationTree.has("keywords")) {
            JSONObject keywords = conversationTree.getJSONObject("keywords");
            Iterator<String> keys = keywords.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                String nodeId = keywords.getString(key);

                // Each key may contain multiple keywords separated by commas
                String[] keywordArray = key.split(",");
                for (String keyword : keywordArray) {
                    keywordMap.put(keyword.trim().toLowerCase(), nodeId);
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

    // Create minimal conversation structure
    private void createMinimalStructure() {
        try {
            // Create a minimal structure that indicates configuration is needed
            String minimalJson = "{\"nodes\":{\"root\":{\"id\":\"root\",\"message\":\"Configuration error: Please check your conversation_tree.json file.\",\"children\":[],\"fallback\":\"The chatbot is not properly configured.\"}}}";
            conversationTree = new JSONObject(minimalJson);
            nodesObject = conversationTree.getJSONObject("nodes");
            keywordMap = new HashMap<>();
        } catch (JSONException e) {
            Log.e(TAG, "Error creating minimal structure", e);
        }
    }

    public String getInitialMessage() {
        try {
            if (isConfigured && nodesObject.has("root")) {
                JSONObject rootNode = nodesObject.getJSONObject("root");
                return rootNode.getString("message");
            } else {
                return "Configuration error: Please check your conversation_tree.json file.";
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error getting initial message", e);
            return "Configuration error: Please check your conversation_tree.json file.";
        }
    }

    public String getLocalResponse(String userInput) {
        try {
            if (!isConfigured) {
                return "Configuration error: Please check your conversation_tree.json file.";
            }

            JSONObject currentNode = nodesObject.getJSONObject(currentNodeId);

            // Try to match keywords in user input
            String nextNodeId = findMatchingNodeByKeywords(userInput);

            if (nextNodeId == null) {
                // If no keywords match, choose a random child
                nextNodeId = getRandomChildId(currentNode);
            }

            if (nextNodeId != null && nodesObject.has(nextNodeId)) {
                currentNodeId = nextNodeId;
                JSONObject nextNode = nodesObject.getJSONObject(nextNodeId);
                return nextNode.getString("message");
            } else {
                // Return fallback message if no valid node found
                return currentNode.has("fallback") ?
                    currentNode.getString("fallback") :
                    "No response available. Please check your conversation configuration.";
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error getting local response", e);
            return "Configuration error: Please check your conversation_tree.json file.";
        }
    }

    private String findMatchingNodeByKeywords(String userInput) {
        if (userInput == null || keywordMap.isEmpty()) {
            return null;
        }

        userInput = userInput.toLowerCase();
        for (Map.Entry<String, String> entry : keywordMap.entrySet()) {
            if (userInput.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    private String getRandomChildId(JSONObject node) throws JSONException {
        if (node.has("children")) {
            List<String> childIds = new ArrayList<>();
            for (int i = 0; i < node.getJSONArray("children").length(); i++) {
                childIds.add(node.getJSONArray("children").getString(i));
            }

            if (!childIds.isEmpty()) {
                return childIds.get(random.nextInt(childIds.size()));
            }
        }
        return null;
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
        currentNodeId = "root";
    }
}