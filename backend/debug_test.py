import json
from information_extractor import extract_review_info
from llm_utils import send_message_to_llm, AVAILABLE_MODELS

def debug_review_extraction():
    test_message = "Κράτηση RES012345 με κωδικό GHI789 - Εξαιρετική παράσταση! Οι ηθοποιοί ήταν καταπληκτικοί και η σκηνοθεσία άψογη. 5 αστέρια!"
    
    print("=== DEBUG REVIEW EXTRACTION ===")
    print(f"Test message: {test_message}")
    
    # Load the review prompt
    with open("prompts/review.txt", "r", encoding="utf-8") as f:
        review_prompt = f.read()
    
    print("\n=== REVIEW SYSTEM PROMPT ===")
    print(review_prompt)
    
    # Call LLM directly with review prompt
    result = send_message_to_llm(
        user_message=test_message,
        system_message=review_prompt,
        model=AVAILABLE_MODELS["primary"],
        max_tokens=300
    )
    
    print("\n=== RAW LLM RESPONSE ===")
    print(f"'{result}'")
    
    # Try to parse JSON
    extracted_info = {}
    try:
        if result and result.strip():
            start_idx = result.find('{')
            end_idx = result.rfind('}')
            if start_idx != -1 and end_idx != -1 and end_idx > start_idx:
                json_str = result[start_idx:end_idx+1]
                print(f"\n=== EXTRACTED JSON STRING ===")
                print(f"'{json_str}'")
                extracted_info = json.loads(json_str)
                print(f"\n=== PARSED JSON ===")
                print(json.dumps(extracted_info, ensure_ascii=False, indent=2))
    except json.JSONDecodeError as e:
        print(f"\n=== JSON PARSE ERROR ===")
        print(f"Error: {e}")
    
    # Call the actual function
    print("\n=== EXTRACT_REVIEW_INFO FUNCTION RESULT ===")
    function_result = extract_review_info(test_message)
    print(json.dumps(function_result, ensure_ascii=False, indent=2))
    
    # Check what the LLM extracted for stars specifically
    if extracted_info and "stars" in extracted_info:
        print(f"\n=== STARS FIELD ANALYSIS ===")
        stars_field = extracted_info["stars"]
        print(f"Stars field from LLM: {stars_field}")
        if "value" in stars_field:
            print(f"Stars value: {stars_field['value']} (type: {type(stars_field['value'])})")
        else:
            print("No 'value' key in stars field")
    else:
        print("\n=== NO STARS FIELD IN LLM RESPONSE ===")
        print("The LLM did not return a stars field in the JSON")

if __name__ == "__main__":
    debug_review_extraction()
