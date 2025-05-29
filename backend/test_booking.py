import json
from information_extractor import extract_booking_info

def test_booking():
    test_message = "Θελω να κλεισω εισιτηρια στο ονομα Δημος Στεργιου για παρασκευη, στον μαγο του οζ"
    
    print("=== TEST BOOKING EXTRACTION ===")
    print(f"Test message: {test_message}")
    
    result = extract_booking_info(test_message)
    print("\n=== BOOKING RESULT ===")
    print(json.dumps(result, ensure_ascii=False, indent=2))

if __name__ == "__main__":
    test_booking()
