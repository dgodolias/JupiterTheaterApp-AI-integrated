import os
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

# Available models
AVAILABLE_MODELS = {
    "llama-4-maverick": "meta-llama/llama-4-maverick:free",
    "llama-4-scout": "meta-llama/llama-4-scout:free",
    "deepseek-v3": "deepseek/deepseek-v3-base:free",
    "gemini-2.5": "google/gemini-2.5-pro-exp-03-25:free"
}

def select_model(message_type):
    """
    Programmatically select a model based on the message type or other criteria
    
    Args:
        message_type (str): Type of message being processed
        
    Returns:
        str: The selected model identifier
    """
    # You can implement your selection logic here
    # This is just an example implementation
    if "complex" in message_type.lower():
        return AVAILABLE_MODELS["gemini-2.5"]  # Use Gemini for complex queries
    elif "creative" in message_type.lower():
        return AVAILABLE_MODELS["llama-4-maverick"]  # Use Llama Maverick for creative tasks
    elif "factual" in message_type.lower():
        return AVAILABLE_MODELS["deepseek-v3"]  # Use Deepseek for factual info
    else:
        return AVAILABLE_MODELS["llama-4-scout"]  # Default to Llama Scout

if __name__ == "__main__":
    print("LLM Message Sender")
    print("------------------")
    
    # Get user input
    user_input = input("Enter your message: ")
    message_type = input("Enter message type (complex/creative/factual/default): ")
    
    # Programmatically select the model
    selected_model = select_model(message_type)
    
    # Send message to LLM and get response
    response = send_message_to_llm(
        user_message=user_input,
        model=selected_model
    )
    
    # Display response
    print("\nLLM Response:")
    print("-------------")
    print(response)