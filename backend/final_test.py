#!/usr/bin/env python3
# -*- coding: utf-8 -*-

import sys
import os
sys.path.append(os.path.dirname(os.path.abspath(__file__)))

from message_categorizer import MessageCategorizer
from information_extractor import InformationExtractor

def test_queries():
    """Τελικό test με όλα τα 25 queries"""
    
    # Initialize components
    categorizer = MessageCategorizer()
    extractor = InformationExtractor()
    
    # Test queries organized by category
    test_cases = {
        "ΠΛΗΡΟΦΟΡΙΕΣ": [
            "Καλησπέρα, πώς πάει η δουλειά σας;",
            "Τι ώρα παίζει ο Μάγος του Οζ;",
            "Έχετε κάτι στη μεγάλη το Σάββατο;",
            "Θέλω να δω τον vissin0okipo στη αιθουσα Απόλλων",
            "Ψάχνω για δραματικές παραστάσεις με την Ελένη Παπαδάκη"
        ],
        "ΚΡΑΤΗΣΗ": [
            "Γεια σας, πώς μπορώ να σας βοηθήσω;",
            "Θέλω να κλείσω εισιτήρια για τον Δήμο Στεργίου την Κυριακή.",
            "Κλείστε μου εισιτήρια για τον Μάγος του Οζ στις 20:00, θέση A15.",
            "Θέλω να κλείσω εισιτήρια στο όνομα Μαρία Παπαδοπούλου για τον Μάγο του Οζ στη μεγάλη την Παρασκευή στις 20:30, θέση B12, ηλικία 35.",
            "Κλείστε μου εισιτήρια στο όνομα Κωνσταντίνος Αλεξίου για τον Μάγο του Οζ στην Διόνυσος Τρίτη στις 20:00, θέση Α5, ηλικία 28."
        ],
        "ΑΚΥΡΩΣΗ": [
            "Έχω ένα πρόβλημα με την κράτησή μου.",
            "Θέλω να ακυρώσω την κράτηση RES001234.",
            "Η κράτησή μου έχει κωδικό ABC123 και θέλω να την ακυρώσω.",
            "Θέλω να ακυρώσω την κράτηση RES005678 με κωδικό XYZ789.",
            "Παρακαλώ ακυρώστε την κράτηση RES12345 με κωδικό PASS1234."
        ],
        "ΠΡΟΣΦΟΡΕΣ": [
            "Μήπως έχετε καμιά προσφορά;",
            "Έχετε έκπτωση για τον Μάγος του Οζ στις 15 Μαΐου;",
            "Είμαι 66 χρονών, δικαιουμαι καποια μείωση για την Τριτη?",
            "ειμαστε 5 ατομα που θελουμε να ερθουμε  στο τερα`ς της λιμνης, δικαιουμαστε καποια εκπτωση?",
            "Είμαι κάτω από 18 και θέλω 1 εισιτήριο για τον Μάγο του Οζ τη Δευτέρα - υπάρχει έκπτωση;"
        ],
        "ΑΞΙΟΛΟΓΗΣΗ": [
            "Θέλω να αφήσω ένα σχόλιο.",
            "Η παράσταση ήταν υπέροχη! 5 αστέρια!",
            "Κράτηση RES009876 με κωδικό DEF456 - θέλω να αξιολογήσω.",
            "Κράτηση RES012345 με κωδικό GHI789 - Εξαιρετική παράσταση! Οι ηθοποιοί ήταν καταπληκτικοί και η σκηνοθεσία άψογη. 5 αστέρια!",
            "Κράτηση RES23456 με κωδικό pass234 - Καταπληκτική ερμηνεία της Παναγιας. Πραγματικά συγκινητικό το έργο και άψογη η απόδοση από τους ηθοποιούς. 5 αστέρια!"
        ]
    }
    
    total_tests = 0
    successful_tests = 0
    
    print("🎭 JUPITER THEATER LLM - ΤΕΛΙΚΟ TEST")
    print("=" * 60)
    
    for category, queries in test_cases.items():
        print(f"\n📂 ΚΑΤΗΓΟΡΙΑ: {category}")
        print("-" * 40)
        
        for i, query in enumerate(queries, 1):
            total_tests += 1
            print(f"\n{i}. QUERY: {query}")
            
            try:
                # Step 1: Categorize
                category_result = categorizer.categorize_message(query)
                predicted_category = category_result.get('category', 'UNKNOWN')
                print(f"   📋 ΚΑΤΗΓΟΡΙΑ: {predicted_category}")
                
                # Step 2: Extract information
                extraction_result = None
                if predicted_category == "ΠΛΗΡΟΦΟΡΙΕΣ":
                    extraction_result = extractor.extract_show_info(query)
                elif predicted_category == "ΚΡΑΤΗΣΗ":
                    extraction_result = extractor.extract_booking_info(query)
                elif predicted_category == "ΑΚΥΡΩΣΗ":
                    extraction_result = extractor.extract_cancellation_info(query)
                elif predicted_category == "ΠΡΟΣΦΟΡΕΣ":
                    extraction_result = extractor.extract_discount_info(query)
                elif predicted_category == "ΑΞΙΟΛΟΓΗΣΗ":
                    extraction_result = extractor.extract_review_info(query)
                
                if extraction_result:
                    print(f"   ✅ ΕΞΑΓΩΓΗ: Επιτυχής")
                    # Print key extracted values
                    if isinstance(extraction_result, dict):
                        for key, value in extraction_result.items():
                            if isinstance(value, dict) and 'value' in value:
                                val = value['value']
                                if val:  # Only show non-empty values
                                    print(f"      {key}: {val}")
                    successful_tests += 1
                else:
                    print(f"   ❌ ΕΞΑΓΩΓΗ: Αποτυχία")
                    
            except Exception as e:
                print(f"   ❌ ΣΦΑΛΜΑ: {str(e)}")
    
    print("\n" + "=" * 60)
    print(f"📊 ΑΠΟΤΕΛΕΣΜΑΤΑ:")
    print(f"   Συνολικά Tests: {total_tests}")
    print(f"   Επιτυχή: {successful_tests}")
    print(f"   Αποτυχίες: {total_tests - successful_tests}")
    print(f"   Ποσοστό Επιτυχίας: {(successful_tests/total_tests)*100:.1f}%")
    
    if successful_tests == total_tests:
        print("🎉 ΌΛΑ ΤΑ TESTS ΠΕΤΥΧΑΝ! Το σύστημα είναι έτοιμο!")
    else:
        print("⚠️  Κάποια tests απέτυχαν. Ελέγξτε τα αποτελέσματα.")

if __name__ == "__main__":
    test_queries()
