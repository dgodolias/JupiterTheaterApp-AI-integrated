import json
import os  # Add os import
from llm_utils import send_message_to_llm, AVAILABLE_MODELS

# Helper function to load prompts from files
def load_prompt(filename):
    with open(os.path.join("prompts", filename), "r", encoding="utf-8") as f:
        return f.read()

# Helper function to load JSON templates from files
def load_json_template(filename):
    with open(os.path.join("json_templates", filename), "r", encoding="utf-8") as f:
        return json.load(f)

def extract_show_info(user_message):
    """
    Extracts show information from user message for filtering purposes.
    
    Args:
        user_message (str): The user's message
        
    Returns:
        dict: Complete show information dictionary with all fields
    """
    # Define template with all fields as arrays (empty by default)
    template = load_json_template("show_info.txt")
    
    system_prompt = load_prompt("show_info.txt")
    
    result = send_message_to_llm(
        user_message=user_message,
        system_message=system_prompt,
        model=AVAILABLE_MODELS["primary"],
        max_tokens=300
    )
    
    extracted_info = {}
    
    try:
        # Try to parse the response as JSON
        if result and result.strip():
            # Find any JSON-like structure in the response
            start_idx = result.find('{')
            end_idx = result.rfind('}')
            
            if start_idx != -1 and end_idx != -1 and end_idx > start_idx:
                json_str = result[start_idx:end_idx+1]
                extracted_info = json.loads(json_str)
    except json.JSONDecodeError:
        print("Failed to parse JSON from LLM response")
    
    # If extraction failed completely, try one more time with a simpler prompt
    if not extracted_info:
        system_prompt = load_prompt("show_info_fallback.txt")
        
        result = send_message_to_llm(
            user_message=user_message,
            system_message=system_prompt,
            model=AVAILABLE_MODELS["fallback"],
            max_tokens=150
        )
        
        try:
            # Try to parse again
            if result and result.strip():
                start_idx = result.find('{')
                end_idx = result.rfind('}')
                
                if start_idx != -1 and end_idx != -1 and end_idx > start_idx:
                    json_str = result[start_idx:end_idx+1]
                    extracted_info = json.loads(json_str)
        except json.JSONDecodeError:
            print("Failed to extract even with simplified prompt")
            
            # Basic pattern matching without hardcoding specific names
            # Only for critical weekend and time patterns
            if "σαββατοκυριακο" in user_message.lower() or "σαββατοκύριακο" in user_message.lower():
                extracted_info = {"day": ["Saturday", "Sunday"]}
                
                # Handle time constraints with basic pattern matching
                if "μετα" in user_message.lower() or "μετά" in user_message.lower():
                    if "7" in user_message or "19" in user_message or "επτα" in user_message.lower() or "επτά" in user_message.lower():
                        extracted_info["time"] = [">19:00"]
                    elif "8" in user_message or "20" in user_message or "οκτω" in user_message.lower() or "οκτώ" in user_message.lower():
                        extracted_info["time"] = [">20:00"]
    
    # Merge extracted info with template to ensure all fields are present
    for key, value in extracted_info.items():
        if key in template:
            # Convert single values to arrays if they're not already
            if not isinstance(value, list):
                template[key] = [value]
            else:
                template[key] = value
    
    return template

