import os
import sys
import json
from message_categorizer import categorize_prompt
from information_extractor import extract_booking_info

def test_main():
    user_input = "Θελω να κλεισω εισιτηρια στο ονομα Δημος Στεργιου για παρασκευη, στον μαγο του οζ"
    
    print("Jupiter Theater Assistant")
    print("-------------------------")
    print(f"Test message: {user_input}")
    
    # Categorize the message
    category = categorize_prompt(user_input)
    print(f"Message category: {category}")
    
    if category == "ΚΡΑΤΗΣΗ":
        # Extract booking information for reservation requests
        booking_info = extract_booking_info(user_input)
        if booking_info:
            print("Extracted booking information:")
            print(json.dumps(booking_info, ensure_ascii=False, indent=2))
        else:
            print("Could not extract booking information.")

if __name__ == "__main__":
    test_main()
