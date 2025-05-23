# Simple Database System for Jupiter Theater Chatbot

This document describes the simple database system implemented for the Jupiter Theater chatbot application.

## Overview

The system loads JSON files from the assets folder when the app starts and provides functionality to query these JSON files based on conversation templates. It's designed to handle the `<results>` tag in message_2 fields of complete conversation nodes.

## Components

1. **SimpleDatabase**: A singleton class that loads and manages JSON data from asset files
   - Loads JSON files when the app starts
   - Maps conversation categories to table names
   - Provides query functionality based on MsgTemplate fields
   - Formats results as human-readable text

2. **MsgTemplate Extensions**: Added query support to all template classes
   - Added `getFieldValuesMap()` method to extract field values for queries
   - Added `hasQueryableFields()` method to check if a template has data for querying

3. **ChatbotNode**: Enhanced to process `<results>` tags
   - Modified `getMessage2()` to check for and process `<results>` tags
   - Uses the ChatbotManager to perform the database query

4. **ChatbotManager**: Extended to handle database queries
   - Added singleton instance access
   - Added `processResultsTag()` method to replace the tag with query results
   - Initializes the SimpleDatabase when the app starts

## How It Works

1. When the app starts, `SimpleDatabase` loads all JSON files into memory
2. When a conversation node with a `<results>` tag in the message_2 field is processed:
   - The `getMessage2()` method detects the tag and calls `ChatbotManager.processResultsTag()`
   - `ChatbotManager` uses the node's category to map to the appropriate table
   - The node's MsgTemplate provides the query criteria
   - `SimpleDatabase` performs the query and returns formatted results
   - The `<results>` tag is replaced with the formatted query results

## Tables and Categories

The system maps conversation categories to JSON tables as follows:

| Category                  | JSON Table          | File                  |
|---------------------------|---------------------|------------------------|
| ΠΛΗΡΟΦΟΡΙΕΣ               | shows               | sample_shows.json      |
| ΚΡΑΤΗΣΗ                   | bookings            | sample_bookings.json   |
| ΑΚΥΡΩΣΗ                   | bookings            | sample_bookings.json   |
| ΠΡΟΣΦΟΡΕΣ & ΕΚΠΤΩΣΕΙΣ     | discounts           | sample_discounts.json  |
| ΑΞΙΟΛΟΓΗΣΕΙΣ & ΣΧΟΛΙΑ     | reviews             | sample_reviews.json    |

## Example Usage

To use the database query functionality, include the `<results>` tag in the `message_2` field of a complete conversation node. For example:

```json
{
  "id": "show_info_complete",
  "type": "COMPLETE",
  "message_1": "Βρήκα τις ακόλουθες παραστάσεις που ταιριάζουν με τα κριτήριά σας.",
  "message_2": "ΑΠΟΤΕΛΕΣΜΑΤΑ ΑΝΑΖΗΤΗΣΗΣ:\n<results>",
  "category": "ΠΛΗΡΟΦΟΡΙΕΣ",
  "children": []
}
```

The `<results>` tag will be replaced with the results of the query based on the fields in the node's MsgTemplate.