def extract_booking_info(user_message):
    """
    Extracts booking information from user message including show details and attendees.
    
    Args:
        user_message (str): The user's booking request message
        
    Returns:
        dict: Structured booking information
    """
    # Define booking template with default empty values
    booking_template = load_json_template("booking.txt")
    
    system_prompt = load_prompt("booking.txt")
    
    result = send_message_to_llm(
        user_message=user_message,
        system_message=system_prompt,
        model=AVAILABLE_MODELS["primary"],
        max_tokens=500  # Larger max_tokens for booking details
    )
    
    extracted_info = {}
    
    try:
        # Try to parse the response as JSON
        if result and result.strip():
            # Find any JSON-like structure in the response
            start_idx = result.find('{')
            end_idx = result.rfind('}')
            
            if start_idx != -1 and end_idx != -1 and end_idx > start_idx:
                json_str = result[start_idx:end_idx+1]
                extracted_info = json.loads(json_str)
    except json.JSONDecodeError:
        print("Failed to parse booking JSON from LLM response")
    
    # If extraction failed, try with a simplified prompt
    if not extracted_info:
        system_prompt = load_prompt("booking_fallback.txt")
        
        result = send_message_to_llm(
            user_message=user_message,
            system_message=system_prompt,
            model=AVAILABLE_MODELS["fallback"],
            max_tokens=300
        )
        
        try:
            # Try to parse again
            if result and result.strip():
                start_idx = result.find('{')
                end_idx = result.rfind('}')
                
                if start_idx != -1 and end_idx != -1 and end_idx > start_idx:
                    json_str = result[start_idx:end_idx+1]
                    extracted_info = json.loads(json_str)
        except json.JSONDecodeError:
            print("Failed to extract booking info even with simplified prompt")
            # Initialize with minimal info to prevent errors
            extracted_info = load_json_template("booking.txt")  # Fallback to empty template
            # Ensure person1 is present for basic fallback if template is complex
            if "person1" not in extracted_info:
                extracted_info["person1"] = {"name1": "", "age1": "", "seat1": ""}
    
    # Merge extracted info with template to ensure all fields are present
    # First handle the top-level fields
    for key in ["show_name", "room", "day", "time"]:
        if key in extracted_info:
            booking_template[key] = extracted_info[key]
    
    # Then handle the person fields
    for i in range(1, 11):
        person_key = f"person{i}"
        if person_key in extracted_info:
            # Get all subfields that exist in the extracted data
            for subkey in [f"name{i}", f"age{i}", f"seat{i}"]:
                if subkey in extracted_info[person_key]:
                    booking_template[person_key][subkey] = extracted_info[person_key][subkey]
    
    return booking_template

def extract_cancellation_info(user_message):
    """
    Extracts cancellation information from user message.
    
    Args:
        user_message (str): The user's cancellation request message
        
    Returns:
        dict: Structured cancellation information with reservation number and passcode
    """
    # Define cancellation template with default empty values
    cancellation_template = load_json_template("cancellation.txt")
    
    system_prompt = load_prompt("cancellation.txt")
    
    result = send_message_to_llm(
        user_message=user_message,
        system_message=system_prompt,
        model=AVAILABLE_MODELS["primary"],
        max_tokens=100  # Short response for simple extraction
    )
    
    extracted_info = {}
    
    try:
        # Try to parse the response as JSON
        if result and result.strip():
            # Find any JSON-like structure in the response
            start_idx = result.find('{')
            end_idx = result.rfind('}')
            
            if start_idx != -1 and end_idx != -1 and end_idx > start_idx:
                json_str = result[start_idx:end_idx+1]
                extracted_info = json.loads(json_str)
    except json.JSONDecodeError:
        print("Failed to parse cancellation JSON from LLM response")
    
    # If extraction failed, try with a simplified prompt
    if not extracted_info:
        system_prompt = load_prompt("cancellation_fallback.txt")
        
        result = send_message_to_llm(
            user_message=user_message,
            system_message=system_prompt,
            model=AVAILABLE_MODELS["fallback"],
            max_tokens=100
        )
        
        try:
            # Try to parse again
            if result and result.strip():
                start_idx = result.find('{')
                end_idx = result.rfind('}')
                
                if start_idx != -1 and end_idx != -1 and end_idx > start_idx:
                    json_str = result[start_idx:end_idx+1]
                    extracted_info = json.loads(json_str)
        except json.JSONDecodeError:
            print("Failed to extract cancellation info even with simplified prompt")
            # Initialize with empty values to prevent errors
            extracted_info = load_json_template("cancellation.txt")  # Fallback to empty template
    
    # Merge extracted info with template to ensure all fields are present
    for key in ["reservation_number", "passcode"]:
        if key in extracted_info:
            cancellation_template[key] = extracted_info[key]
    
    return cancellation_template

