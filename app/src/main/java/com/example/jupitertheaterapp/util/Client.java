package com.example.jupitertheaterapp.util;

import android.content.Context;
import android.content.res.AssetManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.jupitertheaterapp.core.ChatbotManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class Client {
    private static final String TAG = "Client";
    private String serverHost = "192.168.1.18";  // Default from server logs
    private int serverPort = 65432;  // Default port
    private ChatbotManager chatbotManager;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public Client(ChatbotManager chatbotManager) {
        this.chatbotManager = chatbotManager;
        // Test connection to server
        testConnection();
    }

    public interface ServerResponseCallback {
        void onServerResponse(String nodeId);
        void onError(String errorMessage);
    }

    private void testConnection() {
        new Thread(() -> {
            Log.d(TAG, "Testing connection to server at " + serverHost + ":" + serverPort);
            try (Socket socket = new Socket(serverHost, serverPort)) {
                Log.d(TAG, "Connection test successful");
            } catch (Exception e) {
                Log.e(TAG, "Connection test failed", e);
            }
        }).start();
    }

    public void sendMessage(String userMessage, ServerResponseCallback callback) {
        new Thread(() -> {
            try (Socket socket = new Socket(serverHost, serverPort);
                 PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
                 BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {

                Log.d(TAG, "Connected to server at " + serverHost + ":" + serverPort);

                // Remove any embedded newlines
                String cleanMessage = userMessage.replaceAll("\\r?\\n", " ");

                // Send message to server
                out.println(cleanMessage);
                Log.d(TAG, "Sent to server: " + cleanMessage);

                // Receive response from server
                final String serverResponse = in.readLine();
                Log.d(TAG, "Received from server: " + serverResponse);

                // Post callback to main thread
                mainHandler.post(() -> {
                    if (serverResponse != null) {
                        // Check if response is one of the valid node IDs
                        if (isValidNodeId(serverResponse)) {
                            callback.onServerResponse(serverResponse);
                        } else {
                            callback.onError("Server response '" + serverResponse + "' is not a valid node ID");
                        }
                    } else {
                        callback.onError("Server sent no response");
                    }
                });

            } catch (IOException e) {
                Log.e(TAG, "Error communicating with server", e);
                // Post error callback to main thread
                mainHandler.post(() -> callback.onError("Error: " + e.getMessage()));
            }
        }).start();
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