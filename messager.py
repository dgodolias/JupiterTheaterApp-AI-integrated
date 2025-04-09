import os
import sys
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


def send_message_to_llm(user_message, system_message="You are a helpful assistant", 
                       model="meta-llama/llama-4-maverick:free", max_tokens=500):
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
        result = response.choices[0].message.content.strip()
        print(f"API response received successfully")
        return result
    except Exception as e:
        print(f"Error calling OpenRouter API: {e}")
        return f"Error: {str(e)}"


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
    
    # Use a smaller, faster model for classification
    model = AVAILABLE_MODELS["llama-4-scout"]
    
    result = send_message_to_llm(
        user_message=user_message,
        system_message=system_prompt,
        model=model,
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

# Available models
AVAILABLE_MODELS = {
    "llama-4-maverick": "meta-llama/llama-4-maverick:free",
    "llama-4-scout": "meta-llama/llama-4-scout:free",
    "deepseek-v3": "deepseek/deepseek-v3-base:free",
    "gemini-2.5": "google/gemini-2.5-pro-exp-03-25:free"
}

if __name__ == "__main__":
    print("Jupiter Theater Assistant")
    print("-------------------------")
    
    # Get user input
    user_input = input("Enter your message: ")
    
    # Categorize the message
    category = categorize_prompt(user_input)
    print(f"Message category: {category}")
    
    # Exit after displaying the category
    sys.exit(0)
    