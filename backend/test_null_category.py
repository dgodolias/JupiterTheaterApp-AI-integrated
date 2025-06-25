import json
from message_categorizer import categorize_prompt

def test_null_category():
    """Test the NULL category for messages that don't fit anywhere."""
    
    test_messages = [
        "a",  # single letter
        "xyz",  # random letters
        "12345",  # just numbers
        "blablabla",  # nonsense
        "asdfjkl",  # keyboard mashing
        "hello world",  # English (unrelated to theater)
        "???",  # just punctuation
        "",  # empty (though this might cause issues)
        "test test test",  # repetitive nonsense
        "random stuff here"  # unrelated content
    ]
    
    print("=== TESTING NULL CATEGORY ===")
    
    for i, message in enumerate(test_messages, 1):
        if not message:  # Skip empty messages to avoid API issues
            continue
            
        print(f"\nTest {i}: '{message}'")
        try:
            category = categorize_prompt(message)
            print(f"Categorized as: {category}")
            
            if category == "NULL":
                print("✓ Correctly identified as NULL")
            else:
                print(f"⚠ Expected NULL but got: {category}")
                
        except Exception as e:
            print(f"✗ Error: {e}")

if __name__ == "__main__":
    test_null_category()
