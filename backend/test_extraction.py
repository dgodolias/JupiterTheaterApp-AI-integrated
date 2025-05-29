# -*- coding: utf-8 -*-
"""
Test script για τη νέα ελληνική λογική εξαγωγής πληροφοριών
"""

import json
from information_extractor import extract_booking_info

def test_greek_booking_extraction():
    """
    Δοκιμάζει την εξαγωγή booking πληροφοριών με το παράδειγμα μήνυμα
    """
    test_message = "Θελω να κλεισω εισιτηρια στο ονομα Δημος Στεργιου για παρασκευη, στον μαγο του οζ"
    
    print("=" * 60)
    print("TESTING GREEK BOOKING EXTRACTION")
    print("=" * 60)
    print(f"Test Message: {test_message}")
    print("-" * 60)
    
    try:
        result = extract_booking_info(test_message)
        
        print("EXTRACTED RESULT:")
        print(json.dumps(result, indent=2, ensure_ascii=False))
        
        print("\n" + "-" * 60)
        print("EXPECTED VALUES:")
        print("show_name.value should be: 'Ο Μάγος του Οζ'")
        print("day.value should be: 'Παρασκευή'") 
        print("person.name.value should be: 'Δήμος Στεργίου'")
        
        print("\n" + "-" * 60)
        print("ACTUAL VALUES:")
        print(f"show_name.value: '{result.get('show_name', {}).get('value', 'NOT_FOUND')}'")
        print(f"day.value: '{result.get('day', {}).get('value', 'NOT_FOUND')}'")
        print(f"person.name.value: '{result.get('person', {}).get('name', {}).get('value', 'NOT_FOUND')}'")
        
        # Ελέγχουμε αν τα αποτελέσματα είναι σωστά
        success_count = 0
        
        if result.get('show_name', {}).get('value') == 'Ο Μάγος του Οζ':
            print("✅ Show name correctly extracted!")
            success_count += 1
        else:
            print("❌ Show name not correctly extracted")
            
        if result.get('day', {}).get('value') == 'Παρασκευή':
            print("✅ Day correctly extracted!")
            success_count += 1
        else:
            print("❌ Day not correctly extracted")
            
        if result.get('person', {}).get('name', {}).get('value') == 'Δήμος Στεργίου':
            print("✅ Person name correctly extracted!")
            success_count += 1
        else:
            print("❌ Person name not correctly extracted")
            
        print(f"\n🎯 SUCCESS RATE: {success_count}/3 fields correctly extracted")
        
    except Exception as e:
        print(f"❌ ERROR during extraction: {str(e)}")
        import traceback
        traceback.print_exc()

if __name__ == "__main__":
    test_greek_booking_extraction()