def extract_discount_info(user_message):
    """
    Extracts discount/promotion information from user message.
    
    Args:
        user_message (str): The user's discount/promotion request message
        
    Returns:
        dict: Structured discount information with show names, number of people, ages, and dates
    """
    # Define discount template with default empty values
    discount_template = load_json_template("discount.txt")
    
    system_prompt = load_prompt("discount.txt")
    
    result = send_message_to_llm(
        user_message=user_message,
        system_message=system_prompt,
        model=AVAILABLE_MODELS["primary"],
        max_tokens=200
    )
    
    extracted_info = {}
    
    try:
        # Try to parse the response as JSON
        if result and result.strip():
            # Find any JSON-like structure in the response
            start_idx = result.find('{')
            end_idx = result.rfind('}')
            
            if start_idx != -1 and end_idx != -1 and end_idx > start_idx:
                json_str = result[start_idx:end_idx+1]
                extracted_info = json.loads(json_str)
    except json.JSONDecodeError:
        print("Failed to parse discount JSON from LLM response")
    
    # If extraction failed, try with a simplified prompt
    if not extracted_info:
        system_prompt = load_prompt("discount_fallback.txt")
        
        result = send_message_to_llm(
            user_message=user_message,
            system_message=system_prompt,
            model=AVAILABLE_MODELS["fallback"],
            max_tokens=150
        )
        
        try:
            # Try to parse again
            if result and result.strip():
                start_idx = result.find('{')
                end_idx = result.rfind('}')
                
                if start_idx != -1 and end_idx != -1 and end_idx > start_idx:
                    json_str = result[start_idx:end_idx+1]
                    extracted_info = json.loads(json_str)
        except json.JSONDecodeError:
            print("Failed to extract discount info even with simplified prompt")
            # Initialize with minimal info to prevent errors
            extracted_info = load_json_template("discount.txt")  # Fallback to empty template
    
    # Merge extracted info with template to ensure all fields are present
    for key in ["show_name", "age", "date"]:
        if key in extracted_info:
            # Ensure these fields are arrays
            if not isinstance(extracted_info[key], list):
                discount_template[key] = [extracted_info[key]]
            else:
                discount_template[key] = extracted_info[key]
    
    # Handle no_of_people, ensuring it's a number
    if "no_of_people" in extracted_info:
        try:
            discount_template["no_of_people"] = int(extracted_info["no_of_people"])
        except (ValueError, TypeError):
            # If conversion fails, try to extract a number from the value
            if isinstance(extracted_info["no_of_people"], str) and any(c.isdigit() for c in extracted_info["no_of_people"]):
                # Extract digits and convert to int
                digits = ''.join(c for c in extracted_info["no_of_people"] if c.isdigit())
                if digits:
                    discount_template["no_of_people"] = int(digits)
    
    return discount_template

def extract_review_info(user_message):
    """
    Extracts review and rating information from user message.
    
    Args:
        user_message (str): The user's review/comment message
        
    Returns:
        dict: Structured review information with reservation number, passcode, stars, and review text
    """
    # Define review template with default empty values
    review_template = load_json_template("review.txt")
    
    system_prompt = load_prompt("review.txt")
    
    result = send_message_to_llm(
        user_message=user_message,
        system_message=system_prompt,
        model=AVAILABLE_MODELS["primary"],
        max_tokens=300  # Higher token count for reviews
    )
    
    extracted_info = {}
    
    try:
        # Try to parse the response as JSON
        if result and result.strip():
            # Find any JSON-like structure in the response
            start_idx = result.find('{')
            end_idx = result.rfind('}')
            
            if start_idx != -1 and end_idx != -1 and end_idx > start_idx:
                json_str = result[start_idx:end_idx+1]
                extracted_info = json.loads(json_str)
    except json.JSONDecodeError:
        print("Failed to parse review JSON from LLM response")
    
    # If extraction failed, try with a simplified prompt
    if not extracted_info:
        system_prompt = load_prompt("review_fallback.txt")
        
        result = send_message_to_llm(
            user_message=user_message,
            system_message=system_prompt,
            model=AVAILABLE_MODELS["fallback"],
            max_tokens=200
        )
        
        try:
            # Try to parse again
            if result and result.strip():
                start_idx = result.find('{')
                end_idx = result.rfind('}')
                
                if start_idx != -1 and end_idx != -1 and end_idx > start_idx:
                    json_str = result[start_idx:end_idx+1]
                    extracted_info = json.loads(json_str)
        except json.JSONDecodeError:
            print("Failed to extract review info even with simplified prompt")
            # Initialize with default values to prevent errors
            extracted_info = load_json_template("review.txt")  # Fallback to empty template
    
    # Merge extracted info with template to ensure all fields are present
    for key in ["reservation_number", "passcode", "review"]:
        if key in extracted_info:
            review_template[key] = extracted_info[key]
    
    # Handle stars field, ensuring it's a number
    if "stars" in extracted_info:
        try:
            # Convert to integer if possible
            review_template["stars"] = int(extracted_info["stars"])
        except (ValueError, TypeError):
            # Try to extract digits if it's a string with non-numeric characters
            if isinstance(extracted_info["stars"], str):
                digits = ''.join(c for c in extracted_info["stars"] if c.isdigit())
                if digits:
                    # Use only the first digit if multiple were found
                    review_template["stars"] = int(digits[0])
    
    return review_template
