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
                 // Get current node from ChatbotManager and convert to JSONObject
                 ChatbotNode node = chatbotManager.getCurrentNode();
                 JSONObject currentNode = nodeToJson(node);

                 if (currentNode != null && currentNode.has("type")) {
                     String type = currentNode.getString("type");

                     if ("CATEGORISE".equals(type)) {
                         // If at root, we want to categorize the message
                         categorizeMessage(userMessage, callback);
                     } else if ("EXTRACT".equals(type)) {
                         // For EXTRACT nodes, we need the parent node's ID as category
                         String parentId = getParentNodeId(currentNode);
                         extractFromMessage(parentId, userMessage, callback);
                     } else {
                         // Default to categorize
                         categorizeMessage(userMessage, callback);
                     }
                 } else {
                     // Default to categorize if node doesn't have type
                     categorizeMessage(userMessage, callback);
                 }
             } catch (JSONException e) {
                 Log.e(TAG, "Error reading node type", e);
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
                 json.put("type", node.getType());
                 json.put("message", node.getMessage());
                 json.put("content", node.getContent());
                 json.put("fallback", node.getFallback());
             } catch (JSONException e) {
                 Log.e(TAG, "Error converting node to JSON", e);
             }
             return json;
         }

         /**
          * Gets the appropriate category ID for a node.
          * If the parent is root, we use the current node's ID.
          * Otherwise, we use the parent's ID.
          */
         private String getParentNodeId(JSONObject node) {
             try {
                 String currentId = node.getString("id");

                 // Get the parent node ID from ChatbotManager
                 String parentId = chatbotManager.getParentNodeId(currentId);

                 // If parent is "root" or empty, use the current node's ID
                 if ("root".equals(parentId) || parentId.isEmpty()) {
                     return currentId;
                 }

                 // Otherwise use the parent's ID
                 return parentId;
             } catch (JSONException e) {
                 Log.e(TAG, "Error getting parent node ID", e);
                 return "";
             }
         }

         /**
          * Sends a CATEGORISE message to the server
          */
         public void categorizeMessage(String userMessage, ServerResponseCallback callback) {
             JSONObject jsonRequest = new JSONObject();
             try {
                 jsonRequest.put("type", "CATEGORISE");
                 jsonRequest.put("category", "");
                 jsonRequest.put("message", userMessage);
                 sendJsonRequest(jsonRequest, callback);
             } catch (JSONException e) {
                 Log.e(TAG, "Error creating JSON request", e);
                 mainHandler.post(() -> callback.onError("Error formatting request: " + e.getMessage()));
             }
         }

         /**
          * Sends an EXTRACT message to the server
          */
         public void extractFromMessage(String category, String userMessage, ServerResponseCallback callback) {
             JSONObject jsonRequest = new JSONObject();
             try {
                 jsonRequest.put("type", "EXTRACT");
                 jsonRequest.put("category", category);
                 jsonRequest.put("message", userMessage);
                 sendJsonRequest(jsonRequest, callback);
             } catch (JSONException e) {
                 Log.e(TAG, "Error creating JSON request", e);
                 mainHandler.post(() -> callback.onError("Error formatting request: " + e.getMessage()));
             }
         }

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
                     if (out != null && socket != null && !socket.isClosed()) {
                         // Send JSON request to server
                         String requestStr = jsonRequest.toString();
                         out.println(requestStr);
                         Log.d(TAG, "Sent to server: " + requestStr);

                         // Receive response from server
                         if (in != null) {
                             final String serverResponse = in.readLine();
                             Log.d(TAG, "Received from server: " + serverResponse);

                             // Post callback to main thread
                             mainHandler.post(() -> {
                                 if (serverResponse != null) {
                                     try {
                                         // Parse the JSON response
                                         JSONObject jsonResponse = new JSONObject(serverResponse);

                                         // Extract the category field
                                         if (jsonResponse.has("category")) {
                                             String category = jsonResponse.getString("category");

                                             // Check if category is a valid node ID
                                             if (isValidNodeId(category)) {
                                                 callback.onServerResponse(category);
                                             } else {
                                                 callback.onError("Category '" + category + "' is not a valid node ID");
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

         private boolean isValidNodeId(String nodeId) {
             String[] validIds = {"ΚΡΑΤΗΣΗ", "ΑΚΥΡΩΣΗ", "ΠΛΗΡΟΦΟΡΙΕΣ", "ΑΞΙΟΛΟΓΗΣΕΙΣ & ΣΧΟΛΙΑ", "ΠΡΟΣΦΟΡΕΣ & ΕΚΠΤΩΣΕΙΣ"};

             for (String id : validIds) {
                 if (id.equals(nodeId)) {
                     return true;
                 }
             }
             return false;
         }
     }