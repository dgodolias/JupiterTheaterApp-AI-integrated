import os
import sys
import json
from openai import OpenAI
from dotenv import load_dotenv

# Load environment variables from .env file
load_dotenv()

# Get API key from environment variable
api_key = os.getenv("OPENROUTER_API_KEY")

# Initialize the OpenAI client with OpenRouter endpoint
client = OpenAI(
    base_url="https://openrouter.ai/api/v1",
    api_key=api_key,
)

# Available models with fallbacks
AVAILABLE_MODELS = {
    "fallback": "nvidia/llama-3.1-nemotron-nano-8b-v1:free",
    "primary": "meta-llama/llama-4-scout:free"
}

def send_message_to_llm(user_message, system_message="You are a helpful assistant", 
                       model="google/gemini-2.5-pro-exp-03-25:free", max_tokens=500):
    """
    Sends a message to the language model and returns the response.
    
    Args:
        user_message (str): The message to send to the LLM
        system_message (str): The system message to set the LLM's behavior
        model (str): The model identifier to use with OpenRouter
        max_tokens (int): Maximum tokens to generate in the response
        
    Returns:
        str: The LLM's response text
    """
    try:
        print(f"Sending request to OpenRouter API using model: {model}")
        response = client.chat.completions.create(
            model=model,
            messages=[
                {"role": "system", "content": system_message},
                {"role": "user", "content": user_message},
            ],
            max_tokens=max_tokens,
            stream=False
        )
        
        # Check if response and choices exist
        if response and hasattr(response, 'choices') and response.choices:
            if len(response.choices) > 0 and response.choices[0].message:
                result = response.choices[0].message.content.strip()
                print(f"API response received successfully")
                return result
        
        print("Empty or invalid response structure received from API")
        return ""
        
    except Exception as e:
        print(f"Error calling OpenRouter API: {e}")
        # Try with fallback model if primary model fails
        if model != AVAILABLE_MODELS["fallback"]:
            print(f"Trying fallback model: {AVAILABLE_MODELS['fallback']}")
            return send_message_to_llm(
                user_message=user_message,
                system_message=system_message,
                model=AVAILABLE_MODELS["fallback"],
                max_tokens=max_tokens
            )
        return ""


def categorize_prompt(user_message):
    """
    Categorizes the user message into one of the predefined Greek categories.
    
    Args:
        user_message (str): The user's message to categorize
        
    Returns:
        str: The category label
    """
    system_prompt = """
    You are a text classifier. Classify the user's message into EXACTLY ONE of these categories:
    - ΚΡΑΤΗΣΗ (for reservation requests)
    - ΑΚΥΡΩΣΗ (for cancellation requests)
    - ΠΛΗΡΟΦΟΡΙΕΣ (for information requests about shows, times, etc.)
    - ΑΞΙΟΛΟΓΗΣΕΙΣ & ΣΧΟΛΙΑ (for reviews, comments, feedback)
    - ΠΡΟΣΦΟΡΕΣ & ΕΚΠΤΩΣΕΙΣ (for questions about discounts, offers, promotions)
    
    Respond ONLY with the category name in Greek, nothing else.
    """
    
    # Use primary model for classification
    result = send_message_to_llm(
        user_message=user_message,
        system_message=system_prompt,
        model=AVAILABLE_MODELS["primary"],
        max_tokens=20  # Short response for classification
    )
    
    # Clean up and normalize the response
    result = result.strip().upper()
    
    # Validate category
    valid_categories = [
        "ΚΡΑΤΗΣΗ", "ΑΚΥΡΩΣΗ", "ΠΛΗΡΟΦΟΡΙΕΣ", "ΑΞΙΟΛΟΓΗΣΕΙΣ & ΣΧΟΛΙΑ", "ΠΡΟΣΦΟΡΕΣ & ΕΚΠΤΩΣΕΙΣ"
    ]
    
    # Return the category if valid, otherwise default to ΠΛΗΡΟΦΟΡΙΕΣ
    if any(category in result for category in valid_categories):
        for category in valid_categories:
            if category in result:
                return category
    
    return "ΠΛΗΡΟΦΟΡΙΕΣ"  # Default category

