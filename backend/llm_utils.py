import os
import json
from openai import OpenAI
from dotenv import load_dotenv, find_dotenv

# Explicitly find and load .env file, overriding existing environment variables
dotenv_path = find_dotenv(raise_error_if_not_found=False) # Try to find .env
print(f"Found .env file at: {dotenv_path}")

# Print API key from environment BEFORE loading .env (if it exists)
api_key_before_load = os.getenv("OPENROUTER_API_KEY")

if dotenv_path:
    load_dotenv(dotenv_path=dotenv_path, override=True)
    print(f"Loaded .env file from: {dotenv_path} with override=True")
else:
    # Fallback to default load_dotenv behavior if find_dotenv fails, though it should find it if it's in the root
    load_dotenv(override=True)
    print("Could not specifically locate .env, attempting default load_dotenv(override=True)")


# Get API key from environment variable AFTER loading .env
api_key = os.getenv("OPENROUTER_API_KEY")

# Initialize the OpenAI client with OpenRouter endpoint
client = OpenAI(
    base_url="https://openrouter.ai/api/v1",
    api_key=api_key, # Use the loaded api_key variable
)

# Available models with fallbacks
AVAILABLE_MODELS = {
    "fallback": "google/gemma-3-12b-it:free",
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
        print(response)
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
