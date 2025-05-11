# UFO Sighting Data Processing Tools

This directory contains tools for processing the UFO sighting dataset for use in the app.

## Scripts

### csv_to_json.py

Converts the raw UFO sightings CSV dataset to the JSON format used by the app.

#### Usage

```bash
python csv_to_json.py --input scrubbed.csv --output app/src/main/assets/sightings.json [--limit 1000]