def extract_show_info(user_message):
    """
    Extracts show information from user message for filtering purposes.
    
    Args:
        user_message (str): The user's message
        
    Returns:
        dict: Complete show information dictionary with all fields
    """
    # Define template with all fields as arrays (empty by default)
    template = {
        "name": [],
        "day": [],  # Array for multiple days
        "topic": [],  # Array for multiple topics
        "time": [],  # Array for time constraints with operators
        "cast": [],
        "room": [],
        "duration": [],
        "stars": []
    }
    
    system_prompt = """
    Extract information about theater shows from the user's message, which may be in Greek or English.
    
    IMPORTANT: TRANSLITERATE ALL GREEK TEXT TO ENGLISH, including:
    - Names of actors/performers (transliterate properly)
    - Show titles (transliterate or translate when possible)
    - Venue or theater names (transliterate)
    - Topics/genres (translate to English equivalent terms)
    
    For days of the week, use: Monday, Tuesday, Wednesday, Thursday, Friday, Saturday, Sunday
    
    TIME FORMATTING RULES:
    - For "after X time" use ">HH:MM" format (e.g., after 7 PM = ">19:00")
    - For "before X time" use "<HH:MM" format
    - For time ranges "between X and Y" use ">HH:MM,<HH:MM" format (e.g., between 5-7 PM = ">17:00,<19:00")
    - For "before X OR after Y" use "<HH:MM,>HH:MM" format (e.g., before 5 PM or after 8 PM = "<17:00,>20:00")
    
    Return ONLY a valid JSON object with these fields (all must be arrays, even if single value):
    - name: Show names (array of strings)
    - day: Days of the week (array of strings)
    - topic: Show genres/topics (array of strings)
    - time: Time constraints with operators (array of strings)
    - cast: Cast member names mentioned (array of strings) - TRANSLITERATED TO ENGLISH
    - room: Room numbers or theater venues (array of strings)
    - duration: Show durations (array of strings)
    - stars: Star ratings (array of numbers or strings with operators like ">4")
    
    IMPORTANT: If weekend ("Σαββατοκύριακο" or similar) is mentioned, include both Saturday and Sunday in the day array.
    Only include fields explicitly mentioned. Format as valid JSON with no additional text.
    """
    
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
        system_prompt = """
        Extract and TRANSLITERATE all information from the user's message to English.
        
        TRANSLITERATION RULES:
        - Convert Greek letters to their closest English equivalents
        - "α" → "a", "β" → "v", "γ" → "g", "δ" → "d", "ε" → "e", etc.
        - Greek names should be properly transliterated, not translated
        - Greek genres/topics should be translated to English equivalents
        - Greek days should be converted to English day names
        
        TIME FORMATTING:
        - After specific time → ">HH:MM" (e.g., after 7 PM = ">19:00")
        - Before specific time → "<HH:MM" (e.g., before 5 PM = "<17:00")
        - Between times → ">HH:MM,<HH:MM" (e.g., between 5-7 PM = ">17:00,<19:00")
        
        Return a valid JSON with ALL text in English, formatted as arrays.
        Example: {"day": ["Saturday", "Sunday"], "cast": ["Actor Name"], "topic": ["thriller"]}
        """
        
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
    booking_template = {
        "show_name": "",
        "room": "",
        "day": "",
        "time": "",
        # Initialize persons 1-10 with empty subfields
    }
    
    # Initialize person1 through person10 fields with empty subfields
    for i in range(1, 11):
        booking_template[f"person{i}"] = {
            f"name{i}": "",
            f"age{i}": "",
            f"seat{i}": ""
        }
    
    system_prompt = """
    Extract booking information from the user's message, which may be in Greek or English.
    
    IMPORTANT: TRANSLATE/TRANSLITERATE ALL GREEK TEXT TO ENGLISH, including:
    - Show name (translate if possible, otherwise transliterate)
    - Person names (transliterate properly)
    - Theater room or venue names (transliterate) 
    
    For days, use English day names: Monday, Tuesday, Wednesday, Thursday, Friday, Saturday, Sunday
    
    Return a valid JSON object with these fields:
    - show_name: Name of the show (string)
    - room: Theater room number or name (string)
    - day: Day of the week for the booking (string)
    - time: Time of the show in 24-hour format (HH:MM) (string)
    - person1 through person10: Information for up to 10 people in the booking (objects)
      Each person object should have:
      - name{i}: Person's name (string)
      - age{i}: Person's age (string or number)
      - seat{i}: Requested seat number/location if specified (string)
    
    Only include person fields that are explicitly mentioned. If no specific seats are requested,
    leave the seat fields empty. Format as valid JSON with no additional text.
    
    Example for a booking with 2 people:
    {
      "show_name": "Hamlet",
      "room": "Main Stage",
      "day": "Friday",
      "time": "20:00",
      "person1": {"name1": "John Smith", "age1": "35", "seat1": "A12"},
      "person2": {"name2": "Mary Smith", "age2": "30", "seat2": "A13"},
      "person3": {"name3": "", "age3": "", "seat3": ""},
      ...and so on for person4-person10
    }
    """
    
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
        system_prompt = """
        Extract basic booking information from the user's message.
        Translate all Greek text to English.
        
        Return a simple JSON with these fields:
        - show_name: Name of the show
        - room: Theater room/venue 
        - day: Day of the week in English
        - time: Show time in 24-hour format
        - person1: {"name1": "Person's name", "age1": "age", "seat1": "seat number"}
        
        Only include information that is explicitly mentioned.
        Example: {"show_name": "Hamlet", "day": "Friday", "time": "20:00", 
                  "person1": {"name1": "John", "age1": "35", "seat1": ""}}
        """
        
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
            extracted_info = {
                "show_name": "",
                "room": "",
                "day": "",
                "time": "",
                "person1": {"name1": "", "age1": "", "seat1": ""}
            }
    
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
    cancellation_template = {
        "reservation_number": "",
        "passcode": ""
    }
    
    system_prompt = """
    Extract cancellation information from the user's message, which may be in Greek or English.
    
    IMPORTANT: Focus on finding the RESERVATION NUMBER and PASSCODE.
    - Reservation numbers are typically 6-10 digits
    - Passcodes are typically 4-6 digits or alphanumeric codes
    
    Look for phrases like:
    - "Cancel reservation number..."
    - "My booking number is..."
    - "Passcode/PIN/verification code..."
    - "Ακύρωση κράτησης με αριθμό..."
    - "Κωδικός επιβεβαίωσης..."
    
    Return ONLY a valid JSON object with these fields:
    - reservation_number: The booking reference number (string)
    - passcode: The verification code or passcode (string)
    
    Example: {"reservation_number": "12345678", "passcode": "AB123"}
    
    If either field is not found in the message, leave it as an empty string.
    Format as valid JSON with no additional text.
    """
    
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
        system_prompt = """
        Find the reservation number and passcode in the user's message.
        Look for numbers or codes that could identify a booking.
        
        Return a simple JSON with these fields:
        - reservation_number: The booking reference number
        - passcode: The verification code
        
        Example: {"reservation_number": "12345678", "passcode": "AB123"}
        """
        
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
            extracted_info = {
                "reservation_number": "",
                "passcode": ""
            }
    
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
    discount_template = {
        "show_name": [],  # Multiple show names
        "no_of_people": 0,  # Single value for number of people
        "age": [],  # Multiple age values or ranges
        "date": []  # Multiple dates
    }
    
    system_prompt = """
    Extract discount/promotion information from the user's message, which may be in Greek or English.
    
    IMPORTANT: TRANSLATE/TRANSLITERATE ALL GREEK TEXT TO ENGLISH, including:
    - Show names (translate if possible, otherwise transliterate)
    - Dates and age information
    
    Return a valid JSON object with these fields:
    - show_name: Name(s) of the show(s) the user is interested in (array of strings)
    - no_of_people: Number of people for the discount (number)
    - age: Age(s) or age ranges mentioned (array of strings like "15", ">60", "<18")
    - date: Date(s) or date ranges mentioned (array of strings)
    
    For dates, convert Greek month names to English and use the format "DD Month" or "DD/MM".
    For age ranges, use operators like ">60" (over 60), "<18" (under 18), etc.
    
    Only include information that is explicitly mentioned in the message.
    Format as valid JSON with no additional text.
    
    Example: {
      "show_name": ["Hamlet", "Romeo and Juliet"],
      "no_of_people": 3,
      "age": ["student", ">65"],
      "date": ["15 March", "Weekend"]
    }
    """
    
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
        system_prompt = """
        Extract basic discount information from the user's message.
        Translate all Greek text to English.
        
        Return a simple JSON with these fields:
        - show_name: Name(s) of the show(s) (array)
        - no_of_people: Number of people (number)
        - age: Age(s) mentioned (array)
        - date: Date(s) mentioned (array)
        
        Example: {
          "show_name": ["Hamlet"],
          "no_of_people": 2,
          "age": ["student"],
          "date": ["Friday"]
        }
        """
        
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
            extracted_info = {
                "show_name": [],
                "no_of_people": 0,
                "age": [],
                "date": []
            }
    
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
    review_template = {
        "reservation_number": "",
        "passcode": "",
        "stars": 0,
        "review": ""
    }
    
    system_prompt = """
    Extract review/rating information from the user's message, which may be in Greek or English.
    
    IMPORTANT: TRANSLATE ALL GREEK TEXT TO ENGLISH for the review field.
    
    Return a valid JSON object with these fields:
    - reservation_number: The booking reference number (string)
    - passcode: The verification code or passcode (string)
    - stars: Star rating given, typically 1-5 (number)
    - review: The user's review text or comments (string) - TRANSLATED TO ENGLISH
    
    Look for phrases that indicate ratings such as:
    - "I rate this show X stars"
    - "X out of 5"
    - "Rating: X"
    - "Βαθμολογία: X"
    - "X αστέρια"
    
    If specific fields are not found in the message, leave as empty string (or 0 for stars).
    Format as valid JSON with no additional text.
    
    Example: {
      "reservation_number": "12345678",
      "passcode": "AB123",
      "stars": 4,
      "review": "The performance was excellent, with outstanding acting."
    }
    """
    
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
        system_prompt = """
        Extract basic review information from the user's message.
        Translate all Greek text to English.
        
        Return a simple JSON with these fields:
        - reservation_number: Booking reference number
        - passcode: Verification code
        - stars: Rating (number 1-5)
        - review: User's comments (translated to English)
        
        Example: {
          "reservation_number": "12345678",
          "passcode": "AB123",
          "stars": 4,
          "review": "Great performance"
        }
        """
        
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
            extracted_info = {
                "reservation_number": "",
                "passcode": "",
                "stars": 0,
                "review": ""
            }
    
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


