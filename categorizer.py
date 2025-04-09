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
    # Define template with all required fields (empty by default)
    template = {
        "name": "",
        "day": [],  # Changed to array for multiple days
        "topic": [],  # Changed to array for multiple topics
        "time": "",
        "cast": [],
        "room": "",
        "duration": "",
        "stars": ""
    }
    
    system_prompt = """
    Extract information about theater shows from the user's message in Greek or English.
    Translate any Greek terms to their English equivalents.
    
    For days of the week, use: Monday, Tuesday, Wednesday, Thursday, Friday, Saturday, Sunday
    
    Return ONLY a valid JSON object with these fields:
    - name: The show name (string)
    - day: Days of the week as ARRAY (e.g. ["Saturday", "Sunday"] for weekend)
    - topic: Show genres/topics as ARRAY
    - time: Show time (e.g. "20:00")
    - cast: Array of cast member names mentioned
    - room: Room number (e.g. "12A")
    - duration: Show duration in minutes
    - stars: Minimum star rating (as a number)
    
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
        Extract days of week and minimum star rating from the user's message.
        Translate Greek terms to English (e.g. "Σάββατο" → "Saturday", "Σαββατοκύριακο" → ["Saturday", "Sunday"]).
        
        IMPORTANT: If weekend is mentioned, include both Saturday and Sunday in the day array.
        IMPORTANT: If minimum star rating is mentioned, include it as a number.
        
        Return ONLY a valid JSON like {"day": ["Saturday", "Sunday"], "stars": 3.5} - nothing else.
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
            print("Failed to extract even with simplified prompt")
            
            # Last resort - common Greek patterns for weekend
            if "σαββατοκυριακο" in user_message.lower() or "σαββατοκύριακο" in user_message.lower():
                extracted_info = {"day": ["Saturday", "Sunday"]}
                
                # Try to extract stars if mentioned
                if "αστερι" in user_message.lower() or "αστέρι" in user_message.lower():
                    for num in ["3", "3.5", "4", "4.5", "5"]:
                        if num in user_message:
                            extracted_info["stars"] = float(num)
                            break
    
    # Merge extracted info with template to ensure all fields are present
    for key, value in extracted_info.items():
        if key in template:
            template[key] = value
    
    # Convert string day to array if needed
    if template["day"] and isinstance(template["day"], str):
        template["day"] = [template["day"]]
    
    # Convert string topic to array if needed
    if template["topic"] and isinstance(template["topic"], str):
        template["topic"] = [template["topic"]]
    
    return template

if __name__ == "__main__":
    print("Jupiter Theater Assistant")
    print("-------------------------")
    
    # Get user input
    user_input = input("Enter your message: ")
    
    # Categorize the message
    category = categorize_prompt(user_input)
    print(f"Message category: {category}")
    
    # If category is ΠΛΗΡΟΦΟΡΙΕΣ, extract show information
    if category == "ΠΛΗΡΟΦΟΡΙΕΣ":
        # Extract show information from the user's message
        show_info = extract_show_info(user_input)
        print(f"Extracted information: {json.dumps(show_info, ensure_ascii=False)}")
        
        # Create the booking folder if it doesn't exist
        booking_folder = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'booking')
        os.makedirs(booking_folder, exist_ok=True)
        
        # Save extracted information to a file for booking.py to read
        extracted_info_path = os.path.join(booking_folder, 'extracted_info.json')
        with open(extracted_info_path, 'w', encoding='utf-8') as f:
            json.dump(show_info, f, ensure_ascii=False, indent=2)
        
    
    sys.exit(0)