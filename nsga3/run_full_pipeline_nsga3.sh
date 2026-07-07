#!/bin/bash
# Atalho conveniente para o pipeline completo do NSGA-III.
# Equivale a: bash pipeline/pipeline.sh "$@"
exec bash "$(dirname "$0")/pipeline/pipeline.sh" "$@"
