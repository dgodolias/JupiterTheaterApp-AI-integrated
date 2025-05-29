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

# Helper function to remove pvalues from template for LLM prompt
def remove_pvalues_from_template(template):
    """Remove pvalues from template to create a clean template for LLM"""
    clean_template = {}
    for key, value in template.items():
        if isinstance(value, dict):
            if "value" in value:
                # Top-level field with value/pvalues structure
                clean_template[key] = {"value": value["value"]}
            else:
                # Nested structure (like person object in booking)
                clean_template[key] = remove_pvalues_from_template(value)
        else:
            # Direct value (shouldn't happen in our templates but handle it)
            clean_template[key] = value
    return clean_template

def extract_show_info(user_message):
    """
    Εξάγει πληροφορίες παράστασης από το μήνυμα του χρήστη για φιλτράρισμα.
    
    Args:
        user_message (str): Το μήνυμα του χρήστη
        
    Returns:
        dict: Ένα πλήρες dictionary με πληροφορίες παράστασης
    """
    # Φόρτωση template και δημιουργία καθαρού template για το LLM
    show_template = load_json_template("show_info.json")
    clean_template = remove_pvalues_from_template(show_template)
    
    # Φόρτωση prompt - το prompt περιέχει ήδη το template και τις οδηγίες
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
        print("Failed to parse show_info JSON from primary LLM response")
    
    # Αν αποτύχει η κύρια εξαγωγή, δοκιμάζουμε με fallback
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
            return show_template  # Επιστρέφουμε το κενό template    # Συγχώνευση των εξαγόμενων πληροφοριών με το template
    if extracted_info:
        for key in ["name", "day", "topic", "time", "cast", "room", "duration", "stars"]:
            if key in extracted_info and key in show_template:
                extracted_value = extracted_info[key].get("value", "")
                if isinstance(show_template[key]["value"], list):
                    if isinstance(extracted_value, list):
                        show_template[key]["value"] = extracted_value
                    elif extracted_value:
                        show_template[key]["value"] = [extracted_value]
                else:
                    show_template[key]["value"] = extracted_value
                    
    return show_template

