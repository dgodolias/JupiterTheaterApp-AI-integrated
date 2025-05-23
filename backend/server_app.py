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

# Set to True to alternate between full and partial dummy data for consecutive requests
DUAL_MODE = False

# Counter to track requests for alternating between full and partial dummy data in DUAL_MODE
request_counter = 0

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
    Expected JSON format: {"type": "CATEGORISE|EXTRACT", "category": "", "message": "..."}
    """
    global request_counter
    
    # If DUAL_MODE is enabled, determine DUMMY_FULL value based on the request counter
    current_dummy_full = DUMMY_FULL
    if DUAL_MODE:
        current_dummy_full = (request_counter % 2 == 0)
        request_counter += 1
        print(f"DUAL MODE: Using {'full' if current_dummy_full else 'partial'} dummy data for this request")
    
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
                
                # Determine which folder to use based on current_dummy_full value (respects DUAL_MODE)
                folder_path = "full" if current_dummy_full else "nonfull"
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
                        data_completeness = "full" if current_dummy_full else "partial"
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
# ...existing code...