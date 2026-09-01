import os
import re

for root, _, files in os.walk("src/main/java"):
    for file in files:
        if file.endswith(".java"):
            filepath = os.path.join(root, file)
            with open(filepath, "r", encoding="utf-8") as f:
                content = f.read()
                
            # look for "else if" with equals or == 
            matches = re.finditer(r'else if\s*\(([^)]+)\)', content)
            for m in matches:
                condition = m.group(1)
                if ".equals(" in condition or "==" in condition:
                    print(f"{filepath}: {condition}")

