import json
from information_extractor import extract_discount_info
from llm_utils import send_message_to_llm, AVAILABLE_MODELS

def debug_discount_extraction():
    test_message = "ειμαστε 5 ατομα που θελουμε να ερθουμε  στο τερα`ς της λιμνης, δικαιουμαστε καποια εκπτωση?"
    
    print("=== DEBUG DISCOUNT EXTRACTION ===")
    print(f"Test message: {test_message}")
    
    # Load the discount prompt
    with open("prompts/discount.txt", "r", encoding="utf-8") as f:
        discount_prompt = f.read()
    
    print("\n=== DISCOUNT SYSTEM PROMPT ===")
    print(discount_prompt)
    
    # Call LLM directly with discount prompt
    result = send_message_to_llm(
        user_message=test_message,
        system_message=discount_prompt,
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
    print("\n=== EXTRACT_DISCOUNT_INFO FUNCTION RESULT ===")
    function_result = extract_discount_info(test_message)
    print(json.dumps(function_result, ensure_ascii=False, indent=2))
    
    # Check what the LLM extracted for no_of_people specifically
    if extracted_info and "no_of_people" in extracted_info:
        print(f"\n=== NO_OF_PEOPLE FIELD ANALYSIS ===")
        people_field = extracted_info["no_of_people"]
        print(f"No_of_people field from LLM: {people_field}")
        if "value" in people_field:
            print(f"No_of_people value: {people_field['value']} (type: {type(people_field['value'])})")
        else:
            print("No 'value' key in no_of_people field")
    else:
        print("\n=== NO NO_OF_PEOPLE FIELD IN LLM RESPONSE ===")
        print("The LLM did not return a no_of_people field in the JSON")
    
    # Check show_name extraction
    if extracted_info and "show_name" in extracted_info:
        print(f"\n=== SHOW_NAME FIELD ANALYSIS ===")
        show_field = extracted_info["show_name"]
        print(f"Show_name field from LLM: {show_field}")
        if "value" in show_field:
            print(f"Show_name value: {show_field['value']} (type: {type(show_field['value'])})")
        else:
            print("No 'value' key in show_name field")

if __name__ == "__main__":
    debug_discount_extraction()
