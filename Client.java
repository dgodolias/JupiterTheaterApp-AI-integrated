import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class Client {
    public static void main(String[] args) {
        String hostname = "127.0.0.1";
        int port = 65432;
        // Use default charset for console input
        BufferedReader consoleReader = null; 

        System.out.println("Java Client for Jupiter Theater Server");
        System.out.println("--------------------------------------");
        System.out.println("Connecting to server at " + hostname + ":" + port);

        try (Socket socket = new Socket(hostname, port);
             PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {

            // Initialize consoleReader with default charset
            consoleReader = new BufferedReader(new InputStreamReader(System.in));
            System.out.println("Connected to server. Console input reader initialized with system default charset.");
            String userInput;

            while (true) {
                // Read message content from file and send once
                userInput = Files.readString(Path.of("message.txt"), StandardCharsets.UTF_8);
                // Remove any embedded newlines so the entire message is sent as one line
                userInput = userInput.replaceAll("\\r?\\n", " ");

                // Send the file content and exit
                System.out.println("Sending file-based message: " + userInput);
                 
                // Diagnostic: Print the string and its UTF-8 bytes
                System.out.println("User input as string (from file): " + userInput);
                try {
                    System.out.print("User input bytes (expected UTF-8 for network): ");
                    for (byte b : userInput.getBytes(StandardCharsets.UTF_8)) {
                        System.out.print(String.format("%02X ", b));
                    }
                    System.out.println();
                } catch (Exception e) {
                    System.out.println("Error getting bytes: " + e.getMessage());
                }

                // Send file-based message to server
                out.println(userInput);
                System.out.println("Sent to server (from message.txt): " + userInput);

                // Receive response from server
                String serverResponse = in.readLine(); // Assuming server sends a single line JSON response
                if (serverResponse != null) {
                    System.out.println("Received from server: " + serverResponse);
                    // You can add JSON parsing here if needed to pretty-print or process the response
                } else {
                    System.out.println("Server closed the connection or sent no response.");
                }
                // Done sending file-based message, exit loop
                break;
            }

        } catch (java.net.ConnectException e) {
            System.err.println("Connection refused. Ensure the Python server is running at " + hostname + ":" + port);
        } catch (java.io.IOException e) {
            System.err.println("I/O error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Close the consoleReader if it was initialized
            if (consoleReader != null) {
                try {
                    consoleReader.close();
                } catch (java.io.IOException e) {
                    System.err.println("Error closing console reader: " + e.getMessage());
                }
            }
            System.out.println("Client shutdown complete.");
        }
    }
}