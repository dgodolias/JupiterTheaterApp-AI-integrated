package com.example.jupitertheaterapp.util;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServerClient {
    private static final String TAG = "ServerClient";
    private static final String SERVER_HOST = "127.0.0.1";
    private static final int SERVER_PORT = 65432;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());

    public interface ServerResponseCallback {
        void onResponse(String response);
        void onError(String errorMessage);
    }

    public void sendMessage(String message, ServerResponseCallback callback) {
        executor.execute(() -> {
            try (Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
                 PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
                 BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {

                // Remove any embedded newlines
                String cleanMessage = message.replaceAll("\\r?\\n", " ");

                // Send message to server
                out.println(cleanMessage);
                Log.d(TAG, "Sent to server: " + cleanMessage);

                // Receive response from server
                final String serverResponse = in.readLine();
                Log.d(TAG, "Received from server: " + serverResponse);

                // Return to main thread
                handler.post(() -> {
                    if (serverResponse != null) {
                        callback.onResponse(serverResponse);
                    } else {
                        callback.onError("Server sent no response");
                    }
                });

            } catch (IOException e) {
                Log.e(TAG, "Error communicating with server", e);
                handler.post(() -> callback.onError("Error: " + e.getMessage()));
            }
        });
    }

    public void shutdown() {
        executor.shutdown();
    }
}