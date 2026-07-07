#!/usr/bin/env bash
set -euo pipefail

# run_app_10times.sh
# Simple script to run the application's main class (main.App) 10 times using Gradle.
# Run from the repository root: ./run_app_10times.sh

for i in $(seq 1 10); do
  echo "=== Run $i/10 - $(date +%Y-%m-%d_%H:%M:%S) ==="
  ./gradlew run --no-daemon --console=plain
  echo "=== End Run $i/10 ===\n"
done
echo "All 10 runs completed."