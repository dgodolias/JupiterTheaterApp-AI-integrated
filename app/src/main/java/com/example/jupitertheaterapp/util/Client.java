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
    private String serverHost = "192.168.1.18";  // Default from server logs
    private int serverPort = 65432;  // Default port
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
    }

    public interface ServerResponseCallback {
        void onServerResponse(String nodeId);
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
            json.put("category", node.getCategory());         // Include category field
            json.put("type", node.getType());
            json.put("message", node.getMessage());           // Using primary message
            json.put("message_1", node.getMessage());         // Including message_1 explicitly
            json.put("message_2", node.getMessage2());        // Including message_2 explicitly
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

    // Methods categorizeMessage and extractFromMessage have been replaced by using ChatbotNode.createRequestJson

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
                if (out != null && socket != null && !socket.isClosed()) {                         // Send JSON request to server
                    String requestStr = jsonRequest.toString();
                    out.println(requestStr);
                    Log.d(TAG, "Sent to server: " + requestStr);
                    // Add detailed JSON print for debugging
                    System.out.println("JSON SENT TO SERVER: " + requestStr);                         // Receive response from server
                    if (in != null) {
                        final String serverResponse = in.readLine();
                        Log.d(TAG, "Received from server: " + serverResponse);
                        // Add detailed JSON print for debugging
                        System.out.println("JSON RECEIVED FROM SERVER: " + serverResponse);

                        // Post callback to main thread
                        mainHandler.post(() -> {
                            if (serverResponse != null) {                                     try {
                                    // Parse the JSON response
                                    JSONObject jsonResponse = new JSONObject(serverResponse);
                                      // Debug all fields in the JSON response
                                    System.out.println("PARSING SERVER RESPONSE: " + jsonResponse.toString());
                                    System.out.println("JSON FIELDS: " + jsonResponse.keys());                                         // Save the server response to apply templates to message_2 fields
                                         ChatbotNode currentNode = chatbotManager.getCurrentNode();
                                         if (currentNode != null && jsonResponse.length() > 1) {
                                             System.out.println("APPLYING TEMPLATE TO NODE: " + currentNode.getId());
                                             
                                             // Get the appropriate default template based on the node's category
                                             String defaultTemplate = getDefaultTemplateForCategory(currentNode.getCategory());
                                                 
                                             // Apply the server response data to the node's message_2 template
                                             currentNode.applyTemplateToMessage2(serverResponse, defaultTemplate);
                                         }
                                      
                                    // Extract the category field
                                    if (jsonResponse.has("category")) {
                                        String category = jsonResponse.getString("category");
                                        System.out.println("CATEGORY FROM SERVER: " + category);

                                        // Check if category is a valid category
                                        if (isValidNodeId(category)) {
                                            callback.onServerResponse(category);
                                        } else {
                                            callback.onError("Category '" + category + "' is not a valid category");
                                        }
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
    }

    private boolean isValidNodeId(String category) {
        // These are now categories, not IDs
        String[] validCategories = {"ΚΡΑΤΗΣΗ", "ΑΚΥΡΩΣΗ", "ΠΛΗΡΟΦΟΡΙΕΣ", "ΑΞΙΟΛΟΓΗΣΕΙΣ & ΣΧΟΛΙΑ", "ΠΡΟΣΦΟΡΕΣ & ΕΚΠΤΩΣΕΙΣ"};

        for (String validCategory : validCategories) {
            if (validCategory.equals(category)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets a default template string based on the category
     * @param category The category to get a template for
     * @return A default template string with placeholders
     */
    private String getDefaultTemplateForCategory(String category) {
        switch (category) {
            case "ΚΡΑΤΗΣΗ":
                return "Επιβεβαίωση κράτησης για: <show_name> στην αίθουσα <room>, ημέρα <day> και ώρα <time>. " +
                       "Όνομα: <person_name>, Ηλικία: <person_age>, Θέση: <person_seat>. Θέλετε να προχωρήσετε;";
            case "ΑΚΥΡΩΣΗ":
                return "Ακύρωση κράτησης με αριθμό <reservation_number>. Παρακαλώ επιβεβαιώστε με τον κωδικό <passcode>.";
            case "ΠΛΗΡΟΦΟΡΙΕΣ":
                return "Πληροφορίες για την παράσταση \"<name>\": Παίζεται στην <room> κάθε <day> στις <time>. " +
                       "Συμμετέχουν: <cast>. Διάρκεια: <duration> λεπτά. Αξιολόγηση: <stars>/5.";
            case "ΑΞΙΟΛΟΓΗΣΕΙΣ & ΣΧΟΛΙΑ":
                return "Η παράσταση \"<name>\" έχει μέση αξιολόγηση <stars>/5 από τους θεατές μας.";
            case "ΠΡΟΣΦΟΡΕΣ & ΕΚΠΤΩΣΕΙΣ":
                return "Προσφορά για την παράσταση \"<show_name>\": <discount_percentage>% έκπτωση με τον κωδικό <discount_code>.";
            default:
                return "";
        }
    }
}