import os
import sys
import json
from message_categorizer import categorize_prompt
from information_extractor import (
    extract_show_info, 
    extract_booking_info, 
    extract_cancellation_info, 
    extract_discount_info, 
    extract_review_info
)

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
    
    if category == "ΠΛΗΡΟΦΟΡΙΕΣ":
        # Extract show information for information requests
        show_info = extract_show_info(user_input)
        print(f"Extracted information: {json.dumps(show_info, ensure_ascii=False, indent=2)}")
    
    elif category == "ΚΡΑΤΗΣΗ":
        # Extract booking information for reservation requests
        booking_info = extract_booking_info(user_input)
        print(f"Extracted booking: {json.dumps(booking_info, ensure_ascii=False, indent=2)}")
            
    elif category == "ΑΚΥΡΩΣΗ":
        # Extract cancellation information for cancellation requests
        cancellation_info = extract_cancellation_info(user_input)
        print(f"Extracted cancellation info: {json.dumps(cancellation_info, ensure_ascii=False, indent=2)}")
            
    elif category == "ΠΡΟΣΦΟΡΕΣ & ΕΚΠΤΩΣΕΙΣ":
        # Extract discount information for promotion/discount requests
        discount_info = extract_discount_info(user_input)
        print(f"Extracted discount info: {json.dumps(discount_info, ensure_ascii=False, indent=2)}")
            
    elif category == "ΑΞΙΟΛΟΓΗΣΕΙΣ & ΣΧΟΛΙΑ":
        # Extract review information for reviews/comments
        review_info = extract_review_info(user_input)
        print(f"Extracted review info: {json.dumps(review_info, ensure_ascii=False, indent=2)}")
    
    sys.exit(0)
