@echo off
setlocal enabledelayedexpansion

:: --- Config ---
set JAR_NAME=unrealzaruba-0.2.jar
set BUILD_DIR=.\build\libs
set JAR_PATH=%BUILD_DIR%\%JAR_NAME%
set LOCAL_MODS=X:\Games\mc_modpacks\Instances\Zaruba Reborn\mods
set REMOTE_KEY=C:\Users\danyl\OneDrive\Desktop\uf_connect\key
set REMOTE_USER=dod
set REMOTE_HOST=unrealfrontiers.xyz

:: Remote server destinations
set DEST1=~/unrealfrontiers/gameservers/create/servers/spawn/mods/
set DEST2=~/unrealfrontiers/gameservers/create/servers/create-1/mods/
set DEST3=~/unrealfrontiers/gameservers/create/servers/create-2/mods/
set DEST4=~/unrealfrontiers/gameservers/create/servers/zaruba-1/mods/

:: --- Build step ---
echo [BUILD] Running gradlew build...
call .\gradlew build
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Gradle build failed!
    exit /b 1
)

:: --- Local copy ---
echo [COPY] Copying to local mods folder: %LOCAL_MODS%
copy /Y "%JAR_PATH%" "%LOCAL_MODS%"
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Failed to copy to local mods folder!
) else (
    echo [OK] Local copy successful.
)

:: --- Remote copies ---
for %%D in ("%DEST1%" "%DEST2%" "%DEST3%" "%DEST4%") do (
    echo [SCP] Copying to %%D ...
    scp -P 228 -i "%REMOTE_KEY%" "%JAR_PATH%" %REMOTE_USER%@%REMOTE_HOST%:%%D
    if !ERRORLEVEL! neq 0 (
        echo [ERROR] Failed to copy to %%D
    ) else (
        echo [OK] Copied successfully to %%D
    )
)

echo [DONE] Deployment finished.
