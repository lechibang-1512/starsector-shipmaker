@echo off
setlocal enabledelayedexpansion
if exist "%~dp0ship_editor.jar" (
    cd /d "%~dp0"
) else if exist "%~dp0..\..\ship_editor.jar" (
    cd /d "%~dp0..\.."
) else (
    cd /d "%~dp0"
)

set JVM_OPTS=-Xmx4g -XX:+UseG1GC -XX:+UseStringDeduplication -XX:MinHeapFreeRatio=10 -XX:MaxHeapFreeRatio=20

if "%~1"=="--cli" (
    if exist "jre\bin\java.exe" (
        "jre\bin\java.exe" %JVM_OPTS% -cp ship_editor.jar shipeditor.CliMain %2 %3 %4 %5 %6 %7 %8 %9
    ) else (
        java %JVM_OPTS% -cp ship_editor.jar shipeditor.CliMain %2 %3 %4 %5 %6 %7 %8 %9
    )
    exit /b !errorlevel!
)

if exist "jre\bin\java.exe" (
    echo Launching with local JRE...
    "jre\bin\java.exe" %JVM_OPTS% -jar ship_editor.jar
) else (
    echo Local JRE not found. Launching with system Java...
    java %JVM_OPTS% -jar ship_editor.jar
)

if %errorlevel% neq 0 (
    echo Application exited with error code %errorlevel%
    pause
)
