#!/bin/bash
DIRNAME=$(dirname "$0")
if [ -f "$DIRNAME/ship_editor.jar" ]; then
    cd "$DIRNAME"
elif [ -f "$DIRNAME/../../ship_editor.jar" ]; then
    cd "$DIRNAME/../.."
else
    cd "$DIRNAME"
fi

JVM_OPTS="-Xmx512m -XX:+UseG1GC -XX:+UseStringDeduplication -XX:MinHeapFreeRatio=10 -XX:MaxHeapFreeRatio=20"

if [ "$1" = "--cli" ]; then
    shift
    if [ -f "jre/bin/java" ]; then
        jre/bin/java $JVM_OPTS -cp ship_editor.jar shipeditor.CliMain "$@"
    else
        java $JVM_OPTS -cp ship_editor.jar shipeditor.CliMain "$@"
    fi
    exit $?
fi

if [ -f "jre/bin/java" ]; then
    jre/bin/java $JVM_OPTS -jar ./ship_editor.jar
else
    java $JVM_OPTS -jar ./ship_editor.jar
fi