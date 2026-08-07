#!/bin/bash

# Exit on any error
set -e

COMMAND=$1

if [ "$COMMAND" = "run" ]; then
    echo "Running Starsector Ship Editor from source..."
    # Execute the application via Maven with 4GB heap
    MAVEN_OPTS="-Xmx4g -Dorg.lwjgl.opengl.contextAPI=native" mvn compile exec:java -Dexec.mainClass="shipeditor.Main"
elif [ "$COMMAND" = "test" ]; then
    echo "Running tests..."
    mvn clean test
elif [ "$COMMAND" = "clean" ]; then
    echo "Cleaning project..."
    mvn clean
else
    echo "Building Starsector Ship Editor..."
    # Build the shaded fat jar, skipping tests for speed
    mvn clean package -DskipTests
    echo ""
    echo "Build successful! The executable jar is automatically placed in the project root as ship_editor.jar."
    echo "To run the compiled application, use: ./ship_editor.sh or java -jar ship_editor.jar"
fi
