package com.example.jupitertheaterapp.util;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.jupitertheaterapp.core.ChatbotManager;
import com.example.jupitertheaterapp.model.ChatbotNode;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;

public class Client {
    private static final String TAG = "Client";
    private String serverHost = "192.168.1.158"; // Default from server logs
    private int serverPort = 65432; // Default port
    private ChatbotManager chatbotManager;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    // Socket connection components
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private Thread connectionThread;
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private final AtomicBoolean isConnected = new AtomicBoolean(false);

    public Client(ChatbotManager chatbotManager) {
        this.chatbotManager = chatbotManager;
        // Start the persistent connection
        connect();
    }    public interface ServerResponseCallback {
        void onServerResponse(String category, String fullJsonResponse);

        void onError(String errorMessage);
    }

    /**
     * Establishes a persistent connection to the server
     */
    public synchronized void connect() {
        if (isRunning.get()) {
            Log.d(TAG, "Connection thread is already running");
            return;
        }

        isRunning.set(true);
        connectionThread = new Thread(() -> {
            while (isRunning.get()) {
                try {
                    if (!isConnected.get()) {
                        Log.d(TAG, "Attempting to connect to " + serverHost + ":" + serverPort);
                        socket = new Socket(serverHost, serverPort);
                        out = new PrintWriter(new OutputStreamWriter(
                                socket.getOutputStream(), StandardCharsets.UTF_8), true);
                        in = new BufferedReader(new InputStreamReader(
                                socket.getInputStream(), StandardCharsets.UTF_8));
                        isConnected.set(true);
                        Log.d(TAG, "Connected to server successfully");
                    }
                    // Sleep to avoid tight loop
                    Thread.sleep(5000);
                } catch (IOException e) {
                    Log.e(TAG, "Connection error", e);
                    closeConnection();
                    // Wait before trying to reconnect
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException ie) {
                        Log.d(TAG, "Connection sleep interrupted", ie);
                    }
                } catch (InterruptedException e) {
                    Log.d(TAG, "Connection thread interrupted", e);
                }
            }
        });
        connectionThread.start();
    }

    /**
     * Sends a message to the server based on the current node's type
     */
    public void sendMessage(String userMessage, ServerResponseCallback callback) {
        try {
            // Get current node from ChatbotManager
            ChatbotNode node = chatbotManager.getCurrentNode();

            // Use the node's createRequestJson method to generate the JSON object
            JSONObject jsonRequest = node.createRequestJson(userMessage);

            // Send the request directly
            sendJsonRequest(jsonRequest, callback);

        } catch (Exception e) {
            Log.e(TAG, "Error creating or sending request", e);
            mainHandler.post(() -> callback.onError("Error processing message: " + e.getMessage()));
        }
    }

    /**
     * Converts a ChatbotNode to a JSONObject
     */
    private JSONObject nodeToJson(ChatbotNode node) {
        JSONObject json = new JSONObject();
        try {
            json.put("id", node.getId());
            json.put("category", node.getCategory()); // Include category field
            json.put("type", node.getType());
            json.put("message", node.getMessage()); // Using primary message
            json.put("message_1", node.getMessage()); // Including message_1 explicitly
            json.put("message_2", node.getMessage2()); // Including message_2 explicitly
            json.put("content", node.getContent());
            json.put("fallback", node.getFallback());

            // Print the JSON object for debugging
            System.out.println("NODE CONVERTED TO JSON: " + json.toString());
        } catch (JSONException e) {
            Log.e(TAG, "Error converting node to JSON", e);
        }
        return json;
    }

    /**
     * Gets the appropriate category for a node.
     * If the parent is root, we use the current node's category.
     * Otherwise, we use the parent's category.
     */
    private String getParentNodeCategory(JSONObject node) {
        try {
            String currentId = node.getString("id");
            String currentCategory = node.optString("category", "");

            // Get the parent node ID from ChatbotManager
            String parentId = chatbotManager.getParentNodeId(currentId);

            // If parent is "root" or empty, use the current node's category
            if ("root".equals(parentId) || parentId.isEmpty()) {
                return currentCategory;
            }

            // Otherwise get the parent node and use its category
            ChatbotNode parentNode = chatbotManager.getNodeById(parentId);
            if (parentNode != null) {
                return parentNode.getCategory();
            }

            return currentCategory;
        } catch (JSONException e) {
            Log.e(TAG, "Error getting parent node category", e);
            return "";
        }
    }

    // Methods categorizeMessage and extractFromMessage have been replaced by using
    // ChatbotNode.createRequestJson

    /**
     * Sends the JSON request to the server
     */
    private void sendJsonRequest(JSONObject jsonRequest, ServerResponseCallback callback) {
        if (!isConnected.get()) {
            mainHandler.post(() -> callback.onError("Not connected to server. Attempting to reconnect..."));
            connect();
            return;
        }

        new Thread(() -> {
            try {
                if (out != null && socket != null && !socket.isClosed()) { // Send JSON request to server
                    String requestStr = jsonRequest.toString();
                    out.println(requestStr);
                    Log.d(TAG, "Sent to server: " + requestStr);
                    // Add detailed JSON print for debugging
                    System.out.println("JSON SENT TO SERVER: " + requestStr); // Receive response from server
                    if (in != null) {
                        final String serverResponse = in.readLine();
                        Log.d(TAG, "Received from server: " + serverResponse);
                        // Add detailed JSON print for debugging
                        System.out.println("JSON RECEIVED FROM SERVER: " + serverResponse);

                        // Post callback to main thread
                        mainHandler.post(() -> {
                            if (serverResponse != null) {
                                try {
                                    // Parse the JSON response
                                    JSONObject jsonResponse = new JSONObject(serverResponse);
                                    // Debug all fields in the JSON response System.out.println("PARSING SERVER
                                    // RESPONSE: " + jsonResponse.toString());
                                    System.out.println("JSON FIELDS: " + jsonResponse.keys());                                    // Simply pass the server response to the callback
                                    // No template processing here - that will be done in ChatbotManager

                                    // Extract the category field
                                    if (jsonResponse.has("category")) {
                                        String category = jsonResponse.getString("category");
                                        System.out.println("CATEGORY FROM SERVER: " + category);                                            // Pass both the category and the full JSON response to the callback handler
                                            callback.onServerResponse(category, serverResponse);
                                    } else {
                                        callback.onError("Server response missing 'category' field");
                                    }
                                } catch (JSONException e) {
                                    Log.e(TAG, "Error parsing JSON response", e);
                                    callback.onError("Invalid server response format: " + e.getMessage());
                                }
                            } else {
                                callback.onError("Server sent no response");
                                // Connection might be broken if no response
                                closeConnection();
                            }
                        });
                    }
                } else {
                    mainHandler.post(() -> callback.onError("Connection to server lost. Reconnecting..."));
                    closeConnection();
                    connect();
                }
            } catch (IOException e) {
                Log.e(TAG, "Error communicating with server", e);
                closeConnection();
                // Post error callback to main thread
                mainHandler.post(() -> callback.onError("Error: " + e.getMessage()));
            }
        }).start();
    }

    /**
     * Closes the current connection resources
     */
    private synchronized void closeConnection() {
        isConnected.set(false);
        try {
            if (out != null) {
                out.close();
                out = null;
            }
            if (in != null) {
                in.close();
                in = null;
            }
            if (socket != null) {
                socket.close();
                socket = null;
            }
        } catch (IOException e) {
            Log.e(TAG, "Error closing connection", e);
        }
    }

    /**
     * Disconnects from the server and stops the connection thread
     */
    public synchronized void disconnect() {
        isRunning.set(false);
        if (connectionThread != null) {
            connectionThread.interrupt();
        }
        closeConnection();
        Log.d(TAG, "Client disconnected");
    }    // Template handling has been moved to ChatbotNode class
}