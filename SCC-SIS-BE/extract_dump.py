import json
import os

transcript_path = r'C:\Users\admin\.gemini\antigravity\brain\17fd55c5-d9fa-4284-8e54-848262763943\.system_generated\logs\transcript_full.jsonl'
output_path = r'C:\Users\admin\Downloads\duancodegym\duancodegym\duanweb\SCC-SIS-BE\database_dump.sql'

with open(transcript_path, 'r', encoding='utf-8') as f:
    lines = f.readlines()

# Find the last USER_INPUT that contains the mysqldump
dump_content = ""
for line in reversed(lines):
    try:
        data = json.loads(line)
        if data.get('type') == 'USER_INPUT':
            content = data.get('content', '')
            if 'mysqldump: [Warning]' in content or '-- MySQL dump' in content:
                # Remove the XML tags and get the inner content
                # The prompt might look like <USER_REQUEST>mysqldump...</USER_REQUEST>
                if '<USER_REQUEST>' in content:
                    content = content.split('<USER_REQUEST>')[1].split('</USER_REQUEST>')[0].strip()
                dump_content = content
                break
    except Exception as e:
        continue

if dump_content:
    with open(output_path, 'w', encoding='utf-8') as out:
        out.write(dump_content)
    print("Successfully extracted database dump.")
else:
    print("Could not find the database dump in transcript.")
