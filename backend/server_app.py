import socket
import json
import sys
import os
from message_categorizer import categorize_prompt
from information_extractor import (
    extract_show_info,
    extract_booking_info,
    extract_cancellation_info,
    extract_discount_info,
    extract_review_info
)

def process_client_request(client_message):
    """Processes the client's message and returns a structured response."""
    category = categorize_prompt(client_message)
    response_data = {"category": category, "details": None, "error": None}

    print(f"Received message: {client_message}")
    print(f"Categorized as: {category}")

    if category == "ΠΛΗΡΟΦΟΡΙΕΣ":
        show_info = extract_show_info(client_message)
        response_data["details"] = show_info
        print(f"Extracted show info: {show_info}")
    elif category == "ΚΡΑΤΗΣΗ":
        bookings = extract_booking_info(client_message)
        response_data["details"] = bookings
        print(f"Extracted booking(s): {bookings}")
    elif category == "ΑΚΥΡΩΣΗ":
        cancellation_info = extract_cancellation_info(client_message)
        response_data["details"] = cancellation_info
        print(f"Extracted cancellation info: {cancellation_info}")
    elif category == "ΠΡΟΣΦΟΡΕΣ & ΕΚΠΤΩΣΕΙΣ":
        discount_info = extract_discount_info(client_message)
        response_data["details"] = discount_info
        print(f"Extracted discount info: {discount_info}")
    elif category == "ΑΞΙΟΛΟΓΗΣΕΙΣ & ΣΧΟΛΙΑ":
        review_info = extract_review_info(client_message)
        response_data["details"] = review_info
        print(f"Extracted review info: {review_info}")
    elif category == "ΕΞΟΔΟΣ":
        response_data["details"] = "Client requested to close connection."
        # For a server, "ΕΞΟΔΟΣ" might mean the client wants to disconnect
        # The server itself typically keeps running.
    else:
        response_data["error"] = "Unknown category or unable to process request."
        print(f"Unknown category or error processing: {category}")
        # Consider if you have fallback mechanisms for unknown categories

    return response_data

def start_server(host='127.0.0.1', port=65432):
    """Starts the TCP server to listen for client connections."""
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
        s.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        try:
            s.bind((host, port))
        except socket.error as e:
            print(f"Error binding server to {host}:{port} - {e}")
            sys.exit(1)
            
        s.listen()
        print(f"Jupiter Theater Server listening on {host}:{port}")
        
        while True:
            try:
                conn, addr = s.accept()
                with conn:
                    print(f"Connected by {addr}")
                    while True:
                        data = conn.recv(4096)  # Increased buffer size
                        if not data:
                            print(f"Client {addr} disconnected (no data).")
                            break
                        
                        client_message = data.decode('utf-8')
                        
                        # Process the message
                        response_payload = process_client_request(client_message)
                        
                        # Send the response back to the client
                        try:
                            conn.sendall(json.dumps(response_payload, ensure_ascii=False).encode('utf-8'))
                        except socket.error as e:
                            print(f"Error sending data to {addr}: {e}")
                            break 
                    print(f"Connection with {addr} closed.")
            except KeyboardInterrupt:
                print("\\nServer shutting down...")
                break
            except Exception as e:
                print(f"An unexpected error occurred in the server loop: {e}")
                # Optionally, decide if the server should continue or exit
                # For robustness, it might try to continue accepting new connections

if __name__ == "__main__":
    # Ensure the current working directory allows imports from sibling modules
    # This might be needed if running the script directly from a different location
    # or if the project structure isn't automatically on PYTHONPATH
    current_dir = os.path.dirname(os.path.abspath(__file__))
    if current_dir not in sys.path:
        sys.path.insert(0, current_dir)
    
    # If your modules (message_categorizer, information_extractor) are in the parent
    # directory or a specific 'src' directory, adjust sys.path accordingly.
    # For example, if they are in the parent directory:
    # project_root = os.path.dirname(current_dir)
    # if project_root not in sys.path:
    #     sys.path.insert(0, project_root)

    # Import necessary modules after path adjustment if needed
    import os # Re-import if used in __main__ after path adjustments
    from message_categorizer import categorize_prompt
    from information_extractor import (
        extract_show_info,
        extract_booking_info,
        extract_cancellation_info,
        extract_discount_info,
        extract_review_info
    )
    start_server()
