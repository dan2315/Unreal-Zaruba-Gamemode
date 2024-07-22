@echo off
REM Call gradlew build
call ./gradlew clean build

REM Check if build was successful
if %errorlevel% neq 0 (
    color 4F
    echo Build failed. Exiting.
    exit /b %errorlevel%
)

REM Define source and destination paths
set "SOURCE_PATH=.\build\libs\unrealzaruba-0.1.jar"
set "DEST_PATH=D:\multiMC\instances\runClient\.minecraft\mods"

REM Check if the source file exists
if not exist "%SOURCE_PATH%" (
    color 4F
    echo Source file not found: %SOURCE_PATH%
    exit /b 1
)

REM Move the JAR file to the Minecraft mods folder
move /Y "%SOURCE_PATH%" "%DEST_PATH%"

REM Check if move was successful
if %errorlevel% neq 0 (
    color 4F
    echo Failed to move the file.
    exit /b %errorlevel%
)

echo Build and deploy completed successfully.
