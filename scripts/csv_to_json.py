#!/usr/bin/env python3
"""
csv_to_json.py - Convert UFO sightings CSV to JSON format for the UFO Sighting Map app
"""

import csv
import json
import argparse
import sys
import os
from datetime import datetime

def convert_csv_to_json(input_file, output_file, limit=None):
    """
    Convert UFO sightings CSV to JSON format
    """
    try:
        sightings = []
        with open(input_file, 'r', encoding='utf-8') as csv_file:
            # Read the first line to inspect column headers
            first_line = csv_file.readline().strip()
            csv_file.seek(0)  # Go back to beginning

            # Print column headers for debugging
            columns = first_line.split(',')
            print("CSV columns:", columns)

            # Process all rows
            csv_reader = csv.DictReader(csv_file)

            # Print the first row to debug column access
            for i, row in enumerate(csv_reader):
                if i == 0:
                    print("First row:", row)
                    break
            csv_file.seek(0)  # Go back to beginning

            # Now process all rows - using DictReader for cleaner column access
            csv_reader = csv.DictReader(csv_file)
            for i, row in enumerate(csv_reader):
                if limit is not None and i >= limit:
                    break

                # Skip rows with missing critical data
                if not row['datetime'] or not row['city']:
                    continue

                # Get lat/long using column name
                try:
                    lat = float(row['latitude']) if row['latitude'] else 0.0
                    # Notice the space after 'longitude' in your CSV headers - account for this
                    lng_key = 'longitude ' if 'longitude ' in row else 'longitude'
                    lng = float(row[lng_key]) if row[lng_key] else 0.0
                except (ValueError, KeyError) as e:
                    print(f"Error parsing coordinates in row {i}: {e}")
                    lat, lng = 0.0, 0.0

                # Create sighting from row
                sighting = {
                    "dateTime": row['datetime'],
                    "city": row['city'],
                    "state": row['state'],
                    "country": row['country'] or "Unknown",
                    "shape": row['shape'] or "Unknown",
                    "duration": row['duration (hours/min)'] or "Unknown",
                    "summary": row['comments'] or "",
                    "posted": row['date posted'] or datetime.now().strftime("%Y-%m-%d"),
                    "latitude": lat,
                    "longitude": lng
                }
                sightings.append(sighting)

                # Print progress for large files
                if i % 10000 == 0:
                    print(f"Processed {i} rows...")

        # Create output directory if it doesn't exist
        os.makedirs(os.path.dirname(os.path.abspath(output_file)), exist_ok=True)

        # Write JSON output
        with open(output_file, 'w', encoding='utf-8') as json_file:
            json.dump(sightings, json_file)

        return len(sightings)

    except Exception as e:
        print(f"Error: {str(e)}")
        import traceback
        traceback.print_exc()
        sys.exit(1)

def main():
    parser = argparse.ArgumentParser(description="Convert UFO sightings CSV to JSON format")
    parser.add_argument("--input", default="scrubbed.csv", help="Input CSV file")
    parser.add_argument("--output", default="app/src/main/assets/sightings.json", help="Output JSON file")
    parser.add_argument("--limit", type=int, help="Limit the number of records")
    args = parser.parse_args()

    print(f"Converting {args.input} to {args.output}...")
    count = convert_csv_to_json(args.input, args.output, args.limit)
    print(f"Successfully converted {count} records to JSON format")
    print(f"Output file saved to: {os.path.abspath(args.output)}")

if __name__ == "__main__":
    main()