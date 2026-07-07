#!/bin/bash
# Atalho conveniente para o pipeline completo do AEMMT.
# Equivale a: bash pipeline/pipeline.sh "$@"
exec bash "$(dirname "$0")/pipeline/pipeline.sh" "$@"
