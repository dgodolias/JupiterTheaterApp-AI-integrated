import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class Client {
    public static void main(String[] args) {
        String hostname = "127.0.0.1";
        int port = 65432;
        // Explicitly use UTF-8 for console input
        Scanner userInputScanner = new Scanner(System.in, StandardCharsets.UTF_8.name());

        System.out.println("Java Client for Jupiter Theater Server");
        System.out.println("--------------------------------------");
        System.out.println("Connecting to server at " + hostname + ":" + port);

        try (Socket socket = new Socket(hostname, port);
             PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {

            System.out.println("Connected to server.");
            String userInput;

            while (true) {
                System.out.print("Enter message (or 'exit' to quit): ");
                userInput = userInputScanner.nextLine();

                if ("exit".equalsIgnoreCase(userInput)) {
                    // Send an "ΕΞΟΔΟΣ" message to let the server know, if your server handles it
                    // Or simply break and close the client-side connection.
                    // For this example, we'll send "ΕΞΟΔΟΣ" as the Python server might expect it.
                    out.println("ΕΞΟΔΟΣ"); 
                    System.out.println("Exiting client.");
                    break;
                }

                // Send message to server
                out.println(userInput);
                System.out.println("Sent to server: " + userInput);

                // Receive response from server
                String serverResponse = in.readLine(); // Assuming server sends a single line JSON response
                if (serverResponse != null) {
                    System.out.println("Received from server: " + serverResponse);
                    // You can add JSON parsing here if needed to pretty-print or process the response
                } else {
                    System.out.println("Server closed the connection or sent no response.");
                    break;
                }
            }

        } catch (java.net.ConnectException e) {
            System.err.println("Connection refused. Ensure the Python server is running at " + hostname + ":" + port);
        } catch (java.io.IOException e) {
            System.err.println("I/O error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            userInputScanner.close();
            System.out.println("Client shutdown complete.");
        }
    }
}
