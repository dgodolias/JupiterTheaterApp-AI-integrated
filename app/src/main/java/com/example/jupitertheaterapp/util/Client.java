package com.example.jupitertheaterapp.util;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.jupitertheaterapp.core.ChatbotManager;

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
                        Log.d(TAG, "Connecting to server at " + serverHost + ":" + serverPort);
                        socket = new Socket(serverHost, serverPort);
                        out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
                        in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                        isConnected.set(true);
                        Log.d(TAG, "Connected to server successfully");
                    }

                    // Keep the thread alive but don't do anything
                    // The server will keep the connection until timeout
                    Thread.sleep(5000);
                } catch (IOException e) {
                    Log.e(TAG, "Connection error", e);
                    closeConnection();
                    // Wait before trying to reconnect
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException ie) {
                        Log.d(TAG, "Reconnect sleep interrupted", ie);
                    }
                } catch (InterruptedException e) {
                    Log.d(TAG, "Connection thread interrupted", e);
                }
            }
        });
        connectionThread.start();
    }

    /**
     * Sends a message to the server using the persistent connection
     */
    public void sendMessage(String userMessage, ServerResponseCallback callback) {
        if (!isConnected.get()) {
            mainHandler.post(() -> callback.onError("Not connected to server. Attempting to reconnect..."));
            connect();
            return;
        }

        new Thread(() -> {
            try {
                if (out != null && socket != null && !socket.isClosed()) {
                    // Remove any embedded newlines
                    String cleanMessage = userMessage.replaceAll("\\r?\\n", " ");

                    // Send message to server
                    out.println(cleanMessage);
                    Log.d(TAG, "Sent to server: " + cleanMessage);

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
                    } else {
                        mainHandler.post(() -> callback.onError("Input stream is not available"));
                        closeConnection();
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