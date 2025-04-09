import os
import json
import sys

def load_shows():
    """Load all shows from the shows.json file"""
    shows_path = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), 
                             'shows', 'shows.json')
    try:
        with open(shows_path, 'r', encoding='utf-8') as f:
            shows = json.load(f)
        return shows
    except Exception as e:
        print(f"Error loading shows: {e}")
        return []

def load_extracted_info():
    """Load extracted information from categorizer"""
    extracted_info_path = os.path.join(os.path.dirname(os.path.abspath(__file__)), 
                                     'extracted_info.json')
    try:
        if os.path.exists(extracted_info_path):
            with open(extracted_info_path, 'r', encoding='utf-8') as f:
                info = json.load(f)
            return info
        else:
            return {}
    except Exception as e:
        print(f"Error loading extracted info: {e}")
        return {}

def filter_shows(shows, criteria):
    """Filter shows based on extracted criteria"""
    if not criteria:
        return shows  # Return all shows if no criteria
    
    filtered_shows = []
    
    for show in shows:
        match = True
        
        # Check each criterion
        for key, value in criteria.items():
            if key == 'cast':
                # For cast, check if any mentioned cast member is in the show's cast
                if not any(member in show['cast'] for member in value):
                    match = False
                    break
            elif key == 'stars':
                # For stars, compare as float
                if float(show['stars']) < float(value):
                    match = False
                    break
            elif key in show and str(show[key]).lower() != str(value).lower():
                # For other fields, do case-insensitive comparison
                match = False
                break
        
        if match:
            filtered_shows.append(show)
    
    return filtered_shows

def display_shows(shows):
    """Display shows in a formatted way"""
    if not shows:
        print("\nNo shows found matching your criteria.")
        return
    
    print(f"\nFound {len(shows)} show(s):")
    print("=" * 60)
    
    for i, show in enumerate(shows, 1):
        print(f"Show #{i}: {show['name']}")
        print(f"  Day/Time: {show['day']} at {show['time']}")
        print(f"  Genre: {show['topic']}")
        print(f"  Duration: {show['duration']} minutes")
        print(f"  Room: {show['room']}")
        print(f"  Rating: {show['stars']} ★")
        print(f"  Cast: {', '.join(show['cast'])}")
        print("-" * 60)

def main():
    """Main function to run the booking system"""
    print("\nJupiter Theater Booking System")
    print("=" * 30)
    
    # Load all shows
    shows = load_shows()
    if not shows:
        print("No shows available. Please try again later.")
        return
    
    # Load extracted information (if any)
    extracted_info = load_extracted_info()
    
    # Filter shows based on extracted information
    filtered_shows = filter_shows(shows, extracted_info)
    
    # Display the filtered shows
    display_shows(filtered_shows)
    
    if extracted_info:
        print("\nFiltering based on:")
        for key, value in extracted_info.items():
            print(f"- {key}: {value}")

if __name__ == "__main__":
    main()