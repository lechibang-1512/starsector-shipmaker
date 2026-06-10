#!/bin/bash
# Google Antigravity Skill Helper Script
# Automates clean recompiling, packaging, copying the Shade JAR, and running the Ship Editor.

echo "========================================="
echo "Starsector Ship Editor - Build & Run Script"
echo "========================================="

# 1. Clean compile and package
echo "[1/3] Compiling and packaging jar (skipping tests)..."
mvn clean package -DskipTests

if [ $? -eq 0 ]; then
    echo "✔ Build successful!"
else
    echo "❌ Build failed!"
    exit 1
fi

# 2. Copy the shaded JAR to the root directory
echo "[2/3] Updating root ship_editor.jar..."
if [ -f "target/ship_editor-0.0.1c.jar" ]; then
    cp target/ship_editor-0.0.1c.jar ship_editor.jar
    echo "✔ Root jar updated successfully."
else
    echo "❌ Could not locate build jar: target/ship_editor-0.0.1c.jar"
    exit 1
fi

# 3. Launching
echo "[3/3] Launching JVM with 1GB memory limit..."
java -Xmx1g -XX:+UseG1GC -XX:+UseStringDeduplication -XX:MinHeapFreeRatio=10 -XX:MaxHeapFreeRatio=20 -jar ./ship_editor.jar

echo "Application terminated."