def categorize_prompt(user_message):
    """
    Categorizes the user message into one of the predefined Greek categories.
    
    Args:
        user_message (str): The user's message to categorize
        
    Returns:
        str: The category label
    """
    system_prompt = """
    You are a text classifier. Classify the user's message into EXACTLY ONE of these categories:
    - ΚΡΑΤΗΣΗ (for reservation requests)
    - ΑΚΥΡΩΣΗ (for cancellation requests)
    - ΠΛΗΡΟΦΟΡΙΕΣ (for information requests about shows, times, etc.)
    - ΑΞΙΟΛΟΓΗΣΕΙΣ & ΣΧΟΛΙΑ (for reviews, comments, feedback)
    - ΠΡΟΣΦΟΡΕΣ & ΕΚΠΤΩΣΕΙΣ (for questions about discounts, offers, promotions)
    - ΕΞΟΔΟΣ (for exit/quit requests, closing the application)
    
    The ΕΞΟΔΟΣ category should be used for requests to exit, quit, close, or terminate the application.
    Phrases like "exit", "quit", "close", "βγες απο την εφαρμογη", "κλεισε", "εξοδος", "τελος" should be classified as ΕΞΟΔΟΣ.
    
    Respond ONLY with the category name in Greek, nothing else.
    """
    
    # Use primary model for classification
    result = send_message_to_llm(
        user_message=user_message,
        system_message=system_prompt,
        model=AVAILABLE_MODELS["primary"],
        max_tokens=20  # Short response for classification
    )
    
    # Clean up and normalize the response
    result = result.strip().upper()
    
    # Validate category
    valid_categories = [
        "ΚΡΑΤΗΣΗ", "ΑΚΥΡΩΣΗ", "ΠΛΗΡΟΦΟΡΙΕΣ", "ΑΞΙΟΛΟΓΗΣΕΙΣ & ΣΧΟΛΙΑ", 
        "ΠΡΟΣΦΟΡΕΣ & ΕΚΠΤΩΣΕΙΣ", "ΕΞΟΔΟΣ"
    ]
    
    # Return the category if valid, otherwise default to ΠΛΗΡΟΦΟΡΙΕΣ
    if any(category in result for category in valid_categories):
        for category in valid_categories:
            if category in result:
                return category
    
    return "ΠΛΗΡΟΦΟΡΙΕΣ"  # Default category


