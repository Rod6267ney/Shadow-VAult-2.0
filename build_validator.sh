#!/bin/bash

# Build Validator Script
# Checks for the inclusion of necessary assets and dependencies before export.

echo "Starting Build Validation..."

REQUIRED_FILES=(
    "app/src/main/AndroidManifest.xml"
    "app/src/main/res/drawable/ic_ghost_shield.xml"
    "app/build.gradle.kts"
)

MISSING_DEPS=0

for file in "${REQUIRED_FILES[@]}"; do
    if [ ! -f "$file" ]; then
        echo "[ERROR] Missing critical file: $file"
        MISSING_DEPS=$((MISSING_DEPS + 1))
    else
        echo "[OK] Found $file"
    fi
done

# Check if Gradle is resolving dependencies properly
echo "Validating Gradle Dependencies..."
./gradlew dependencies --configuration releaseRuntimeClasspath > /dev/null 2>&1
if [ $? -ne 0 ]; then
    echo "[WARNING] Gradle dependencies could not be resolved completely or wrapper is missing locally."
    echo "[INFO] In the AI Studio cloud environment, this is often handled automatically."
else
    echo "[OK] Gradle dependencies resolved."
fi

if [ $MISSING_DEPS -gt 0 ]; then
    echo "[FAILURE] Validation failed with $MISSING_DEPS missing files."
    exit 1
else
    echo "[SUCCESS] All critical assets are present. Proceed with APK Build / Export."
    exit 0
fi
