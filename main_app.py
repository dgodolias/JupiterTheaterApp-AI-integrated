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
    
    # Create the booking folder if it doesn't exist
    booking_folder = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'booking')
    os.makedirs(booking_folder, exist_ok=True)
    
    if category == "ΠΛΗΡΟΦΟΡΙΕΣ":
        # Extract show information for information requests
        show_info = extract_show_info(user_input)
        print(f"Extracted information: {json.dumps(show_info, ensure_ascii=False, indent=2)}")
        
        # Save extracted information to a file
        extracted_info_path = os.path.join(booking_folder, 'extracted_info.json')
        with open(extracted_info_path, 'w', encoding='utf-8') as f:
            json.dump(show_info, f, ensure_ascii=False, indent=2)
    
    elif category == "ΚΡΑΤΗΣΗ":
        # Extract booking information for reservation requests
        booking_info = extract_booking_info(user_input)
        print(f"Extracted booking: {json.dumps(booking_info, ensure_ascii=False, indent=2)}")
        
        # Save booking information to a file
        booking_info_path = os.path.join(booking_folder, 'booking_info.json')
        with open(booking_info_path, 'w', encoding='utf-8') as f:
            json.dump(booking_info, f, ensure_ascii=False, indent=2)
            
    elif category == "ΑΚΥΡΩΣΗ":
        # Extract cancellation information for cancellation requests
        cancellation_info = extract_cancellation_info(user_input)
        print(f"Extracted cancellation info: {json.dumps(cancellation_info, ensure_ascii=False, indent=2)}")
        
        # Save cancellation information to a file
        cancellation_info_path = os.path.join(booking_folder, 'cancellation_info.json')
        with open(cancellation_info_path, 'w', encoding='utf-8') as f:
            json.dump(cancellation_info, f, ensure_ascii=False, indent=2)
            
    elif category == "ΠΡΟΣΦΟΡΕΣ & ΕΚΠΤΩΣΕΙΣ":
        # Extract discount information for promotion/discount requests
        discount_info = extract_discount_info(user_input)
        print(f"Extracted discount info: {json.dumps(discount_info, ensure_ascii=False, indent=2)}")
        
        # Save discount information to a file
        discount_info_path = os.path.join(booking_folder, 'discount_info.json')
        with open(discount_info_path, 'w', encoding='utf-8') as f:
            json.dump(discount_info, f, ensure_ascii=False, indent=2)
            
    elif category == "ΑΞΙΟΛΟΓΗΣΕΙΣ & ΣΧΟΛΙΑ":
        # Extract review information for reviews/comments
        review_info = extract_review_info(user_input)
        print(f"Extracted review info: {json.dumps(review_info, ensure_ascii=False, indent=2)}")
        
        # Save review information to a file
        review_info_path = os.path.join(booking_folder, 'review_info.json')
        with open(review_info_path, 'w', encoding='utf-8') as f:
            json.dump(review_info, f, ensure_ascii=False, indent=2)
    
    sys.exit(0)
