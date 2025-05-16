package com.example.jupitertheaterapp;

import android.util.Log;

import com.example.jupitertheaterapp.core.ChatbotManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class Client {
    private static final String TAG = "Client";
    private static final String SERVER_HOST = "127.0.0.1";
    private static final int SERVER_PORT = 65432;

    private ChatbotManager chatbotManager;

    public Client(ChatbotManager chatbotManager) {
        this.chatbotManager = chatbotManager;
    }

    public interface ServerResponseCallback {
        void onServerResponse(String nodeId);
        void onError(String errorMessage);
    }

    public void sendMessage(String userMessage, ServerResponseCallback callback) {
        new Thread(() -> {
            try (Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
                 PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
                 BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {

                Log.d(TAG, "Connected to server at " + SERVER_HOST + ":" + SERVER_PORT);

                // Remove any embedded newlines
                String cleanMessage = userMessage.replaceAll("\\r?\\n", " ");

                // Log diagnostic info
                Log.d(TAG, "Sending message to server: " + cleanMessage);
                Log.d(TAG, "Message bytes (UTF-8): " + bytesToHex(cleanMessage.getBytes(StandardCharsets.UTF_8)));

                // Send message to server
                out.println(cleanMessage);

                // Receive response from server
                String serverResponse = in.readLine();
                Log.d(TAG, "Received from server: " + serverResponse);

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

            } catch (IOException e) {
                Log.e(TAG, "Error communicating with server", e);
                callback.onError("Error: " + e.getMessage());
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

    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02X ", b));
        }
        return result.toString();
    }
}