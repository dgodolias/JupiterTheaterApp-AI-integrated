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

# Set to True to use full dummy data, False for partial dummy data
DUMMY_FULL = True

# Set to True to enable alternating DUMMY_FULL state for EXTRACT requests
DUAL = False # Or False to disable
DUAL_STATE_FULL = True  # Initial state for DUAL mode (True for full, False for nonfull)

def get_dummy_category():
    """Returns a random category from the predefined list to save LLM API calls."""
    valid_categories = [
        "ΚΡΑΤΗΣΗ", "ΑΚΥΡΩΣΗ", "ΠΛΗΡΟΦΟΡΙΕΣ", "ΑΞΙΟΛΟΓΗΣΕΙΣ & ΣΧΟΛΙΑ", 
        "ΠΡΟΣΦΟΡΕΣ & ΕΚΠΤΩΣΕΙΣ"
    ]
    choice = random.choice(valid_categories)
    choice = "ΚΡΑΤΗΣΗ" 
    return choice

def process_client_request(client_data):
    """
    Processes the client's JSON request and returns a structured response.
    Expected JSON format: {"type": "CATEGORISE|EXTRACT", "category": "", "message": "..."
    """
    global DUAL_STATE_FULL # Allow modification of the global variable

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
            
            if DUMMY_RESPONSES:
                dummy_data = None
                
                # Determine which folder to use
                current_dummy_full_state = DUMMY_FULL # Default to global DUMMY_FULL
                
                if DUAL:
                    current_dummy_full_state = DUAL_STATE_FULL
                    print(f"DUAL mode active. Using DUMMY_FULL = {current_dummy_full_state}")
                    DUAL_STATE_FULL = not DUAL_STATE_FULL # Toggle for the next DUAL request
                
                folder_path = "full" if current_dummy_full_state else "nonfull"
                base_path = os.path.join(os.path.dirname(os.path.abspath(__file__)), "fake_data", folder_path)
                
                # Map categories to file names
                file_mapping = {
                    "ΠΛΗΡΟΦΟΡΙΕΣ": "show_info.json",
                    "ΚΡΑΤΗΣΗ": "booking.json",
                    "ΑΚΥΡΩΣΗ": "cancellation.json",
                    "ΠΡΟΣΦΟΡΕΣ & ΕΚΠΤΩΣΕΙΣ": "discount.json",
                    "ΑΞΙΟΛΟΓΗΣΕΙΣ & ΣΧΟΛΙΑ": "review.json"
                }
                
                if request_category in file_mapping:
                    json_file = os.path.join(base_path, file_mapping[request_category])
                    try:
                        with open(json_file, 'r', encoding='utf-8') as f:
                            dummy_data = json.load(f)
                        data_completeness = "full" if current_dummy_full_state else "partial"
                        print(f"Using {data_completeness} DUMMY data for {request_category} from {json_file}:",dummy_data)
                    except Exception as e:
                        print(f"Error loading dummy data from {json_file}: {e}")
                        # Fallback to empty data structure if file cannot be loaded
                        dummy_data = {}
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
