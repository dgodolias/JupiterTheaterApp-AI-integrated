import os
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
        print(f"Sending request to OpenRouter API using model: {model} for categorization")
        response = client.chat.completions.create(
            model=model,
            messages=[
                {"role": "system", "content": system_message},
                {"role": "user", "content": user_message},
            ],
            max_tokens=max_tokens,
            stream=False
        )
        
        if response and hasattr(response, 'choices') and response.choices:
            if len(response.choices) > 0 and response.choices[0].message:
                result = response.choices[0].message.content.strip()
                print(f"API response received successfully for categorization")
                return result
        
        print("Empty or invalid response structure received from API during categorization")
        return ""
        
    except Exception as e:
        print(f"Error calling OpenRouter API during categorization: {e}")
        if model != AVAILABLE_MODELS["fallback"]:
            print(f"Trying fallback model for categorization: {AVAILABLE_MODELS['fallback']}")
            return send_message_to_llm(
                user_message=user_message,
                system_message=system_message,
                model=AVAILABLE_MODELS["fallback"],
                max_tokens=max_tokens
            )
        return ""

from llm_utils import send_message_to_llm, AVAILABLE_MODELS

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