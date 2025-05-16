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
    template = load_json_template("show_info.json")
    system_prompt = load_prompt("show_info.txt")
    
    result = send_message_to_llm(
        user_message=user_message,
        system_message=system_prompt,
        model=AVAILABLE_MODELS["primary"],
        max_tokens=300
    )
    
    extracted_info = {}
    try:
        if result and result.strip():
            start_idx = result.find('{')
            end_idx = result.rfind('}')
            if start_idx != -1 and end_idx != -1 and end_idx > start_idx:
                json_str = result[start_idx:end_idx+1]
                extracted_info = json.loads(json_str)
    except json.JSONDecodeError:
        print("Failed to parse JSON from LLM response for show_info")
    
    if not extracted_info:
        system_prompt_fallback = load_prompt("show_info_fallback.txt")
        result_fallback = send_message_to_llm(
            user_message=user_message,
            system_message=system_prompt_fallback,
            model=AVAILABLE_MODELS["fallback"],
            max_tokens=150
        )
        try:
            if result_fallback and result_fallback.strip():
                start_idx = result_fallback.find('{')
                end_idx = result_fallback.rfind('}')
                if start_idx != -1 and end_idx != -1 and end_idx > start_idx:
                    json_str_fallback = result_fallback[start_idx:end_idx+1]
                    extracted_info = json.loads(json_str_fallback)
        except json.JSONDecodeError:
            print("Failed to extract show_info even with simplified prompt")
            if "σαββατοκυριακο" in user_message.lower() or "σαββατοκύριακο" in user_message.lower():
                extracted_info["day"] = ["Saturday", "Sunday"]
                if "μετα" in user_message.lower() or "μετά" in user_message.lower():
                    if "7" in user_message or "19" in user_message or "επτα" in user_message.lower() or "επτά" in user_message.lower():
                        extracted_info["time"] = [">19:00"]
                    elif "8" in user_message or "20" in user_message or "οκτω" in user_message.lower() or "οκτώ" in user_message.lower():
                        extracted_info["time"] = [">20:00"]
    
    # Merge extracted info with template
    for key, extracted_value in extracted_info.items():
        if key in template:
            if isinstance(template[key]["value"], list):
                if not isinstance(extracted_value, list):
                    template[key]["value"] = [extracted_value]
                else:
                    template[key]["value"] = extracted_value
            else:
                template[key]["value"] = extracted_value
    
    return template

