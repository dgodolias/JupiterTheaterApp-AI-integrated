import socket
import json
import sys
import os
import random
import signal  # Import signal module for handling Ctrl+C and other signals
from message_categorizer import categorize_prompt
from information_extractor import (
    extract_show_info,
    extract_booking_info,
    extract_cancellation_info,
    extract_discount_info,
    extract_review_info
)

# Set to True to use random category responses instead of the LLM
# This saves API calls/resources when testing
DUMMY_RESPONSES = True

def get_dummy_category():
    """Returns a random category from the predefined list to save LLM API calls."""
    valid_categories = [
        "ΚΡΑΤΗΣΗ", "ΑΚΥΡΩΣΗ", "ΠΛΗΡΟΦΟΡΙΕΣ", "ΑΞΙΟΛΟΓΗΣΕΙΣ & ΣΧΟΛΙΑ", 
        "ΠΡΟΣΦΟΡΕΣ & ΕΚΠΤΩΣΕΙΣ", "ΕΞΟΔΟΣ"
    ]
    choice = random.choice(valid_categories)
    choice = "ΠΛΗΡΟΦΟΡΙΕΣ"
    return choice

def process_client_request(client_data):
    """
    Processes the client's JSON request and returns a structured response.
    Expected JSON format: {"type": "CATEGORISE|EXTRACT", "category": "", "message": "..."}
    """
    try:
        # Try to parse the client message as JSON
        try:
            request = json.loads(client_data)
            print(f"Received JSON request: {request}")
            
            # Validate JSON structure
            if not isinstance(request, dict):
                raise ValueError("Request must be a JSON object")
                
            if "type" not in request:
                raise ValueError("Request must contain 'type' field")
                
            if "message" not in request:
                raise ValueError("Request must contain 'message' field")
                
            request_type = request.get("type")
            request_category = request.get("category", "")
            request_message = request.get("message", "")
            
            if not request_message:
                raise ValueError("Message field cannot be empty")
                
        except json.JSONDecodeError:
            # Legacy support for plain text messages (optional, can be removed)
            print(f"Received plain text message (legacy): {client_data}")
            request_type = "CATEGORISE"
            request_category = ""
            request_message = client_data
        
        # Process request based on type
        if request_type == "CATEGORISE":
            print(f"Processing CATEGORISE request: {request_message}")
            
            # Use dummy responses if the flag is enabled
            if DUMMY_RESPONSES:
                category = get_dummy_category()
                print(f"Using DUMMY response. Categorized as: {category}")
                response_data = {"category": category, "details": None, "error": None}
            else:
                # Call the categorization function
                category = categorize_prompt(request_message)
                print(f"Categorized as: {category}")
                response_data = {"category": category, "details": None, "error": None}
                
        elif request_type == "EXTRACT":
            print(f"Processing EXTRACT request for category '{request_category}': {request_message}")
            
            if not request_category:
                raise ValueError("Category field is required for EXTRACT requests")
                
            # Use dummy responses if the flag is enabled
            if DUMMY_RESPONSES:
                dummy_data = None
                
                if request_category == "ΠΛΗΡΟΦΟΡΙΕΣ":
                    dummy_data = {
                        "name": {"value": ["A Midsummer Night's Dream"], "pvalues": []},
                        "day": {"value": ["Friday", "Saturday", "Sunday"], "pvalues": ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"]},
                        "topic": {"value": ["Comedy", "Fantasy"], "pvalues": []},
                        "time": {"value": ["20:00", "15:00"], "pvalues": []},
                        "cast": {"value": ["George Dimitriou", "Elena Papadaki", "Nikos Ioannou"], "pvalues": []},
                        "room": {"value": ["Grand Hall"], "pvalues": []},
                        "duration": {"value": ["120 minutes"], "pvalues": []},
                        "stars": {"value": [4], "pvalues": [1, 2, 3, 4, 5, ">3", "<4"]}
                    }
                    print(f"Using DUMMY show info: {dummy_data}")
                elif request_category == "ΚΡΑΤΗΣΗ":
                    dummy_data = {
                        "show_name": {"value": "Romeo and Juliet", "pvalues": []},
                        "room": {"value": "Main Theater", "pvalues": []},
                        "day": {"value": "Saturday", "pvalues": ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"]},
                        "time": {"value": "19:30", "pvalues": []},
                        "person": {
                            "name": {"value": "Maria Papadopoulos", "pvalues": []},
                            "age": {"value": "grownup > 18", "pvalues": ["child < 18", "grownup > 18", "granny > 65"]},
                            "seat": {"value": "B12", "pvalues": []}
                        }
                    }
                    print(f"Using DUMMY booking info: {dummy_data}")
                elif request_category == "ΑΚΥΡΩΣΗ":
                    dummy_data = {
                        "reservation_number": {"value": "RES78901", "pvalues": []},
                        "passcode": {"value": "JUPITER2025", "pvalues": []}
                    }
                    print(f"Using DUMMY cancellation info: {dummy_data}")
                elif request_category == "ΠΡΟΣΦΟΡΕΣ & ΕΚΠΤΩΣΕΙΣ":
                    dummy_data = {
                        "show_name": {"value": ["Hamlet", "Macbeth"], "pvalues": []},
                        "no_of_people": {"value": 3, "pvalues": []},
                        "age": {"value": ["child < 18", "granny > 65"], "pvalues": ["child < 18", "grownup > 18", "granny > 65"]},
                        "date": {"value": ["2025-05-20", "2025-05-21"], "pvalues": []}
                    }
                    print(f"Using DUMMY discount info: {dummy_data}")
                elif request_category == "ΑΞΙΟΛΟΓΗΣΕΙΣ & ΣΧΟΛΙΑ":
                    dummy_data = {
                        "reservation_number": {"value": "DUMMY123", "pvalues": []},
                        "passcode": {"value": "12345", "pvalues": []},
                        "stars": {"value": 5, "pvalues": [1, 2, 3, 4, 5]},
                        "review": {"value": "This is a dummy review for testing.", "pvalues": []}
                    }
                    print(f"Using DUMMY review info: {dummy_data}")
                else:
                    raise ValueError(f"Unsupported category: {request_category}")
                
                response_data = {"category": request_category, "details": dummy_data, "error": None}
            else:
                # Direct extraction based on provided category
                details = None
                
                if request_category == "ΠΛΗΡΟΦΟΡΙΕΣ":
                    details = extract_show_info(request_message)
                    print(f"Extracted show info: {details}")
                elif request_category == "ΚΡΑΤΗΣΗ":
                    details = extract_booking_info(request_message)
                    print(f"Extracted booking(s): {details}")
                elif request_category == "ΑΚΥΡΩΣΗ":
                    details = extract_cancellation_info(request_message)
                    print(f"Extracted cancellation info: {details}")
                elif request_category == "ΠΡΟΣΦΟΡΕΣ & ΕΚΠΤΩΣΕΙΣ":
                    details = extract_discount_info(request_message)
                    print(f"Extracted discount info: {details}")
                elif request_category == "ΑΞΙΟΛΟΓΗΣΕΙΣ & ΣΧΟΛΙΑ":
                    details = extract_review_info(request_message)
                    print(f"Extracted review info: {details}")
                elif request_category == "ΕΞΟΔΟΣ":
                    details = "Client requested to close connection."
                else:
                    raise ValueError(f"Unsupported category: {request_category}")
                
                response_data = {"category": request_category, "details": details, "error": None}
        else:
            raise ValueError(f"Unsupported request type: {request_type}. Must be 'CATEGORISE' or 'EXTRACT'")
            
    except Exception as e:
        print(f"Error processing request: {e}")
        response_data = {"category": None, "details": None, "error": str(e)}
    
    return response_data

def get_local_ip():
    """Get the local IP address of this machine."""
    try:
        # Create a socket to determine the IP address this machine uses to connect to the outside world
        s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        # You don't actually need to send data - just start a connection
        s.connect(("8.8.8.8", 80))
        local_ip = s.getsockname()[0]
        s.close()
        return local_ip
    except Exception as e:
        print(f"Error getting local IP: {e}")
        return "127.0.0.1"  # Fallback to localhost

def start_server(host=None, port=65432):
    """Starts the TCP server to listen for client connections."""
    if host is None:
        host = get_local_ip()
    
    # Create server socket outside the with block so we can reference it in signal handlers
    server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    server_socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    
    # Flag to track server state
    server_running = True
    
    # Define signal handler function
    def signal_handler(sig, frame):
        nonlocal server_running
        print(f"\nReceived interrupt signal {sig}. Shutting down server...")
        server_running = False
        # Close the server socket
        try:
            server_socket.shutdown(socket.SHUT_RDWR)
        except OSError:
            # Socket might already be closed
            pass
        server_socket.close()
        print("Server socket closed.")
        sys.exit(0)
    
    # Register signal handlers
    signal.signal(signal.SIGINT, signal_handler)  # Handle Ctrl+C
    signal.signal(signal.SIGTERM, signal_handler)  # Handle termination signal
    
    try:
        server_socket.bind((host, port))
    except socket.error as e:
        print(f"Error binding server to {host}:{port} - {e}")
        sys.exit(1)
        
    server_socket.listen()
    print(f"Jupiter Theater Server listening on {host}:{port}")
    print("Press Ctrl+C to stop the server")
    
    while server_running:
        try:
            # Set a timeout so the server can check for the running flag
            server_socket.settimeout(1.0)
            try:
                conn, addr = server_socket.accept()
                # Reset timeout for normal operation
                server_socket.settimeout(None)
                
                with conn:
                    print(f"Connected by {addr}")
                    while server_running:
                        print(f"Waiting for message from {addr}...")
                        data = conn.recv(4096)  # Increased buffer size
                        if not data:
                            print(f"Client {addr} disconnected (no data).")
                            break
                        client_data = data.decode('utf-8').strip()
                        
                        # Process the client data (now expecting JSON format)
                        response_payload = process_client_request(client_data)
                        
                        # Send the response back to the client
                        try:
                            response_json = json.dumps(response_payload, ensure_ascii=False)
                            conn.sendall(response_json.encode('utf-8') + b'\n')  # Add newline for easier client parsing
                            print(f"Response sent to {addr}. Waiting for next message...")
                        except socket.error as e:
                            print(f"Error sending data to {addr}: {e}")
                            break
                    print(f"Connection with {addr} closed.")
            except socket.timeout:
                # This is expected due to the timeout we set
                continue
        except KeyboardInterrupt:
            print("\nServer shutting down from KeyboardInterrupt...")
            server_running = False
            break
        except Exception as e:
            print(f"An unexpected error occurred in the server loop: {e}")
            # Continue running to accept new connections unless server_running was changed
    
    # Ensure socket is closed before exiting
    try:
        server_socket.close()
        print("Server socket closed.")
    except Exception as e:
        print(f"Error closing server socket: {e}")

if __name__ == "__main__":
    # Ensure the current working directory allows imports from sibling modules
    current_dir = os.path.dirname(os.path.abspath(__file__))
    if (current_dir not in sys.path):
        sys.path.insert(0, current_dir)
    
    # Option to specify port via command line
    port = 65432  # Default port
    if len(sys.argv) > 1:
        try:
            port = int(sys.argv[1])
        except ValueError:
            print(f"Invalid port number: {sys.argv[1]}. Using default: {port}")
    
    # Start server on the automatically detected IP address
    start_server(port=port)