def extract_booking_info(user_message):
    """
    Εξάγει πληροφορίες κράτησης από το μήνυμα του χρήστη.
    
    Args:
        user_message (str): Το μήνυμα κράτησης του χρήστη
        
    Returns:
        dict: Ένα δομημένο dictionary με πληροφορίες κράτησης
    """
    # Φόρτωση template και δημιουργία καθαρού template για το LLM
    booking_template = load_json_template("booking.json")
    clean_template = remove_pvalues_from_template(booking_template)
    
    # Φόρτωση prompt - το prompt περιέχει ήδη το template και τις οδηγίες
    system_prompt = load_prompt("booking.txt")
    
    result = send_message_to_llm(
        user_message=user_message,
        system_message=system_prompt,
        model=AVAILABLE_MODELS["primary"],
        max_tokens=1000
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
        print("Failed to parse booking JSON from primary LLM response")
    
    # Αν αποτύχει η κύρια εξαγωγή, δοκιμάζουμε με fallback
    if not extracted_info:
        system_prompt_fallback = load_prompt("booking_fallback.txt")
        result_fallback = send_message_to_llm(
            user_message=user_message,
            system_message=system_prompt_fallback,
            model=AVAILABLE_MODELS["fallback"],
            max_tokens=600
        )
        try:
            if result_fallback and result_fallback.strip():
                start_idx = result_fallback.find('{')
                end_idx = result_fallback.rfind('}')
                if start_idx != -1 and end_idx != -1 and end_idx > start_idx:
                    json_str_fallback = result_fallback[start_idx:end_idx+1]
                    extracted_info = json.loads(json_str_fallback)
        except json.JSONDecodeError:
            print("Failed to extract booking info even with simplified prompt")
            return booking_template  # Επιστρέφουμε το κενό template

    # Συγχώνευση των εξαγόμενων πληροφοριών με το template
    if extracted_info:
        for key in ["show_name", "room", "day", "time"]:
            if key in extracted_info and key in booking_template:
                booking_template[key]["value"] = extracted_info[key].get("value", "")

        # Συγχώνευση person sub-fields
        if "person" in extracted_info and "person" in booking_template:
            person_data = extracted_info["person"]
            for sub_key in ["name", "age", "seat"]:
                if sub_key in person_data and sub_key in booking_template["person"]:
                    booking_template["person"][sub_key]["value"] = person_data[sub_key].get("value", "")
                    
    return booking_template

def extract_cancellation_info(user_message):
    """
    Εξάγει πληροφορίες ακύρωσης από το μήνυμα του χρήστη.
    
    Args:
        user_message (str): Το μήνυμα αίτησης ακύρωσης του χρήστη
        
    Returns:
        dict: Δομημένες πληροφορίες ακύρωσης με αριθμό κράτησης και κωδικό
    """
    # Φόρτωση template και δημιουργία καθαρού template για το LLM
    cancellation_template = load_json_template("cancellation.json")
    clean_template = remove_pvalues_from_template(cancellation_template)
    
    # Φόρτωση prompt - το prompt περιέχει ήδη το template και τις οδηγίες
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
        print("Failed to parse cancellation JSON from primary LLM response")

    # Αν αποτύχει η κύρια εξαγωγή, δοκιμάζουμε με fallback
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
            return cancellation_template  # Επιστρέφουμε το κενό template

    # Συγχώνευση των εξαγόμενων πληροφοριών με το template
    if extracted_info:
        for key in ["reservation_number", "passcode"]:
            if key in extracted_info and key in cancellation_template:
                cancellation_template[key]["value"] = extracted_info[key].get("value", "")
                
    return cancellation_template

def extract_discount_info(user_message):
    """
    Εξάγει πληροφορίες έκπτωσης/προσφοράς από το μήνυμα του χρήστη.
    
    Args:
        user_message (str): Το μήνυμα αίτησης έκπτωσης/προσφοράς του χρήστη
        
    Returns:
        dict: Δομημένες πληροφορίες έκπτωσης
    """
    # Φόρτωση template και δημιουργία καθαρού template για το LLM
    discount_template = load_json_template("discount.json")
    clean_template = remove_pvalues_from_template(discount_template)
    
    # Φόρτωση prompt - το prompt περιέχει ήδη το template και τις οδηγίες
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
        print("Failed to parse discount JSON from primary LLM response")
        
    # Αν αποτύχει η κύρια εξαγωγή, δοκιμάζουμε με fallback
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
            return discount_template  # Επιστρέφουμε το κενό template

    # Συγχώνευση των εξαγόμενων πληροφοριών με το template
    if extracted_info:
        for key in ["show_name", "age", "date"]:
            if key in extracted_info and key in discount_template:
                extracted_value = extracted_info[key].get("value", "")
                if isinstance(discount_template[key]["value"], list):
                    if isinstance(extracted_value, list):
                        discount_template[key]["value"] = extracted_value
                    elif extracted_value:
                        discount_template[key]["value"] = [extracted_value]
                else:
                    discount_template[key]["value"] = extracted_value

        # Ειδική διαχείριση για no_of_people (integer field)
        if "no_of_people" in extracted_info and "no_of_people" in discount_template:
            try:
                people_value = extracted_info["no_of_people"].get("value", "")
                if people_value:
                    discount_template["no_of_people"]["value"] = int(people_value)
            except (ValueError, TypeError):
                if isinstance(people_value, str) and any(c.isdigit() for c in people_value):
                    digits = ''.join(c for c in people_value if c.isdigit())
                    if digits:
                        discount_template["no_of_people"]["value"] = int(digits)
    
    return discount_template

def extract_review_info(user_message):
    """
    Εξάγει πληροφορίες αξιολόγησης και βαθμολογίας από το μήνυμα του χρήστη.
    
    Args:
        user_message (str): Το μήνυμα αξιολόγησης/σχολίου του χρήστη
        
    Returns:
        dict: Δομημένες πληροφορίες αξιολόγησης
    """
    # Φόρτωση template και δημιουργία καθαρού template για το LLM
    review_template = load_json_template("review.json")
    clean_template = remove_pvalues_from_template(review_template)
    
    # Φόρτωση prompt - το prompt περιέχει ήδη το template και τις οδηγίες
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
        print("Failed to parse review JSON from primary LLM response")

    # Αν αποτύχει η κύρια εξαγωγή, δοκιμάζουμε με fallback
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
            return review_template  # Επιστρέφουμε το κενό template    # Συγχώνευση των εξαγόμενων πληροφοριών με το template
    if extracted_info:
        for key in ["reservation_number", "passcode", "review"]:
            if key in extracted_info and key in review_template:
                review_template[key]["value"] = extracted_info[key].get("value", "")
        
        # Ειδική διαχείριση για stars field
        if "stars" in extracted_info and "stars" in review_template:
            stars_value = extracted_info["stars"].get("value", "")
            if stars_value:
                # Το LLM μπορεί να επιστρέψει λίστα [5] ή απλό αριθμό 5
                if isinstance(stars_value, list) and len(stars_value) > 0:
                    review_template["stars"]["value"] = stars_value[0]
                else:
                    review_template["stars"]["value"] = stars_value
    
    return review_template