def extract_booking_info(user_message):
    """
    Extracts booking information from user message including show details and attendees.
    
    Args:
        user_message (str): The user's booking request message
        
    Returns:
        list: A list of structured booking information dictionaries, one for each person.
    """
    # Define booking template with default empty values
    booking_template_for_person = load_json_template("booking.json") # This is a single person template
    
    system_prompt = load_prompt("booking.txt")
    
    result = send_message_to_llm(
        user_message=user_message,
        system_message=system_prompt,
        model=AVAILABLE_MODELS["primary"],
        max_tokens=1000  # Increased max_tokens for potentially multiple JSON objects
    )
    
    extracted_bookings = []
    
    try:
        # Try to parse the response as JSON
        # The LLM might return a single JSON object or an array of JSON objects
        if result and result.strip():
            # Attempt to find the start and end of the JSON content
            json_start_index = -1
            json_end_index = -1

            if result.strip().startswith('['):
                json_start_index = result.find('[')
                json_end_index = result.rfind(']')
            elif result.strip().startswith('{'):
                json_start_index = result.find('{')
                json_end_index = result.rfind('}')
            
            if json_start_index != -1 and json_end_index != -1 and json_end_index > json_start_index:
                json_str = result[json_start_index : json_end_index + 1]
                # Check if the extracted string is an array or object
                if json_str.strip().startswith('['):
                    parsed_result = json.loads(json_str)
                    if isinstance(parsed_result, list):
                        extracted_bookings.extend(parsed_result)
                    else:
                        print(f"Parsed result from primary LLM is a list, but not in the expected format. Content: {json_str}")
                elif json_str.strip().startswith('{'):
                    parsed_object = json.loads(json_str)
                    extracted_bookings.append(parsed_object) # Add as a single item list if it's one booking
                else:
                    print(f"LLM response (primary) after stripping non-JSON content is not a valid JSON object or array. Content: {json_str}")
            else:
                 print(f"Could not find valid JSON structure in primary LLM response. Raw response: {result}")

    except json.JSONDecodeError as e:
        print(f"Failed to parse booking JSON from primary LLM response: {e}. Raw response: {result}")

    # If primary extraction failed or didn't yield results, try with a simplified prompt
    if not extracted_bookings:
        system_prompt_fallback = load_prompt("booking_fallback.txt") # Ensure this prompt asks for the new structure
        
        result_fallback = send_message_to_llm(
            user_message=user_message,
            system_message=system_prompt_fallback,
            model=AVAILABLE_MODELS["fallback"],
            max_tokens=600 # Adjusted for fallback
        )
        
        try:
            if result_fallback and result_fallback.strip():
                json_start_index_fb = -1
                json_end_index_fb = -1

                if result_fallback.strip().startswith('['):
                    json_start_index_fb = result_fallback.find('[')
                    json_end_index_fb = result_fallback.rfind(']')
                elif result_fallback.strip().startswith('{'):
                    json_start_index_fb = result_fallback.find('{')
                    json_end_index_fb = result_fallback.rfind('}')

                if json_start_index_fb != -1 and json_end_index_fb != -1 and json_end_index_fb > json_start_index_fb:
                    json_str_fb = result_fallback[json_start_index_fb : json_end_index_fb + 1]
                    if json_str_fb.strip().startswith('['):
                        parsed_fallback = json.loads(json_str_fb)
                        if isinstance(parsed_fallback, list):
                            extracted_bookings.extend(parsed_fallback)
                    elif json_str_fb.strip().startswith('{'):
                        parsed_fallback_object = json.loads(json_str_fb)
                        extracted_bookings.append(parsed_fallback_object)
                    else:
                        print(f"Fallback LLM response after stripping non-JSON content is not a valid JSON object or array. Content: {json_str_fb}")
                else:
                    print(f"Could not find valid JSON structure in fallback LLM response. Raw response: {result_fallback}")
        except json.JSONDecodeError as e:
            print(f"Failed to extract booking info even with simplified prompt: {e}. Raw response: {result_fallback}")

    # Validate and structure each booking in the list
    final_bookings = []
    if not extracted_bookings: # If still no bookings (e.g. LLM returned empty or unparsable)
        # Add one empty template structure to signify failure but maintain type consistency
        # final_bookings.append(booking_template_for_person)
        # Or, based on requirements, could return an empty list:
        return []


    for booking_data in extracted_bookings:
        # Create a fresh template for each booking to ensure no data leakage between them
        current_booking_filled = json.loads(json.dumps(booking_template_for_person)) # Deep copy

        # Merge top-level fields
        for key in ["show_name", "room", "day", "time"]:
            if key in booking_data and key in current_booking_filled: # Check if key exists in template
                current_booking_filled[key]["value"] = booking_data.get(key, current_booking_filled[key]["value"])

        # Merge person sub-fields
        if "person" in booking_data and "person" in current_booking_filled: # Check if person key exists
            person_data = booking_data["person"]
            template_person_fields = current_booking_filled["person"]
            for sub_key in template_person_fields.keys(): # name, age, seat
                # The value from LLM is directly under person_data[sub_key], not person_data[sub_key]['value']
                # The template has { "value": "", "pvalues": [] }
                if sub_key in person_data:
                     # Ensure that the value being assigned is not None or an empty dict if that's not desired
                    actual_value = person_data.get(sub_key)
                    if actual_value is not None : # and actual_value != {}: # Add more specific checks if needed
                        current_booking_filled["person"][sub_key]["value"] = actual_value
                    # else:
                        # current_booking_filled["person"][sub_key]["value"] remains default from template
        
        final_bookings.append(current_booking_filled)
        
    return final_bookings

def extract_cancellation_info(user_message):
    """
    Extracts cancellation information from user message.
    
    Args:
        user_message (str): The user's cancellation request message
        
    Returns:
        dict: Structured cancellation information with reservation number and passcode
    """
    cancellation_template = load_json_template("cancellation.json")
    system_prompt = load_prompt("cancellation.txt")
    
    result = send_message_to_llm(
        user_message=user_message,
        system_message=system_prompt,
        model=AVAILABLE_MODELS["primary"],
        max_tokens=100
    )
    
    extracted_info = {}
    try:
        if result and result.strip():
            start_idx = result.find('{')
            end_idx = result.rfind('}')
            if start_idx != -1 and end_idx != -1 and end_idx > start_idx:
                json_str = result[start_idx:end_idx+1]
                extracted_info = json.loads(json_str)
    except json.JSONDecodeError:
        print("Failed to parse cancellation JSON from LLM response")
    
    if not extracted_info:
        system_prompt_fallback = load_prompt("cancellation_fallback.txt")
        result_fallback = send_message_to_llm(
            user_message=user_message,
            system_message=system_prompt_fallback,
            model=AVAILABLE_MODELS["fallback"],
            max_tokens=100
        )
        try:
            if result_fallback and result_fallback.strip():
                start_idx = result_fallback.find('{')
                end_idx = result_fallback.rfind('}')
                if start_idx != -1 and end_idx != -1 and end_idx > start_idx:
                    json_str_fallback = result_fallback[start_idx:end_idx+1]
                    extracted_info = json.loads(json_str_fallback)
        except json.JSONDecodeError:
            print("Failed to extract cancellation info even with simplified prompt")
            # extracted_info remains empty, template will be returned with default values
    
    # Merge extracted info with template
    for key in ["reservation_number", "passcode"]:
        if key in extracted_info and key in cancellation_template:
            cancellation_template[key]["value"] = extracted_info[key]
            
    return cancellation_template