# Update main function to handle "ΕΞΟΔΟΣ" category
if __name__ == "__main__":
    print("Jupiter Theater Assistant")
    print("-------------------------")
    
    # Get user input
    user_input = input("Enter your message: ")
    
    # Categorize the message
    category = categorize_prompt(user_input)
    print(f"Message category: {category}")
    
    # Handle exit requests immediately
    if category == "ΕΞΟΔΟΣ":
        print("Exiting the application...")
        sys.exit(0)
    
    # Create the booking folder if it doesn't exist
    booking_folder = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'booking')
    os.makedirs(booking_folder, exist_ok=True)
    
    if category == "ΠΛΗΡΟΦΟΡΙΕΣ":
        # Extract show information for information requests
        show_info = extract_show_info(user_input)
        print(f"Extracted information: {json.dumps(show_info, ensure_ascii=False)}")
        
        # Save extracted information to a file
        extracted_info_path = os.path.join(booking_folder, 'extracted_info.json')
        with open(extracted_info_path, 'w', encoding='utf-8') as f:
            json.dump(show_info, f, ensure_ascii=False, indent=2)
    
    elif category == "ΚΡΑΤΗΣΗ":
        # Extract booking information for reservation requests
        booking_info = extract_booking_info(user_input)
        print(f"Extracted booking: {json.dumps(booking_info, ensure_ascii=False)}")
        
        # Save booking information to a file
        booking_info_path = os.path.join(booking_folder, 'booking_info.json')
        with open(booking_info_path, 'w', encoding='utf-8') as f:
            json.dump(booking_info, f, ensure_ascii=False, indent=2)
            
    elif category == "ΑΚΥΡΩΣΗ":
        # Extract cancellation information for cancellation requests
        cancellation_info = extract_cancellation_info(user_input)
        print(f"Extracted cancellation info: {json.dumps(cancellation_info, ensure_ascii=False)}")
        
        # Save cancellation information to a file
        cancellation_info_path = os.path.join(booking_folder, 'cancellation_info.json')
        with open(cancellation_info_path, 'w', encoding='utf-8') as f:
            json.dump(cancellation_info, f, ensure_ascii=False, indent=2)
            
    elif category == "ΠΡΟΣΦΟΡΕΣ & ΕΚΠΤΩΣΕΙΣ":
        # Extract discount information for promotion/discount requests
        discount_info = extract_discount_info(user_input)
        print(f"Extracted discount info: {json.dumps(discount_info, ensure_ascii=False)}")
        
        # Save discount information to a file
        discount_info_path = os.path.join(booking_folder, 'discount_info.json')
        with open(discount_info_path, 'w', encoding='utf-8') as f:
            json.dump(discount_info, f, ensure_ascii=False, indent=2)
            
    elif category == "ΑΞΙΟΛΟΓΗΣΕΙΣ & ΣΧΟΛΙΑ":
        # Extract review information for reviews/comments
        review_info = extract_review_info(user_input)
        print(f"Extracted review info: {json.dumps(review_info, ensure_ascii=False)}")
        
        # Save review information to a file
        review_info_path = os.path.join(booking_folder, 'review_info.json')
        with open(review_info_path, 'w', encoding='utf-8') as f:
            json.dump(review_info, f, ensure_ascii=False, indent=2)
    
    sys.exit(0)