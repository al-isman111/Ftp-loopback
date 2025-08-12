#!/bin/bash
set -e

# Install Gradle (if not already installed)
if ! command -v gradle &> /dev/null; then
    sdk install gradle
fi

# Accept Android licenses
yes | sdkmanager --licenses

# Create local.properties for Android Studio compatibility
if [ ! -f local.properties ]; then
    ANDROID_HOME=${ANDROID_HOME:-/opt/android-sdk}
    echo "sdk.dir=$ANDROID_HOME" > local.properties
fi