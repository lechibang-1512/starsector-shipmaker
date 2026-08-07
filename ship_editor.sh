#!/bin/bash

DIRNAME=$(dirname "$0")
if [ -f "$DIRNAME/ship_editor.jar" ]; then
    cd "$DIRNAME"
elif [ -f "$DIRNAME/../../ship_editor.jar" ]; then
    cd "$DIRNAME/../.."
else
    cd "$DIRNAME"
fi

JVM_OPTS="-Xmx4g -XX:+UseG1GC -XX:+UseStringDeduplication -XX:MinHeapFreeRatio=10 -XX:MaxHeapFreeRatio=20 -Dsun.awt.noerasebackground=true -Dsun.java2d.noddraw=true"

JAVA_CMD="java"
for jre_path in "jre/bin/java" "../jre/bin/java" "../../jre/bin/java" "../jre_linux/bin/java" "../../jre_linux/bin/java" "../jre_mac/bin/java" "../../jre_mac/bin/java"; do
    if [ -f "$jre_path" ]; then
        JAVA_CMD="$jre_path"
        echo "Found JRE: $jre_path"
        break
    fi
done

if [ "$JAVA_CMD" = "java" ]; then
    echo "Local JRE not found. Launching with system Java..."
fi

$JAVA_CMD $JVM_OPTS -jar ./ship_editor.jar