def extract_discount_info(user_message):
    """
    Extracts discount/promotion information from user message.
    
    Args:
        user_message (str): The user's discount/promotion request message
        
    Returns:
        dict: Structured discount information
    """
    discount_template = load_json_template("discount.json")
    system_prompt = load_prompt("discount.txt")
    
    result = send_message_to_llm(
        user_message=user_message,
        system_message=system_prompt,
        model=AVAILABLE_MODELS["primary"],
        max_tokens=200
    )
    
    extracted_info = {}
    try:
        if result and result.strip():
            start_idx = result.find('{')
            end_idx = result.rfind('}')
            if start_idx != -1 and end_idx != -1 and end_idx > start_idx:
                json_str = result[start_idx:end_idx+1]
                extracted_info = json.loads(json_str)
    except json.JSONDecodeError:
        print("Failed to parse discount JSON from LLM response")
        
    if not extracted_info:
        system_prompt_fallback = load_prompt("discount_fallback.txt")
        result_fallback = send_message_to_llm(
            user_message=user_message,
            system_message=system_prompt_fallback,
            model=AVAILABLE_MODELS["fallback"],
            max_tokens=150
        )
        try:
            if result_fallback and result_fallback.strip():
                start_idx = result_fallback.find('{')
                end_idx = result_fallback.rfind('}')
                if start_idx != -1 and end_idx != -1 and end_idx > start_idx:
                    json_str_fallback = result_fallback[start_idx:end_idx+1]
                    extracted_info = json.loads(json_str_fallback)
        except json.JSONDecodeError:
            print("Failed to extract discount info even with simplified prompt")

    # Merge extracted info with template
    for key in ["show_name", "age", "date"]:
        if key in extracted_info and key in discount_template:
            extracted_value = extracted_info[key]
            if isinstance(discount_template[key]["value"], list):
                if not isinstance(extracted_value, list):
                    discount_template[key]["value"] = [extracted_value]
                else:
                    discount_template[key]["value"] = extracted_value
            else: #Should not happen for these keys as template value is list
                discount_template[key]["value"] = extracted_value
    
    if "no_of_people" in extracted_info and "no_of_people" in discount_template:
        try:
            discount_template["no_of_people"]["value"] = int(extracted_info["no_of_people"])
        except (ValueError, TypeError):
            if isinstance(extracted_info["no_of_people"], str) and any(c.isdigit() for c in extracted_info["no_of_people"]):
                digits = ''.join(c for c in extracted_info["no_of_people"] if c.isdigit())
                if digits:
                    discount_template["no_of_people"]["value"] = int(digits)
    
    return discount_template

def extract_review_info(user_message):
    """
    Extracts review and rating information from user message.
    
    Args:
        user_message (str): The user's review/comment message
        
    Returns:
        dict: Structured review information
    """
    review_template = load_json_template("review.json")
    system_prompt = load_prompt("review.txt")
    
    result = send_message_to_llm(
        user_message=user_message,
        system_message=system_prompt,
        model=AVAILABLE_MODELS["primary"],
        max_tokens=300
    )
    
    extracted_info = {}
    try:
        if result and result.strip():
            start_idx = result.find('{')
            end_idx = result.rfind('}')
            if start_idx != -1 and end_idx != -1 and end_idx > start_idx:
                json_str = result[start_idx:end_idx+1]
                extracted_info = json.loads(json_str)
    except json.JSONDecodeError:
        print("Failed to parse review JSON from LLM response")

    if not extracted_info:
        system_prompt_fallback = load_prompt("review_fallback.txt")
        result_fallback = send_message_to_llm(
            user_message=user_message,
            system_message=system_prompt_fallback,
            model=AVAILABLE_MODELS["fallback"],
            max_tokens=200
        )
        try:
            if result_fallback and result_fallback.strip():
                start_idx = result_fallback.find('{')
                end_idx = result_fallback.rfind('}')
                if start_idx != -1 and end_idx != -1 and end_idx > start_idx:
                    json_str_fallback = result_fallback[start_idx:end_idx+1]
                    extracted_info = json.loads(json_str_fallback)
        except json.JSONDecodeError:
            print("Failed to extract review info even with simplified prompt")

    # Merge extracted info with template
    for key in ["reservation_number", "passcode", "review"]:
        if key in extracted_info and key in review_template:
            review_template[key]["value"] = extracted_info[key]
            
    if "stars" in extracted_info and "stars" in review_template:
        try:
            review_template["stars"]["value"] = int(extracted_info["stars"])
        except (ValueError, TypeError):
            if isinstance(extracted_info["stars"], str):
                digits = ''.join(c for c in extracted_info["stars"] if c.isdigit())
                if digits:
                    review_template["stars"]["value"] = int(digits[0])
    
    return review_template
